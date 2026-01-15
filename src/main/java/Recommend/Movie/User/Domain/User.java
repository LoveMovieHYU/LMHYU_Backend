package Recommend.Movie.User.Domain;

import Recommend.Movie.Diary.Domain.Diary;
import Recommend.Movie.Feedback.Domain.FeedbackEvent;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int userId;

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String nickname;

    @Column(name = "provider_id", nullable = false, unique = true)
    private String providerId;

    @Column(nullable = false)
    private String email;

    @Column(name = "is_social", nullable = false)
    private Boolean isSocial;

    @Column(name = "is_lock", nullable = false)
    private Boolean isLock;

    @Enumerated(EnumType.STRING)
    @Column(name = "social_provider_type")
    private SocialProviderType socialProviderType;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_type", nullable = false)
    private UserRoleType roleType;

    private String ageGroup;
    private String location;
    private String gender;

    private LocalDate createAt;

    @OneToMany(mappedBy = "user", fetch =  FetchType.LAZY)
    @Builder.Default
    private List<Diary> diaryList = new ArrayList<>();

    @OneToMany(mappedBy = "user", fetch =  FetchType.LAZY)
    @Builder.Default
    private List<FeedbackEvent> feedbackEvents = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (this.createAt == null) {
            this.createAt = LocalDate.now();
        }
    }
}
