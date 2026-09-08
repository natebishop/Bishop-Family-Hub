package com.familyhub.demo.repository;

import com.familyhub.demo.model.Family;
import com.familyhub.demo.model.ListKind;
import com.familyhub.demo.model.SharedList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SharedListRepository extends JpaRepository<SharedList, UUID> {

    @Query("""
            select distinct l from SharedList l
            left join fetch l.items item
            left join fetch item.category
            where l.family = :family
            order by l.createdAt desc
            """)
    List<SharedList> findByFamilyWithItems(@Param("family") Family family);

    @Query("""
            select distinct l from SharedList l
            left join fetch l.items item
            left join fetch item.category
            where l.family = :family
              and l.id = :id
            """)
    Optional<SharedList> findDetailByFamilyAndId(@Param("family") Family family, @Param("id") UUID id);

    @Query("select l.kind from SharedList l where l.family = :family and l.id = :id")
    Optional<ListKind> findKindByFamilyAndId(@Param("family") Family family, @Param("id") UUID id);

    @Query("""
            select count(list) from SharedList list
            where list.family = :family
              and list.kind = :kind
              and list.categoryDisplayMode = com.familyhub.demo.model.ListCategoryDisplayMode.GROUPED
            """)
    long countGroupedLists(@Param("family") Family family, @Param("kind") ListKind kind);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update SharedList list
            set list.categoryDisplayMode = com.familyhub.demo.model.ListCategoryDisplayMode.FLAT,
                list.updatedAt = CURRENT_TIMESTAMP
            where list.family = :family
              and list.kind = :kind
              and list.categoryDisplayMode = com.familyhub.demo.model.ListCategoryDisplayMode.GROUPED
            """)
    int flattenGroupedLists(@Param("family") Family family, @Param("kind") ListKind kind);
}
