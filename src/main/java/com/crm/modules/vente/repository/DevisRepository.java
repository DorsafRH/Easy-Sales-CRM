package com.crm.modules.vente.repository;

import com.crm.modules.vente.entity.Devis;
import com.crm.shared.enums.StatutDevis;
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
public interface DevisRepository
        extends JpaRepository<Devis, Long>, JpaSpecificationExecutor<Devis> {

    Optional<Devis> findByIdAndProprietaireId(Long id, Long proprietaireId);

    List<Devis> findByProprietaireIdOrderByDateCreationDesc(Long proprietaireId);

    long countByProprietaireIdAndStatut(Long proprietaireId, StatutDevis statut);

    Optional<Devis> findTopByProprietaireIdAndNumeroStartingWithOrderByNumeroDesc(
            Long proprietaireId, String prefixe);

    List<Devis> findByOpportuniteIdAndProprietaireId(Long opportuniteId, Long proprietaireId);

    boolean existsByOpportuniteIdAndStatutNotIn(Long opportuniteId, List<StatutDevis> statuts);

    /**
     * Compte les devis d'un propriétaire créés dans une fenêtre temporelle.
     * Utilisé par ReportingService pour les KPIs filtrés par période.
     */
    long countByProprietaireIdAndDateCreationBetween(
            Long proprietaireId, LocalDateTime debut, LocalDateTime fin);

    /**
     * Compte les devis dont le statut est dans la liste fournie.
     * Utilisé pour calculer le taux d'acceptation (dénominateur = non-BROUILLON).
     */
    long countByProprietaireIdAndStatutIn(
            Long proprietaireId, Collection<StatutDevis> statuts);
}