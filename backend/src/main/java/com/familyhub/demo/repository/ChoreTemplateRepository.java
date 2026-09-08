package com.familyhub.demo.repository;

import com.familyhub.demo.model.ChoreTemplate;
import com.familyhub.demo.model.Family;
import com.familyhub.demo.model.FamilyMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChoreTemplateRepository extends JpaRepository<ChoreTemplate, UUID> {
    @Query("""
            select ct
            from ChoreTemplate ct
            join fetch ct.assignedToMember m
            where ct.family = :family
              and ct.archivedAt is null
            order by lower(m.name), ct.createdAt, lower(ct.title)
            """)
    List<ChoreTemplate> findActiveByFamily(@Param("family") Family family);

    Optional<ChoreTemplate> findByFamilyAndId(Family family, UUID id);

    boolean existsByAssignedToMemberAndArchivedAtIsNull(FamilyMember member);
}
