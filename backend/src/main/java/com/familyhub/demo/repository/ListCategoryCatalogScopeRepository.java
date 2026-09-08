package com.familyhub.demo.repository;

import com.familyhub.demo.model.Family;
import com.familyhub.demo.model.ListCategoryCatalogScope;
import com.familyhub.demo.model.ListKind;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ListCategoryCatalogScopeRepository extends JpaRepository<ListCategoryCatalogScope, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select scope from ListCategoryCatalogScope scope
            where scope.family = :family and scope.kind = :kind
            """)
    Optional<ListCategoryCatalogScope> lockByFamilyAndKind(@Param("family") Family family, @Param("kind") ListKind kind);

    // Family-scoped route-and-lock: resolves the category's kind and locks the matching scope row in one
    // statement. The implicit join is intentional (matches the catalog-scope locking design).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select scope from ListCategoryCatalogScope scope, ListCategory category
            where category.family = :family
              and category.id = :categoryId
              and scope.family = :family
              and scope.kind = category.kind
            """)
    Optional<ListCategoryCatalogScope> lockByFamilyAndCategoryId(@Param("family") Family family, @Param("categoryId") UUID categoryId);
}
