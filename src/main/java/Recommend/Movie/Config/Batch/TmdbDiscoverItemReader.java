package Recommend.Movie.Config.Batch;

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
    private final int maxPage = 500;

    private final Queue<WorkItem> itemBuffer = new LinkedList<>();

    public TmdbDiscoverItemReader(TmdbService tmdbService, int startYear, boolean includeAdult) {
        this.tmdbService = tmdbService;
        this.currentYear = startYear;
        this.endYear = LocalDate.now().getYear();
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
                log.debug("Finished Year {}. Moving to next year.", currentYear);
                currentYear++;
                currentPage = 1;
                continue; // 다음 연도 체크를 위해 루프 처음으로
            }

            // API 호출
            if (currentPage % 10 == 0 || currentPage == 1) {
                log.debug("Fetching API - Year: {}, Page: {}", currentYear, currentPage);
            }

            List<WorkItem> fetchedItems = tmdbService.fetchDiscoverPage(currentYear, currentPage, includeAdult);

            currentPage++;

            if (fetchedItems != null && !fetchedItems.isEmpty()) {
                itemBuffer.addAll(fetchedItems);
                break;
            } else {
                log.debug("No more data for Year {}. Moving to next year.", currentYear);
                currentYear++;
                currentPage = 1;
            }
        }

        return itemBuffer.poll();
    }
}