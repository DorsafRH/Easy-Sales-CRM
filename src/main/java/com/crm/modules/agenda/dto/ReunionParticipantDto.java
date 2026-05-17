package com.crm.modules.agenda.dto;

import com.crm.shared.enums.TypeParticipant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

/**
 * DTO représentant un participant à une réunion.
 * Utilisé dans {@link ReunionRequest} et {@link ReunionResponse}.
 *
 * @author Riahi Dorsaf
 */
@Data
@Builder
public class ReunionParticipantDto {

    /**
     * Nom du participant — obligatoire.
     */
    @NotBlank(message = "Le nom du participant est obligatoire")
    @Size(max = 100)
    private String nom;

    /**
     * Prénom du participant — optionnel.
     */
    @Size(max = 100)
    private String prenom;

    /**
     * Email — utilisé pour l'envoi de l'invitation.
     */
    @Size(max = 200)
    private String email;

    /**
     * Téléphone — utilisé pour l'envoi WhatsApp.
     */
    @Size(max = 20)
    private String telephone;

    /**
     * Type du participant.
     * {@code CLIENT} | {@code CONTACT} | {@code EXTERNE}
     */
    @NotNull(message = "Le type de participant est obligatoire")
    private TypeParticipant type;
}