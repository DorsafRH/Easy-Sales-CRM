package com.crm.modules.contact.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO de réponse pour un contact.
 *
 * @author Riahi Dorsaf
 */
@Data
public class ContactResponse {

    private Long id;
    private String nom;
    private String prenom;
    private String nomComplet;
    private String email;
    private String telephone;
    private String poste;
    private boolean isPrincipal;
    private LocalDateTime dateCreation;

    private Long clientId;
    private String clientNomAffichage;
}