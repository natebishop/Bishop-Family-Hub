package com.familyhub.demo.service;

import com.familyhub.demo.dto.ChoreBoardItemResponse;
import com.familyhub.demo.dto.ChoreBoardResponse;
import com.familyhub.demo.dto.ChoreCurrentPeriodStateResponse;
import com.familyhub.demo.dto.ChoreTemplateResponse;
import com.familyhub.demo.dto.CreateChoreTemplateRequest;
import com.familyhub.demo.dto.UpdateChoreTemplateRequest;
import com.familyhub.demo.dto.UpdateCurrentPeriodCompletionRequest;
import com.familyhub.demo.exception.BadRequestException;
import com.familyhub.demo.model.ChoreCadence;
import com.familyhub.demo.model.ChorePeriodCompletion;
import com.familyhub.demo.model.ChoreScope;
import com.familyhub.demo.model.ChoreTemplate;
import com.familyhub.demo.model.Family;
import com.familyhub.demo.model.FamilyMember;
import com.familyhub.demo.repository.ChorePeriodCompletionRepository;
import com.familyhub.demo.repository.ChoreTemplateRepository;
import com.familyhub.demo.repository.FamilyMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static com.familyhub.demo.TestDataFactory.CHORE_TEMPLATE_ID;
import static com.familyhub.demo.TestDataFactory.MEMBER_ID;
import static com.familyhub.demo.TestDataFactory.createChoreTemplate;
import static com.familyhub.demo.TestDataFactory.createFamily;
import static com.familyhub.demo.TestDataFactory.createFamilyMember;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChoreServiceTest {

    @Mock
    private ChoreTemplateRepository choreTemplateRepository;

    @Mock
    private ChorePeriodCompletionRepository chorePeriodCompletionRepository;

    @Mock
    private ChorePeriodCompletionWriter chorePeriodCompletionWriter;

    @Mock
    private FamilyMemberRepository familyMemberRepository;

    private ChoreService choreService;
    private Family family;
    private FamilyMember member;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-05-17T16:00:00Z"), ZoneOffset.UTC);
        choreService = new ChoreService(
                choreTemplateRepository,
                chorePeriodCompletionRepository,
                chorePeriodCompletionWriter,
                familyMemberRepository,
                clock
        );
        family = createFamily();
        family.setTimezone("America/Los_Angeles");
        member = createFamilyMember(family);
    }

    @Test
    void getBoard_groupsTemplatesIntoTodayWeekAndMonth() {
        ChoreTemplate daily = createChoreTemplate(
                family,
                member,
                "Brush teeth",
                ChoreCadence.DAILY,
                LocalDate.of(2026, 5, 17)
        );
        ChoreTemplate weekly = createChoreTemplate(
                family,
                member,
                "Take out trash",
                ChoreCadence.WEEKLY,
                LocalDate.of(2026, 5, 17)
        );
        ChoreTemplate monthly = createChoreTemplate(
                family,
                member,
                "Deep clean fridge",
                ChoreCadence.MONTHLY,
                LocalDate.of(2026, 5, 17)
        );

        when(choreTemplateRepository.findActiveByFamily(family)).thenReturn(List.of(daily, weekly, monthly));
        when(chorePeriodCompletionRepository.findByTemplateIdsAndPeriod(any(), any(), any())).thenReturn(List.of());

        ChoreBoardResponse board = choreService.getBoard(family);

        assertThat(board.timezone()).isEqualTo("America/Los_Angeles");
        assertThat(board.today().periodStartDate()).isEqualTo(LocalDate.of(2026, 5, 17));
        assertThat(board.thisWeek().periodStartDate()).isEqualTo(LocalDate.of(2026, 5, 17));
        assertThat(board.thisWeek().periodEndDate()).isEqualTo(LocalDate.of(2026, 5, 23));
        assertThat(board.thisMonth().periodStartDate()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(board.thisMonth().periodEndDate()).isEqualTo(LocalDate.of(2026, 5, 31));
        assertThat(board.today().summary().total()).isEqualTo(1);
        assertThat(board.thisWeek().summary().total()).isEqualTo(1);
        assertThat(board.thisMonth().summary().total()).isEqualTo(1);
    }

    @Test
    void getBoard_excludesTemplateBeforeActiveFromDate() {
        ChoreTemplate futureMonthly = createChoreTemplate(
                family,
                member,
                "Deep clean fridge",
                ChoreCadence.MONTHLY,
                LocalDate.of(2026, 6, 1)
        );

        when(choreTemplateRepository.findActiveByFamily(family)).thenReturn(List.of(futureMonthly));

        ChoreBoardResponse board = choreService.getBoard(family);

        assertThat(board.thisMonth().summary().total()).isZero();
    }

    @Test
    void getBoard_withInvalidStoredTimezone_fallsBackToPacificDefault() {
        family.setTimezone("Mars/Olympus");
        when(choreTemplateRepository.findActiveByFamily(family)).thenReturn(List.of());

        ChoreBoardResponse board = choreService.getBoard(family);

        assertThat(board.timezone()).isEqualTo("America/Los_Angeles");
    }

    @Test
    void getBoard_excludesWeeklyAndMonthlyTemplatesUntilTheirActiveFromDateWithinCurrentPeriod() {
        ChoreTemplate weeklyLaterThisWeek = createChoreTemplate(
                family,
                member,
                "Take out trash",
                ChoreCadence.WEEKLY,
                LocalDate.of(2026, 5, 20)
        );
        ChoreTemplate monthlyLaterThisMonth = createChoreTemplate(
                family,
                member,
                "Deep clean fridge",
                ChoreCadence.MONTHLY,
                LocalDate.of(2026, 5, 25)
        );

        when(choreTemplateRepository.findActiveByFamily(family))
                .thenReturn(List.of(weeklyLaterThisWeek, monthlyLaterThisMonth));

        ChoreBoardResponse board = choreService.getBoard(family);

        assertThat(board.thisWeek().summary().total()).isZero();
        assertThat(board.thisMonth().summary().total()).isZero();
        verifyNoInteractions(chorePeriodCompletionRepository);
    }

    @Test
    void getBoard_ordersIncompleteBeforeCompleted_thenByCreatedAtAndTitle() {
        ChoreTemplate bravo = createChoreTemplate(
                family,
                member,
                "Bravo",
                ChoreCadence.DAILY,
                LocalDate.of(2026, 5, 17)
        );
        bravo.setCreatedAt(LocalDateTime.of(2026, 5, 17, 9, 0));

        ChoreTemplate alpha = createChoreTemplate(
                family,
                member,
                "Alpha",
                ChoreCadence.DAILY,
                LocalDate.of(2026, 5, 17)
        );
        alpha.setCreatedAt(LocalDateTime.of(2026, 5, 17, 8, 0));

        ChoreTemplate completed = createChoreTemplate(
                family,
                member,
                "Completed",
                ChoreCadence.DAILY,
                LocalDate.of(2026, 5, 17)
        );
        completed.setCreatedAt(LocalDateTime.of(2026, 5, 17, 7, 0));

        ChorePeriodCompletion completion = new ChorePeriodCompletion();
        completion.setChoreTemplate(completed);
        completion.setPeriodStartDate(LocalDate.of(2026, 5, 17));
        completion.setPeriodEndDate(LocalDate.of(2026, 5, 17));
        completion.setCompletedAt(LocalDateTime.of(2026, 5, 17, 10, 0));

        when(choreTemplateRepository.findActiveByFamily(family)).thenReturn(List.of(bravo, completed, alpha));
        when(chorePeriodCompletionRepository.findByTemplateIdsAndPeriod(any(), any(), any()))
                .thenReturn(List.of(completion));

        ChoreBoardResponse board = choreService.getBoard(family);

        assertThat(board.today().assignees().getFirst().chores())
                .extracting(ChoreBoardItemResponse::title)
                .containsExactly("Alpha", "Bravo", "Completed");
        assertThat(board.today().assignees().getFirst().chores().get(2).completed()).isTrue();
    }

    @Test
    void createTemplate_savesRecurringTemplate() {
        CreateChoreTemplateRequest request = new CreateChoreTemplateRequest(
                " Brush teeth ",
                MEMBER_ID,
                ChoreCadence.DAILY,
                LocalDate.of(2026, 5, 17)
        );
        when(familyMemberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
        when(choreTemplateRepository.save(any(ChoreTemplate.class))).thenAnswer(invocation -> {
            ChoreTemplate saved = invocation.getArgument(0);
            saved.setId(CHORE_TEMPLATE_ID);
            saved.setCreatedAt(LocalDateTime.of(2026, 5, 17, 8, 0));
            saved.setUpdatedAt(LocalDateTime.of(2026, 5, 17, 8, 0));
            return saved;
        });

        ChoreTemplateResponse response = choreService.createTemplate(request, family);

        assertThat(response.id()).isEqualTo(CHORE_TEMPLATE_ID);
        assertThat(response.title()).isEqualTo("Brush teeth");
        assertThat(response.cadence()).isEqualTo(ChoreCadence.DAILY);
        assertThat(response.activeFrom()).isEqualTo(LocalDate.of(2026, 5, 17));
        assertThat(response.archived()).isFalse();
    }

    @Test
    void updateTemplate_updatesEditableFieldsAndArchives() {
        FamilyMember newAssignee = createFamilyMember(family);
        newAssignee.setId(MEMBER_ID);
        ChoreTemplate template = createChoreTemplate(
                family,
                member,
                "Brush teeth",
                ChoreCadence.DAILY,
                LocalDate.of(2026, 5, 17)
        );
        when(choreTemplateRepository.findByFamilyAndId(family, template.getId())).thenReturn(Optional.of(template));
        when(familyMemberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(newAssignee));
        when(choreTemplateRepository.save(template)).thenReturn(template);

        ChoreTemplateResponse response = choreService.updateTemplate(
                template.getId(),
                new UpdateChoreTemplateRequest(
                        "Take out trash",
                        MEMBER_ID,
                        ChoreCadence.WEEKLY,
                        LocalDate.of(2026, 5, 18),
                        true
                ),
                family
        );

        assertThat(response.title()).isEqualTo("Take out trash");
        assertThat(response.cadence()).isEqualTo(ChoreCadence.WEEKLY);
        assertThat(response.activeFrom()).isEqualTo(LocalDate.of(2026, 5, 18));
        assertThat(response.archived()).isTrue();
        assertThat(template.getArchivedAt()).isEqualTo(LocalDateTime.of(2026, 5, 17, 16, 0));
    }

    @Test
    void completeCurrentPeriod_createsCompletionForCurrentPeriod() {
        ChoreTemplate daily = createChoreTemplate(
                family,
                member,
                "Brush teeth",
                ChoreCadence.DAILY,
                LocalDate.of(2026, 5, 17)
        );
        when(choreTemplateRepository.findByFamilyAndId(family, daily.getId())).thenReturn(Optional.of(daily));
        when(chorePeriodCompletionRepository.findByChoreTemplateAndPeriodStartDateAndPeriodEndDate(
                daily,
                LocalDate.of(2026, 5, 17),
                LocalDate.of(2026, 5, 17)
        )).thenReturn(Optional.empty());
        ChorePeriodCompletion savedCompletion = new ChorePeriodCompletion();
        savedCompletion.setChoreTemplate(daily);
        savedCompletion.setPeriodStartDate(LocalDate.of(2026, 5, 17));
        savedCompletion.setPeriodEndDate(LocalDate.of(2026, 5, 17));
        savedCompletion.setCompletedAt(LocalDateTime.of(2026, 5, 17, 16, 0));
        when(chorePeriodCompletionWriter.createCompletion(
                daily.getId(),
                LocalDate.of(2026, 5, 17),
                LocalDate.of(2026, 5, 17),
                LocalDateTime.of(2026, 5, 17, 16, 0)
        )).thenReturn(savedCompletion);

        ChoreCurrentPeriodStateResponse response = choreService.completeCurrentPeriod(
                daily.getId(),
                new UpdateCurrentPeriodCompletionRequest(ChoreScope.TODAY, LocalDate.of(2026, 5, 17)),
                family
        );

        assertThat(response.item().completed()).isTrue();
        assertThat(response.item().completedAt()).isEqualTo(LocalDateTime.of(2026, 5, 17, 16, 0));
    }

    @Test
    void completeCurrentPeriod_whenTemplateArchived_throwsBadRequest() {
        ChoreTemplate archived = createChoreTemplate(
                family,
                member,
                "Brush teeth",
                ChoreCadence.DAILY,
                LocalDate.of(2026, 5, 17)
        );
        archived.setArchivedAt(LocalDateTime.of(2026, 5, 17, 15, 0));
        when(choreTemplateRepository.findByFamilyAndId(family, archived.getId())).thenReturn(Optional.of(archived));

        assertThatThrownBy(() -> choreService.completeCurrentPeriod(
                archived.getId(),
                new UpdateCurrentPeriodCompletionRequest(ChoreScope.TODAY, LocalDate.of(2026, 5, 17)),
                family
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Chore template is not active for the current period.");
    }

    @Test
    void completeCurrentPeriod_whenTemplateNotYetActive_throwsBadRequest() {
        ChoreTemplate futureWeekly = createChoreTemplate(
                family,
                member,
                "Take out trash",
                ChoreCadence.WEEKLY,
                LocalDate.of(2026, 5, 20)
        );
        when(choreTemplateRepository.findByFamilyAndId(family, futureWeekly.getId()))
                .thenReturn(Optional.of(futureWeekly));

        assertThatThrownBy(() -> choreService.completeCurrentPeriod(
                futureWeekly.getId(),
                new UpdateCurrentPeriodCompletionRequest(ChoreScope.THIS_WEEK, LocalDate.of(2026, 5, 17)),
                family
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Chore template is not active for the current period.");
    }

    @Test
    void completeCurrentPeriod_duplicateInsertRace_returnsExistingCompletion() {
        ChoreTemplate daily = createChoreTemplate(
                family,
                member,
                "Brush teeth",
                ChoreCadence.DAILY,
                LocalDate.of(2026, 5, 17)
        );
        ChorePeriodCompletion existingCompletion = new ChorePeriodCompletion();
        existingCompletion.setChoreTemplate(daily);
        existingCompletion.setPeriodStartDate(LocalDate.of(2026, 5, 17));
        existingCompletion.setPeriodEndDate(LocalDate.of(2026, 5, 17));
        existingCompletion.setCompletedAt(LocalDateTime.of(2026, 5, 17, 15, 59));

        when(choreTemplateRepository.findByFamilyAndId(family, daily.getId())).thenReturn(Optional.of(daily));
        when(chorePeriodCompletionRepository.findByChoreTemplateAndPeriodStartDateAndPeriodEndDate(
                daily,
                LocalDate.of(2026, 5, 17),
                LocalDate.of(2026, 5, 17)
        )).thenReturn(Optional.empty(), Optional.of(existingCompletion));
        when(chorePeriodCompletionWriter.createCompletion(
                daily.getId(),
                LocalDate.of(2026, 5, 17),
                LocalDate.of(2026, 5, 17),
                LocalDateTime.of(2026, 5, 17, 16, 0)
        ))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        ChoreCurrentPeriodStateResponse response = choreService.completeCurrentPeriod(
                daily.getId(),
                new UpdateCurrentPeriodCompletionRequest(ChoreScope.TODAY, LocalDate.of(2026, 5, 17)),
                family
        );

        assertThat(response.item().completed()).isTrue();
        assertThat(response.item().completedAt()).isEqualTo(LocalDateTime.of(2026, 5, 17, 15, 59));
    }

    @Test
    void uncompleteCurrentPeriod_deletesCurrentCompletion() {
        ChoreTemplate daily = createChoreTemplate(
                family,
                member,
                "Brush teeth",
                ChoreCadence.DAILY,
                LocalDate.of(2026, 5, 17)
        );
        ChorePeriodCompletion completion = new ChorePeriodCompletion();
        completion.setChoreTemplate(daily);
        completion.setPeriodStartDate(LocalDate.of(2026, 5, 17));
        completion.setPeriodEndDate(LocalDate.of(2026, 5, 17));
        completion.setCompletedAt(LocalDateTime.of(2026, 5, 17, 10, 0));
        when(choreTemplateRepository.findByFamilyAndId(family, daily.getId())).thenReturn(Optional.of(daily));
        when(chorePeriodCompletionRepository.findByChoreTemplateAndPeriodStartDateAndPeriodEndDate(
                daily,
                LocalDate.of(2026, 5, 17),
                LocalDate.of(2026, 5, 17)
        )).thenReturn(Optional.of(completion));

        ChoreCurrentPeriodStateResponse response = choreService.uncompleteCurrentPeriod(
                daily.getId(),
                new UpdateCurrentPeriodCompletionRequest(ChoreScope.TODAY, LocalDate.of(2026, 5, 17)),
                family
        );

        assertThat(response.item().completed()).isFalse();
    }

    @Test
    void uncompleteCurrentPeriod_whenTemplateArchived_throwsBadRequest() {
        ChoreTemplate archived = createChoreTemplate(
                family,
                member,
                "Brush teeth",
                ChoreCadence.DAILY,
                LocalDate.of(2026, 5, 17)
        );
        archived.setArchivedAt(LocalDateTime.of(2026, 5, 17, 15, 0));
        when(choreTemplateRepository.findByFamilyAndId(family, archived.getId())).thenReturn(Optional.of(archived));

        assertThatThrownBy(() -> choreService.uncompleteCurrentPeriod(
                archived.getId(),
                new UpdateCurrentPeriodCompletionRequest(ChoreScope.TODAY, LocalDate.of(2026, 5, 17)),
                family
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Chore template is not active for the current period.");
    }

    @Test
    void uncompleteCurrentPeriod_staleRequest_throwsBadRequest() {
        ChoreTemplate daily = createChoreTemplate(
                family,
                member,
                "Brush teeth",
                ChoreCadence.DAILY,
                LocalDate.of(2026, 5, 17)
        );
        when(choreTemplateRepository.findByFamilyAndId(family, daily.getId())).thenReturn(Optional.of(daily));

        assertThatThrownBy(() -> choreService.uncompleteCurrentPeriod(
                daily.getId(),
                new UpdateCurrentPeriodCompletionRequest(ChoreScope.TODAY, LocalDate.of(2026, 5, 16)),
                family
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Chore period is stale. Refresh and try again.");
    }
}
