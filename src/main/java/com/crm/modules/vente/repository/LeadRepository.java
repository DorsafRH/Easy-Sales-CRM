package com.crm.modules.vente.repository;

import com.crm.modules.vente.entity.Lead;
import com.crm.shared.enums.SourceLead;
import com.crm.shared.enums.StatutLead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Zéro @Query — méthodes dérivées Spring Data uniquement.
 *
 * @author Riahi Dorsaf
 */
@Repository
public interface LeadRepository
        extends JpaRepository<Lead, Long>, JpaSpecificationExecutor<Lead> {

    Optional<Lead> findByIdAndProprietaireId(Long id, Long proprietaireId);

    List<Lead> findByProprietaireIdAndStatutOrderByDateCreationDesc(
            Long proprietaireId, StatutLead statut);

    List<Lead> findByProprietaireIdOrderByDateCreationDesc(Long proprietaireId);

    long countByProprietaireId(Long proprietaireId);

    long countByProprietaireIdAndStatut(Long proprietaireId, StatutLead statut);

    /**
     * Compte les leads dont le statut n'est PAS dans la liste fournie.
     * Utilisé pour comptabiliser les leads "actifs" (hors CONVERTI et PERDU).
     */
    long countByProprietaireIdAndStatutNotIn(
            Long proprietaireId, Collection<StatutLead> statuts);

    // ── Reporting commercial automatisé (fenêtres période écoulée) ────────────

    /** Nombre de leads créés dans la fenêtre [debut, fin]. */
    long countByProprietaireIdAndDateCreationBetween(
            Long proprietaireId, LocalDateTime debut, LocalDateTime fin);

    /** Nombre de leads créés dans la fenêtre et ayant le statut donné (ex. CONVERTI). */
    long countByProprietaireIdAndStatutAndDateCreationBetween(
            Long proprietaireId, StatutLead statut, LocalDateTime debut, LocalDateTime fin);

    /** Nombre de leads créés dans la fenêtre pour une source donnée. */
    long countByProprietaireIdAndSourceAndDateCreationBetween(
            Long proprietaireId, SourceLead source, LocalDateTime debut, LocalDateTime fin);
}