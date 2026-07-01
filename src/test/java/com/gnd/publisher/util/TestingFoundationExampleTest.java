package com.gnd.publisher.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class TestingFoundationExampleTest {

    @Test
    void trimsTextWithAssertJ() {
        Optional<String> result = Optional.ofNullable("  Greek news  ")
                .map(String::trim);

        assertThat(result)
                .contains("Greek news");
    }

    @Test
    void verifiesCollaboratorWithMockito() {
        ExamplePublisher publisher = mock(ExamplePublisher.class);

        publisher.publish("source-attributed-message");

        verify(publisher).publish("source-attributed-message");
    }

    private interface ExamplePublisher {
        void publish(String message);
    }
}
