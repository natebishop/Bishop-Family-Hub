package com.familyhub.demo.repository;

import com.familyhub.demo.model.Family;
import com.familyhub.demo.model.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecipeRepository extends JpaRepository<Recipe, UUID> {

    @Query("""
            select distinct r from Recipe r
            left join fetch r.tags t
            where r.family = :family
            order by r.updatedAt desc, r.createdAt desc
            """)
    List<Recipe> findByFamilyForSummary(@Param("family") Family family);

    Optional<Recipe> findByIdAndFamily(UUID id, Family family);
}
