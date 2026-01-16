package Recommend.Movie.Tmdb.Domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@Table(name = "people")
public class People {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    // 이름
    private String name;
    // 성별 ( 0, 1, 2, 3 )
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

    @Transient
    private boolean isNew = false;

    public void markNew(){
        this.isNew = true;
    }
}
