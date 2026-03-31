package com.crm.modules.entreprise.service;

import com.crm.modules.auth.service.EmailService;
import com.crm.modules.entreprise.dto.*;
import com.crm.modules.entreprise.entity.EntrepriseCompte;
import com.crm.modules.entreprise.repository.EntrepriseCompteRepository;
import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import com.crm.modules.utilisateur.entity.SuperAdmin;
import com.crm.modules.utilisateur.entity.Utilisateur;
import com.crm.modules.utilisateur.repository.ProprietaireRepository;
import com.crm.modules.utilisateur.repository.SuperAdminRepository;
import com.crm.modules.utilisateur.repository.UtilisateurRepository;
import com.crm.shared.enums.RoleUtilisateur;
import com.crm.shared.enums.StatutCompte;
import com.crm.shared.exception.BusinessException;
import com.crm.shared.exception.ResourceNotFoundException;
import com.crm.shared.response.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service métier pour la gestion des comptes entreprises.
 *
 * DEV-18 : Création d'un compte entreprise (mobile)
 * DEV-19 : Afficher statut "compte en attente" (mobile)
 * DEV-20 : Consulter la liste des comptes en attente (Angular admin)
 * DEV-21 : Valider un compte entreprise (Angular admin)
 * DEV-53 : Consulter les détails d'une entreprise (Angular admin)
 * DEV-54 : Supprimer une entreprise logiquement (Angular admin)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EntrepriseService implements IEntrepriseService {


    private final EntrepriseCompteRepository entrepriseRepository;
    private final ProprietaireRepository proprietaireRepository;
    private final SuperAdminRepository superAdminRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final EntrepriseMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    // ─────────────────────────────────────────────────────────────────────────
    // DEV-18 : Inscription (création compte entreprise depuis mobile)
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public EntrepriseCompteResponse inscrireEntreprise(InscriptionEntrepriseRequest request) {
        // Unicité email
        if (utilisateurRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Un compte existe déjà avec l'email : " + request.getEmail());
        }
        // Unicité matricule fiscale
        if (entrepriseRepository.existsByMatriculeFiscale(request.getMatriculeFiscale())) {
            throw new BusinessException("Un compte existe déjà avec la matricule fiscale : "
                    + request.getMatriculeFiscale());
        }

        // Création EntrepriseCompte (statut EN_ATTENTE par défaut via @PrePersist)
        EntrepriseCompte entreprise = EntrepriseCompte.builder()
                .nomEntreprise(request.getNomEntreprise())
                .matriculeFiscale(request.getMatriculeFiscale())
                .secteurActivite(request.getSecteurActivite())
                .tailleEntreprise(request.getTailleEntreprise())
                .telephone(request.getTelephoneEntreprise())
                .adresse(request.getAdresse())
                .ville(request.getVille())
                .pays(request.getPays())
                .siteWeb(request.getSiteWeb())
                .build();

        // Création ProprietaireEntreprise
        ProprietaireEntreprise proprietaire = new ProprietaireEntreprise();
        proprietaire.setNom(request.getNom());
        proprietaire.setPrenom(request.getPrenom());
        proprietaire.setEmail(request.getEmail());
        proprietaire.setMotDePasseHash(passwordEncoder.encode(request.getMotDePasse()));
        proprietaire.setTelephone(request.getTelephone());
        proprietaire.setRole(RoleUtilisateur.ROLE_PROPRIETAIRE);
        proprietaire.setEntrepriseCompte(entreprise); // Cascade → sauvegarde aussi EntrepriseCompte

        proprietaireRepository.save(proprietaire);

        log.info("Nouveau compte entreprise créé : {} ({})",
                request.getNomEntreprise(), request.getEmail());

        // Email de bienvenue asynchrone
        emailService.envoyerEmailBienvenue(
                request.getEmail(),
                request.getPrenom() + " " + request.getNom(),
                request.getNomEntreprise()
        );

        return mapper.toResponseWithProprietaire(proprietaire.getEntrepriseCompte(), proprietaire);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DEV-19 : Consulter le statut de son compte (mobile, connecté)
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public EntrepriseCompteResponse consulterMonStatut(String emailProprietaire) {
        ProprietaireEntreprise proprietaire = proprietaireRepository
                .findByEmail(emailProprietaire)
                .orElseThrow(() -> new ResourceNotFoundException("Propriétaire introuvable."));

        EntrepriseCompte entreprise = proprietaire.getEntrepriseCompte();
        if (entreprise == null) {
            throw new BusinessException("Aucun compte entreprise associé.");
        }

        return mapper.toResponseWithProprietaire(entreprise, proprietaire);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DEV-20 : Lister les comptes en attente (Angular admin)
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<EntrepriseCompteResponse> listerComptesEnAttente(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("dateCreation").descending());
        Page<EntrepriseCompte> pageResult = entrepriseRepository
                .findByStatutCompteAndIsDeletedFalse(StatutCompte.EN_ATTENTE, pageable);

        Page<EntrepriseCompteResponse> mapped = pageResult.map(e ->
                mapper.toResponseWithProprietaire(e, trouverProprietaire(e)));

        return PageResponse.from(mapped);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DEV-20 (extension) : Lister toutes les entreprises avec filtre (admin)
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<EntrepriseCompteResponse> listerEntreprises(
            StatutCompte statut, String keyword, int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("dateCreation").descending());
        Page<EntrepriseCompte> pageResult;

        if (keyword != null && !keyword.isBlank()) {
            pageResult = entrepriseRepository.searchByKeyword(keyword, pageable);
        } else {
            pageResult = entrepriseRepository.findAllActiveWithFilter(statut, pageable);
        }

        Page<EntrepriseCompteResponse> mapped = pageResult.map(e ->
                mapper.toResponseWithProprietaire(e, trouverProprietaire(e)));

        return PageResponse.from(mapped);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DEV-21 : Valider ou refuser un compte entreprise (admin)
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public EntrepriseCompteResponse traiterDemande(
            Long entrepriseId,
            ValiderEntrepriseRequest request,
            String emailAdmin) {

        EntrepriseCompte entreprise = entrepriseRepository
                .findByIdAndIsDeletedFalse(entrepriseId)
                .orElseThrow(() -> new ResourceNotFoundException("EntrepriseCompte", entrepriseId));

        if (entreprise.getStatutCompte() != StatutCompte.EN_ATTENTE) {
            throw new BusinessException(
                "Ce compte a déjà été traité. Statut actuel : " + entreprise.getStatutCompte());
        }

        SuperAdmin admin = superAdminRepository.findByEmail(emailAdmin)
                .orElseThrow(() -> new ResourceNotFoundException("SuperAdmin introuvable."));

        ProprietaireEntreprise proprietaire = trouverProprietaire(entreprise);

        if (Boolean.TRUE.equals(request.getValider())) {
            entreprise.valider(admin.getId());
            log.info("Entreprise {} validée par {}", entreprise.getNomEntreprise(), emailAdmin);
            emailService.envoyerEmailValidation(
                    proprietaire.getEmail(),
                    entreprise.getNomEntreprise()
            );
        } else {
            if (request.getMotifRefus() == null || request.getMotifRefus().isBlank()) {
                throw new BusinessException("Le motif de refus est obligatoire.");
            }
            entreprise.refuser(admin.getId(), request.getMotifRefus());
            log.info("Entreprise {} refusée par {} — Motif : {}",
                    entreprise.getNomEntreprise(), emailAdmin, request.getMotifRefus());
            emailService.envoyerEmailRefus(
                    proprietaire.getEmail(),
                    entreprise.getNomEntreprise(),
                    request.getMotifRefus()
            );
        }

        EntrepriseCompte saved = entrepriseRepository.save(entreprise);
        return mapper.toResponseWithProprietaire(saved, proprietaire);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DEV-53 : Consulter les détails d'une entreprise (admin)
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public EntrepriseCompteResponse consulterDetails(Long entrepriseId) {
        EntrepriseCompte entreprise = entrepriseRepository
                .findByIdAndIsDeletedFalse(entrepriseId)
                .orElseThrow(() -> new ResourceNotFoundException("EntrepriseCompte", entrepriseId));

        ProprietaireEntreprise proprietaire = trouverProprietaire(entreprise);
        return mapper.toResponseWithProprietaire(entreprise, proprietaire);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DEV-54 : Suppression logique d'une entreprise (admin)
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public void supprimerLogiquement(Long entrepriseId, String emailAdmin) {
        EntrepriseCompte entreprise = entrepriseRepository
                .findByIdAndIsDeletedFalse(entrepriseId)
                .orElseThrow(() -> new ResourceNotFoundException("EntrepriseCompte", entrepriseId));

        entreprise.supprimerLogiquement();
        entrepriseRepository.save(entreprise);

        log.info("Entreprise {} supprimée logiquement par {}",
                entreprise.getNomEntreprise(), emailAdmin);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers internes
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Retrouve le ProprietaireEntreprise lié à un EntrepriseCompte.
     * Exploite la relation inverse via une requête JPQL.
     */
    private ProprietaireEntreprise trouverProprietaire(EntrepriseCompte entreprise) {
        return proprietaireRepository.findByEntrepriseCompteId(entreprise.getId()).orElse(null);
    }
}
