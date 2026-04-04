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
public class TmdbPopularityBatchRunner {

    private final Job popularityUpdateJob;
    private final JobLauncher jobLauncher;
    private final BatchGuard guard;
    private final String SEED_VERSION;

    public TmdbPopularityBatchRunner(
            @Qualifier("popularityUpdateJob") Job popularityUpdateJob,
            JobLauncher jobLauncher,
            BatchGuard guard,
            @Value("${batch.seedversion}") String seedVersion) {
        this.popularityUpdateJob = popularityUpdateJob;
        this.jobLauncher = jobLauncher;
        this.guard = guard;
        this.SEED_VERSION = seedVersion;
    }

    @Order(2)
    @EventListener(ApplicationReadyEvent.class)
    public void runOnceAfterStartup() throws Exception {
        try {

            if (guard.alreadyDone("popularityUpdateJob", SEED_VERSION)) {
                log.info("Already popularityUpdateJob done for version: " + SEED_VERSION);
                return;
            }

            JobParameters params = new JobParametersBuilder()
                    .addString("seedVersion", SEED_VERSION)
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            JobExecution exec = jobLauncher.run(popularityUpdateJob, params);
            log.info("popularityUpdateJob started: id={}, params={}", exec.getId(), params);

        } catch (Exception e) {
            log.error("popularityUpdateJob failed : {}", e.getMessage(), e);
        }
    }
}