package com.crm.modules.vente.service;

import com.crm.modules.client.entity.Client;
import com.crm.modules.client.entity.ClientEntreprise;
import com.crm.modules.client.entity.ClientIndividuel;
import com.crm.modules.client.repository.ClientRepository;
import com.crm.modules.contact.entity.Contact;
import com.crm.modules.contact.repository.ContactRepository;
import com.crm.modules.reporting.service.IActiviteService;
import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import com.crm.modules.utilisateur.repository.ProprietaireRepository;
import com.crm.modules.vente.dto.LeadQualifieRequest;
import com.crm.modules.vente.dto.LeadRequest;
import com.crm.modules.vente.dto.LeadResponse;
import com.crm.modules.vente.dto.OpportuniteResponse;
import com.crm.modules.vente.entity.Lead;
import com.crm.modules.vente.entity.Opportunite;
import com.crm.modules.vente.mapper.LeadMapper;
import com.crm.modules.vente.mapper.OpportuniteMapper;
import com.crm.modules.vente.repository.LeadRepository;
import com.crm.modules.vente.repository.OpportuniteRepository;
import com.crm.shared.enums.StatutLead;
import com.crm.shared.enums.StatutOpportunite;
import com.crm.shared.enums.TypeActivite;
import com.crm.shared.exception.BusinessException;
import com.crm.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Riahi Dorsaf
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class LeadService implements ILeadService {

    private final LeadRepository leadRepository;
    private final OpportuniteRepository opportuniteRepository;
    private final ClientRepository clientRepository;
    private final ContactRepository contactRepository;          // ← AJOUT
    private final ProprietaireRepository proprietaireRepository;
    private final LeadMapper leadMapper;
    private final OpportuniteMapper opportuniteMapper;
    private final IActiviteService activiteService;

    // ─────────────────────────────────────────────────────────
    //  LISTE
    // ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    @Override
    public List<LeadResponse> lister(Long proprietaireId,
                                     StatutLead statut,
                                     String keyword) {
        List<Lead> leads = (statut != null)
                ? leadRepository.findByProprietaireIdAndStatutOrderByDateCreationDesc(proprietaireId, statut)
                : leadRepository.findByProprietaireIdOrderByDateCreationDesc(proprietaireId);

        return leads.stream()
                .filter(l -> keyword == null || keyword.isBlank()
                        || l.getNom().toLowerCase().contains(keyword.toLowerCase())
                        || (l.getEmail() != null && l.getEmail().toLowerCase().contains(keyword.toLowerCase()))
                        || (l.getEntreprise() != null && l.getEntreprise().toLowerCase().contains(keyword.toLowerCase())))
                .map(this::enrichir)
                .toList();
    }

    // ─────────────────────────────────────────────────────────
    //  DÉTAIL
    // ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    @Override
    public LeadResponse obtenir(Long id, Long proprietaireId) {
        return enrichir(charger(id, proprietaireId));
    }

    // ─────────────────────────────────────────────────────────
    //  CRÉATION
    // ─────────────────────────────────────────────────────────

    @Override
    public LeadResponse creer(LeadRequest req, Long proprietaireId) {
        ProprietaireEntreprise proprietaire = chargerProprietaire(proprietaireId);

        Lead lead = Lead.builder()
                .nom(req.getNom())
                .email(req.getEmail())
                .telephone(req.getTelephone())
                .entreprise(req.getEntreprise())
                .poste(req.getPoste())
                .source(req.getSource())
                .descriptionBesoin(req.getDescriptionBesoin())
                .statut(StatutLead.NOUVEAU)
                .score(calculerScore(req))
                .proprietaire(proprietaire)
                .build();

        if (req.getClientId() != null) {
            lead.setClient(chargerClient(req.getClientId(), proprietaireId));
        }

        lead = leadRepository.save(lead);
        log.info("[LEAD] Créé — id={} nom={}", lead.getId(), lead.getNom());

        activiteService.enregistrer(
                TypeActivite.LEAD_CREE, "Nouveau lead ajouté", lead.getNom(),
                lead.getId(), "LEAD", null, proprietaire);

        return enrichir(lead);
    }

    // ─────────────────────────────────────────────────────────
    //  CRÉATION AUTOMATISÉE (lead déjà qualifié — ex. Messenger)
    // ─────────────────────────────────────────────────────────

    @Override
    public LeadResponse creerQualifie(LeadQualifieRequest req, Long proprietaireId) {
        ProprietaireEntreprise proprietaire = chargerProprietaire(proprietaireId);

        Lead lead = Lead.builder()
                .nom(req.getNom())
                .email(req.getEmail())
                .telephone(req.getTelephone())
                .source(req.getSource())
                .descriptionBesoin(req.getResume())
                .statut(StatutLead.QUALIFIE)
                .score(borner(req.getScore()))
                .proprietaire(proprietaire)
                .build();

        lead = leadRepository.save(lead);
        log.info("[LEAD] Créé (qualifié auto) — id={} nom={} source={} score={}",
                lead.getId(), lead.getNom(), lead.getSource(), lead.getScore());

        activiteService.enregistrer(
                TypeActivite.LEAD_CREE, "Lead qualifié automatiquement", lead.getNom(),
                lead.getId(), "LEAD", null, proprietaire);

        return enrichir(lead);
    }

    // ─────────────────────────────────────────────────────────
    //  MODIFICATION
    // ─────────────────────────────────────────────────────────

    @Override
    public LeadResponse modifier(Long id, LeadRequest req, Long proprietaireId) {
        Lead lead = charger(id, proprietaireId);

        lead.setNom(req.getNom());
        lead.setEmail(req.getEmail());
        lead.setTelephone(req.getTelephone());
        lead.setEntreprise(req.getEntreprise());
        lead.setPoste(req.getPoste());
        lead.setSource(req.getSource());
        lead.setDescriptionBesoin(req.getDescriptionBesoin());
        lead.setScore(calculerScore(req));

        if (req.getClientId() != null) {
            lead.setClient(chargerClient(req.getClientId(), proprietaireId));
        }

        lead = leadRepository.save(lead);
        log.info("[LEAD] Modifié — id={}", id);

        activiteService.enregistrer(
                TypeActivite.LEAD_MODIFIE, "Lead mis à jour", lead.getNom(),
                lead.getId(), "LEAD", null, lead.getProprietaire());

        return enrichir(lead);
    }

    // ─────────────────────────────────────────────────────────
    //  CHANGEMENT DE STATUT
    // ─────────────────────────────────────────────────────────

    @Override
    public LeadResponse changerStatut(Long id, StatutLead nouveauStatut,
                                      String raisonPerte, Long proprietaireId) {
        Lead lead = charger(id, proprietaireId);

        if (lead.getStatut() == StatutLead.CONVERTI) {
            throw new BusinessException("Un lead converti ne peut plus changer de statut.");
        }

        lead.setStatut(nouveauStatut);
        if (nouveauStatut == StatutLead.PERDU && raisonPerte != null) {
            lead.setRaisonPerte(raisonPerte);
        }

        lead = leadRepository.save(lead);

        TypeActivite typeActivite = (nouveauStatut == StatutLead.PERDU)
                ? TypeActivite.LEAD_PERDU : TypeActivite.LEAD_QUALIFIE;

        activiteService.enregistrer(
                typeActivite, "Statut lead : " + nouveauStatut.name(), lead.getNom(),
                lead.getId(), "LEAD", null, lead.getProprietaire());

        return enrichir(lead);
    }

    // ─────────────────────────────────────────────────────────
    //  CONVERSION → OPPORTUNITÉ
    // ─────────────────────────────────────────────────────────

    @Override
    public OpportuniteResponse convertirEnOpportunite(Long leadId,
                                                      Long clientExistantId,
                                                      boolean creerNouveauClient,
                                                      String titreOpportunite,
                                                      Long proprietaireId) {
        Lead lead = charger(leadId, proprietaireId);
        ProprietaireEntreprise proprietaire = chargerProprietaire(proprietaireId);

        if (lead.getStatut() == StatutLead.CONVERTI) {
            throw new BusinessException("Ce lead a déjà été converti.");
        }

        Client client = resoudreClient(lead, clientExistantId, creerNouveauClient,
                proprietaireId, proprietaire);
        Opportunite opportunite = creerOpportunite(lead, client, titreOpportunite, proprietaire);

        lead.setStatut(StatutLead.CONVERTI);
        lead.setClient(client);
        leadRepository.save(lead);

        enregistrerActivitesConversion(lead, opportunite, proprietaire);
        log.info("[LEAD] Converti — leadId={} opportuniteId={}", leadId, opportunite.getId());
        return opportuniteMapper.toResponse(opportunite);
    }

    // ─────────────────────────────────────────────────────────
    //  SUPPRESSION
    // ─────────────────────────────────────────────────────────

    @Override
    public void supprimer(Long id, Long proprietaireId) {
        Lead lead = charger(id, proprietaireId);
        leadRepository.deleteById(lead.getId());
        log.info("[LEAD] Supprimé — id={}", id);
    }

    // ─────────────────────────────────────────────────────────
    //  HELPERS PRIVÉS — RÉSOLUTION CLIENT
    // ─────────────────────────────────────────────────────────

    private Client resoudreClient(Lead lead, Long clientExistantId,
                                  boolean creerNouveauClient,
                                  Long proprietaireId,
                                  ProprietaireEntreprise proprietaire) {
        if (clientExistantId != null) {
            return chargerClient(clientExistantId, proprietaireId);
        }
        if (!creerNouveauClient) {
            throw new BusinessException(
                    "Veuillez sélectionner un client ou activer la création automatique.");
        }
        boolean avecEntreprise = lead.getEntreprise() != null
                && !lead.getEntreprise().isBlank();
        return avecEntreprise
                ? creerClientEntrepriseDepuisLead(lead, proprietaire)
                : creerClientIndividuelDepuisLead(lead, proprietaire);
    }

    private Client creerClientIndividuelDepuisLead(Lead lead,
                                                   ProprietaireEntreprise proprietaire) {
        ClientIndividuel client = new ClientIndividuel();
        String[] parts = lead.getNom().trim().split("\\s+", 2);
        client.setPrenom(parts.length > 1 ? parts[0] : lead.getNom());
        client.setNom(parts.length > 1 ? parts[1] : "");
        client.setEmail(lead.getEmail());
        client.setTelephone(lead.getTelephone());
        client.setProprietaire(proprietaire);
        client.recalculerNomAffichage();
        Client saved = clientRepository.save(client);
        log.info("[LEAD] Client individuel créé depuis lead — clientId={}", saved.getId());
        return saved;
    }

    private Client creerClientEntrepriseDepuisLead(Lead lead,
                                                   ProprietaireEntreprise proprietaire) {
        ClientEntreprise entreprise = new ClientEntreprise();
        entreprise.setRaisonSociale(lead.getEntreprise());
        entreprise.setEmail(lead.getEmail());
        entreprise.setTelephone(lead.getTelephone());
        entreprise.setProprietaire(proprietaire);
        entreprise.recalculerNomAffichage();
        Client client = clientRepository.save(entreprise);
        log.info("[LEAD] Client entreprise créé depuis lead — clientId={}", client.getId());
        creerContactPrincipalDepuisLead(lead, client);
        return client;
    }

    private void creerContactPrincipalDepuisLead(Lead lead, Client client) {
        String[] parts = lead.getNom().trim().split("\\s+", 2);
        Contact contact = Contact.builder()
                .nom(parts.length > 1 ? parts[1] : lead.getNom())
                .prenom(parts.length > 1 ? parts[0] : null)
                .email(lead.getEmail())
                .telephone(lead.getTelephone())
                .poste(lead.getPoste())
                .isPrincipal(true)
                .client(client)
                .build();
        Contact saved = contactRepository.save(contact);
        log.info("[LEAD] Contact principal créé pour entreprise — contactId={}", saved.getId());
    }

    // ─────────────────────────────────────────────────────────
    //  HELPERS PRIVÉS — CRÉATION OPPORTUNITÉ + ACTIVITÉS
    // ─────────────────────────────────────────────────────────

    private Opportunite creerOpportunite(Lead lead, Client client,
                                         String titreOpportunite,
                                         ProprietaireEntreprise proprietaire) {
        String titre = (titreOpportunite != null && !titreOpportunite.isBlank())
                ? titreOpportunite
                : "Opportunité — " + lead.getNom();
        Opportunite opportunite = Opportunite.builder()
                .titre(titre)
                .statut(StatutOpportunite.PROSPECTION)
                .client(client)
                .lead(lead)
                .proprietaire(proprietaire)
                .build();
        return opportuniteRepository.save(opportunite);
    }

    private void enregistrerActivitesConversion(Lead lead, Opportunite opportunite,
                                                ProprietaireEntreprise proprietaire) {
        activiteService.enregistrer(
                TypeActivite.LEAD_CONVERTI, "Lead converti en opportunité", lead.getNom(),
                lead.getId(), "LEAD", null, proprietaire);
        activiteService.enregistrer(
                TypeActivite.OPPORTUNITE_CREEE, "Opportunité créée", opportunite.getTitre(),
                opportunite.getId(), "OPPORTUNITE", null, proprietaire);
    }

    // ─────────────────────────────────────────────────────────
    //  HELPERS PRIVÉS — CHARGEMENT + SCORING
    // ─────────────────────────────────────────────────────────

    private Lead charger(Long id, Long proprietaireId) {
        return leadRepository.findByIdAndProprietaireId(id, proprietaireId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead introuvable"));
    }

    private Client chargerClient(Long clientId, Long proprietaireId) {
        return clientRepository.findByIdAndProprietaireIdAndIsDeletedFalse(clientId, proprietaireId)
                .orElseThrow(() -> new ResourceNotFoundException("Client introuvable"));
    }

    private ProprietaireEntreprise chargerProprietaire(Long id) {
        return proprietaireRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Propriétaire introuvable"));
    }

    /**
     * Score simple côté serveur (0-100).
     * Critères : email renseigné (+20), téléphone (+20),
     * description besoin (+30), entreprise (+15), poste (+15).
     */
    private int calculerScore(LeadRequest req) {
        int score = 0;
        if (req.getEmail() != null && !req.getEmail().isBlank()) score += 20;
        if (req.getTelephone() != null && !req.getTelephone().isBlank()) score += 20;
        if (req.getDescriptionBesoin() != null && !req.getDescriptionBesoin().isBlank()) score += 30;
        if (req.getEntreprise() != null && !req.getEntreprise().isBlank()) score += 15;
        if (req.getPoste() != null && !req.getPoste().isBlank()) score += 15;
        return score;
    }

    /** Borne un score externe (LLM) dans l'intervalle [0, 100] ; null → 0. */
    private int borner(Integer score) {
        if (score == null) return 0;
        return Math.max(0, Math.min(100, score));
    }

    private LeadResponse enrichir(Lead lead) {
        LeadResponse response = leadMapper.toResponse(lead);
        response.setDateRelative(dateRelative(lead.getDateCreation()));
        return response;
    }

    private String dateRelative(LocalDateTime date) {
        if (date == null) return "";
        Duration d = Duration.between(date, LocalDateTime.now());
        long minutes = d.toMinutes();
        if (minutes < 1) return "à l'instant";
        if (minutes < 60) return "il y a " + minutes + " min";
        long h = d.toHours();
        if (h < 24) return "il y a " + h + "h";
        long j = d.toDays();
        if (j < 30) return "il y a " + j + "j";
        return "il y a " + (j / 30) + " mois";
    }
}