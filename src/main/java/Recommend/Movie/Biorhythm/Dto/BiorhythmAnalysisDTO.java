package Recommend.Movie.Biorhythm.Dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BiorhythmAnalysisDTO {
    private int physicalIndex;    // 신체 지수
    private int emotionalIndex;   // 감성 지수
    private int intellectualIndex;// 지성 지수
    private String statusMessage; // 분석 멘트
}
