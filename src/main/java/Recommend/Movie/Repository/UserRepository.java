package Recommend.Movie.Repository;

import Recommend.Movie.Domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    User findByUserId(int userId);

    Optional<User> findByProviderIdAndIsSocial(String providerId, Boolean social);

    Optional<User> findByName(String nameInDb);
}