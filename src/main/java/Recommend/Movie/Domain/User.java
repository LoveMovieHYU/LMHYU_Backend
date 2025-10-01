package Recommend.Movie.Domain;

import Recommend.Movie.DTO.UserRequestDTO;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor // Builder 사용 시 기본 생성자 추가
@AllArgsConstructor // Builder 사용 시 모든 필드 생성자 추가
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false, unique = true)
    private String name;

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
    @PrePersist
    protected void onCreate() {
        if (this.createAt == null) {
            this.createAt = LocalDate.now();
        }
    }
    public void updateUser(UserRequestDTO dto) {
        this.email = dto.getEmail();
        this.name = dto.getName();
    }
}
