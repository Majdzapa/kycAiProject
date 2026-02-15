package com.kyc.ai.repository;

import com.kyc.ai.entity.KnowledgeBase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, UUID> {

    List<KnowledgeBase> findByCategoryOrderByCreatedAtDesc(KnowledgeBase.Category category);

    @Query("SELECT k FROM KnowledgeBase k WHERE k.language = :language ORDER BY k.createdAt DESC")
    List<KnowledgeBase> findByLanguage(@Param("language") String language);

    @Query("SELECT k FROM KnowledgeBase k WHERE k.category = :category AND k.language = :language")
    List<KnowledgeBase> findByCategoryAndLanguage(@Param("category") KnowledgeBase.Category category,
            @Param("language") String language);

    Optional<KnowledgeBase> findByTitleAndVersion(String title, String version);

    @Query("SELECT k FROM KnowledgeBase k WHERE k.title LIKE %:searchTerm% OR k.text LIKE %:searchTerm%")
    List<KnowledgeBase> searchByContent(@Param("searchTerm") String searchTerm);

    @Query("SELECT DISTINCT k.category FROM KnowledgeBase k")
    List<KnowledgeBase.Category> findAllCategories();

    @Query("SELECT k FROM KnowledgeBase k WHERE k.effectiveDate <= CURRENT_DATE ORDER BY k.effectiveDate DESC")
    List<KnowledgeBase> findActiveKnowledge();
}
