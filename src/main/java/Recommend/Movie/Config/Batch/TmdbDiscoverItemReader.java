package Recommend.Movie.Config.Batch;

import Recommend.Movie.Tmdb.Dto.DiscoverPageResult;
import Recommend.Movie.Tmdb.Dto.WorkItem;
import Recommend.Movie.Tmdb.Service.TmdbService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.*;

import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

@Slf4j
public class TmdbDiscoverItemReader implements ItemStreamReader<WorkItem> {

    private final TmdbService tmdbService;
    private final boolean includeAdult;

    // 배치 상태 관리 변수
    private int currentYear;
    private final int endYear;
    private int currentPage;
    // TMDB discover API 는 최대 500 페이지까지만 제공
    private final int maxPage = 500;

    private final Queue<WorkItem> itemBuffer = new LinkedList<>();

    public TmdbDiscoverItemReader(TmdbService tmdbService, int startYear, int endYear, boolean includeAdult) {
        this.tmdbService = tmdbService;
        this.currentYear = startYear;
        // endYear 가 0 이하이면 현재 연도를 종료 연도로 사용
        this.endYear = (endYear <= 0) ? LocalDate.now().getYear() : endYear;
        this.currentPage = 1;
        this.includeAdult = includeAdult;
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        if (executionContext.containsKey("currentYear")) {
            this.currentYear = executionContext.getInt("currentYear");
            this.currentPage = executionContext.getInt("currentPage");
            log.debug("이전 실패 지점부터 이어서 시작. Year: {}, Page: {}", currentYear, currentPage);
        }
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        executionContext.putInt("currentYear", this.currentYear);
        executionContext.putInt("currentPage", this.currentPage);
    }

    @Override
    public void close() throws ItemStreamException {
        itemBuffer.clear();
    }

    @Override
    public WorkItem read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        if (!itemBuffer.isEmpty()) {
            return itemBuffer.poll();
        }

        while (itemBuffer.isEmpty()) {
            if (currentYear > endYear) {
                return null;
            }

            if (currentPage > maxPage) {
                log.debug("Reached max page for Year {}. Moving to next year.", currentYear);
                moveToNextYear();
                continue;
            }

            int fetchingPage = currentPage;

            // API 호출
            if (fetchingPage % 10 == 0 || fetchingPage == 1) {
                log.debug("Fetching API - Year: {}, Page: {}", currentYear, fetchingPage);
            }

            DiscoverPageResult result = tmdbService.fetchDiscoverPage(currentYear, fetchingPage, includeAdult);
            List<WorkItem> fetchedItems = result.items();

            // TMDB total_pages 기준으로 "해당 연도의 마지막 페이지" 여부 판정
            boolean lastPageOfYear = result.totalPages() <= 0 || fetchingPage >= result.totalPages();

            if (fetchedItems != null && !fetchedItems.isEmpty()) {
                itemBuffer.addAll(fetchedItems);
                if (lastPageOfYear) {
                    moveToNextYear();
                } else {
                    currentPage++;
                }
                break;
            }

            // 신규 영화가 없어 items 가 비어 있는 경우:
            // 실제 끝(total_pages 도달)이면 다음 연도로, 단순 필터링 결과 0 이면 같은 연도 다음 페이지로.
            if (lastPageOfYear) {
                log.debug("No more pages for Year {} (page {}/{}). Moving to next year.",
                        currentYear, fetchingPage, result.totalPages());
                moveToNextYear();
            } else {
                log.debug("Empty (filtered) page for Year {}, page {}. Continue to next page.",
                        currentYear, fetchingPage);
                currentPage++;
            }
        }

        return itemBuffer.poll();
    }

    private void moveToNextYear() {
        currentYear++;
        currentPage = 1;
    }
}
