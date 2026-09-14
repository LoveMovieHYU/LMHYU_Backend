package Recommend.Movie.Tmdb.Service;

import Recommend.Movie.Tmdb.Dto.CreditsResponse;
import Recommend.Movie.Tmdb.Dto.PeopleDetailDTO;
import Recommend.Movie.Tmdb.Domain.People;
import Recommend.Movie.Tmdb.Repository.PeopleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;

@Service
@Slf4j
public class PeopleService {

    private final RestTemplate restTemplate;
    private final PeopleRepository peopleRepository;


    public PeopleService(RestTemplate restTemplate, PeopleRepository peopleRepository) {
        this.restTemplate = restTemplate;
        this.peopleRepository = peopleRepository;
    }

    @Value("${tmdb.api.key}")
    private String apikey;

    @Value("${tmdb.api.base-url}")
    private String baseUrl;

    /**
     * TMDB 크레딧(출연/제작진)을 조회하기만 하는 메서드 (DB 저장 없음)
     * <p>
     * 배치 처리 도중 일시적 오류로 영화 row 가 통째로 유실되지 않도록 리질리언시를 둔다.
     * ({@link TmdbService#fetchMovieDetailOnly(int)} 의 429 처리 방식과 일관되게 동작한다.)
     * - 404(NotFound): 크레딧이 없는 영화이므로 그대로 스킵(null 반환)
     * - 429(TooManyRequests): 짧게 대기 후 한 번 더 재시도
     * - 그 외 예외(5xx/네트워크 등): credits 없이 정상 진행하도록 log.warn 후 null 반환
     */
    public CreditsResponse fetchCreditsOnly(Long tmdbId) {
        String creditsUrl = UriComponentsBuilder.fromHttpUrl(baseUrl + "/movie/" + tmdbId + "/credits")
                .queryParam("api_key", apikey)
                .queryParam("language", "ko-KR")
                .toUriString();

        try {
            return restTemplate.getForObject(creditsUrl, CreditsResponse.class);
        } catch (HttpClientErrorException.NotFound nf) {
            // 404면 크레딧이 없는 영화이므로 스킵
            log.debug("TMDB 404 NotFound credits. tmdbId={}", tmdbId);
            return null;
        } catch (HttpClientErrorException.TooManyRequests tmr) {
            // 429면 잠깐 쉬었다가 한 번 더 시도
            log.warn("TMDB 429 TooManyRequests credits. tmdbId={}, retry after 2s", tmdbId);
            sleepSilently(Duration.ofSeconds(2));
            try {
                return restTemplate.getForObject(creditsUrl, CreditsResponse.class);
            } catch (Exception e2) {
                log.error("Retry after 429 failed. credits tmdbId={}", tmdbId, e2);
                return null;
            }
        } catch (Exception e) {
            // 5xx/네트워크 오류 등은 credits 없이 정상 진행 (영화 row 유실 방지)
            log.warn("credits fetch failed. tmdbId={}", tmdbId, e);
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
     * 모두 채워졌을 때만 "적재됨" 으로 본다.
     */
    public boolean isPersonDetailStored(int tmdbPeopleId) {
        People people = peopleRepository.findByTmdbId(tmdbPeopleId);
        return people != null
                && !isNullOrBlank(people.getBiography())
                && people.getBirthDay() != null;
    }

    private boolean isNullOrBlank(String s) {
        return s == null || s.isBlank();
    }

    private void sleepSilently(Duration d) {
        try {
            Thread.sleep(d.toMillis());
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
