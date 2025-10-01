package Recommend.Movie.Repository;

import Recommend.Movie.Domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
// 변경점 1: JpaRepository<User, String> -> JpaRepository<User, Integer>
public interface UserRepository extends JpaRepository<User, Integer> {

    // 변경점 2: 모든 메소드의 ByUsername -> ByName 으로 변경
    Boolean existsByName(String name);
    Optional<User> findByNameAndIsLockAndIsSocial(String name, Boolean isLock, Boolean isSocial);
    Optional<User> findByNameAndIsSocial(String name, Boolean social);
    Optional<User> findByNameAndIsLock(String name, Boolean isLock);

    @Transactional
    void deleteByName(String name);

    Optional<User> findByName(String name);
}