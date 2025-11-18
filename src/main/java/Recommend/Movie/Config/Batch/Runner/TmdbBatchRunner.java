package Recommend.Movie.Config.Batch.Runner;

import Recommend.Movie.Util.BatchGuard;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TmdbBatchRunner {
    private final Job tmdbJob;
    private final JobLauncher jobLauncher;
    private final BatchGuard guard;
    private final String SEED_VERSION;

    public TmdbBatchRunner(@Qualifier("tmdbJob") Job tmdbJob, JobLauncher jobLauncher,
                           BatchGuard guard,
                           @Value("${batch.seedversion}") String seedVersion) {
        this.tmdbJob = tmdbJob;
        this.jobLauncher = jobLauncher;
        this.guard = guard;
        this.SEED_VERSION = seedVersion;
    }

    @Order(1)
    @EventListener(ApplicationReadyEvent.class)
    public void runOnceAfterStartup() throws Exception {
        try{

            if(guard.alreadyDone("tmdbJob",SEED_VERSION)){
                log.info("Already tmdbJob : " + SEED_VERSION );
                return;
            }

            JobParameters params = new JobParametersBuilder()
                    .addLong("startPage", 51L)
                    .addLong("endPage", 80L)
                    .addString("includeAdult", "false")
                    .addString("seedVersion", SEED_VERSION)
                    .toJobParameters();
            JobExecution exec = jobLauncher.run(tmdbJob, params);
            log.info("tmdbJob started: id={}, params={}", exec.getId(), params);

        } catch (Exception e){
            log.error("tmdbJob failed : {}", e.getMessage(), e);
        }
    }
}
