package Recommend.Movie.Tmdb.Domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.function.Supplier;

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
     * job 값은 Supplier 로 받아 이미 직업이 있으면 평가하지 않는다(지연 평가).
     * 이렇게 하면 통제 밖 값(Job.valueOf 실패 등)이 유입되어도 불필요한 예외가 발생하지 않는다.
     */
    public void assignJobIfAbsent(Supplier<Job> jobSupplier) {
        if (this.job == null) {
            this.job = jobSupplier.get();
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
