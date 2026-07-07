package com.crm.modules.marketing.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Optional;

/**
 * Récupère les statistiques publiques d'un post Facebook via l'API Graph :
 * impressions ({@code /{postId}/insights}) et engagement
 * ({@code /{postId}?fields=shares,comments,reactions}).
 *
 * <p>Ne lève jamais d'exception : tout échec (token expiré, réseau, post
 * supprimé) est journalisé et retourné sous forme d'{@link Optional} vide,
 * pour laisser l'appelant appliquer un repli local.</p>
 *
 * @author Riahi Dorsaf
 */
@Slf4j
@Service
public class FacebookInsightsService {

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String graphApiVersion;

    public FacebookInsightsService(
            @Value("${meta.oauth.graph-api-version}") String graphApiVersion) {
        this.graphApiVersion = graphApiVersion;
    }

    /** Métriques agrégées d'un post Facebook (valeurs lifetime). */
    public record InsightsPost(long vues, long vuesUniques, long reactions,
                               long commentaires, long partages) {
    }

    // ─────────────────────────────────────────────────────────
    //  RÉCUPÉRATION DES INSIGHTS
    // ─────────────────────────────────────────────────────────

    /**
     * Interroge l'API Graph pour un post donné.
     *
     * @param postId      identifiant externe Facebook (pageId_postId)
     * @param accessToken token d'accès de la page connectée
     * @return les métriques du post, ou vide si l'API est indisponible
     */
    public Optional<InsightsPost> recupererInsights(String postId, String accessToken) {
        try {
            JsonNode impressions = lireJson(uriInsights(postId, accessToken));
            JsonNode engagement = lireJson(uriEngagement(postId, accessToken));

            return Optional.of(new InsightsPost(
                    valeurInsight(impressions, "post_impressions"),
                    valeurInsight(impressions, "post_impressions_unique"),
                    totalSummary(engagement, "reactions"),
                    totalSummary(engagement, "comments"),
                    engagement.path("shares").path("count").asLong(0)));
        } catch (Exception e) {
            log.warn("[FACEBOOK-INSIGHTS] Insights indisponibles pour le post {} : {}",
                    postId, e.getMessage());
            return Optional.empty();
        }
    }

    // ─────────────────────────────────────────────────────────
    //  HELPERS PRIVÉS
    // ─────────────────────────────────────────────────────────

    private URI uriInsights(String postId, String accessToken) {
        return UriComponentsBuilder
                .fromHttpUrl(grapheBase() + "/" + postId + "/insights")
                .queryParam("metric", "post_impressions,post_impressions_unique")
                .queryParam("access_token", accessToken)
                .build().encode().toUri();
    }

    private URI uriEngagement(String postId, String accessToken) {
        return UriComponentsBuilder
                .fromHttpUrl(grapheBase() + "/" + postId)
                .queryParam("fields",
                        "shares,comments.summary(true).limit(0),reactions.summary(true).limit(0)")
                .queryParam("access_token", accessToken)
                .build().encode().toUri();
    }

    /**
     * L'API Graph renvoie parfois Content-Type "text/javascript" : on lit
     * le corps brut puis on le parse en JSON (cf. FacebookPublicationService).
     */
    private JsonNode lireJson(URI uri) throws Exception {
        String corps = restClient.get().uri(uri).retrieve().body(String.class);
        return objectMapper.readTree(corps);
    }

    /** Valeur lifetime d'une métrique du tableau {@code data} des insights. */
    private long valeurInsight(JsonNode insights, String metrique) {
        for (JsonNode noeud : insights.path("data")) {
            if (metrique.equals(noeud.path("name").asText())) {
                JsonNode valeurs = noeud.path("values");
                if (!valeurs.isEmpty()) {
                    return valeurs.get(0).path("value").asLong(0);
                }
            }
        }
        return 0;
    }

    /** Total du résumé d'un champ à summary ({@code reactions}, {@code comments}). */
    private long totalSummary(JsonNode engagement, String champ) {
        return engagement.path(champ).path("summary").path("total_count").asLong(0);
    }

    private String grapheBase() {
        return "https://graph.facebook.com/" + graphApiVersion;
    }
}
