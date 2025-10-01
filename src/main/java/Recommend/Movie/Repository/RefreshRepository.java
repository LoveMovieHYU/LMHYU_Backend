package Recommend.Movie.Repository;

import Recommend.Movie.Domain.RefreshEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

public interface RefreshRepository extends JpaRepository<RefreshEntity, Long> {
    Boolean existsByRefresh(String refreshToken);
    Optional<RefreshEntity> findByName(String name);
    @Transactional
    void deleteByRefresh(String refresh);

    // 변경점: deleteByUsername -> deleteByName (RefreshEntity의 필드명은 name)
    @Transactional
    void deleteByName(String name);
}