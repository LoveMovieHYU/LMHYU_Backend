package Recommend.Movie.Tmdb.Dto;

import java.util.List;

/**
 * TMDB Discover 페이지 조회 결과.
 * items : DB 에 없는 신규 영화만 필터링된 결과 (비어 있어도 페이지 자체가 끝은 아닐 수 있음)
 * totalPages : TMDB 가 알려준 해당 연도의 총 페이지 수 (실제 끝 판단용)
 */
public record DiscoverPageResult(List<WorkItem> items, int totalPages) {

    public static DiscoverPageResult empty() {
        return new DiscoverPageResult(List.of(), 0);
    }
}
