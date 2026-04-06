package com.crm.modules.entreprise.service;

import com.crm.modules.entreprise.specification.EntrepriseCompteSpecification;
import com.crm.modules.auth.service.EmailService;
import com.crm.modules.entreprise.dto.*;
import com.crm.modules.entreprise.entity.EntrepriseCompte;
import com.crm.modules.entreprise.repository.EntrepriseCompteRepository;
import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import com.crm.modules.utilisateur.entity.SuperAdmin;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service métier pour la gestion des comptes entreprises.
 *
 * @author Riahi Dorsaf
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EntrepriseService implements IEntrepriseService {

    private final EntrepriseCompteRepository entrepriseRepository;
    private final ProprietaireRepository     proprietaireRepository;
    private final SuperAdminRepository       superAdminRepository;
    private final UtilisateurRepository      utilisateurRepository;
    private final EntrepriseMapper           mapper;
    private final PasswordEncoder            passwordEncoder;
    private final EmailService               emailService;

    // ── Inscription ───────────────────────────────────────────────────────────

    @Transactional
    public EntrepriseCompteResponse inscrireEntreprise(InscriptionEntrepriseRequest request) {

        if (proprietaireRepository.existsByEmailAndCompteNonSupprime(request.getEmail())) {
            throw new BusinessException(
                    "Un compte existe déjà avec l'email : " + request.getEmail());
        }

        if (entrepriseRepository.existsByMatriculeFiscaleAndIsDeletedFalse(
                request.getMatriculeFiscale())) {
            throw new BusinessException(
                    "Un compte existe déjà avec la matricule fiscale : "
                            + request.getMatriculeFiscale());
        }

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

        ProprietaireEntreprise proprietaire = new ProprietaireEntreprise();
        proprietaire.setNom(request.getNom());
        proprietaire.setPrenom(request.getPrenom());
        proprietaire.setEmail(request.getEmail());
        proprietaire.setMotDePasseHash(passwordEncoder.encode(request.getMotDePasse()));
        proprietaire.setTelephone(request.getTelephone());
        proprietaire.setRole(RoleUtilisateur.ROLE_PROPRIETAIRE);
        proprietaire.setEntrepriseCompte(entreprise);

        proprietaireRepository.save(proprietaire);

        log.info("Nouveau compte entreprise créé : {} ({})",
                request.getNomEntreprise(), request.getEmail());

        emailService.envoyerEmailBienvenue(
                request.getEmail(),
                request.getPrenom() + " " + request.getNom(),
                request.getNomEntreprise()
        );

        return mapper.toResponseWithProprietaire(
                proprietaire.getEntrepriseCompte(), proprietaire);
    }

    // ── Statut compte (mobile) ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public EntrepriseCompteResponse consulterMonStatut(String emailProprietaire) {
        ProprietaireEntreprise proprietaire = proprietaireRepository
                .findByEmail(emailProprietaire)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Propriétaire introuvable."));

        EntrepriseCompte entreprise = proprietaire.getEntrepriseCompte();
        if (entreprise == null) {
            throw new BusinessException("Aucun compte entreprise associé.");
        }

        return mapper.toResponseWithProprietaire(entreprise, proprietaire);
    }

    // ── Liste comptes en attente (admin) ──────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<EntrepriseCompteResponse> listerComptesEnAttente(
            int page, int size) {

        Pageable pageable = PageRequest.of(page, size,
                Sort.by("dateCreation").descending());

        Page<EntrepriseCompte> pageResult = entrepriseRepository
                .findByStatutCompteAndIsDeletedFalse(StatutCompte.EN_ATTENTE, pageable);

        Page<EntrepriseCompteResponse> mapped = pageResult.map(e ->
                mapper.toResponseWithProprietaire(e, trouverProprietaire(e)));

        return PageResponse.from(mapped);
    }

    // ── Liste toutes les entreprises avec filtres (admin) ─────────────────────

    @Transactional(readOnly = true)
    public PageResponse<EntrepriseCompteResponse> listerEntreprises(
            StatutCompte statut, String keyword, int page, int size) {

        Pageable pageable = PageRequest.of(page, size,
                Sort.by("dateCreation").descending());

        Specification<EntrepriseCompte> spec =
                EntrepriseCompteSpecification.nonSupprime()
                        .and(EntrepriseCompteSpecification.avecStatut(statut))
                        .and(EntrepriseCompteSpecification.recherche(keyword));

        Page<EntrepriseCompte> pageResult =
                entrepriseRepository.findAll(spec, pageable);

        Page<EntrepriseCompteResponse> mapped = pageResult.map(e ->
                mapper.toResponseWithProprietaire(e, trouverProprietaire(e)));

        return PageResponse.from(mapped);
    }

    // ── Valider ou refuser un compte (admin) ──────────────────────────────────

    @Transactional
    public EntrepriseCompteResponse traiterDemande(
            Long entrepriseId,
            ValiderEntrepriseRequest request,
            String emailAdmin) {

        EntrepriseCompte entreprise = entrepriseRepository
                .findByIdAndIsDeletedFalse(entrepriseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "EntrepriseCompte", entrepriseId));

        if (entreprise.getStatutCompte() != StatutCompte.EN_ATTENTE) {
            throw new BusinessException(
                    "Ce compte a déjà été traité. Statut actuel : "
                            + entreprise.getStatutCompte());
        }

        SuperAdmin admin = superAdminRepository.findByEmail(emailAdmin)
                .orElseThrow(() ->
                        new ResourceNotFoundException("SuperAdmin introuvable."));

        ProprietaireEntreprise proprietaire = trouverProprietaire(entreprise);

        if (Boolean.TRUE.equals(request.getValider())) {
            entreprise.valider(admin.getId());
            log.info("Entreprise {} validée par {}",
                    entreprise.getNomEntreprise(), emailAdmin);
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

    // ── Détails d'une entreprise (admin) ──────────────────────────────────────

    @Transactional(readOnly = true)
    public EntrepriseCompteResponse consulterDetails(Long entrepriseId) {
        EntrepriseCompte entreprise = entrepriseRepository
                .findByIdAndIsDeletedFalse(entrepriseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "EntrepriseCompte", entrepriseId));

        ProprietaireEntreprise proprietaire = trouverProprietaire(entreprise);
        return mapper.toResponseWithProprietaire(entreprise, proprietaire);
    }

    // ── Suppression logique (admin) ───────────────────────────────────────────

    @Transactional
    public void supprimerLogiquement(Long entrepriseId, String emailAdmin) {
        EntrepriseCompte entreprise = entrepriseRepository
                .findByIdAndIsDeletedFalse(entrepriseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "EntrepriseCompte", entrepriseId));

        entreprise.supprimerLogiquement();
        entrepriseRepository.save(entreprise);

        log.info("Entreprise {} supprimée logiquement par {}",
                entreprise.getNomEntreprise(), emailAdmin);
    }

    // ── Helper interne ────────────────────────────────────────────────────────

    /**
     * Retrouve le ProprietaireEntreprise lié à un EntrepriseCompte.
     */
    private ProprietaireEntreprise trouverProprietaire(EntrepriseCompte entreprise) {
        return proprietaireRepository
                .findByEntrepriseCompteId(entreprise.getId())
                .orElse(null);
    }
}