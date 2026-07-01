package com.crm.modules.marketing.service;

import com.crm.modules.marketing.dto.request.AmeliorerContenuRequestDTO;
import com.crm.modules.marketing.dto.request.GenererContenuRequestDTO;
import com.crm.modules.marketing.dto.request.GenererPublicationRequestDTO;
import com.crm.modules.marketing.dto.request.PublicationRequestDTO;
import com.crm.modules.marketing.dto.response.CompteSocialResponseDTO;
import com.crm.modules.marketing.dto.response.GenererContenuResponseDTO;
import com.crm.modules.marketing.dto.response.PublicationResponseDTO;

import java.util.List;

/**
 * @author Riahi Dorsaf
 */
public interface IMarketingService {

    /**
     * Génère du contenu marketing via l'IA (Groq, pattern Generator → Critic).
     *
     * @param request paramètres de génération (sujet, type, tonalité, langue, mots-clés)
     * @return le contenu généré et la version améliorée
     */
    GenererContenuResponseDTO genererContenu(GenererContenuRequestDTO request);

    /**
     * Génère une publication Facebook pilotée par le catalogue : selon la portée choisie,
     * le backend récupère les produits/catégorie, calcule les prix promo, construit le prompt
     * et délègue la rédaction au LLM. L'utilisateur ne ressaisit jamais les données produit.
     *
     * @param request        portée + sélection + remise + consigne + tonalité/langue
     * @param proprietaireId identifiant du propriétaire courant (multi-tenant)
     * @return le contenu généré et la version améliorée
     */
    GenererContenuResponseDTO genererPublication(GenererPublicationRequestDTO request,
                                                 Long proprietaireId);

    /**
     * Améliore un texte de publication déjà rédigé (raffinage itératif par l'IA).
     * Peut être relancé autant de fois que souhaité jusqu'au résultat voulu.
     *
     * @param request texte courant + consigne/tonalité facultatives
     * @return le texte amélioré
     */
    GenererContenuResponseDTO ameliorerContenu(AmeliorerContenuRequestDTO request);

    /**
     * Crée une nouvelle publication marketing pour le propriétaire courant.
     *
     * @param request       données de la publication et réseaux ciblés
     * @param proprietaireId identifiant du propriétaire courant
     * @return la publication créée
     */
    PublicationResponseDTO creerPublication(PublicationRequestDTO request, Long proprietaireId);

    /**
     * Liste les publications du propriétaire, des plus récentes aux plus anciennes.
     *
     * @param proprietaireId identifiant du propriétaire courant
     * @return la liste des publications
     */
    List<PublicationResponseDTO> listerPublications(Long proprietaireId);

    /**
     * Récupère le détail d'une publication.
     *
     * @param id            identifiant de la publication
     * @param proprietaireId identifiant du propriétaire courant
     * @return la publication demandée
     */
    PublicationResponseDTO obtenirPublication(Long id, Long proprietaireId);

    /**
     * Modifie une publication à l'état BROUILLON uniquement.
     *
     * @param id            identifiant de la publication
     * @param request       nouvelles données
     * @param proprietaireId identifiant du propriétaire courant
     * @return la publication modifiée
     */
    PublicationResponseDTO modifierPublication(Long id, PublicationRequestDTO request,
                                               Long proprietaireId);

    /**
     * Supprime une publication.
     *
     * @param id            identifiant de la publication
     * @param proprietaireId identifiant du propriétaire courant
     */
    void supprimerPublication(Long id, Long proprietaireId);

    /**
     * Publie immédiatement une publication sur ses réseaux ciblés (API Graph),
     * puis met à jour son statut selon le résultat des diffusions.
     *
     * @param id            identifiant de la publication
     * @param proprietaireId identifiant du propriétaire courant
     * @return la publication après diffusion (PUBLIEE, ECHEC…)
     */
    PublicationResponseDTO publier(Long id, Long proprietaireId);

    /**
     * Publie les publications PROGRAMMEE dont la date de programmation est échue.
     * Appelée périodiquement par le scheduler, hors contexte propriétaire.
     */
    void publierPublicationsProgrammees();

    /**
     * Annule une publication et ses diffusions.
     *
     * @param id            identifiant de la publication
     * @param proprietaireId identifiant du propriétaire courant
     * @return la publication annulée
     */
    PublicationResponseDTO annuler(Long id, Long proprietaireId);

    /**
     * Liste les comptes sociaux connectés du propriétaire.
     *
     * @param proprietaireId identifiant du propriétaire courant
     * @return la liste des comptes connectés
     */
    List<CompteSocialResponseDTO> listerReseaux(Long proprietaireId);

    /**
     * Déconnecte (supprime) un compte social connecté.
     *
     * @param id            identifiant du compte social
     * @param proprietaireId identifiant du propriétaire courant
     */
    void deconnecterCompte(Long id, Long proprietaireId);
}
