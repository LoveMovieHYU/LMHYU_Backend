package Recommend.Movie.Tmdb.Service;

import Recommend.Movie.Tmdb.Domain.Job;
import Recommend.Movie.Tmdb.Domain.Movie;
import Recommend.Movie.Tmdb.Domain.People;
import Recommend.Movie.Tmdb.Dto.CreditsPeople;
import Recommend.Movie.Tmdb.Dto.CreditsResponse;
import Recommend.Movie.Tmdb.Repository.MoviePeopleRepository;
import Recommend.Movie.Tmdb.Repository.PeopleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
    @Mock
    private MoviePeopleRepository moviePeopleRepository;

    @Test
    @DisplayName("영화-인물 중복 판정은 TMDB id 가 아니라 저장된 People 의 내부 PK(people.getId()) 로 수행한다")
    void upsertPersonAndLink_usesPeopleInternalPkForDuplicateCheck() {
        // given
        Movie movie = Movie.builder().id(10).tmdbId(671L).title("테스트 영화").build();

        int tmdbPeopleId = 777;
        int peopleInternalPk = 42;

        CreditsPeople cast = new CreditsPeople();
        cast.setId(tmdbPeopleId);
        cast.setName("배우A");
        cast.setGender(2);
        cast.setOrder(0);

        CreditsResponse credits = new CreditsResponse();
        credits.setCast(List.of(cast));

        // 이미 상세정보(biography)가 있는 기존 인물 → 내부 PK 42
        People existing = People.builder()
                .id(peopleInternalPk)
                .tmdbId((long) tmdbPeopleId)
                .job(Job.ACTOR)
                .biography("소개")
                .build();

        when(restTemplate.getForObject(anyString(), eq(CreditsResponse.class))).thenReturn(credits);
        when(peopleRepository.findByTmdbId(tmdbPeopleId)).thenReturn(existing);
        when(moviePeopleRepository.existsByMovie_IdAndPeople_Id(movie.getId(), peopleInternalPk)).thenReturn(true);

        // when (fetchPersonDetail=false 로 외부 상세 조회는 스킵)
        peopleService.fetchAndSaveCreditsByMovieId(movie, false);

        // then : 내부 PK(42) 로 중복 판정, TMDB id(777) 로는 호출되지 않아야 함
        verify(moviePeopleRepository, times(1)).existsByMovie_IdAndPeople_Id(movie.getId(), peopleInternalPk);
        verify(moviePeopleRepository, never()).existsByMovie_IdAndPeople_Id(movie.getId(), tmdbPeopleId);
        // 이미 연결되어 있으므로 새 링크 저장은 발생하지 않음
        verify(moviePeopleRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
