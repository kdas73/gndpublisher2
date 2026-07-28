package com.gnd.publisher.scheduler;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.gnd.publisher.config.SchedulerProperties;
import com.gnd.publisher.config.SchedulerProperties.ScheduledJob;
import com.gnd.publisher.service.ImportantNewsDigestService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ImportantNewsDigestSchedulerTest {

    @Mock
    private ImportantNewsDigestService importantNewsDigestService;

    @Test
    void callsDigestServiceWhenDigestJobIsEnabled() {
        ImportantNewsDigestScheduler scheduler =
                new ImportantNewsDigestScheduler(importantNewsDigestService, schedulerProperties(true));

        scheduler.publishImportantNewsDigest();

        verify(importantNewsDigestService).publishImportantNewsDigest();
    }

    @Test
    void doesNotCallDigestServiceWhenDigestJobIsDisabled() {
        ImportantNewsDigestScheduler scheduler =
                new ImportantNewsDigestScheduler(importantNewsDigestService, schedulerProperties(false));

        scheduler.publishImportantNewsDigest();

        verify(importantNewsDigestService, never()).publishImportantNewsDigest();
    }

    private SchedulerProperties schedulerProperties(boolean digestEnabled) {
        return new SchedulerProperties(
                new ScheduledJob(true, "0 */3 * * * *"),
                new ScheduledJob(true, "0 */20 * * * *"),
                new ScheduledJob(digestEnabled, "0 0 */6 * * *"),
                new ScheduledJob(true, "0 0 3 * * *"));
    }
}
