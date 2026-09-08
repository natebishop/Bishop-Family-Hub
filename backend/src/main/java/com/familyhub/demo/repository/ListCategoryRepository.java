package com.familyhub.demo.repository;

import com.familyhub.demo.model.Family;
import com.familyhub.demo.model.ListCategory;
import com.familyhub.demo.model.ListKind;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ListCategoryRepository extends JpaRepository<ListCategory, UUID> {

    Optional<ListCategory> findByFamilyAndId(Family family, UUID id);

    List<ListCategory> findByFamilyAndKindOrderBySortOrderAsc(Family family, ListKind kind);

    @Query("""
            select case when count(category) > 0 then true else false end
            from ListCategory category
            where category.family = :family
              and category.kind = :kind
              and lower(trim(category.name)) = lower(trim(:name))
              and (:excludeId is null or category.id <> :excludeId)
            """)
    boolean existsByNormalizedName(@Param("family") Family family, @Param("kind") ListKind kind,
                                   @Param("name") String name, @Param("excludeId") UUID excludeId);

    long countByFamilyAndKind(Family family, ListKind kind);
}
