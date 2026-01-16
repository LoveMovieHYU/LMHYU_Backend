package Recommend.Movie.Tmdb.Dto;

import lombok.Data;

import java.util.List;

@Data
public class CreditsResponse {
    private List<CreditsPeople> cast;
    private List<CreditsPeople> crew;
}
