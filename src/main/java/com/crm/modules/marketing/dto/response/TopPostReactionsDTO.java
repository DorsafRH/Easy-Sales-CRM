package com.crm.modules.marketing.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Engagement d'un post pour le classement des meilleures publications.
 *
 * @author Riahi Dorsaf
 */
@Data
@Builder
public class TopPostReactionsDTO {
    private String postId;
    private String titre;
    private int likes;
    private int comments;
    private int shares;
    private Map<String, Integer> reactionsBreakdown;
}
