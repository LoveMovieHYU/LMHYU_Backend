package Recommend.Movie.Config.Batch;

import Recommend.Movie.Tmdb.Dto.WorkItem;
import Recommend.Movie.Tmdb.Service.TmdbService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;

import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * [신규] 커스텀 ItemReader
 * - read()가 호출될 때 버퍼가 비어있으면 API를 호출하여 채웁니다.
 * - 연도별, 페이지별로 순차적으로 데이터를 가져옵니다.
 */
@Slf4j
public class TmdbDiscoverItemReader implements ItemReader<WorkItem> {

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
    public WorkItem read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        if (!itemBuffer.isEmpty()) {
            return itemBuffer.poll();
        }

        while (itemBuffer.isEmpty()) {
            if (currentYear > endYear) {
                return null;
            }

            if (currentPage > maxPage) {
                log.info("Finished Year {}. Moving to next year.", currentYear);
                currentYear++;
                currentPage = 1;
                continue; // 다음 연도 체크를 위해 루프 처음으로
            }

            // API 호출
            if (currentPage % 10 == 0 || currentPage == 1) {
                log.info("Fetching API - Year: {}, Page: {}", currentYear, currentPage);
            }

            List<WorkItem> fetchedItems = tmdbService.fetchDiscoverPage(currentYear, currentPage, includeAdult);

            // 페이지 증가 (다음 호출을 위해 미리 증가)
            currentPage++;

            if (fetchedItems != null && !fetchedItems.isEmpty()) {
                itemBuffer.addAll(fetchedItems);
                break;
            } else {
                // 데이터가 비어있다면 (해당 연도의 끝이거나 데이터 없음), 다음 연도로 바로 점프
                log.info("No more data for Year {}. Moving to next year.", currentYear);
                currentYear++;
                currentPage = 1;
            }
        }

        return itemBuffer.poll();
    }
}