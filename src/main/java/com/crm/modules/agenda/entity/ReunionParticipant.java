package com.crm.modules.agenda.entity;

import com.crm.shared.enums.TypeParticipant;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

/**
 * Participant à une réunion — entité embarquée ({@code @Embeddable}).
 *
 * <p>Stocké dans la table {@code reunion_participants} via
 * {@code @ElementCollection} sur {@link Reunion}.</p>
 *
 * <p>Un participant peut être :</p>
 * <ul>
 *   <li>Le client principal — {@link TypeParticipant#CLIENT}</li>
 *   <li>Un contact du client — {@link TypeParticipant#CONTACT}</li>
 *   <li>Un invité externe (collaborateur…) — {@link TypeParticipant#EXTERNE}</li>
 * </ul>
 *
 * @author Riahi Dorsaf
 */
@Embeddable
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReunionParticipant {

    /** Nom du participant — obligatoire. */
    @Column(name = "participant_nom", nullable = false, length = 100)
    private String nom;

    /** Prénom du participant — optionnel. */
    @Column(name = "participant_prenom", length = 100)
    private String prenom;

    /** Email du participant — utilisé pour l'invitation. */
    @Column(name = "participant_email", length = 200)
    private String email;

    /** Téléphone — utilisé pour l'envoi WhatsApp. */
    @Column(name = "participant_telephone", length = 20)
    private String telephone;

    /**
     * Type du participant.
     * Permet de distinguer clients, contacts et invités externes.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "participant_type", nullable = false, length = 20)
    private TypeParticipant type;

    /**
     * Retourne le nom complet du participant.
     *
     * @return "{prénom} {nom}" ou "{nom}" si prénom absent
     */
    public String getNomComplet() {
        if (prenom != null && !prenom.isBlank()) return prenom + " " + nom;
        return nom;
    }
}