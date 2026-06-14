package com.crm.modules.marketing.service;

import com.crm.modules.marketing.entity.CompteSocialConnecte;
import com.crm.modules.marketing.entity.DiffusionPublication;
import com.crm.modules.marketing.entity.PublicationMarketing;
import com.crm.shared.enums.StatutDiffusion;
import com.crm.shared.enums.TypeReseau;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Publie une diffusion directement sur Facebook via l'API Graph
 * (POST /{pageId}/feed). Remplace l'ancien relais N8N.
 *
 * <p>Pour l'instant : texte seul. La gestion de l'image (mediaUrl → /photos)
 * sera ajoutée dans un second temps.</p>
 *
 * @author Riahi Dorsaf
 */
@Slf4j
@Service
public class FacebookPublicationService {

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String graphApiVersion;

    public FacebookPublicationService(
            @Value("${meta.oauth.graph-api-version}") String graphApiVersion) {
        this.graphApiVersion = graphApiVersion;
    }

    /**
     * Publie une diffusion sur son compte social cible et met à jour son statut
     * (PUBLIEE + id/url externes en cas de succès, ECHEC + message sinon).
     * Ne lève jamais d'exception : l'échec est porté par la diffusion.
     *
     * @param diffusion diffusion à publier (entité gérée par la transaction appelante)
     */
    public void publierDiffusion(DiffusionPublication diffusion) {
        CompteSocialConnecte compte = diffusion.getCompteSocial();
        if (compte.getTypeReseau() != TypeReseau.FACEBOOK) {
            marquerEchec(diffusion, "Publication directe non supportée pour "
                    + compte.getTypeReseau());
            return;
        }
        try {
            URI uri = UriComponentsBuilder
                    .fromHttpUrl(grapheBase() + "/" + compte.getIdentifiantExterne() + "/feed")
                    .queryParam("message", texteOuVide(diffusion.getPublication()))
                    .queryParam("access_token", compte.getAccessToken())
                    .build().encode().toUri();
            // L'API Graph renvoie parfois Content-Type "text/javascript" :
            // on lit le corps brut puis on le parse en JSON (cf. MetaOAuthService).
            String corps = restClient.post().uri(uri).retrieve().body(String.class);
            Map<?, ?> reponse = objectMapper.readValue(corps, Map.class);
            Object postId = reponse.get("id");
            if (postId == null) {
                marquerEchec(diffusion, "Réponse Facebook sans id : " + corps);
                return;
            }
            marquerSucces(diffusion, postId.toString());
        } catch (RestClientResponseException e) {
            String corps = e.getResponseBodyAsString();
            log.warn("[FACEBOOK] Réponse erreur brute Graph (diffusion {}, HTTP {}): {}",
                    diffusion.getId(), e.getStatusCode().value(), corps);
            marquerEchec(diffusion, extraireErreurGraph(corps));
        } catch (Exception e) {
            marquerEchec(diffusion, e.getMessage());
        }
    }

    private void marquerSucces(DiffusionPublication diffusion, String postId) {
        diffusion.setStatutDiffusion(StatutDiffusion.PUBLIEE);
        diffusion.setIdPublicationExt(postId);
        diffusion.setUrlPublication("https://www.facebook.com/" + postId);
        diffusion.setMessageErreur(null);
        diffusion.setDateDiffusion(LocalDateTime.now());
        log.info("[FACEBOOK] Diffusion {} publiée — postId={}", diffusion.getId(), postId);
    }

    private void marquerEchec(DiffusionPublication diffusion, String message) {
        diffusion.setStatutDiffusion(StatutDiffusion.ECHEC);
        diffusion.setMessageErreur(message);
        diffusion.setDateDiffusion(LocalDateTime.now());
        log.warn("[FACEBOOK] Diffusion {} en échec : {}", diffusion.getId(), message);
    }

    private String texteOuVide(PublicationMarketing pub) {
        return pub.getTexte() != null ? pub.getTexte() : "";
    }

    private String extraireErreurGraph(String corps) {
        try {
            Map<?, ?> map = objectMapper.readValue(corps, Map.class);
            if (map.get("error") instanceof Map<?, ?> erreur) {
                Object message = erreur.get("message");
                StringBuilder sb = new StringBuilder(
                        message != null ? message.toString() : "Erreur Facebook");
                Object code = erreur.get("code");
                Object subcode = erreur.get("error_subcode");
                Object userMsg = erreur.get("error_user_msg");
                if (code != null) {
                    sb.append(" (code ").append(code);
                    if (subcode != null) {
                        sb.append(", sous-code ").append(subcode);
                    }
                    sb.append(")");
                }
                if (userMsg != null) {
                    sb.append(" — ").append(userMsg);
                }
                return sb.toString();
            }
        } catch (Exception ignore) {
            // corps non-JSON : on retourne le brut
        }
        return corps;
    }

    private String grapheBase() {
        return "https://graph.facebook.com/" + graphApiVersion;
    }
}
