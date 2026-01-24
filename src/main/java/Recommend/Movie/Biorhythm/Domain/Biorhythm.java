package Recommend.Movie.Biorhythm.Domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Biorhythm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "biorhythm_id")
    private int biorhythmId;

    @Column(name = "physical_index")
    private float physicalIndex;

    @Column(name = "emotional_index")
    private float emotionalIndex;

    @Column(name = "intellectual_index")
    private float intellectualIndex;

    @Column(name = "calculated_score")
    private float calculatedScore;

    @Column(name = "create_at")
    private LocalDate createAt;

}
