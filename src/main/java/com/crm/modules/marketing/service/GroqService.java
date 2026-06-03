package com.crm.modules.marketing.service;

import com.crm.modules.marketing.dto.request.GenererContenuRequestDTO;
import com.crm.modules.marketing.dto.response.GenererContenuResponseDTO;
import com.crm.shared.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/**
 * Service d'appel à l'API Groq — pattern Generator → Critic.
 *
 * @author Riahi Dorsaf
 */
@Slf4j
@Service
public class GroqService {

    private static final String SYSTEM_GENERATOR =
            "Tu es un expert en marketing digital pour PMEs tunisiennes. Génère du contenu engageant.";
    private static final String SYSTEM_CRITIC =
            "Tu es un éditeur marketing. Améliore ce contenu : rends-le plus percutant, "
                    + "corrige les fautes, optimise pour l'engagement.";
    private static final int MAX_TOKENS = 500;
    private static final double TEMPERATURE = 0.7;

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public GroqService(@Value("${groq.api.url}") String url,
                       @Value("${groq.api.key}") String apiKey,
                       @Value("${groq.api.model}") String model) {
        this.restClient = RestClient.builder().baseUrl(url).build();
        this.apiKey = apiKey;
        this.model = model;
    }

    public GenererContenuResponseDTO genererContenu(GenererContenuRequestDTO request) {
        AppelResultat generator = appeler(SYSTEM_GENERATOR, construirePromptUtilisateur(request));
        AppelResultat critic = appeler(SYSTEM_CRITIC, generator.contenu());
        return GenererContenuResponseDTO.builder()
                .contenuGenere(generator.contenu())
                .contenuAmeliore(critic.contenu())
                .tokensUtilises(generator.tokens() + critic.tokens())
                .build();
    }

    private AppelResultat appeler(String systemPrompt, String userPrompt) {
        Map<?, ?> reponse = envoyer(construireBody(systemPrompt, userPrompt));
        return new AppelResultat(extraireContenu(reponse), extraireTokens(reponse));
    }

    private String construirePromptUtilisateur(GenererContenuRequestDTO req) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Sujet : ").append(req.getSujet()).append('\n');
        prompt.append("Type de contenu : ").append(req.getTypeContenu()).append('\n');
        prompt.append("Tonalité : ").append(req.getTonalite()).append('\n');
        prompt.append("Langue : ").append(req.getLangue()).append('\n');
        if (req.getMotsCles() != null && !req.getMotsCles().isEmpty()) {
            prompt.append("Mots-clés : ").append(String.join(", ", req.getMotsCles()));
        }
        return prompt.toString();
    }

    private Map<String, Object> construireBody(String systemPrompt, String userPrompt) {
        return Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)),
                "max_tokens", MAX_TOKENS,
                "temperature", TEMPERATURE);
    }

    private Map<?, ?> envoyer(Map<String, Object> body) {
        try {
            return restClient.post()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientException e) {
            log.error("[GROQ] Erreur appel API : {}", e.getMessage());
            throw new BusinessException("Erreur lors de l'appel à l'API Groq : " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private String extraireContenu(Map<?, ?> reponse) {
        Object choices = reponse != null ? reponse.get("choices") : null;
        if (!(choices instanceof List<?> liste) || liste.isEmpty()) {
            throw new BusinessException("Réponse Groq invalide : aucun contenu généré.");
        }
        Map<String, Object> message = (Map<String, Object>) ((Map<?, ?>) liste.get(0)).get("message");
        return message != null ? String.valueOf(message.get("content")) : "";
    }

    private int extraireTokens(Map<?, ?> reponse) {
        if (reponse != null && reponse.get("usage") instanceof Map<?, ?> usage
                && usage.get("total_tokens") instanceof Number total) {
            return total.intValue();
        }
        return 0;
    }

    private record AppelResultat(String contenu, int tokens) {
    }
}
