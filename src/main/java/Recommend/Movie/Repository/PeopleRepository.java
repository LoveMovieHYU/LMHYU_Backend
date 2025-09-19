package Recommend.Movie.Repository;

import Recommend.Movie.Domain.People;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PeopleRepository extends JpaRepository<People, String> {
    People findById(long id);

}
