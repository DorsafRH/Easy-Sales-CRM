package com.crm.modules.client.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * DTO de création / modification d'un client.
 *
 * @author Riahi Dorsaf
 */
@Data
public class ClientRequest {

    /**
     * Type du client : INDIVIDUEL ou ENTREPRISE.
     * Obligatoire uniquement à la création.
     */
    @NotBlank(message = "Le type client est obligatoire (INDIVIDUEL ou ENTREPRISE)")
    @Pattern(regexp = "INDIVIDUEL|ENTREPRISE",
            message = "Le type client doit être INDIVIDUEL ou ENTREPRISE")
    private String typeClient;

    // ── Champs communs ────────────────────────────────────────────────────────

    @Email(message = "Format d'email invalide")
    private String email;

    private String telephone;
    private String adresse;
    private String ville;
    private String pays;

    // ── Spécifique INDIVIDUEL ─────────────────────────────────────────────────

    private String nom;
    private String prenom;

    // ── Spécifique ENTREPRISE ─────────────────────────────────────────────────

    private String raisonSociale;
}