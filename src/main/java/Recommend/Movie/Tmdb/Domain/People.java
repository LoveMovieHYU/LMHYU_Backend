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
    @Builder.Default
    private boolean isNew = false;

    /**
     * 신규 People 생성 (tmdbId 기준)
     */
    public static People createNew(long tmdbId) {
        People people = People.builder()
                .tmdbId(tmdbId)
                .build();
        people.isNew = true;
        return people;
    }

    public void markNew() {
        this.isNew = true;
    }

    /**
     * 크레딧 기본 정보(이름/성별/프로필) 반영. null 값은 기존 값을 유지한다.
     */
    public void updateBasicInfo(String name, Integer gender, String profileImagePath) {
        if (name != null) {
            this.name = name;
        }
        if (gender != null) {
            this.gender = gender;
        }
        if (profileImagePath != null) {
            this.profileImagePath = profileImagePath;
        }
    }

    /**
     * 직업 정보가 없을 때만 설정한다.
     */
    public void assignJobIfAbsent(Job job) {
        if (this.job == null) {
            this.job = job;
        }
    }

    /**
     * 인물 상세 정보(소개/생일/프로필) 반영. 비어있는 값은 기존 값을 유지한다.
     */
    public void updateDetail(String biography, LocalDate birthDay, String profileImagePath) {
        if (biography != null && !biography.isBlank()) {
            this.biography = biography;
        }
        if (birthDay != null) {
            this.birthDay = birthDay;
        }
        if (profileImagePath != null && !profileImagePath.isBlank()
                && (this.profileImagePath == null || this.profileImagePath.isBlank())) {
            this.profileImagePath = profileImagePath;
        }
    }
}
