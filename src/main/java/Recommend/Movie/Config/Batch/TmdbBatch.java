package Recommend.Movie.Config.Batch;

import Recommend.Movie.DTO.TmdbDTO.MovieDetailDTO;
import Recommend.Movie.DTO.TmdbDTO.WorkItem;
import Recommend.Movie.Service.MovieService;
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
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.support.IteratorItemReader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.List;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class TmdbBatch {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final MovieService movieService;
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
                .<WorkItem, Integer>chunk(1000, transactionManager)
                .reader(tmdbReader)
                .processor(tmdbProcessor)
                .writer(tmdbWriter)
                .faultTolerant()
                .retry(org.springframework.web.client.ResourceAccessException.class) // Read timed out 포함
                .retry(java.net.SocketTimeoutException.class)
                .retryLimit(3)
                .backOffPolicy(new FixedBackOffPolicy() {{ setBackOffPeriod(1000L); }})
                .build();
    }

    @Bean
    @StepScope
    public IteratorItemReader<WorkItem> tmdbReader(
            @Value("#{jobParameters['startPage']}") Long startPage,
            @Value("#{jobParameters['endPage']}") Long endPage,
            @Value("#{jobParameters['includeAdult']}") String includeAdult
    ){
        int sPage = (startPage != null) ? startPage.intValue() : 1;
        int ePage = (endPage != null) ? endPage.intValue() : sPage;
        boolean incAdult = Boolean.parseBoolean(includeAdult);

        List<WorkItem> items = movieService.buildWorkItemsFromDiscover(sPage, ePage, incAdult);
        return new IteratorItemReader<>(items);
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
                movieService.fetchAndSaveMovieDetail(movieId);
            }
        };
    }
}
