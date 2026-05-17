package com.crm.modules.reporting.controller;

import com.crm.modules.reporting.dto.ActiviteResponse;
import com.crm.modules.reporting.dto.ReportingKpisResponse;
import com.crm.modules.reporting.service.IReportingService;
import com.crm.shared.response.ApiResponse;
import com.crm.shared.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller — Reporting.
 * Base path : /api/reporting
 *
 * @author Riahi Dorsaf
 */
@Tag(name = "Reporting", description = "Indicateurs de performance et activité récente")
@RestController
@RequestMapping("/reporting")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_PROPRIETAIRE')")
public class ReportingController {

    private final IReportingService reportingService;

    @Operation(
            summary = "Récupérer les KPIs du tableau de bord",
            description = "Paramètre periode : AUJOURD_HUI | CE_MOIS (défaut) | CETTE_ANNEE"
    )
    @GetMapping("/kpis")
    public ResponseEntity<ApiResponse<ReportingKpisResponse>> getKpis(
            @RequestParam(defaultValue = "CE_MOIS") String periode,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(ApiResponse.success(
                reportingService.getKpis(userDetails.getUsername(), periode),
                "KPIs récupérés."));
    }

    @Operation(
            summary = "Lister toutes les activités",
            description = "Liste paginée des activités — utilisé pour l'écran 'Voir tout'."
    )
    @GetMapping("/activites")
    public ResponseEntity<ApiResponse<PageResponse<ActiviteResponse>>> getActivites(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(ApiResponse.success(
                reportingService.getActivites(userDetails.getUsername(), page, size),
                "Activités récupérées."));
    }
}