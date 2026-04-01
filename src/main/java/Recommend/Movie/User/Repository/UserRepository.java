package Recommend.Movie.User.Repository;

import Recommend.Movie.User.Domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    User findByUserId(int userId);

    Optional<User> findByProviderIdAndIsSocial(String providerId, Boolean social);

    boolean existsByNickname(String nickname);

}