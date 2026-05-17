package com.crm.modules.utilisateur.service;

import com.crm.modules.entreprise.entity.EntrepriseCompte;
import com.crm.modules.notification.entity.TypeNotification;
import com.crm.modules.notification.service.NotificationService;
import com.crm.modules.utilisateur.dto.ModifierEntrepriseRequest;
import com.crm.modules.utilisateur.dto.ModifierProfilRequest;
import com.crm.modules.utilisateur.dto.ProfilProprietaireResponse;
import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import com.crm.modules.utilisateur.repository.ProprietaireRepository;
import com.crm.shared.exception.BusinessException;
import com.crm.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service de gestion du profil du propriétaire d'entreprise.
 *
 * @author Riahi Dorsaf
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProprietaireService {

    private final ProprietaireRepository proprietaireRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public ProfilProprietaireResponse consulterProfil(String email) {
        ProprietaireEntreprise proprietaire = chargerProprietaire(email);
        return construireReponse(proprietaire);
    }

    @Transactional
    public ProfilProprietaireResponse modifierProfil(
            String email, ModifierProfilRequest request) {

        ProprietaireEntreprise proprietaire = chargerProprietaire(email);

        proprietaire.setNom(request.getNom());
        proprietaire.setPrenom(request.getPrenom());
        proprietaire.setTelephone(request.getTelephone());

        proprietaireRepository.save(proprietaire);
        log.info("[PROFIL] Données personnelles mises à jour pour : {}", email);

        return construireReponse(proprietaire);
    }

    @Transactional
    public ProfilProprietaireResponse modifierEntreprise(
            String email, ModifierEntrepriseRequest request) {

        ProprietaireEntreprise proprietaire = chargerProprietaire(email);
        EntrepriseCompte entreprise = proprietaire.getEntrepriseCompte();

        if (entreprise == null) {
            throw new BusinessException("Aucun compte entreprise associé.");
        }

        entreprise.setNomEntreprise(request.getNomEntreprise());
        entreprise.setSecteurActivite(request.getSecteurActivite());
        entreprise.setTailleEntreprise(request.getTailleEntreprise());
        entreprise.setTelephone(request.getTelephone());
        entreprise.setAdresse(request.getAdresse());
        entreprise.setVille(request.getVille());
        entreprise.setPays(request.getPays());
        entreprise.setSiteWeb(request.getSiteWeb());

        proprietaireRepository.save(proprietaire);

        log.info("[PROFIL] Données entreprise mises à jour pour : {} (entreprise : {})",
                email, entreprise.getNomEntreprise());

        // Notification au Super Admin
        notificationService.creerNotification(
                "Modification entreprise",
                "Le propriétaire de \"" + entreprise.getNomEntreprise()
                        + "\" a modifié les données de son entreprise.",
                TypeNotification.MODIFICATION_ENTREPRISE,
                "/admin/entreprises/" + entreprise.getId()
        );

        return construireReponse(proprietaire);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ProprietaireEntreprise chargerProprietaire(String email) {
        return proprietaireRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Propriétaire introuvable."));
    }

    private ProfilProprietaireResponse construireReponse(ProprietaireEntreprise p) {
        ProfilProprietaireResponse.ProfilProprietaireResponseBuilder builder =
                ProfilProprietaireResponse.builder()
                        .userId(p.getId())
                        .nom(p.getNom())
                        .prenom(p.getPrenom())
                        .email(p.getEmail())
                        .telephone(p.getTelephone());

        if (p.getEntrepriseCompte() != null) {
            EntrepriseCompte e = p.getEntrepriseCompte();
            builder.entrepriseId(e.getId())
                    .nomEntreprise(e.getNomEntreprise())
                    .secteurActivite(e.getSecteurActivite())
                    .tailleEntreprise(e.getTailleEntreprise() != null
                            ? e.getTailleEntreprise().name() : null)
                    .telephoneEntreprise(e.getTelephone())
                    .adresse(e.getAdresse())
                    .ville(e.getVille())
                    .pays(e.getPays())
                    .siteWeb(e.getSiteWeb());
        }

        return builder.build();
    }
}