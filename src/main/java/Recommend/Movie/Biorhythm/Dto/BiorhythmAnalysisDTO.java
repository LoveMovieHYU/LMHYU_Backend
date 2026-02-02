package Recommend.Movie.Biorhythm.Dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BiorhythmAnalysisDTO {
    @Schema(title = "신체 지수", example = "-51")
    private double physicalIndex;

    @Schema(title = "감성 지수", example = "0")
    private double emotionalIndex;

    @Schema(title = "지성 지수", example = "97")
    private double intellectualIndex;

    @Schema(title = "분석 멘트", example = "두뇌 회전이 빠른 날입니다. 몰입감 넘치는 스토리에 도전해보세요.")
    private String statusMessage;
}
