package com.crm.modules.utilisateur.dto;

import lombok.Builder;
import lombok.Data;

/**
 * DTO de réponse pour le profil du propriétaire d'entreprise.
 *
 * @author Riahi Dorsaf
 */
@Data
@Builder
public class ProfilProprietaireResponse {

    private Long userId;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;

    // Informations de l'entreprise
    private Long entrepriseId;
    private String nomEntreprise;
    private String secteurActivite;
    private String tailleEntreprise;
    private String telephoneEntreprise;
    private String adresse;
    private String ville;
    private String pays;
    private String siteWeb;
}