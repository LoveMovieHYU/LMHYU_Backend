package Recommend.Movie.Biorhythm.Service;

import Recommend.Movie.Biorhythm.Dto.BiorhythmScore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiDatasetService {

    private final WebClient webClient;

    /**
     * AI 학습용 데이터셋 전송 (비동기)
     * - @Async("logExecutor"): 별도의 스레드 풀에서 실행됨 (메인 스레드 차단 X)
     * - 리턴 타입이 void여야 함 (Fire-and-Forget)
     */
    @Async("logExecutor")
    public void sendInteractionLog(int userId, Long movieId, BiorhythmScore score) {

        double p = score.getPhysical();
        double e = score.getEmotional();
        double i = score.getIntellectual();

        log.debug("Sending AI Log - User:{}, Movie:{}, P:{}, E:{}, I:{}", userId, movieId, p, e, i);

        try {
            webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/dataset/{userId}/{movieId}/{p}/{e}/{i}")
                            .build(userId, movieId, p, e, i))
                    .retrieve()
                    .toBodilessEntity()
                    .subscribe(
                            response -> log.debug("AI Log Sent Success: {}", response.getStatusCode()),
                            error -> log.error("AI Log Sending Failed: {}", error.getMessage())
                    );

        } catch (Exception ex) {
            log.error("Error in sendInteractionLog", ex);
        }
    }
}