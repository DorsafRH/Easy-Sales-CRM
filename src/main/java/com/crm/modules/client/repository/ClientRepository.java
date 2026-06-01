package com.crm.modules.client.repository;

import com.crm.modules.client.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository JPA pour les clients.
 * Aucune @Query — tout passe par des méthodes dérivées Spring Data
 * afin de garantir la détection des erreurs à la compilation.
 *
 * @author Riahi Dorsaf
 */
@Repository
public interface ClientRepository
        extends JpaRepository<Client, Long>,
        JpaSpecificationExecutor<Client> {

    /**
     * Recherche sécurisée multi-tenant : id + proprietaire + non supprimé.
     */
    Optional<Client> findByIdAndProprietaireIdAndIsDeletedFalse(
            Long id, Long proprietaireId);

    /**
     * Vérifie l'unicité d'un email pour un propriétaire donné.
     * isDeleted = false : on autorise la réutilisation d'un email
     * d'un client supprimé logiquement.
     */
    boolean existsByEmailAndProprietaireIdAndIsDeletedFalse(
            String email, Long proprietaireId);

    /**
     * Compte les clients actifs d'un propriétaire.
     * Utilisé par ReportingService (KPI nbClients — toutes périodes).
     */
    long countByProprietaireIdAndIsDeletedFalseAndStatut(
            Long proprietaireId, String statut);

    /**
     * Retourne les 5 clients les plus récemment créés (activité récente dashboard).
     */
    List<Client> findTop5ByProprietaireIdAndIsDeletedFalseOrderByDateCreationDesc(
            Long proprietaireId);

    // ── Ajout Sprint 2 — filtre par période ──────────────────

    /**
     * Compte les clients actifs créés après une date donnée.
     * Utilisé par ReportingService pour filtrer les KPIs par période
     * (AUJOURD_HUI / CE_MOIS / CETTE_ANNEE).
     *
     * @param proprietaireId identifiant du propriétaire
     * @param statut         statut du client (ex: "ACTIF")
     * @param since          date de début de la période (inclus)
     */
    long countByProprietaireIdAndIsDeletedFalseAndStatutAndDateCreationAfter(
            Long proprietaireId, String statut, LocalDateTime since);

    /**
     * Compte les clients d'un propriétaire créés dans une fenêtre temporelle.
     * Utilisé par ReportingService pour les KPIs filtrés par période.
     */
    long countByProprietaireIdAndDateCreationBetween(
            Long proprietaireId, LocalDateTime debut, LocalDateTime fin);
}