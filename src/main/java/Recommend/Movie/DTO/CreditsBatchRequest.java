package Recommend.Movie.DTO;

import lombok.Data;

import java.util.List;

@Data
public class CreditsBatchRequest {
    private List<Integer> movieIds;
    private Boolean fetchDetail;
}
