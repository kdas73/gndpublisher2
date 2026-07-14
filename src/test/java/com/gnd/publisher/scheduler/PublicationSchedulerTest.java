package com.gnd.publisher.scheduler;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.gnd.publisher.config.SchedulerProperties;
import com.gnd.publisher.config.SchedulerProperties.ScheduledJob;
import com.gnd.publisher.service.PublicationService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PublicationSchedulerTest {

    @Mock
    private PublicationService publicationService;

    @Test
    void callsPublicationServiceWhenPublicationJobIsEnabled() {
        PublicationScheduler scheduler = new PublicationScheduler(publicationService, schedulerProperties(true));

        scheduler.publishSelectedContent();

        verify(publicationService).publishSelectedContent();
    }

    @Test
    void doesNotCallPublicationServiceWhenPublicationJobIsDisabled() {
        PublicationScheduler scheduler = new PublicationScheduler(publicationService, schedulerProperties(false));

        scheduler.publishSelectedContent();

        verify(publicationService, never()).publishSelectedContent();
    }

    private SchedulerProperties schedulerProperties(boolean publicationEnabled) {
        return new SchedulerProperties(
                new ScheduledJob(true, "0 */3 * * * *"),
                new ScheduledJob(publicationEnabled, "0 */20 * * * *"),
                new ScheduledJob(true, "0 0 */6 * * *"),
                new ScheduledJob(true, "0 0 3 * * *"));
    }
}
