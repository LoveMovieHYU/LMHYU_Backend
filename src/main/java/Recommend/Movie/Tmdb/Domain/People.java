package Recommend.Movie.Tmdb.Domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "people")
public class People {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    // 이름
    private String name;
    // 성별: TMDB gender 코드를 그대로 저장한다. (0=미지정, 1=여성, 2=남성, 3=논바이너리)
    private int gender;
    // 직업, ACTOR, DIRECTOR
    @Enumerated(EnumType.STRING)
    private Job job;
    // 생일
    private LocalDate birthDay;
    // 인물소개
    @Column(columnDefinition = "TEXT")
    private String biography;
    // 프로필 사진
    private String profileImagePath;

    // tmdb ID
    @Column(unique = true)
    private Long tmdbId;
}
