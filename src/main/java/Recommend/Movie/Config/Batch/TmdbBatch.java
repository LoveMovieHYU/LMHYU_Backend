package Recommend.Movie.Config.Batch;

import Recommend.Movie.Tmdb.Dto.WorkItem;
import Recommend.Movie.Tmdb.Service.TmdbService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;


@Configuration
@Slf4j
@RequiredArgsConstructor
public class TmdbBatch {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final TmdbService tmdbService;
    private final @Qualifier("dataDBSource") DataSource dataDataSource;

    @Bean
    public Job tmdbJob(Step tmdbStep){
        return new JobBuilder("tmdbJob", jobRepository)
                .listener(new TmdbJobListener())
                .start(tmdbStep)
                .build();
    }

    @Bean
    public Step tmdbStep(ItemReader<WorkItem> tmdbReader,
                         ItemProcessor<WorkItem, Integer> tmdbProcessor,
                         ItemWriter<Integer> tmdbWriter){
        return new StepBuilder("tmdbStep", jobRepository)
                .<WorkItem, Integer>chunk(50, transactionManager)
                .reader(tmdbReader)
                .processor(tmdbProcessor)
                .writer(tmdbWriter)
                .faultTolerant()
                .retry(org.springframework.web.client.ResourceAccessException.class)
                .retry(java.net.SocketTimeoutException.class)
                .retryLimit(3)
                .backOffPolicy(new FixedBackOffPolicy() {{ setBackOffPeriod(2000L); }})
                .skip(Exception.class)
                .skipLimit(1000)
                .build();
    }

    @Bean
    @StepScope
    public TmdbDiscoverItemReader tmdbReader(
            @Value("#{jobParameters['startPage']}") Long startPage,
            @Value("#{jobParameters['includeAdult']}") String includeAdult
    ){
        int startYear = 2026;

        boolean incAdult = Boolean.parseBoolean(includeAdult);

        return new TmdbDiscoverItemReader(tmdbService, startYear, incAdult);
    }
    @Bean
    @StepScope
    public ItemProcessor<WorkItem, Integer> tmdbProcessor(){
        return WorkItem::getMovieId;
    }

    @Bean
    public ItemWriter<Integer> tmdbWriter(){
        return items -> {
            for (Integer movieId : items) {
                log.debug("Processing movieId: {}", movieId);
                tmdbService.fetchAndSaveMovieDetail(movieId);
            }
        };
    }
}
