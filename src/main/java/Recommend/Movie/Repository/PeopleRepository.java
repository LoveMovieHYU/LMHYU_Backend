package Recommend.Movie.Repository;

import Recommend.Movie.Domain.People;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PeopleRepository extends JpaRepository<People, String> {

}
