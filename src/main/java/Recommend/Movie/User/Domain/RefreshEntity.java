package Recommend.Movie.User.Domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "refresh_entity")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "refresh", nullable = false, length = 512)
    private String refresh;

    @CreatedDate
    @Column(name = "createAt", updatable = false)
    private LocalDateTime createAt;

    public void updateRefresh(String newRefreshToken) {
        this.createAt = LocalDateTime.now();
        this.refresh = newRefreshToken;
    }
}
