package Recommend.Movie.Login.Repository;

import Recommend.Movie.Login.Domain.LoginUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LoginUserRepository extends JpaRepository<LoginUser, Long> {
    Optional<LoginUser> findByEmail(String email);
}
