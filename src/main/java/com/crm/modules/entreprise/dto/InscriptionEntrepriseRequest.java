package com.crm.modules.entreprise.dto;

import com.crm.shared.enums.TailleEntreprise;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO pour la création d'un compte entreprise depuis l'application mobile.
 * Création d'un compte entreprise
 */
@Data
public class InscriptionEntrepriseRequest {

    // ── Informations du ProprietaireEntreprise ────────────────────────────────

    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    private String prenom;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
    private String motDePasse;

    @NotBlank(message = "Le numéro de téléphone est obligatoire")
    private String telephone;

    // ── Informations de l'EntrepriseCompte ───────────────────────────────────

    @NotBlank(message = "Le nom de l'entreprise est obligatoire")
    private String nomEntreprise;

    @NotBlank(message = "La matricule fiscale est obligatoire")
    private String matriculeFiscale;

    private String secteurActivite;

    @NotNull(message = "La taille de l'entreprise est obligatoire")
    private TailleEntreprise tailleEntreprise;

    private String adresse;
    private String ville;
    private String pays;
    private String siteWeb;
    private String telephoneEntreprise;
}
