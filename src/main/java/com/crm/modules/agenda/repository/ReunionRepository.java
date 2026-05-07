package com.crm.modules.agenda.repository;

import com.crm.modules.agenda.entity.Reunion;
import com.crm.shared.enums.StatutReunion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository JPA pour l'entité {@link Reunion}.
 *
 * <p>Aucune {@code @Query} — tout passe par des méthodes dérivées
 * Spring Data afin de garantir la détection des erreurs à la
 * compilation.</p>
 *
 * @author Riahi Dorsaf
 */
@Repository
public interface ReunionRepository extends JpaRepository<Reunion, Long> {

    /**
     * Recherche sécurisée multi-tenant : id + proprietaire.
     *
     * @param id             identifiant de la réunion
     * @param proprietaireId identifiant du propriétaire connecté
     * @return la réunion ou {@link Optional#empty()}
     */
    Optional<Reunion> findByIdAndProprietaireId(Long id, Long proprietaireId);

    /**
     * Retourne toutes les réunions d'un propriétaire,
     * triées par date croissante.
     *
     * @param proprietaireId identifiant du propriétaire
     * @return liste des réunions triées par date
     */
    List<Reunion> findByProprietaireIdOrderByDateHeureAsc(Long proprietaireId);

    /**
     * Retourne les réunions d'un propriétaire filtrées par statut.
     *
     * @param proprietaireId identifiant du propriétaire
     * @param statut         statut de filtrage
     * @return liste filtrée triée par date
     */
    List<Reunion> findByProprietaireIdAndStatutOrderByDateHeureAsc(
            Long proprietaireId, StatutReunion statut);

    /**
     * Retourne les réunions d'un client spécifique.
     * Utilisé pour afficher l'agenda depuis la fiche client.
     *
     * @param clientId       identifiant du client
     * @param proprietaireId identifiant du propriétaire (contrôle d'accès)
     * @return liste des réunions du client triées par date
     */
    List<Reunion> findByClientIdAndProprietaireIdOrderByDateHeureAsc(
            Long clientId, Long proprietaireId);

    /**
     * Retourne les réunions d'une période donnée (ex : semaine courante).
     * Utilisé pour l'écran Agenda hebdomadaire.
     *
     * @param proprietaireId identifiant du propriétaire
     * @param debut          début de la période (inclus)
     * @param fin            fin de la période (inclus)
     * @return liste des réunions dans la période, triées par date
     */
    List<Reunion> findByProprietaireIdAndDateHeureBetweenOrderByDateHeureAsc(
            Long proprietaireId, LocalDateTime debut, LocalDateTime fin);

    /**
     * Retourne les réunions à venir d'un propriétaire.
     * Utilisé pour le widget Dashboard Sprint 3.
     *
     * @param proprietaireId identifiant du propriétaire
     * @param maintenant     date/heure de référence (LocalDateTime.now())
     * @param statut         statut (généralement PLANIFIEE)
     * @return liste des réunions à venir triées par date
     */
    List<Reunion> findByProprietaireIdAndDateHeureAfterAndStatutOrderByDateHeureAsc(
            Long proprietaireId, LocalDateTime maintenant, StatutReunion statut);
}