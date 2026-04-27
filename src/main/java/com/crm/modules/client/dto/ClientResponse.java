package com.crm.modules.client.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de réponse pour un client.
 * Les champs nom/prenom sont renseignés pour INDIVIDUEL,
 * raisonSociale pour ENTREPRISE.
 *
 * @author Riahi Dorsaf
 */
@Data
public class ClientResponse {

    private Long   id;
    private String typeClient;      // INDIVIDUEL | ENTREPRISE
    private String nomAffichage;
    private String email;
    private String telephone;
    private String adresse;
    private String ville;
    private String pays;
    private String statut;
    private LocalDateTime dateCreation;

    // INDIVIDUEL
    private String nom;
    private String prenom;

    // ENTREPRISE
    private String raisonSociale;

    // Calculés par le service (non mappés par MapStruct)
    private int        nbContacts;
    private BigDecimal chiffreAffaires; // 0.0 Sprint 2, réel Sprint 3
}