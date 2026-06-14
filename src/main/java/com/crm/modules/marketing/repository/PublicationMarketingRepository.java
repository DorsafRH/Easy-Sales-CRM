package com.crm.modules.marketing.repository;

import com.crm.modules.marketing.entity.PublicationMarketing;
import com.crm.shared.enums.StatutPublication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Zéro @Query — méthodes dérivées Spring Data uniquement.
 *
 * @author Riahi Dorsaf
 */
@Repository
public interface PublicationMarketingRepository
        extends JpaRepository<PublicationMarketing, Long> {

    List<PublicationMarketing> findByProprietaireIdOrderByDateCreationDesc(Long proprietaireId);

    Optional<PublicationMarketing> findByIdAndProprietaireId(Long id, Long proprietaireId);

    List<PublicationMarketing> findByProprietaireIdAndStatutOrderByDateCreationDesc(
            Long proprietaireId, StatutPublication statut);

    long countByProprietaireIdAndStatut(Long proprietaireId, StatutPublication statut);

    List<PublicationMarketing> findByStatutAndDateProgrammationLessThanEqual(
            StatutPublication statut, LocalDateTime dateProgrammation);
}
