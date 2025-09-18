package Recommend.Movie.Repository;

import Recommend.Movie.Domain.MovieCompany;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MovieCompanyRepository extends JpaRepository<MovieCompany, Integer> {
    boolean existsByMovie_IdAndCompany_Id(int movieId, int companyId);
}
