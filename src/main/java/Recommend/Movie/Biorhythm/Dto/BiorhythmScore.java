package Recommend.Movie.Biorhythm.Dto;

import lombok.Getter;

@Getter
public class BiorhythmScore {
    double physical, emotional, intellectual;
    public BiorhythmScore(double p, double e, double i) { this.physical = p; this.emotional = e; this.intellectual = i; }

}
