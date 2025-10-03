package Recommend.Movie.DTO.TmdbDTO;

import lombok.Data;

import java.util.List;

@Data
public class CreditsResponse {
    private List<CreditsPeople> cast;
    private List<CreditsPeople> crew;
}
