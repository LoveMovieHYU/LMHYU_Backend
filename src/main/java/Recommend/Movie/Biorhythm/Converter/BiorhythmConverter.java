package Recommend.Movie.Biorhythm.Converter;

import Recommend.Movie.Biorhythm.Dto.BiorhythmAnalysisDTO;
import Recommend.Movie.Biorhythm.Dto.BiorhythmScore;

public class BiorhythmConverter {

    public static BiorhythmAnalysisDTO toAnalysisDTO(BiorhythmScore score, String message, String birthDay){
        return BiorhythmAnalysisDTO.builder()
                .physicalIndex(score.getPhysical())
                .emotionalIndex(score.getEmotional())
                .intellectualIndex(score.getIntellectual())
                .statusMessage(message)
                .birthDay(birthDay)
                .build();
    }
}
