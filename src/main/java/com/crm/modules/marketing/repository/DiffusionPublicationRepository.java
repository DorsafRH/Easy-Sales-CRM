package com.crm.modules.marketing.repository;

import com.crm.modules.marketing.entity.DiffusionPublication;
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
public interface DiffusionPublicationRepository
        extends JpaRepository<DiffusionPublication, Long> {

    List<DiffusionPublication> findByPublicationId(Long publicationId);

    Optional<DiffusionPublication> findByPublicationIdAndCompteSocialId(
            Long publicationId, Long compteSocialId);
}
