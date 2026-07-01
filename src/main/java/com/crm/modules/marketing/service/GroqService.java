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

    private static final String REGLES_POST =
            "Écris un post Facebook prêt à publier, bien structuré et vivant :\n"
                    + "1) une accroche courte avec un emoji ;\n"
                    + "2) 2 ou 3 lignes d'avantages, chacune sur sa propre ligne et commençant par un "
                    + "emoji pertinent (✅ 🚀 💡 📦 💰 …) ;\n"
                    + "3) un appel à l'action clair avec un emoji ;\n"
                    + "4) sur une NOUVELLE ligne tout en bas, 3 à 5 hashtags, chacun OBLIGATOIREMENT "
                    + "précédé du symbole # et collé au mot (ex : #Marketing #Tunisie #Promo). "
                    + "N'écris JAMAIS un hashtag sans le #.\n"
                    + "Utilise plusieurs emojis (au moins un par ligne). "
                    + "N'utilise AUCUN markdown d'emphase : pas de ** ni de * autour des mots, pas de "
                    + "titre en #Titre — le texte doit s'afficher tel quel sur Facebook. "
                    + "N'invente RIEN qui ne soit pas fourni. En particulier : AUCUNE date, AUCUN délai "
                    + "ou durée (« sous 30 jours », « dans 10 jours »), AUCUN chiffre, pourcentage ou "
                    + "caractéristique technique, et aucune annonce de « lancement / nouveauté / produit "
                    + "révolutionnaire » qui ne soit pas explicitement indiquée. Utilise UNIQUEMENT le "
                    + "nom du produit, son prix, la remise et la consigne fournis ; si une information "
                    + "manque, reste général plutôt que d'inventer. "
                    + "N'entoure JAMAIS les noms de produits ou de catégories de guillemets. "
                    + "Interdits : liens, numéros de téléphone ou e-mails inventés, texte entre crochets []. "
                    + "Environ 60 à 100 mots. "
                    + "Réponds UNIQUEMENT avec le texte du post, sans aucune phrase d'introduction.";

    private static final String SYSTEM_GENERATOR =
            "Tu es un expert en marketing digital pour PME tunisiennes. " + REGLES_POST;
    private static final String SYSTEM_CRITIC =
            "Tu es relecteur. Améliore le post ci-dessous (style, impact, fautes) en gardant "
                    + "exactement les mêmes règles. " + REGLES_POST;
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
                .contenuGenere(nettoyer(generator.contenu()))
                .contenuAmeliore(nettoyer(critic.contenu()))
                .tokensUtilises(generator.tokens() + critic.tokens())
                .build();
    }

    /**
     * Nettoie la sortie du LLM pour un rendu Facebook propre : retire le markdown d'emphase
     * (**, *, _, `, #) et un éventuel encadrement par des guillemets.
     */
    private String nettoyer(String texte) {
        if (texte == null) return "";
        String t = texte.trim();
        // retire une phrase d'introduction du type « Voici une version améliorée … : »
        t = t.replaceAll("(?is)^\\s*voici[^\\n:]*:\\s*", "");
        // retire un éventuel encadrement par des guillemets
        if (t.length() >= 2 && t.startsWith("\"") && t.endsWith("\"")) {
            t = t.substring(1, t.length() - 1).trim();
        }
        // retire le markdown d'emphase (** et *) et les accents graves, MAIS garde le # des hashtags
        t = t.replaceAll("\\*+", "").replace("`", "");
        // retire les guillemets (droits et français) qui encadrent souvent les noms de produits/catégories
        t = t.replaceAll("[«»\"“”]", "");
        // retire uniquement les titres markdown « # » / « ## » en début de ligne (pas les #hashtags)
        t = t.replaceAll("(?m)^\\s*#{1,6}\\s+", "");
        t = t.replaceAll("[ \\t]+\n", "\n");
        return forcerHashtags(t.trim());
    }

    /**
     * Si la dernière ligne non vide ressemble à des hashtags écrits SANS le symbole #
     * (mots en CamelCase/concaténés), remet le # devant chacun. Conservateur : ne touche
     * pas une phrase normale (ponctuation, mots courts en minuscules).
     */
    private String forcerHashtags(String texte) {
        String[] lignes = texte.split("\n");
        for (int i = lignes.length - 1; i >= 0; i--) {
            String ligne = lignes[i].trim();
            if (ligne.isEmpty()) continue;

            String[] tokens = ligne.split("\\s+");
            if (tokens.length < 2 || tokens.length > 6) return texte;

            boolean camel = false;
            for (String tk : tokens) {
                if (tk.startsWith("#")) return texte;              // déjà des hashtags → on ne touche pas
                if (!tk.matches("[\\p{L}\\p{N}]{3,}")) return texte; // pas une ligne de hashtags
                if (tk.substring(1).chars().anyMatch(Character::isUpperCase)) camel = true;
            }
            if (!camel) return texte; // pas de signal CamelCase → probablement une vraie phrase

            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < tokens.length; j++) {
                if (j > 0) sb.append(' ');
                sb.append('#').append(tokens[j]);
            }
            lignes[i] = sb.toString();
            return String.join("\n", lignes);
        }
        return texte;
    }

    /**
     * Améliore un texte existant en une passe (raffinage itératif), en tenant compte
     * d'une consigne et d'une tonalité facultatives. Les mêmes règles de format s'appliquent.
     */
    public GenererContenuResponseDTO ameliorer(String texte, String consigne, String tonalite) {
        StringBuilder u = new StringBuilder("Voici le post actuel :\n").append(texte).append('\n');
        if (consigne != null && !consigne.isBlank()) {
            u.append("Demande de modification : ").append(consigne.trim()).append('\n');
        }
        if (tonalite != null && !tonalite.isBlank()) {
            u.append("Tonalité souhaitée : ").append(tonalite.trim()).append('\n');
        }
        u.append("RÉÉCRIS ENTIÈREMENT le post en intégrant la demande de façon cohérente. "
                + "Supprime toute information qui contredit la demande ; ne laisse JAMAIS deux "
                + "informations contradictoires (ex : deux délais ou deux dates différentes). "
                + "Ne garde pas les anciennes formulations qui ne collent plus.\n");
        AppelResultat resultat = appeler(SYSTEM_CRITIC, u.toString());
        return GenererContenuResponseDTO.builder()
                .contenuGenere(texte)
                .contenuAmeliore(nettoyer(resultat.contenu()))
                .tokensUtilises(resultat.tokens())
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
