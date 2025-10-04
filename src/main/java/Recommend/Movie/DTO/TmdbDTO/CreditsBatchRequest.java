package Recommend.Movie.DTO.TmdbDTO;

import lombok.Data;

import java.util.List;

@Data
public class CreditsBatchRequest {
    private List<Integer> movieIds;
    private Boolean fetchDetail;
}
