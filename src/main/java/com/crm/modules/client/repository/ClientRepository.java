package com.crm.modules.client.repository;

import com.crm.modules.client.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

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
     * Utilisé par DashboardService (KPI nbClients).
     * Appelé avec statut = "ACTIF".
     */
    long countByProprietaireIdAndIsDeletedFalseAndStatut(
            Long proprietaireId, String statut);

    /**
     * Retourne les 5 clients les plus récemment créés (activité récente dashboard).
     */
    List<Client> findTop5ByProprietaireIdAndIsDeletedFalseOrderByDateCreationDesc(
            Long proprietaireId);
}