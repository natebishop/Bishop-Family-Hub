package com.familyhub.demo.integration;

import com.familyhub.demo.config.TestcontainersConfig;
import com.familyhub.demo.scheduler.GoogleCalendarSyncScheduler;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

class GoogleCalendarSyncSchedulerIntegrationTest {

    @Nested
    @SpringBootTest(properties = "google.oauth.client-id=test-client-id")
    @Import(TestcontainersConfig.class)
    @ActiveProfiles("test")
    class WhenGoogleConfigured {

        @Autowired
        private ApplicationContext context;

        @Test
        void schedulerBeanIsLoaded() {
            assertThat(context.getBeansOfType(GoogleCalendarSyncScheduler.class)).isNotEmpty();
        }
    }

    @Nested
    @SpringBootTest(properties = "google.oauth.client-id=")
    @Import(TestcontainersConfig.class)
    @ActiveProfiles("test")
    class WhenGoogleNotConfigured {

        @Autowired
        private ApplicationContext context;

        @Test
        void schedulerBeanIsNotLoaded() {
            assertThat(context.getBeansOfType(GoogleCalendarSyncScheduler.class)).isEmpty();
        }
    }
}
