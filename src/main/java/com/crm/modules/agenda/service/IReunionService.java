package com.crm.modules.agenda.service;

import com.crm.modules.agenda.dto.ReunionRequest;
import com.crm.modules.agenda.dto.ReunionResponse;

import java.util.List;

/**
 * Contrat du service de gestion des réunions client.
 *
 * <p>Ce service gère le cycle de vie complet d'une réunion :
 * planification, modification, changement de statut et suppression.
 * Chaque opération est cloisonnée par {@code proprietaireId}
 * (multi-tenant).</p>
 *
 * @author Riahi Dorsaf
 * @see ReunionService
 */
public interface IReunionService {

    /**
     * Retourne toutes les réunions du propriétaire connecté,
     * triées par date croissante.
     *
     * @param proprietaireId identifiant du propriétaire connecté
     * @return liste de toutes les réunions
     */
    List<ReunionResponse> lister(Long proprietaireId);

    /**
     * Retourne les réunions d'une semaine donnée.
     * La semaine est calculée côté service à partir des dates fournies.
     *
     * @param proprietaireId identifiant du propriétaire connecté
     * @param debutSemaine   date ISO de début de semaine (ex: "2026-05-04")
     * @param finSemaine     date ISO de fin de semaine (ex: "2026-05-10")
     * @return liste des réunions de la semaine, triées par date
     */
    List<ReunionResponse> listerSemaine(Long proprietaireId,
                                        String debutSemaine,
                                        String finSemaine);

    /**
     * Retourne toutes les réunions d'un client spécifique.
     * Utilisé pour l'onglet "Réunions" de la fiche client.
     *
     * @param clientId       identifiant du client
     * @param proprietaireId identifiant du propriétaire connecté
     * @return liste des réunions du client, triées par date
     */
    List<ReunionResponse> listerParClient(Long clientId, Long proprietaireId);

    /**
     * Retourne le détail d'une réunion.
     *
     * @param id             identifiant de la réunion
     * @param proprietaireId identifiant du propriétaire connecté
     * @return le détail de la réunion
     * @throws com.crm.shared.exception.ResourceNotFoundException si introuvable
     */
    ReunionResponse obtenir(Long id, Long proprietaireId);

    /**
     * Crée une nouvelle réunion.
     *
     * <p>Vérifie que le client appartient au propriétaire.
     * Si un contact est fourni, vérifie qu'il appartient au même client.</p>
     *
     * @param request        données de la réunion à créer
     * @param proprietaireId identifiant du propriétaire connecté
     * @return la réunion créée
     * @throws com.crm.shared.exception.ResourceNotFoundException si client ou contact introuvable
     */
    ReunionResponse creer(ReunionRequest request, Long proprietaireId);

    /**
     * Modifie une réunion existante.
     *
     * <p>Seules les réunions au statut {@code PLANIFIEE} peuvent être modifiées.</p>
     *
     * @param id             identifiant de la réunion à modifier
     * @param request        nouvelles données
     * @param proprietaireId identifiant du propriétaire connecté
     * @return la réunion mise à jour
     * @throws com.crm.shared.exception.BusinessException         si la réunion n'est pas PLANIFIEE
     * @throws com.crm.shared.exception.ResourceNotFoundException si introuvable
     */
    ReunionResponse modifier(Long id, ReunionRequest request, Long proprietaireId);

    /**
     * Marque une réunion comme terminée (statut → TERMINEE).
     *
     * @param id             identifiant de la réunion
     * @param proprietaireId identifiant du propriétaire connecté
     * @throws com.crm.shared.exception.BusinessException         si déjà TERMINEE ou ANNULEE
     * @throws com.crm.shared.exception.ResourceNotFoundException si introuvable
     */
    void terminer(Long id, Long proprietaireId);

    /**
     * Annule une réunion (statut → ANNULEE).
     *
     * @param id             identifiant de la réunion
     * @param proprietaireId identifiant du propriétaire connecté
     * @throws com.crm.shared.exception.BusinessException         si déjà ANNULEE ou TERMINEE
     * @throws com.crm.shared.exception.ResourceNotFoundException si introuvable
     */
    void annuler(Long id, Long proprietaireId);

    /**
     * Supprime définitivement une réunion.
     *
     * <p>Contrairement aux clients et produits, les réunions
     * sont supprimées physiquement (pas de soft delete).</p>
     *
     * @param id             identifiant de la réunion à supprimer
     * @param proprietaireId identifiant du propriétaire connecté
     * @throws com.crm.shared.exception.ResourceNotFoundException si introuvable
     */
    void supprimer(Long id, Long proprietaireId);
}