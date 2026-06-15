package com.crm.modules.vente.repository;

import com.crm.modules.vente.entity.ActiviteCommerciale;
import com.crm.shared.enums.ResultatActivite;
import com.crm.shared.enums.TypeActiviteCommerciale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Riahi Dorsaf
 */
@Repository
public interface ActiviteCommercialeRepository extends JpaRepository<ActiviteCommerciale, Long> {

    List<ActiviteCommerciale> findByLeadIdOrderByDateActiviteDesc(Long leadId);

    List<ActiviteCommerciale> findByOpportuniteIdOrderByDateActiviteDesc(Long opportuniteId);

    List<ActiviteCommerciale> findByProprietaireIdOrderByDateActiviteDesc(Long proprietaireId);

    // ── Reporting commercial automatisé (fenêtres période écoulée) ────────────

    /** Total d'activités réalisées dans la fenêtre [debut, fin] (date d'activité). */
    long countByProprietaireIdAndDateActiviteBetween(
            Long proprietaireId, LocalDateTime debut, LocalDateTime fin);

    /** Activités d'un type donné réalisées dans la fenêtre. */
    long countByProprietaireIdAndTypeAndDateActiviteBetween(
            Long proprietaireId, TypeActiviteCommerciale type,
            LocalDateTime debut, LocalDateTime fin);

    /** Activités d'un résultat donné réalisées dans la fenêtre. */
    long countByProprietaireIdAndResultatAndDateActiviteBetween(
            Long proprietaireId, ResultatActivite resultat,
            LocalDateTime debut, LocalDateTime fin);
}