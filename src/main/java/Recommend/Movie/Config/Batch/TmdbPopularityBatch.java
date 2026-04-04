package Recommend.Movie.Config.Batch;

import Recommend.Movie.Tmdb.Domain.Movie;
import Recommend.Movie.Tmdb.Service.TmdbService;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaCursorItemReader;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JpaCursorItemReaderBuilder;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class TmdbPopularityBatch {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final TmdbService tmdbService;
    private final EntityManagerFactory entityManagerFactory;

    @Bean
    public Job popularityUpdateJob(Step popularityUpdateStep) {
        return new JobBuilder("popularityUpdateJob", jobRepository)
                .listener(new TmdbJobListener())
                .start(popularityUpdateStep)
                .build();
    }

    @Bean
    public Step popularityUpdateStep(JpaCursorItemReader<Movie> popularityReader,
                                     ItemProcessor<Movie, Movie> popularityProcessor,
                                     JpaItemWriter<Movie> popularityWriter) {
        return new StepBuilder("popularityUpdateStep", jobRepository)
                .<Movie, Movie>chunk(100, transactionManager)
                .reader(popularityReader)
                .processor(popularityProcessor)
                .writer(popularityWriter)
                .faultTolerant()
                .retry(org.springframework.web.client.ResourceAccessException.class)
                .retry(java.net.SocketTimeoutException.class)
                .retryLimit(3)
                .backOffPolicy(new FixedBackOffPolicy() {{ setBackOffPeriod(1000L); }})
                .build();
    }

    /**
     * Reader: DB에서 popularity가 null인 영화만 가져옵니다.
     * 페이징(Paging) 대신 커서(Cursor)를 사용한 이유:
     * 페이징으로 가져올 경우, Writer가 null을 채우면 다음 페이지 조회 시 데이터가 밀리는 버그가 발생합니다.
     * Cursor 기반은 DB 연결을 열어두고 빨대 꽂듯이 하나씩 순서대로 가져오므로 밀림 현상이 완벽히 방지됩니다!
     */
    @Bean
    @StepScope
    public JpaCursorItemReader<Movie> popularityReader() {
        return new JpaCursorItemReaderBuilder<Movie>()
                .name("popularityReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT m FROM Movie m WHERE m.popularity IS NULL")
                .build();
    }

    /**
     * [2] Processor: TMDB API를 호출하여 Popularity 점수를 채워 넣습니다.
     */
    @Bean
    @StepScope
    public ItemProcessor<Movie, Movie> popularityProcessor() {
        return movie -> {
            try {
                // 1. TMDB에서 popularity 가져오기 (가정한 메서드명, 실제 메서드에 맞게 수정)
                // 여기서 반환값이 TMDB의 popularity (Double)
                Double popScore = tmdbService.getPopularityFromTmdb(movie.getTmdbId());
                movie.setPopularity(popScore);

                Thread.sleep(40);

            } catch (Exception e) {
                log.error("Failed to fetch popularity for TMDB ID: {}", movie.getTmdbId(), e);
                movie.setPopularity(-1.0);
            }
            return movie;
        };
    }

    /**
     * [3] Writer: Processor에서 넘어온 100개의 완성된 Movie 객체를 DB에 한 번에 Update(Merge) 합니다.
     */
    @Bean
    @StepScope
    public JpaItemWriter<Movie> popularityWriter() {
        return new JpaItemWriterBuilder<Movie>()
                .entityManagerFactory(entityManagerFactory)
                .usePersist(false) // Update(Merge) 모드로 작동
                .build();
    }
}