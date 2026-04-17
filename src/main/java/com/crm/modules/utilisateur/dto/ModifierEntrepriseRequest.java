package com.crm.modules.utilisateur.dto;

import com.crm.shared.enums.TailleEntreprise;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO pour la modification des données de l'entreprise par son propriétaire.
 * Génère une notification au Super Admin.
 *
 * @author Riahi Dorsaf
 */
@Data
public class ModifierEntrepriseRequest {

    @NotBlank(message = "Le nom de l'entreprise est obligatoire")
    private String nomEntreprise;

    private String secteurActivite;

    @NotNull(message = "La taille de l'entreprise est obligatoire")
    private TailleEntreprise tailleEntreprise;

    private String telephone;
    private String adresse;
    private String ville;
    private String pays;
    private String siteWeb;
}