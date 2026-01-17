package Recommend.Movie.Diary.Repository;

import Recommend.Movie.Diary.Domain.Diary;
import Recommend.Movie.User.Domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface DiaryRepository extends JpaRepository<Diary, Integer> {
    Diary findByUserAndCreateAt(User user, LocalDate createAt);
}
