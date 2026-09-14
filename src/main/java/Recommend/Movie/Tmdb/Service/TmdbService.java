package Recommend.Movie.Tmdb.Service;

import Recommend.Movie.Tmdb.Dto.*;
import Recommend.Movie.Tmdb.Repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class TmdbService {
    private final RestTemplate restTemplate;
    private final MovieRepository movieRepository;


    @Value("${tmdb.api.key}")
    private String apikey;

    @Value("${tmdb.api.base-url}")
    private String baseUrl;

    /**
     * 특정 연도(year)와 페이지(page)에 해당하는 데이터만 API로 가져와서 반환
     * ItemReader에서 이 메서드를 반복 호출하게 됩니다.
     */
    public DiscoverPageResult fetchDiscoverPage(int year, int page, boolean includeAdult) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/discover/movie")
                .queryParam("api_key", apikey)
                .queryParam("language", "ko-KR")
                .queryParam("sort_by", "popularity.desc")
                .queryParam("include_adult", includeAdult)
                .queryParam("include_video", false)
                .queryParam("primary_release_year", year)
                .queryParam("popularity.gte", 1.0)
                .queryParam("page", page)
                .toUriString();

        try {
            ResponseEntity<DiscoverResponse> resp = restTemplate.getForEntity(url, DiscoverResponse.class);
            DiscoverResponse body = resp.getBody();

            if (body == null || body.results == null || body.results.isEmpty()) {
                return DiscoverPageResult.empty(); // 데이터 없음
            }

            int totalPages = body.total_pages == null ? 0 : body.total_pages;

            List<WorkItem> items = new ArrayList<>();
            for (DiscoverMovieSummary summary : body.results) {
                if (summary == null) continue;

                // 이미 DB에 있는지 확인 (필터링되어 items 가 비어도 페이지 자체는 끝이 아님)
                if (movieRepository.findByTmdbId((long) summary.getId()).isPresent()) {
                    log.debug("Already Data : {}", summary.getTitle());
                    continue;
                }
                items.add(new WorkItem(summary.getId()));
            }

            sleepSilently(Duration.ofMillis(100));

            return new DiscoverPageResult(items, totalPages);

        } catch (HttpClientErrorException e) {
            log.error("TMDB API Error for year={}, page={}: {}", year, page, e.getMessage());
            if (e.getStatusCode().value() == 429) {
                log.warn("Rate limit exceeded. Sleeping for 2 seconds...");
                sleepSilently(Duration.ofSeconds(2));
            }
            // 정상 종료(끝 페이지)가 아니라 일시적 오류이므로 실패 신호를 반환한다.
            // 리더는 이 신호를 받아 연도를 넘기지 않고 같은 페이지를 재시도한다.
            return DiscoverPageResult.failure();
        } catch (Exception e) {
            log.error("Unexpected error for year={}, page={}", year, page, e);
            return DiscoverPageResult.failure();
        }
    }

    /**
     * TMDB에서 특정 movieId의 상세 정보를 가져오기만 하는 메서드 (DB 저장 없음)
     */
    public MovieDetailDTO fetchMovieDetailOnly(int movieId) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/movie/" + movieId)
                .queryParam("api_key", apikey)
                .queryParam("language", "ko-KR")
                .toUriString();

        MovieDetailDTO detailDTO = null;
        try {
            detailDTO = restTemplate.getForObject(url, MovieDetailDTO.class);
        } catch (HttpClientErrorException.NotFound nf) {
            // 404면 그냥 스킵
            log.debug("TMDB 404 NotFound movieId={}", movieId);
            return null;
        } catch (HttpClientErrorException.TooManyRequests tmr) {
            // 429면 잠깐 쉬었다가 한 번 더 시도
            log.warn("TMDB 429 TooManyRequests movieId={}, retry after 2s", movieId);
            sleepSilently(Duration.ofSeconds(2));
            try {
                detailDTO = restTemplate.getForObject(url, MovieDetailDTO.class);
            } catch (Exception e2) {
                log.error("Retry after 429 failed. movieId={}", movieId, e2);
                return null;
            }
        }

        if (detailDTO == null) {
            log.warn("MovieDetailDTO is null. movieId={}", movieId);
            return null;
        }

        return detailDTO;
    }

    private void sleepSilently(Duration d) {
        try {
            Thread.sleep(d.toMillis());
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

}
