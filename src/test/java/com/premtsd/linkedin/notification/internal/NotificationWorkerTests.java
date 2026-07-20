package com.premtsd.linkedin.notification.internal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the event-driven worker end to end: signing up publishes a
 * UserRegisteredEvent, and the async @ApplicationModuleListener in the
 * notification module creates a welcome notification. Awaitility polls until the
 * after-commit worker finishes, so the async step is asserted without sleeps.
 */
@SpringBootTest
@AutoConfigureMockMvc
class NotificationWorkerTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    NotificationRepository notifications;

    @Test
    void signup_triggers_welcome_notification_from_worker() throws Exception {
        mvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Ada","email":"ada.worker@example.com","password":"secret123"}"""))
                .andExpect(status().isOk());

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(notifications.findAll())
                        .extracting(Notification::getMessage)
                        .anyMatch(m -> m.contains("Welcome")));
    }
}
