package com.crm.modules.entreprise.service;

import com.crm.modules.entreprise.dto.EntrepriseCompteResponse;
import com.crm.modules.entreprise.dto.InscriptionEntrepriseRequest;
import com.crm.modules.entreprise.dto.ValiderEntrepriseRequest;
import com.crm.shared.enums.StatutCompte;
import com.crm.shared.response.PageResponse;

public interface IEntrepriseService {

    EntrepriseCompteResponse inscrireEntreprise(InscriptionEntrepriseRequest request);

    EntrepriseCompteResponse consulterMonStatut(String emailProprietaire);

    PageResponse<EntrepriseCompteResponse> listerComptesEnAttente(int page, int size);

    PageResponse<EntrepriseCompteResponse> listerEntreprises(
            StatutCompte statut, String keyword, int page, int size);

    EntrepriseCompteResponse traiterDemande(
            Long entrepriseId, ValiderEntrepriseRequest request, String emailAdmin);

    EntrepriseCompteResponse consulterDetails(Long entrepriseId);

    void supprimerLogiquement(Long entrepriseId, String emailAdmin);
}