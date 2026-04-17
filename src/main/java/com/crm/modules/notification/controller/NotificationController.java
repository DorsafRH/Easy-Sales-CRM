package com.crm.modules.notification.controller;

import com.crm.modules.notification.dto.NotificationResponse;
import com.crm.modules.notification.service.NotificationService;
import com.crm.shared.response.ApiResponse;
import com.crm.shared.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur des notifications Super Admin.
 * Consulté par polling depuis Angular toutes les 30 secondes.
 *
 * <p>Base path : {@code /api/admin/notifications}</p>
 *
 * @author Riahi Dorsaf
 */
@Tag(
        name = "Administration — Notifications",
        description = "Gestion des notifications du Super Admin (polling 30s)"
)
@RestController
@RequestMapping("/admin/notifications")
@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Liste toutes les notifications paginées triées par date décroissante.
     */
    @Operation(summary = "Lister toutes les notifications (paginées)")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> lister(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.listerNotifications(page, size),
                "Notifications récupérées."));
    }

    /**
     * Retourne le nombre de notifications non lues pour le badge Angular.
     */
    @Operation(summary = "Nombre de notifications non lues — badge Angular")
    @GetMapping("/non-lues/count")
    public ResponseEntity<ApiResponse<Long>> compterNonLues() {
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.compterNonLues(),
                "Compteur récupéré."));
    }

    /**
     * Marque une notification comme lue.
     */
    @Operation(summary = "Marquer une notification comme lue")
    @PatchMapping("/{id}/lue")
    public ResponseEntity<ApiResponse<NotificationResponse>> marquerCommeLue(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.marquerCommeLue(id),
                "Notification marquée comme lue."));
    }

    /**
     * Marque toutes les notifications comme lues.
     */
    @Operation(summary = "Marquer toutes les notifications comme lues")
    @PatchMapping("/tout-lire")
    public ResponseEntity<ApiResponse<Void>> marquerToutesCommeLues() {
        notificationService.marquerToutesCommeLues();
        return ResponseEntity.ok(ApiResponse.success(
                "Toutes les notifications marquées comme lues."));
    }
}