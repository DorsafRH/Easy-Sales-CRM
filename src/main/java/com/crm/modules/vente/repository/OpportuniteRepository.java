package com.crm.modules.vente.repository;

import com.crm.modules.vente.entity.Opportunite;
import com.crm.shared.enums.StatutOpportunite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * @author Riahi Dorsaf
 */
@Repository
public interface OpportuniteRepository
        extends JpaRepository<Opportunite, Long>, JpaSpecificationExecutor<Opportunite> {

    Optional<Opportunite> findByIdAndProprietaireId(Long id, Long proprietaireId);

    List<Opportunite> findByProprietaireIdOrderByDateCreationDesc(Long proprietaireId);

    List<Opportunite> findByProprietaireIdAndStatutOrderByMontantEstimeDesc(
            Long proprietaireId, StatutOpportunite statut);

    long countByProprietaireId(Long proprietaireId);

    long countByProprietaireIdAndStatut(Long proprietaireId, StatutOpportunite statut);

    /**
     * Compte les opportunités d'un propriétaire créées dans une fenêtre temporelle.
     * Utilisé par ReportingService pour les KPIs filtrés par période.
     */
    long countByProprietaireIdAndDateCreationBetween(
            Long proprietaireId, LocalDateTime debut, LocalDateTime fin);

    /**
     * Retourne les opportunités dont le statut est dans la liste fournie.
     * Utilisé pour calculer la valeur totale du pipeline actif.
     */
    List<Opportunite> findByProprietaireIdAndStatutIn(
            Long proprietaireId, Collection<StatutOpportunite> statuts);

    /**
     * Retourne les 3 meilleures opportunités actives triées par montantEstime décroissant.
     * Utilisé pour afficher le Top 3 dans le dashboard.
     */
    List<Opportunite> findTop3ByProprietaireIdAndStatutInOrderByMontantEstimeDesc(
            Long proprietaireId, Collection<StatutOpportunite> statuts);
}