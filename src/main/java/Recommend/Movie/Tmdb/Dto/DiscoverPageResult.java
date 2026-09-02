package Recommend.Movie.Tmdb.Dto;

import java.util.List;

/**
 * TMDB Discover 페이지 조회 결과.
 * items : DB 에 없는 신규 영화만 필터링된 결과 (비어 있어도 페이지 자체가 끝은 아닐 수 있음)
 * totalPages : TMDB 가 알려준 해당 연도의 총 페이지 수 (실제 끝 판단용)
 * failed : API 호출 자체가 일시적 오류(rate limit/네트워크 등)로 실패했는지 여부.
 *          true 이면 "연도의 끝" 이 아니라 "재시도가 필요한 오류" 로 해석해야 한다.
 */
public record DiscoverPageResult(List<WorkItem> items, int totalPages, boolean failed) {

    /**
     * 성공 응답용 2-인자 생성자 (failed=false).
     */
    public DiscoverPageResult(List<WorkItem> items, int totalPages) {
        this(items, totalPages, false);
    }

    /**
     * 정상적으로 조회했으나 신규 데이터가 없는 경우 (진짜 마지막 페이지 판단은 totalPages 로).
     */
    public static DiscoverPageResult empty() {
        return new DiscoverPageResult(List.of(), 0, false);
    }

    /**
     * API 호출이 일시적 오류로 실패한 경우. 리더는 이 신호를 받으면 연도를 넘기지 않고 같은 페이지를 재시도한다.
     */
    public static DiscoverPageResult failure() {
        return new DiscoverPageResult(List.of(), 0, true);
    }
}
