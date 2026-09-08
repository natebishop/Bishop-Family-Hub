package com.familyhub.demo.scheduler;

import com.familyhub.demo.model.FamilyMember;
import com.familyhub.demo.model.GoogleOAuthToken;
import com.familyhub.demo.repository.GoogleOAuthTokenRepository;
import com.familyhub.demo.service.GoogleCalendarSyncService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoogleCalendarSyncSchedulerTest {

    @Mock
    private GoogleOAuthTokenRepository tokenRepository;
    @Mock
    private GoogleCalendarSyncService syncService;

    @InjectMocks
    private GoogleCalendarSyncScheduler scheduler;

    @Test
    void syncAllConnectedMembers_callsSyncMemberForEachToken() {
        UUID memberId1 = UUID.randomUUID();
        UUID memberId2 = UUID.randomUUID();

        GoogleOAuthToken token1 = createTokenWithMember(memberId1);
        GoogleOAuthToken token2 = createTokenWithMember(memberId2);

        when(tokenRepository.findAll()).thenReturn(List.of(token1, token2));

        scheduler.syncAllConnectedMembers();

        verify(syncService).syncMember(memberId1);
        verify(syncService).syncMember(memberId2);
    }

    @Test
    void syncAllConnectedMembers_callsSyncMemberForEveryToken() {
        UUID memberId1 = UUID.randomUUID();
        UUID memberId2 = UUID.randomUUID();
        UUID memberId3 = UUID.randomUUID();

        when(tokenRepository.findAll()).thenReturn(List.of(
                createTokenWithMember(memberId1),
                createTokenWithMember(memberId2),
                createTokenWithMember(memberId3)));

        scheduler.syncAllConnectedMembers();

        verify(syncService).syncMember(memberId1);
        verify(syncService).syncMember(memberId2);
        verify(syncService).syncMember(memberId3);
        verifyNoMoreInteractions(syncService);
    }

    private GoogleOAuthToken createTokenWithMember(UUID memberId) {
        FamilyMember member = new FamilyMember();
        member.setId(memberId);
        GoogleOAuthToken token = new GoogleOAuthToken();
        token.setMember(member);
        return token;
    }
}
