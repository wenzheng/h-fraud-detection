package com.vincent.fraud.alert.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vincent.fraud.alert.config.AlertRoutingProperties;
import com.vincent.fraud.alert.config.ConsumerProperties;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class MnsAlertConsumerLifecycleTest {

    @Test
    void shouldSubmitConfiguredNumberOfPollersOnStart() {
        RecordingExecutorService executorService = new RecordingExecutorService();
        MnsAlertConsumerLifecycle lifecycle = new MnsAlertConsumerLifecycle(
                null,
                new ConsumerProperties(3, 1, 1),
                executorService,
                new ObjectMapper(),
                new AlertRoutingService(
                        new AlertRoutingProperties(null, null, null),
                        List.of(new TelegramAlertSender(), new EmailAlertSender())
                )
        );

        lifecycle.start();

        assertThat(lifecycle.isRunning()).isTrue();
        assertThat(executorService.submittedTasks).hasSize(3);
    }

    @Test
    void shouldStopLifecycle() {
        RecordingExecutorService executorService = new RecordingExecutorService();
        MnsAlertConsumerLifecycle lifecycle = new MnsAlertConsumerLifecycle(
                null,
                new ConsumerProperties(1, 1, 1),
                executorService,
                new ObjectMapper(),
                new AlertRoutingService(
                        new AlertRoutingProperties(null, null, null),
                        List.of(new TelegramAlertSender(), new EmailAlertSender())
                )
        );
        lifecycle.start();

        lifecycle.stop();

        assertThat(lifecycle.isRunning()).isFalse();
    }

    private static final class RecordingExecutorService extends AbstractExecutorService {

        private final List<Runnable> submittedTasks = new ArrayList<>();
        private boolean shutdown;

        @Override
        public void shutdown() {
            shutdown = true;
        }

        @Override
        public List<Runnable> shutdownNow() {
            shutdown = true;
            return List.of();
        }

        @Override
        public boolean isShutdown() {
            return shutdown;
        }

        @Override
        public boolean isTerminated() {
            return shutdown;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return true;
        }

        @Override
        public void execute(Runnable command) {
            submittedTasks.add(command);
        }
    }
}
