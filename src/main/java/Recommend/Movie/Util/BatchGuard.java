package Recommend.Movie.Util;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BatchGuard {
    private final JobExplorer jobExplorer;

    /** 한 번에 조회할 JobInstance 페이지 크기 */
    private static final int PAGE_SIZE = 100;

    /**
     * 같은 jobName과 seedVersion 파라미터로 completed 가 1개라도 있으면 true.
     * 인스턴스가 누적돼도 완료 시드를 놓치지 않도록 전체를 페이지네이션으로 순회한다.
     * */
    public boolean alreadyDone(String jobName, String seedVersion) {
        int start = 0;
        while (true) {
            List<JobInstance> instances = jobExplorer.getJobInstances(jobName, start, PAGE_SIZE);
            if (instances.isEmpty()) {
                return false;
            }
            for (JobInstance instance : instances) {
                List<JobExecution> execs = jobExplorer.getJobExecutions(instance);
                for (JobExecution exec : execs) {
                    JobParameters param = exec.getJobParameters();
                    String v = param.getString("seedVersion"); // 고정 파라미터 키
                    if (seedVersion.equals(v) && exec.getStatus() == BatchStatus.COMPLETED) {
                        return true;
                    }
                }
            }
            if (instances.size() < PAGE_SIZE) {
                return false;
            }
            start += PAGE_SIZE;
        }
    }
}
