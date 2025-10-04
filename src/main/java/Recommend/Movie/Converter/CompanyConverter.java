package Recommend.Movie.Converter;

import Recommend.Movie.DTO.TmdbDTO.CompanyDTO;
import Recommend.Movie.Domain.Company;

public class CompanyConverter {
    public static Company toEntity(CompanyDTO companyDTO) {
        return Company.builder()
                .id(companyDTO.getId())
                .name(companyDTO.getName())
                .logoPath(companyDTO.getLogoPath())
                .build();

    }
}
