package com.crm.modules.marketing.repository;

import com.crm.modules.marketing.entity.ReactionPublication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Zéro @Query — méthodes dérivées Spring Data uniquement.
 *
 * @author Riahi Dorsaf
 */
@Repository
public interface ReactionPublicationRepository
        extends JpaRepository<ReactionPublication, Long> {

    List<ReactionPublication> findByProprietaireId(Long proprietaireId);

    Optional<ReactionPublication> findByProprietaireIdAndPostId(Long proprietaireId, String postId);

    List<ReactionPublication> findTop5ByProprietaireIdOrderByLikesDesc(Long proprietaireId);
}
