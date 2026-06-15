package com.crm.modules.reporting.service;

import com.crm.modules.reporting.dto.RapportCommercialResponse;
import com.crm.modules.reporting.dto.RapportCommercialResponse.DevisFactures;
import com.crm.modules.reporting.dto.RapportCommercialResponse.Synthese;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static com.crm.modules.reporting.service.RapportPresentation.montant;
import static com.crm.modules.reporting.service.RapportPresentation.pointsDAttention;
import static com.crm.modules.reporting.service.RapportPresentation.pourcent;

/**
 * Génère la synthèse rédigée (langage naturel) du rapport commercial via un LLM.
 *
 * <p>Autonome (ne dépend pas du module marketing) mais réutilise la même configuration
 * {@code groq.api.*}. L'API ciblée est au format OpenAI {@code /v1/chat/completions} :
 * on peut donc pointer indifféremment vers <b>Groq</b> (cloud) ou <b>Ollama / DeepSeek
 * local</b> (en changeant uniquement {@code groq.api.url} et {@code groq.api.model}).</p>
 *
 * <p><b>Tolérant à la panne</b> : si l'appel IA échoue (timeout, modèle local éteint,
 * quota…), on logge et on renvoie {@code null}. L'envoi du rapport n'est jamais bloqué :
 * l'email part simplement sans l'encadré « Synthèse ».</p>
 *
 * @author Riahi Dorsaf
 */
@Slf4j
@Service
public class RapportSyntheseService {

    private static final String SYSTEM_PROMPT =
            "Tu es un analyste commercial pour des PME tunisiennes. À partir des chiffres d'un "
                    + "bilan commercial, rédige en français une synthèse claire de 4 à 6 phrases, "
                    + "suivie de 2 à 3 recommandations concrètes et actionnables. Ton professionnel "
                    + "et bienveillant. N'invente aucun chiffre, appuie-toi uniquement sur les données "
                    + "fournies. Pas de markdown, pas de titres, juste le texte.";

    private static final int MAX_TOKENS = 400;
    private static final double TEMPERATURE = 0.5;

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public RapportSyntheseService(@Value("${groq.api.url}") String url,
                                  @Value("${groq.api.key}") String apiKey,
                                  @Value("${groq.api.model}") String model) {
        this.restClient = RestClient.builder().baseUrl(url).build();
        this.apiKey = apiKey;
        this.model = model;
    }

    /**
     * Produit la synthèse IA du rapport, ou {@code null} si l'IA est indisponible.
     *
     * @param rapport rapport commercial agrégé
     * @return texte de synthèse, ou {@code null} en cas d'échec (envoi non bloqué)
     */
    public String genererSynthese(RapportCommercialResponse rapport) {
        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", SYSTEM_PROMPT),
                            Map.of("role", "user", "content", construirePrompt(rapport))),
                    "max_tokens", MAX_TOKENS,
                    "temperature", TEMPERATURE);

            Map<?, ?> reponse = restClient.post()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            String contenu = extraireContenu(reponse);
            if (contenu == null || contenu.isBlank()) {
                log.warn("[SYNTHESE IA] Réponse vide du modèle — rapport envoyé sans synthèse.");
                return null;
            }
            log.info("[SYNTHESE IA] Synthèse générée pour le propriétaire id={}",
                    rapport.getEntreprise().getProprietaireId());
            return contenu.trim();

        } catch (Exception e) {
            log.error("[SYNTHESE IA] Échec de génération (envoi sans synthèse) : {}", e.getMessage());
            return null;
        }
    }

    // ── Construction du prompt ────────────────────────────────────────────────────

    private String construirePrompt(RapportCommercialResponse r) {
        Synthese s = r.getSynthese();
        DevisFactures d = r.getDevisFactures();

        StringBuilder p = new StringBuilder();
        p.append("Entreprise : ").append(r.getEntreprise().getNomEntreprise()).append('\n');
        p.append("Période : ").append(r.getPeriode().getLibelle()).append('\n');
        p.append("CA encaissé : ").append(montant(s.getCaRealise())).append('\n');
        p.append("CA période précédente : ").append(montant(s.getCaPeriodePrecedente()))
                .append(" (variation : ")
                .append(s.getVariationCaPct() == null ? "non comparable" : pourcent(s.getVariationCaPct()))
                .append(")\n");
        p.append("Nouveaux leads : ").append(s.getNouveauxLeads())
                .append(", convertis : ").append(s.getLeadsConvertis())
                .append(" (taux ").append(pourcent(s.getTauxConversionLeads())).append(")\n");
        p.append("Pipeline : ").append(montant(r.getPipeline().getValeur())).append('\n');
        p.append("Activités commerciales : ").append(r.getActivites().getTotal()).append('\n');
        p.append("Devis émis : ").append(d.getDevisEmis())
                .append(" (acceptation ").append(pourcent(d.getTauxAcceptationDevis())).append(")\n");
        p.append("Factures impayées : ").append(d.getFacturesImpayees())
                .append(" pour ").append(montant(d.getMontantImpaye())).append('\n');

        p.append("Points d'attention détectés :\n");
        for (String pt : pointsDAttention(r)) {
            p.append("- ").append(pt).append('\n');
        }
        return p.toString();
    }

    @SuppressWarnings("unchecked")
    private String extraireContenu(Map<?, ?> reponse) {
        Object choices = reponse != null ? reponse.get("choices") : null;
        if (!(choices instanceof List<?> liste) || liste.isEmpty()) {
            return null;
        }
        Object message = ((Map<?, ?>) liste.get(0)).get("message");
        if (message instanceof Map<?, ?> m) {
            Object content = ((Map<String, Object>) m).get("content");
            return content != null ? String.valueOf(content) : null;
        }
        return null;
    }
}
