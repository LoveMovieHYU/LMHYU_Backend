package Recommend.Movie.Config.Batch;

import Recommend.Movie.Tmdb.Dto.DiscoverPageResult;
import Recommend.Movie.Tmdb.Dto.WorkItem;
import Recommend.Movie.Tmdb.Service.TmdbService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.ExecutionContext;

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

    @Test
    @DisplayName("일시적 오류(failure) 시 연도를 넘기지 않고 같은 페이지를 재시도하며, 재시도 성공 시 신규 영화를 반환한다")
    void read_failureThenSuccess_retriesSamePage() throws Exception {
        // given: 2020년만 대상. 1페이지 첫 호출은 일시적 오류, 재시도 시 신규 영화 반환
        TmdbDiscoverItemReader reader = new TmdbDiscoverItemReader(tmdbService, 2020, 2020, false);

        when(tmdbService.fetchDiscoverPage(2020, 1, false))
                .thenReturn(DiscoverPageResult.failure())
                .thenReturn(new DiscoverPageResult(List.of(new WorkItem(111)), 3));

        // when
        WorkItem item = reader.read();

        // then: 같은 (연도, 페이지) 로 재시도(총 2회)한 뒤 신규 영화 반환
        assertThat(item).isNotNull();
        assertThat(item.getMovieId()).isEqualTo(111);
        verify(tmdbService, times(2)).fetchDiscoverPage(2020, 1, false);
        // 오류 때문에 연도나 페이지를 건너뛰지 않았어야 함
        verify(tmdbService, never()).fetchDiscoverPage(2020, 2, false);
        verify(tmdbService, never()).fetchDiscoverPage(2021, 1, false);
    }

    @Test
    @DisplayName("연속 오류가 재시도 상한(3회)을 초과하면 연도 전체를 스킵하지 않고 같은 연도의 다음 페이지로만 진행한다")
    void read_exceedRetryLimit_advancesPageNotYear() throws Exception {
        // given: 2020년만 대상. 1페이지는 계속 실패, 2페이지에 신규 영화 존재
        TmdbDiscoverItemReader reader = new TmdbDiscoverItemReader(tmdbService, 2020, 2020, false);

        when(tmdbService.fetchDiscoverPage(2020, 1, false))
                .thenReturn(DiscoverPageResult.failure());
        when(tmdbService.fetchDiscoverPage(2020, 2, false))
                .thenReturn(new DiscoverPageResult(List.of(new WorkItem(222)), 3));

        // when
        WorkItem item = reader.read();

        // then: 1페이지는 (초기 1회 + 재시도 3회) = 4회 시도 후 상한 초과로 2페이지로 진행
        assertThat(item).isNotNull();
        assertThat(item.getMovieId()).isEqualTo(222);
        verify(tmdbService, times(4)).fetchDiscoverPage(2020, 1, false);
        verify(tmdbService, times(1)).fetchDiscoverPage(2020, 2, false);
        // 연도는 유지되어야 함 (연도 전체 스킵 금지)
        verify(tmdbService, never()).fetchDiscoverPage(2021, 1, false);
    }

    @Test
    @DisplayName("정상 empty(totalPages<=0)는 재시도하지 않고 바로 다음 연도로 넘어간다 (failure 와 구분)")
    void read_normalEmpty_movesToNextYearWithoutRetry() throws Exception {
        // given: 2020~2021. 2020년 1페이지는 정상 empty(totalPages=0), 2021년 1페이지에 신규 영화 존재
        TmdbDiscoverItemReader reader = new TmdbDiscoverItemReader(tmdbService, 2020, 2021, false);

        when(tmdbService.fetchDiscoverPage(2020, 1, false))
                .thenReturn(DiscoverPageResult.empty());
        when(tmdbService.fetchDiscoverPage(2021, 1, false))
                .thenReturn(new DiscoverPageResult(List.of(new WorkItem(333)), 1));

        // when
        WorkItem item = reader.read();

        // then: 정상 empty 는 재시도 없이(1회만 호출) 다음 연도로 전진
        assertThat(item).isNotNull();
        assertThat(item.getMovieId()).isEqualTo(333);
        verify(tmdbService, times(1)).fetchDiscoverPage(2020, 1, false);
        verify(tmdbService, times(1)).fetchDiscoverPage(2021, 1, false);
    }

    @Test
    @DisplayName("한 페이지를 소비하는 도중 update() 가 저장하는 currentPage 는 아직 소비 중인 현재 페이지를 가리키며, 재시작 시 같은 페이지를 다시 읽는다")
    void update_and_restart_reReadsCurrentPage() throws Exception {
        // given: 2020년 1페이지가 신규 영화 2건 반환 (total_pages=3)
        when(tmdbService.fetchDiscoverPage(2020, 1, false))
                .thenReturn(new DiscoverPageResult(List.of(new WorkItem(1), new WorkItem(2)), 3));

        TmdbDiscoverItemReader reader = new TmdbDiscoverItemReader(tmdbService, 2020, 2020, false);
        reader.open(new ExecutionContext());

        // when: 1페이지를 fetch 하여 첫 아이템만 소비 (버퍼에 두 번째 아이템이 남아 있음)
        WorkItem first = reader.read();
        assertThat(first).isNotNull();
        assertThat(first.getMovieId()).isEqualTo(1);

        // then: fetch 직후 커서를 전진시키지 않으므로 update() 는 여전히 1페이지를 저장한다
        ExecutionContext savedContext = new ExecutionContext();
        reader.update(savedContext);
        assertThat(savedContext.getInt("currentYear")).isEqualTo(2020);
        assertThat(savedContext.getInt("currentPage")).isEqualTo(1);

        // when: 저장된 지점(1페이지)에서 재시작 (새 리더가 ExecutionContext 를 복원)
        TmdbDiscoverItemReader restarted = new TmdbDiscoverItemReader(tmdbService, 2020, 2020, false);
        restarted.open(savedContext);
        WorkItem afterRestart = restarted.read();

        // then: 재시작 후 같은 1페이지를 다시 읽는다 (upsert 멱등성 전제 하에 아이템 유실 없음)
        assertThat(afterRestart).isNotNull();
        assertThat(afterRestart.getMovieId()).isEqualTo(1);
        verify(tmdbService, times(2)).fetchDiscoverPage(2020, 1, false);
    }
}
