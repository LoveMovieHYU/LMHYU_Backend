package Recommend.Movie.Tmdb.Service;

import Recommend.Movie.Tmdb.Dto.CreditsPeople;
import Recommend.Movie.Tmdb.Dto.CreditsResponse;
import Recommend.Movie.Tmdb.Dto.PeopleDetailDTO;
import Recommend.Movie.Tmdb.Domain.Job;
import Recommend.Movie.Tmdb.Domain.Movie;
import Recommend.Movie.Tmdb.Domain.MoviePeople;
import Recommend.Movie.Tmdb.Domain.People;
import Recommend.Movie.Tmdb.Repository.MoviePeopleRepository;
import Recommend.Movie.Tmdb.Repository.PeopleRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.util.Comparator;

@Service
@Slf4j
public class PeopleService {

    private final RestTemplate restTemplate;
    private final PeopleRepository peopleRepository;
    private final MoviePeopleRepository moviePeopleRepository;


    public PeopleService(RestTemplate restTemplate, PeopleRepository peopleRepository, MoviePeopleRepository moviePeopleRepository) {
        this.restTemplate = restTemplate;
        this.peopleRepository = peopleRepository;
        this.moviePeopleRepository = moviePeopleRepository;
    }

    @Value("${tmdb.api.key}")
    private String apikey;

    @Value("${tmdb.api.base-url}")
    private String baseUrl;

    @Transactional
    public void fetchAndSaveCreditsByMovieId(Movie movie, boolean fetchPersonDetail){
        Long tmdbId = movie.getTmdbId();
        if(tmdbId == null){
            log.error("Movie tmdbId is null, cannot fetch credits.");
            return;
        }

        CreditsResponse credits = fetchCreditsOnly(tmdbId);
        if (credits == null) {
            log.error("Failed to fetch credits for movie with tmdbId: " + tmdbId);
            return;
        }

        if (credits.getCast() != null) {
            credits.getCast().stream()
                    .sorted(Comparator.comparing(
                            cp -> cp.getOrder() == null ? Integer.MAX_VALUE : cp.getOrder()
                    ))
                    .limit(20) //  상위 20명만
                    .forEach(cp -> upsertPersonAndLink(movie, cp, "ACTOR", fetchPersonDetail));
        }

        if (credits.getCrew() != null) {
            for (CreditsPeople creditsPeople : credits.getCrew()) {
                if ("Director".equalsIgnoreCase(creditsPeople.getJob())) {
                    upsertPersonAndLink(movie, creditsPeople, "DIRECTOR", fetchPersonDetail);
                }
            }
        }
        log.info("[PeopleBatch] DONE fetch credits. movieId={}, tmdbId={}", movie.getId(), tmdbId);

    }

    public CreditsResponse fetchCreditsOnly(Long tmdbId) {
        String creditsUrl = UriComponentsBuilder.fromHttpUrl(baseUrl + "/movie/" + tmdbId + "/credits")
                .queryParam("api_key", apikey)
                .queryParam("language", "ko-KR")
                .toUriString();

        try {
            return restTemplate.getForObject(creditsUrl, CreditsResponse.class);
        } catch (HttpClientErrorException.NotFound nf) {
            log.error("not found credits for movie with tmdbId: {}", tmdbId);
            return null;
        }
    }

    public PeopleDetailDTO fetchPersonDetailOnly(int peopleId) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/person/" + peopleId)
                .queryParam("api_key", apikey)
                .queryParam("language", "ko-KR")
                .toUriString();

        try {
            return restTemplate.getForObject(url, PeopleDetailDTO.class);
        } catch (HttpClientErrorException.NotFound nf) {
            return null;
        } catch (Exception e) {
            log.warn("person detail fetch failed. personId={}", peopleId, e);
            return null;
        }
    }

    /**
     * 인물 상세 정보가 이미 DB 에 적재되어 있는지 여부.
     * 배치에서 불필요한 외부 상세 조회를 스킵하기 위한 사전 확인용.
     *
     * biography 만으로 판정하면 biography 는 있으나 birthDay 가 비어 있는 인물의 생일이
     * 영원히 백필되지 않으므로, 상세 조회로 채워지는 핵심 필드(biography + birthDay)가
     * 모두 채워졌을 때만 "적재됨" 으로 본다. (upsertPersonAndLink 의 재조회 조건과 일치)
     */
    public boolean isPersonDetailStored(int tmdbPeopleId) {
        People people = peopleRepository.findByTmdbId(tmdbPeopleId);
        return people != null
                && !isNullOrBlank(people.getBiography())
                && people.getBirthDay() != null;
    }

    private void upsertPersonAndLink(Movie movie, CreditsPeople creditsPeople,
                                     String jobKor, boolean fetchDetail) {
        if(creditsPeople == null) return;
        int tmdbPeopleId = creditsPeople.getId();
        People people = peopleRepository.findByTmdbId(tmdbPeopleId);
        if (people == null) {
            people = People.createNew(tmdbPeopleId);
        }

        people.updateBasicInfo(creditsPeople.getName(), creditsPeople.getGender(), creditsPeople.getProfilePath());
        // 이미 직업이 있으면 Job.valueOf 를 평가하지 않도록 Supplier 로 지연 전달한다.
        people.assignJobIfAbsent(() -> Job.valueOf(jobKor));

        if (fetchDetail && (isNullOrBlank(people.getBiography()) || people.getBirthDay() == null)) {
            fillPersonDetail(tmdbPeopleId, people);
        }
        peopleRepository.save(people);
        log.debug("Updated person: " + people.getName() + " (tmdbId: " + tmdbPeopleId + ")");
        // 저장 후의 내부 PK(people.getId()) 로 중복 판정해야 올바르게 동작한다.
        if (!moviePeopleRepository.existsByMovie_IdAndPeople_Id(movie.getId(), people.getId())) {
            moviePeopleRepository.save(MoviePeople.of(movie, people));
        }
        log.debug("Linked person " + people.getName() + " to movie " + movie.getTitle());
    }

    /**
     * TMDB person 상세를 조회해 People 엔티티에 반영한다.
     * (상세 조회 로직은 {@link #fetchPersonDetailOnly(int)} 로 일원화)
     */
    private void fillPersonDetail(int peopleId, People people){
        PeopleDetailDTO detail = fetchPersonDetailOnly(peopleId);
        if (detail == null) {
            return;
        }
        people.updateDetail(detail.getBiography(), parseBirthDay(detail.getBirthday()), detail.getProfile_path());
    }

    private LocalDate parseBirthDay(String birthday) {
        if (isNullOrBlank(birthday)) {
            return null;
        }
        try {
            return LocalDate.parse(birthday);
        } catch (Exception e) {
            log.debug("Invalid person birthday. birthday={}", birthday);
            return null;
        }
    }

    private boolean isNullOrBlank(String s) {
        return s == null || s.isBlank();
    }
}
