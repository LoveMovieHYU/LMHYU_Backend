package Recommend.Movie.Config.Batch;

import Recommend.Movie.Tmdb.Dto.DiscoverPageResult;
import Recommend.Movie.Tmdb.Dto.WorkItem;
import Recommend.Movie.Tmdb.Service.TmdbService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.*;

import java.time.Duration;
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

    // 페이지별 재시도 상한 (일시적 오류 시 무한 재시도 방지)
    private final int maxRetryPerPage = 3;

    // --- 재시작 시 복원되지 않는 순수 실행 상태 (ExecutionContext 에 저장하지 않음) ---
    // currentPage 를 이미 fetch 하여 버퍼에 채웠는지 여부. 커서 전진을 "버퍼 소비 완료 시점" 까지 미루기 위한 플래그.
    private boolean currentPageFetched = false;
    // 마지막 fetch 가 해당 연도의 마지막 페이지였는지 (전진 시 연도/페이지 결정에 사용)
    private boolean lastPageOfYear = false;
    // 현재 페이지의 연속 실패 횟수
    private int retryCount = 0;

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
            log.debug("이전 지점부터 이어서 시작. Year: {}, Page: {}", currentYear, currentPage);
        }
        // 재시작 시에는 저장된 currentPage 를 처음부터 다시 읽는다 (fetch 안 된 상태로 시작).
        this.currentPageFetched = false;
        this.lastPageOfYear = false;
        this.retryCount = 0;
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        // currentPage 는 "아직 완전히 소비되지 않았을 수 있는 현재 페이지" 를 가리킨다.
        // (커서 전진을 버퍼 소비 완료 시점까지 미루므로) 재시작 시 이 페이지를 다시 읽어도
        // 하위 upsert 가 멱등하여 데이터 정확성이 보장된다.
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

        while (true) {
            // 직전에 fetch 한 페이지를 모두 소비한 뒤에야 커서를 다음 페이지/연도로 전진한다.
            // 커서 전진을 소비 완료 시점까지 미루면, update() 가 저장하는 currentPage 가
            // 항상 아직 소비 중인 페이지를 가리켜 read 도중 잡이 죽어도 아이템이 유실되지 않는다.
            if (currentPageFetched) {
                if (lastPageOfYear) {
                    moveToNextYear();
                } else {
                    currentPage++;
                }
                currentPageFetched = false;
                retryCount = 0;
            }

            if (currentYear > endYear) {
                return null;
            }

            if (currentPage > maxPage) {
                log.debug("Reached max page for Year {}. Moving to next year.", currentYear);
                moveToNextYear();
                continue;
            }

            if (currentPage % 10 == 0 || currentPage == 1) {
                log.debug("Fetching API - Year: {}, Page: {}", currentYear, currentPage);
            }

            DiscoverPageResult result = tmdbService.fetchDiscoverPage(currentYear, currentPage, includeAdult);

            // 일시적 오류: "연도의 끝" 이 아니라 재시도 대상. 연도를 넘기지 말고 같은 페이지를 재시도한다.
            if (result.failed()) {
                retryCount++;
                if (retryCount > maxRetryPerPage) {
                    log.warn("페이지 재시도 상한({}회) 초과. Year: {}, Page: {} 를 건너뛰고 같은 연도 다음 페이지로 진행합니다.",
                            maxRetryPerPage, currentYear, currentPage);
                    retryCount = 0;
                    // 연도 전체를 스킵하지 않도록 같은 연도의 다음 페이지로만 넘어간다.
                    currentPage++;
                    continue;
                }
                log.warn("TMDB 조회 실패. 재시도 {}/{} - Year: {}, Page: {}",
                        retryCount, maxRetryPerPage, currentYear, currentPage);
                // 재시도 전 백오프 (TmdbService 의 429 sleep 과 별개로 네트워크 오류 등에 대한 완충)
                sleepSilently(Duration.ofMillis(1000L * retryCount));
                continue; // currentPageFetched=false 이므로 커서를 전진시키지 않고 같은 페이지를 재fetch
            }

            // 정상 응답
            retryCount = 0;
            lastPageOfYear = result.totalPages() <= 0 || currentPage >= result.totalPages();
            currentPageFetched = true;

            List<WorkItem> fetchedItems = result.items();
            if (fetchedItems != null && !fetchedItems.isEmpty()) {
                itemBuffer.addAll(fetchedItems);
                return itemBuffer.poll();
            }

            // 신규 영화가 없어 버퍼가 비어 있는 경우: 루프를 계속 돌려 (currentPageFetched=true 이므로)
            // 다음 페이지 또는 다음 연도로 전진한다.
            if (lastPageOfYear) {
                log.debug("No more pages for Year {} (page {}/{}). Moving to next year.",
                        currentYear, currentPage, result.totalPages());
            } else {
                log.debug("Empty (filtered) page for Year {}, page {}. Continue to next page.",
                        currentYear, currentPage);
            }
        }
    }

    private void moveToNextYear() {
        currentYear++;
        currentPage = 1;
    }

    private void sleepSilently(Duration d) {
        try {
            Thread.sleep(d.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
