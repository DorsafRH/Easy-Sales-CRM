package com.crm.modules.marketing.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Engagement public des publications transmis par n8n (ou injecté pour la démo)
 * pour alimenter le dashboard marketing. Le propriétaire est résolu via {@code pageId}.
 *
 * @author Riahi Dorsaf
 */
@Data
public class ReactionsSeedRequestDTO {

    @NotBlank(message = "L'identifiant de page Meta est obligatoire")
    private String pageId;

    @NotNull(message = "La liste des items est obligatoire")
    @Valid
    private List<ReactionItemDTO> items;

    /**
     * Engagement d'un post : compteurs et détail des réactions par emoji.
     */
    @Data
    public static class ReactionItemDTO {

        @NotBlank(message = "L'identifiant du post est obligatoire")
        private String postId;

        /** Libellé lisible de la publication (nom/sujet). */
        private String titre;

        private Integer likes;
        private Integer comments;
        private Integer shares;

        /** Détail par type : like, love, wow, sad, angry, haha. */
        private Map<String, Integer> reactionsBreakdown;
    }
}
