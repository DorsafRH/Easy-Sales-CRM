package com.crm.modules.marketing.controller;

import com.crm.modules.marketing.dto.request.LeadAutomationRequestDTO;
import com.crm.modules.marketing.dto.request.ReactionsSeedRequestDTO;
import com.crm.modules.marketing.dto.response.PageConnecteeDTO;
import com.crm.modules.marketing.security.MarketingCallbackGuard;
import com.crm.modules.marketing.service.IMarketingAutomationService;
import com.crm.modules.marketing.service.IMarketingStatsService;
import com.crm.modules.vente.dto.LeadResponse;
import com.crm.shared.response.ApiResponse;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller — Marketing automatisé (machine-à-machine, sans JWT).
 * Base path : /api/marketing/automation
 *
 * <p>Consommé par n8n (chatbot Messenger, collecte de statistiques). Chaque appel
 * est authentifié par le secret partagé dédié, en-tête {@code X-Callback-Secret}
 * (cf {@link MarketingCallbackGuard}). Distinct du controller JWT
 * {@code MarketingController} (deux modes d'auth séparés).</p>
 *
 * @author Riahi Dorsaf
 */
@Tag(name = "Marketing automatisé",
        description = "Endpoints multi-tenant consommés par n8n (secret partagé)")
@RestController
@RequestMapping("/marketing/automation")
@RequiredArgsConstructor
public class MarketingAutomationController {

    private final IMarketingAutomationService marketingAutomationService;
    private final IMarketingStatsService      marketingStatsService;
    private final MarketingCallbackGuard      marketingCallbackGuard;

    @Operation(
            summary = "Créer un lead qualifié depuis le chatbot Messenger",
            description = "Reçu de n8n à la fin d'une conversation qualifiée. Le propriétaire est "
                    + "résolu via pageId (multi-tenant). Requiert l'en-tête X-Callback-Secret."
    )
    @PostMapping("/leads-messenger")
    public ResponseEntity<ApiResponse<LeadResponse>> creerLeadMessenger(
            @Valid @RequestBody LeadAutomationRequestDTO requete,
            @RequestHeader(value = MarketingCallbackGuard.HEADER, required = false) String secret) {

        marketingCallbackGuard.verifier(secret);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                marketingAutomationService.creerLeadDepuisMessenger(requete),
                "Lead qualifié créé."));
    }

    @Operation(
            summary = "Lister les pages Facebook connectées",
            description = "Énumération (id de page, jeton, propriétaire) pour la collecte d'engagement "
                    + "par n8n. Requiert l'en-tête X-Callback-Secret."
    )
    @GetMapping("/pages")
    public ResponseEntity<ApiResponse<List<PageConnecteeDTO>>> listerPages(
            @RequestHeader(value = MarketingCallbackGuard.HEADER, required = false) String secret) {

        marketingCallbackGuard.verifier(secret);
        return ResponseEntity.ok(ApiResponse.success(
                marketingAutomationService.listerPagesConnectees(),
                "Pages connectées récupérées."));
    }

    @Operation(
            summary = "Enregistrer l'engagement public des publications",
            description = "Reçu de n8n (ou injecté pour la démo) : réactions, commentaires, partages "
                    + "par post. Le propriétaire est résolu via pageId. Requiert l'en-tête X-Callback-Secret."
    )
    @PostMapping("/reactions")
    public ResponseEntity<ApiResponse<Void>> enregistrerReactions(
            @Valid @RequestBody ReactionsSeedRequestDTO requete,
            @RequestHeader(value = MarketingCallbackGuard.HEADER, required = false) String secret) {

        marketingCallbackGuard.verifier(secret);
        marketingStatsService.upsertReactions(requete);
        return ResponseEntity.ok(ApiResponse.success("Engagement enregistré."));
    }
}
