package Recommend.Movie.Tmdb.Converter;

import Recommend.Movie.Tmdb.Dto.CompanyDTO;
import Recommend.Movie.Tmdb.Domain.Company;

public class CompanyConverter {
    public static Company toEntity(CompanyDTO companyDTO) {
        return Company.builder()
                .id(companyDTO.getId())
                .name(companyDTO.getName())
                .logoPath(companyDTO.getLogoPath())
                .build();

    }
}
