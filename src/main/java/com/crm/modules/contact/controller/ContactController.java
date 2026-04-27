package com.crm.modules.contact.controller;

import com.crm.modules.contact.dto.ContactRequest;
import com.crm.modules.contact.dto.ContactResponse;
import com.crm.modules.contact.service.IContactService;
import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import com.crm.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST pour la gestion des contacts d'un client.
 * Base path : {@code /api/clients/{clientId}/contacts}
 *
 * @author Riahi Dorsaf
 */
@Tag(name = "Contacts")
@RestController
@RequestMapping("/clients/{clientId}/contacts")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_PROPRIETAIRE')")
public class ContactController {

    private final IContactService contactService;

    @Operation(summary = "Lister les contacts d'un client",
            description = "Paramètre optionnel : keyword (nom, prénom, email, poste)")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ContactResponse>>> lister(
            @PathVariable Long clientId,
            @RequestParam(required = false) String keyword,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {

        return ResponseEntity.ok(ApiResponse.success(
                contactService.lister(clientId, proprietaire.getId(), keyword),
                "Contacts récupérés."));
    }

    @Operation(summary = "Détail d'un contact")
    @GetMapping("/{contactId}")
    public ResponseEntity<ApiResponse<ContactResponse>> obtenir(
            @PathVariable Long clientId,
            @PathVariable Long contactId,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {

        return ResponseEntity.ok(ApiResponse.success(
                contactService.obtenir(contactId, clientId, proprietaire.getId()),
                "Contact récupéré."));
    }

    @Operation(summary = "Ajouter un contact")
    @PostMapping
    public ResponseEntity<ApiResponse<ContactResponse>> creer(
            @PathVariable Long clientId,
            @Valid @RequestBody ContactRequest request,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                contactService.creer(clientId, request, proprietaire.getId()),
                "Contact créé."));
    }

    @Operation(summary = "Modifier un contact")
    @PutMapping("/{contactId}")
    public ResponseEntity<ApiResponse<ContactResponse>> modifier(
            @PathVariable Long clientId,
            @PathVariable Long contactId,
            @Valid @RequestBody ContactRequest request,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {

        return ResponseEntity.ok(ApiResponse.success(
                contactService.modifier(contactId, clientId, request, proprietaire.getId()),
                "Contact modifié."));
    }

    @Operation(summary = "Supprimer un contact")
    @DeleteMapping("/{contactId}")
    public ResponseEntity<ApiResponse<Void>> supprimer(
            @PathVariable Long clientId,
            @PathVariable Long contactId,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {

        contactService.supprimer(contactId, clientId, proprietaire.getId());
        return ResponseEntity.ok(ApiResponse.success("Contact supprimé."));
    }
}