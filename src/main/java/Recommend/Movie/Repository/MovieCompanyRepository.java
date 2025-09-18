package Recommend.Movie.Repository;

import Recommend.Movie.Domain.MovieCompany;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieCompanyRepository extends JpaRepository<MovieCompany, Integer> {
    boolean existsByMovie_IdAndCompany_Id(int movieId, int companyId);
}
