package com.crm.modules.marketing.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Requête d'amélioration itérative d'un texte de publication déjà généré/rédigé.
 * L'utilisateur peut relancer l'IA autant de fois qu'il le souhaite, en guidant
 * éventuellement le raffinage via une consigne (« plus court », « ajoute les horaires »…).
 *
 * @author Riahi Dorsaf
 */
@Data
public class AmeliorerContenuRequestDTO {

    @NotBlank(message = "Le texte à améliorer est obligatoire")
    private String texte;

    /** Consigne facultative pour orienter l'amélioration. */
    private String consigne;

    /** Tonalité souhaitée (optionnel). */
    private String tonalite;
}
