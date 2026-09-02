package Recommend.Movie.Config.Batch;

import Recommend.Movie.Tmdb.Dto.DiscoverPageResult;
import Recommend.Movie.Tmdb.Dto.WorkItem;
import Recommend.Movie.Tmdb.Service.TmdbService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TmdbDiscoverItemReaderTest {

    @Mock
    private TmdbService tmdbService;

    @Test
    @DisplayName("필터링으로 빈 페이지(items=0, total_pages 미도달)는 연도를 넘기지 않고 같은 연도 다음 페이지를 읽는다")
    void read_emptyFilteredPage_doesNotSkipYear() throws Exception {
        // given: 2020년만 대상. 1페이지는 필터링되어 비었지만 total_pages=3 이라 아직 끝이 아님
        TmdbDiscoverItemReader reader = new TmdbDiscoverItemReader(tmdbService, 2020, 2020, false);

        when(tmdbService.fetchDiscoverPage(2020, 1, false))
                .thenReturn(new DiscoverPageResult(List.of(), 3));
        when(tmdbService.fetchDiscoverPage(2020, 2, false))
                .thenReturn(new DiscoverPageResult(List.of(new WorkItem(111)), 3));

        // when
        WorkItem item = reader.read();

        // then: 빈 페이지에서 연도를 넘기지 않고 2페이지의 신규 영화를 반환
        assertThat(item).isNotNull();
        assertThat(item.getMovieId()).isEqualTo(111);
        verify(tmdbService, times(1)).fetchDiscoverPage(2020, 1, false);
        verify(tmdbService, times(1)).fetchDiscoverPage(2020, 2, false);
        // 다음 연도로 넘어가지 않았어야 함
        verify(tmdbService, never()).fetchDiscoverPage(2021, 1, false);
    }

    @Test
    @DisplayName("total_pages 에 도달한 빈 페이지는 실제 끝으로 보고 다음 연도로 넘어간다")
    void read_reachedLastPage_movesToNextYear() throws Exception {
        // given: 2020~2021. 2020년은 1페이지(total_pages=1)에서 끝, 2021년 1페이지에 신규 영화 존재
        TmdbDiscoverItemReader reader = new TmdbDiscoverItemReader(tmdbService, 2020, 2021, false);

        when(tmdbService.fetchDiscoverPage(2020, 1, false))
                .thenReturn(new DiscoverPageResult(List.of(), 1));
        when(tmdbService.fetchDiscoverPage(2021, 1, false))
                .thenReturn(new DiscoverPageResult(List.of(new WorkItem(222)), 1));

        // when
        WorkItem item = reader.read();

        // then: 2020년은 끝났으므로 2021년으로 넘어가 신규 영화 반환
        assertThat(item).isNotNull();
        assertThat(item.getMovieId()).isEqualTo(222);
        verify(tmdbService, times(1)).fetchDiscoverPage(2020, 1, false);
        verify(tmdbService, times(1)).fetchDiscoverPage(2021, 1, false);
    }
}
