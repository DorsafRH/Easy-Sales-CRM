package com.crm.modules.entreprise.service;

import com.crm.modules.entreprise.dto.EntrepriseCompteResponse;
import com.crm.modules.entreprise.dto.InscriptionEntrepriseRequest;
import com.crm.modules.entreprise.dto.ValiderEntrepriseRequest;
import com.crm.shared.enums.StatutCompte;
import com.crm.shared.response.PageResponse;

/**
 * Contrat du service de gestion des comptes entreprises.
 *
 * @author Riahi Dorsaf
 * @see EntrepriseService
 */

public interface IEntrepriseService {
    /**
     * @param request les données d'inscription
     * @return le compte créé avec le statut EN_ATTENTE
     * @throws com.crm.shared.exception.BusinessException si l'e-mail ou la matricule est déjà utilisé
     */

    EntrepriseCompteResponse inscrireEntreprise(InscriptionEntrepriseRequest request);

    /**
     * @param emailProprietaire l'e-mail extrait du token JWT
     * @return les informations du compte et son statut
     * @throws com.crm.shared.exception.ResourceNotFoundException si introuvable
     */
    EntrepriseCompteResponse consulterMonStatut(String emailProprietaire);

    /**
     * @param page numéro de page (0-indexed)
     * @param size nombre d'éléments par page
     * @return la page des comptes EN_ATTENTE
     */
    PageResponse<EntrepriseCompteResponse> listerComptesEnAttente(int page, int size);

    /**
     * @param statut  filtre optionnel par statut
     * @param keyword filtre optionnel par nom ou matricule
     * @param page    numéro de page
     * @param size    taille de page
     * @return la page filtrée
     */
    PageResponse<EntrepriseCompteResponse> listerEntreprises(
            StatutCompte statut, String keyword, int page, int size);

    /**
     * @param entrepriseId l'identifiant du compte
     * @param request      décision + motif éventuel
     * @param emailAdmin   e-mail de l'admin
     * @return le compte mis à jour
     */
    EntrepriseCompteResponse traiterDemande(
            Long entrepriseId, ValiderEntrepriseRequest request, String emailAdmin);

    /**
     * @param entrepriseId l'identifiant du compte
     * @return les informations détaillées
     */
    EntrepriseCompteResponse consulterDetails(Long entrepriseId);

    /**
     * @param entrepriseId l'identifiant du compte
     * @param emailAdmin   e-mail de l'admin
     */
    void supprimerLogiquement(Long entrepriseId, String emailAdmin);
}