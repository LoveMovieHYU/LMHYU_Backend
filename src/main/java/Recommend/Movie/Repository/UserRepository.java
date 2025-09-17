package Recommend.Movie.Repository;

import Recommend.Movie.Domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {

}
