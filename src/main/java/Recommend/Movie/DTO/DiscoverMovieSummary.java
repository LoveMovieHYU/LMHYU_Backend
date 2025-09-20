package Recommend.Movie.DTO;

import lombok.Getter;

@Getter
public class DiscoverMovieSummary {
    public int id;
    public String title;
    public String original_title;
    public String release_date;
    public String poster_path;
    public String backdrop_path;
    public Boolean adult;
    public Double popularity;
    public Double vote_average;
    public Integer vote_count;
}
