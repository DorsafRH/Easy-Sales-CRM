package com.crm.modules.marketing.controller;

import com.crm.modules.marketing.dto.response.MarketingOverviewResponseDTO;
import com.crm.modules.marketing.dto.response.TopPostReactionsDTO;
import com.crm.modules.marketing.service.IMarketingStatsService;
import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import com.crm.shared.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller — Statistiques marketing (dashboard mobile).
 * Base path : /api/marketing/stats
 *
 * @author Riahi Dorsaf
 */
@Tag(name = "Statistiques marketing",
        description = "Indicateurs de leads, publications et engagement du module marketing")
@RestController
@RequestMapping("/marketing/stats")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_PROPRIETAIRE')")
public class MarketingStatsController {

    private final IMarketingStatsService marketingStatsService;

    @Operation(summary = "Vue d'ensemble du dashboard marketing",
            description = "Leads issus des réseaux sociaux, état des publications et engagement agrégé")
    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<MarketingOverviewResponseDTO>> getOverview(
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {
        return ResponseEntity.ok(ApiResponse.success(
                marketingStatsService.getOverview(proprietaire.getId()),
                "Statistiques marketing récupérées."));
    }

    @Operation(summary = "Publications les plus engageantes")
    @GetMapping("/top-posts")
    public ResponseEntity<ApiResponse<List<TopPostReactionsDTO>>> getTopPosts(
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {
        return ResponseEntity.ok(ApiResponse.success(
                marketingStatsService.getTopPosts(proprietaire.getId()),
                "Top publications récupérées."));
    }
}
