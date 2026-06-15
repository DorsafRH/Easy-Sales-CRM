package com.crm.modules.reporting.controller;

import com.crm.modules.reporting.dto.ProprietaireResumeResponse;
import com.crm.modules.reporting.dto.RapportCommercialResponse;
import com.crm.modules.reporting.security.CallbackSecretGuard;
import com.crm.modules.reporting.service.IRapportCommercialService;
import com.crm.shared.enums.PeriodeRapport;
import com.crm.shared.response.ApiResponse;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller — Reporting commercial automatisé (machine-à-machine, sans JWT).
 * Base path : /api/reporting/automation
 *
 * <p>Consommé par n8n (cron). Chaque appel est authentifié par le secret partagé
 * dédié, en-tête {@code X-Callback-Secret} (cf {@link CallbackSecretGuard}).</p>
 *
 * @author Riahi Dorsaf
 */
@Tag(name = "Reporting automatisé",
        description = "Endpoints multi-tenant consommés par n8n (secret partagé)")
@RestController
@RequestMapping("/reporting/automation")
@RequiredArgsConstructor
public class RapportCommercialController {

    private final IRapportCommercialService rapportCommercialService;
    private final CallbackSecretGuard       callbackSecretGuard;

    @Operation(
            summary = "Lister les propriétaires actifs",
            description = "Énumération multi-tenant (id + email) pour boucler côté n8n. "
                    + "Requiert l'en-tête X-Callback-Secret."
    )
    @GetMapping("/proprietaires")
    public ResponseEntity<ApiResponse<List<ProprietaireResumeResponse>>> listerProprietaires(
            @RequestHeader(value = CallbackSecretGuard.HEADER, required = false) String secret) {

        callbackSecretGuard.verifier(secret);
        return ResponseEntity.ok(ApiResponse.success(
                rapportCommercialService.listerProprietairesActifs(),
                "Propriétaires actifs récupérés."));
    }

    @Operation(
            summary = "Générer le rapport commercial d'un propriétaire",
            description = "Période complète écoulée : SEMAINE | MOIS | ANNEE. "
                    + "Requiert l'en-tête X-Callback-Secret."
    )
    @GetMapping("/rapport")
    public ResponseEntity<ApiResponse<RapportCommercialResponse>> genererRapport(
            @RequestParam Long proprietaireId,
            @RequestParam PeriodeRapport periode,
            @RequestHeader(value = CallbackSecretGuard.HEADER, required = false) String secret) {

        callbackSecretGuard.verifier(secret);
        return ResponseEntity.ok(ApiResponse.success(
                rapportCommercialService.genererRapport(proprietaireId, periode),
                "Rapport commercial généré."));
    }
}
