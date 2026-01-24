//package Recommend.Movie.Emotion.Dto;
//
//import io.swagger.v3.oas.annotations.media.Schema;
//import lombok.Getter;
//import lombok.Setter;
//
//@Getter
//@Setter
//public class TodayEmotionResponseDTO {
//
//    @Schema(description = "오늘 감정 선택 여부", example = "true")
//    private boolean hasSelected;
//
//    @Schema(description = "선택한 감정 태그 (안했으면 null)", example = "SAD")
//    private String emotionTag;
//
//    @Schema(description = "메시지", example = "성공했습니다.")
//    private String message;
//
//    public TodayEmotionResponseDTO(String message, boolean hasSelected, String emotionTag) {
//        this.emotionTag = emotionTag;
//        this.hasSelected = hasSelected;
//        this.message = message;
//    }
//}
