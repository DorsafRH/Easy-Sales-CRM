package com.crm.modules.entreprise.dto;

import com.crm.shared.enums.StatutCompte;
import com.crm.shared.enums.TailleEntreprise;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO de réponse pour EntrepriseCompte.
 * Utilisé pour DEV-19, DEV-20, DEV-53.
 */
@Data
@Builder
public class EntrepriseCompteResponse {

    private Long id;
    private String nomEntreprise;
    private String matriculeFiscale;
    private String secteurActivite;
    private TailleEntreprise tailleEntreprise;
    private String telephone;
    private String adresse;
    private String ville;
    private String pays;
    private String siteWeb;
    private StatutCompte statutCompte;
    private LocalDateTime dateCreation;
    private LocalDateTime dateValidation;
    private String motifRefus;

    // Infos du propriétaire (pour la vue admin DEV-53)
    private String proprietaireNom;
    private String proprietairePrenom;
    private String proprietaireEmail;
    private String proprietaireTelephone;
}
