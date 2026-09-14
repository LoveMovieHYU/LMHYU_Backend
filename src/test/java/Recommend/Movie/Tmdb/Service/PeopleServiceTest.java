package Recommend.Movie.Tmdb.Service;

import Recommend.Movie.Tmdb.Domain.People;
import Recommend.Movie.Tmdb.Dto.CreditsResponse;
import Recommend.Movie.Tmdb.Repository.PeopleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PeopleServiceTest {

    @InjectMocks
    private PeopleService peopleService;

    @Mock
    private RestTemplate restTemplate;
    @Mock
    private PeopleRepository peopleRepository;

    @Test
    @DisplayName("크레딧 조회 시 404(NotFound)면 null 을 반환한다")
    void fetchCreditsOnly_notFoundReturnsNull() {
        when(restTemplate.getForObject(anyString(), eq(CreditsResponse.class)))
                .thenThrow(HttpClientErrorException.create(
                        HttpStatus.NOT_FOUND, "Not Found", HttpHeaders.EMPTY, null, null));

        CreditsResponse result = peopleService.fetchCreditsOnly(671L);

        assertThat(result).isNull();
        verify(restTemplate, times(1)).getForObject(anyString(), eq(CreditsResponse.class));
    }

    @Test
    @DisplayName("크레딧 조회 시 429(TooManyRequests)면 대기 후 한 번 더 재시도하고 성공값을 반환한다")
    void fetchCreditsOnly_tooManyRequestsRetriesAndSucceeds() {
        CreditsResponse credits = new CreditsResponse();
        when(restTemplate.getForObject(anyString(), eq(CreditsResponse.class)))
                .thenThrow(HttpClientErrorException.create(
                        HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", HttpHeaders.EMPTY, null, null))
                .thenReturn(credits);

        CreditsResponse result = peopleService.fetchCreditsOnly(671L);

        assertThat(result).isSameAs(credits);
        verify(restTemplate, times(2)).getForObject(anyString(), eq(CreditsResponse.class));
    }

    @Test
    @DisplayName("크레딧 조회 시 예상치 못한 오류(5xx/네트워크 등)면 영화 유실 방지를 위해 null 을 반환한다")
    void fetchCreditsOnly_unexpectedErrorReturnsNull() {
        when(restTemplate.getForObject(anyString(), eq(CreditsResponse.class)))
                .thenThrow(new RuntimeException("connection reset"));

        CreditsResponse result = peopleService.fetchCreditsOnly(671L);

        assertThat(result).isNull();
        verify(restTemplate, times(1)).getForObject(anyString(), eq(CreditsResponse.class));
    }

    @Test
    @DisplayName("biography 와 birthDay 가 모두 채워진 인물만 적재됨으로 판정한다")
    void isPersonDetailStored_requiresBiographyAndBirthDay() {
        People stored = People.builder()
                .tmdbId(777L)
                .biography("소개")
                .birthDay(LocalDate.of(1990, 1, 1))
                .build();
        when(peopleRepository.findByTmdbId(777)).thenReturn(stored);

        assertThat(peopleService.isPersonDetailStored(777)).isTrue();
    }

    @Test
    @DisplayName("birthDay 가 비어 있으면 아직 적재되지 않은 것으로 판정한다")
    void isPersonDetailStored_missingBirthDayIsNotStored() {
        People stored = People.builder()
                .tmdbId(777L)
                .biography("소개")
                .build();
        when(peopleRepository.findByTmdbId(777)).thenReturn(stored);

        assertThat(peopleService.isPersonDetailStored(777)).isFalse();
    }
}
