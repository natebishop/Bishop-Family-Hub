package com.familyhub.demo.repository;

import com.familyhub.demo.model.ListKind;
import com.familyhub.demo.model.SharedListItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface SharedListItemRepository extends JpaRepository<SharedListItem, UUID> {

    public interface CategoryUsageCount {
        UUID getCategoryId();
        long getItemCount();
    }

    @Query("""
            select item.category.id as categoryId, count(item.id) as itemCount
            from SharedListItem item
            where item.familyId = :familyId
              and item.listKind = :kind
              and item.category is not null
            group by item.category.id
            """)
    List<CategoryUsageCount> countUsage(@Param("familyId") UUID familyId, @Param("kind") ListKind kind);

    @Query("""
            select count(item.id) from SharedListItem item
            where item.familyId = :familyId
              and item.category.id = :categoryId
            """)
    long countByCategory(@Param("familyId") UUID familyId, @Param("categoryId") UUID categoryId);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update SharedListItem item
            set item.category = null, item.updatedAt = CURRENT_TIMESTAMP
            where item.familyId = :familyId
              and item.listKind = :kind
              and item.category.id = :categoryId
            """)
    int clearCategory(@Param("familyId") UUID familyId, @Param("kind") ListKind kind, @Param("categoryId") UUID categoryId);
}
