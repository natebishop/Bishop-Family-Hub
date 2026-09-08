package com.familyhub.demo.service;

import com.familyhub.demo.model.ChorePeriodCompletion;
import com.familyhub.demo.model.ChoreTemplate;
import com.familyhub.demo.repository.ChorePeriodCompletionRepository;
import com.familyhub.demo.repository.ChoreTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChorePeriodCompletionWriter {
    private final ChorePeriodCompletionRepository chorePeriodCompletionRepository;
    private final ChoreTemplateRepository choreTemplateRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ChorePeriodCompletion createCompletion(
            UUID templateId,
            LocalDate periodStartDate,
            LocalDate periodEndDate,
            LocalDateTime completedAt
    ) {
        ChoreTemplate template = choreTemplateRepository.getReferenceById(templateId);

        ChorePeriodCompletion completion = new ChorePeriodCompletion();
        completion.setChoreTemplate(template);
        completion.setPeriodStartDate(periodStartDate);
        completion.setPeriodEndDate(periodEndDate);
        completion.setCompletedAt(completedAt);

        return chorePeriodCompletionRepository.saveAndFlush(completion);
    }
}
