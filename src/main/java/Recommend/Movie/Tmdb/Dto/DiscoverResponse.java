package Recommend.Movie.Tmdb.Dto;

import java.util.List;

public class DiscoverResponse {
    public Integer page;
    public List<DiscoverMovieSummary> results;
    public Integer total_pages;
    public Integer total_results;
}
