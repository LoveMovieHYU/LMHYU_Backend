package Recommend.Movie.Tmdb.Dto;

import lombok.Data;

import java.util.List;

@Data
public class CreditsBatchRequest {
    private List<Integer> movieIds;
    private Boolean fetchDetail;
}
