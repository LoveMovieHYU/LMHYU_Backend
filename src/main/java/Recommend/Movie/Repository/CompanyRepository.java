package Recommend.Movie.Repository;

import Recommend.Movie.Domain.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompanyRepository extends JpaRepository<Company, String> {
    boolean existsById(int id);
    Company getReferenceById(int id);
}
