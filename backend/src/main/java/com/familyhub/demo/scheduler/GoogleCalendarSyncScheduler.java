package com.familyhub.demo.scheduler;

import com.familyhub.demo.model.GoogleOAuthToken;
import com.familyhub.demo.repository.GoogleOAuthTokenRepository;
import com.familyhub.demo.service.GoogleCalendarSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Runs incremental sync for all Google-connected members on a fixed interval.
 *
 * DD-2: @ConditionalOnExpression prevents this bean from loading when Google OAuth
 * is not configured. Uses !isEmpty() because @ConditionalOnProperty treats an empty
 * string value as present (not absent), so it cannot be used to gate on a non-empty
 * client ID.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnExpression("!'${google.oauth.client-id:}'.isEmpty()")
public class GoogleCalendarSyncScheduler {

    private final GoogleOAuthTokenRepository tokenRepository;
    private final GoogleCalendarSyncService syncService;

    @Scheduled(fixedRate = 900_000) // 15 minutes in milliseconds
    public void syncAllConnectedMembers() {
        List<GoogleOAuthToken> tokens = tokenRepository.findAll();
        log.info("Scheduled sync: {} connected members", tokens.size());

        for (GoogleOAuthToken token : tokens) {
            syncService.syncMember(token.getMember().getId());
        }
    }
}
