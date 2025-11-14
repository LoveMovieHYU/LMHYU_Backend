package Recommend.Movie.Service;

import Recommend.Movie.DTO.TmdbDTO.CreditsPeople;
import Recommend.Movie.DTO.TmdbDTO.CreditsResponse;
import Recommend.Movie.DTO.TmdbDTO.PeopleDetailDTO;
import Recommend.Movie.Domain.Job;
import Recommend.Movie.Domain.Movie;
import Recommend.Movie.Domain.MoviePeople;
import Recommend.Movie.Domain.People;
import Recommend.Movie.Repository.MoviePeopleRepository;
import Recommend.Movie.Repository.PeopleRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;

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
        log.info("[PeopleBatch] START fetch credits. movieId={}, tmdbId={}", movie.getId(), tmdbId);

        String creditsUrl = UriComponentsBuilder.fromHttpUrl(baseUrl + "/movie/" + tmdbId + "/credits")
                .queryParam("api_key", apikey)
                .queryParam("language", "ko-KR")
                .toUriString();

        CreditsResponse credits;
        try{
            credits = restTemplate.getForObject(creditsUrl, CreditsResponse.class);
        } catch (HttpClientErrorException.NotFound nf) {
            log.error("not found credits for movie with tmdbId: " + tmdbId);
            return;
        }
        if (credits == null) {
            log.error("Failed to fetch credits for movie with tmdbId: " + tmdbId);
            return;
        }

        if (credits.getCast() != null) {
            for (CreditsPeople creditsPeople : credits.getCast()) {
                upsertPersonAndLink(movie, creditsPeople, "ACTOR", fetchPersonDetail);
            }
        }

        if (credits.getCrew() != null) {
            for (CreditsPeople creditsPeople : credits.getCrew()) {
                upsertPersonAndLink(movie, creditsPeople, "PRODUCER", fetchPersonDetail);
            }
        }
        log.info("[PeopleBatch] DONE fetch credits. movieId={}, tmdbId={}", movie.getId(), tmdbId);

    }

    private void upsertPersonAndLink(Movie movie, CreditsPeople creditsPeople,
                                     String jobKor, boolean fetchDetail) {
        if(creditsPeople == null) return;
        int tmdbPeopleId = creditsPeople.getId();
        People people = peopleRepository.findByTmdbId(tmdbPeopleId);
        if (people == null) {
            people = new People();
            people.setTmdbId((long) tmdbPeopleId);
            people.markNew();
        }

        if(creditsPeople.getName() != null){
            people.setName(creditsPeople.getName());
        }
        if(creditsPeople.getGender() != null){
            people.setGender(creditsPeople.getGender());
        }
        if(creditsPeople.getProfilePath() != null){
            people.setProfileImagePath(creditsPeople.getProfilePath());
        }

        if(people.getJob() == null){
           people.setJob(Job.valueOf(jobKor));
        }

        if (fetchDetail && (isNullOrBlank(people.getBiography()) || isNullOrBlank(String.valueOf(people.getBirthDay())))) {
            fillPersonDetail(tmdbPeopleId, people);
        }
        peopleRepository.save(people);
        log.debug("Updated person: " + people.getName() + " (tmdbId: " + tmdbPeopleId + ")");
        if (!moviePeopleRepository.existsByMovie_IdAndPeople_Id(movie.getId(), tmdbPeopleId)) {
            MoviePeople moviePeople = new MoviePeople();
            moviePeople.setMovie(movie);
            moviePeople.setPeople(people);
            moviePeopleRepository.save(moviePeople);
        }
        log.debug("Linked person " + people.getName() + " to movie " + movie.getTitle());
    }

    private void fillPersonDetail(int peopleId, People people){
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/person/" + peopleId)
                .queryParam("api_key", apikey)
                .queryParam("language", "ko-KR")
                .toUriString();
        try {
            PeopleDetailDTO detail = restTemplate.getForObject(url, PeopleDetailDTO.class);
            if (detail != null) {
                if (!isNullOrBlank(detail.getBiography())) people.setBiography(detail.getBiography());
                if (!isNullOrBlank(detail.getBirthday())) people.setBirthDay(LocalDate.parse(detail.getBirthday()));
                if (!isNullOrBlank(detail.getProfile_path()) && isNullOrBlank(people.getProfileImagePath())) {
                    people.setProfileImagePath(detail.getProfile_path());
                }
            }
        } catch (HttpClientErrorException.NotFound nf) {
            // ignore
        } catch (Exception e) {
            log.warn("person 상세 조회 실패 personId={}", peopleId, e);
        }
    }
    private boolean isNullOrBlank(String s) {
        return s == null || s.isBlank();
    }
}
