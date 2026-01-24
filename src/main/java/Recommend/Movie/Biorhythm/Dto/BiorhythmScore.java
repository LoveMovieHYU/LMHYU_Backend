package Recommend.Movie.Biorhythm.Dto;

import lombok.Getter;

@Getter
public class BiorhythmScore {
    int physical, emotional, intellectual;
    public BiorhythmScore(int p, int e, int i) { this.physical = p; this.emotional = e; this.intellectual = i; }

}
