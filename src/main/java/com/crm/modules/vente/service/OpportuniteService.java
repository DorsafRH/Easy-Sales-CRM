package com.crm.modules.vente.service;

import com.crm.modules.client.repository.ClientRepository;
import com.crm.modules.reporting.service.IActiviteService;
import com.crm.modules.utilisateur.repository.ProprietaireRepository;
import com.crm.modules.vente.dto.DevisResponse;
import com.crm.modules.vente.dto.OpportuniteRequest;
import com.crm.modules.vente.dto.OpportuniteResponse;
import com.crm.modules.vente.entity.Devis;
import com.crm.modules.vente.entity.Opportunite;
import com.crm.modules.vente.mapper.DevisMapper;
import com.crm.modules.vente.mapper.OpportuniteMapper;
import com.crm.modules.vente.repository.DevisRepository;
import com.crm.modules.vente.repository.LeadRepository;
import com.crm.modules.vente.repository.OpportuniteRepository;
import com.crm.shared.enums.StatutDevis;
import com.crm.shared.enums.StatutOpportunite;
import com.crm.shared.enums.TypeActivite;
import com.crm.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Riahi Dorsaf
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OpportuniteService implements IOpportuniteService {

    private final OpportuniteRepository opportuniteRepository;
    private final LeadRepository leadRepository;
    private final ClientRepository clientRepository;
    private final DevisRepository devisRepository;
    private final ProprietaireRepository proprietaireRepository;
    private final OpportuniteMapper opportuniteMapper;
    private final DevisMapper devisMapper;
    private final IActiviteService activiteService;

    @Transactional(readOnly = true)
    @Override
    public List<OpportuniteResponse> lister(Long proprietaireId,
                                            StatutOpportunite statut,
                                            String keyword) {
        List<Opportunite> list = (statut != null)
                ? opportuniteRepository.findByProprietaireIdAndStatutOrderByMontantEstimeDesc(proprietaireId, statut)
                : opportuniteRepository.findByProprietaireIdOrderByDateCreationDesc(proprietaireId);

        return list.stream()
                .filter(o -> keyword == null || keyword.isBlank()
                        || o.getTitre().toLowerCase().contains(keyword.toLowerCase())
                        || o.getClient().getNomAffichage().toLowerCase().contains(keyword.toLowerCase()))
                .map(this::enrichir)
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public Map<StatutOpportunite, List<OpportuniteResponse>> listerParStatut(Long proprietaireId) {
        List<Opportunite> toutes = opportuniteRepository
                .findByProprietaireIdOrderByDateCreationDesc(proprietaireId);

        Map<StatutOpportunite, List<OpportuniteResponse>> map = new LinkedHashMap<>();
        for (StatutOpportunite s : StatutOpportunite.values()) {
            map.put(s, new ArrayList<>());
        }
        toutes.forEach(o -> map.get(o.getStatut()).add(enrichir(o)));
        return map;
    }

    @Transactional(readOnly = true)
    @Override
    public OpportuniteResponse obtenir(Long id, Long proprietaireId) {
        return enrichir(charger(id, proprietaireId));
    }

    @Override
    public OpportuniteResponse creer(OpportuniteRequest req, Long proprietaireId) {
        var proprietaire = proprietaireRepository.findById(proprietaireId)
                .orElseThrow(() -> new ResourceNotFoundException("Propriétaire introuvable"));
        var client = clientRepository.findByIdAndProprietaireIdAndIsDeletedFalse(req.getClientId(), proprietaireId)
                .orElseThrow(() -> new ResourceNotFoundException("Client introuvable"));

        Opportunite o = Opportunite.builder()
                .titre(req.getTitre())
                .description(req.getDescription())
                .montantEstime(req.getMontantEstime())
                .probabilite(req.getProbabilite())
                .statut(req.getStatut() != null ? req.getStatut() : StatutOpportunite.PROSPECTION)
                .dateCloturePrevue(req.getDateCloturePrevue())
                .client(client)
                .proprietaire(proprietaire)
                .build();

        if (req.getLeadId() != null) {
            o.setLead(leadRepository.findByIdAndProprietaireId(req.getLeadId(), proprietaireId)
                    .orElseThrow(() -> new ResourceNotFoundException("Lead introuvable")));
        }

        o = opportuniteRepository.save(o);
        log.info("[OPPORTUNITE] Créée — id={}", o.getId());

        activiteService.enregistrer(
                TypeActivite.OPPORTUNITE_CREEE, "Opportunité créée", o.getTitre(),
                o.getId(), "OPPORTUNITE", null, proprietaire);

        return enrichir(o);
    }

    @Override
    public OpportuniteResponse modifier(Long id, OpportuniteRequest req, Long proprietaireId) {
        Opportunite o = charger(id, proprietaireId);

        o.setTitre(req.getTitre());
        o.setDescription(req.getDescription());
        o.setMontantEstime(req.getMontantEstime());
        o.setProbabilite(req.getProbabilite());
        o.setDateCloturePrevue(req.getDateCloturePrevue());
        if (req.getStatut() != null) o.setStatut(req.getStatut());

        o = opportuniteRepository.save(o);

        activiteService.enregistrer(
                TypeActivite.OPPORTUNITE_MODIFIEE, "Opportunité mise à jour", o.getTitre(),
                o.getId(), "OPPORTUNITE", null, o.getProprietaire());

        return enrichir(o);
    }

    @Override
    public OpportuniteResponse changerStatut(Long id, StatutOpportunite nouveauStatut,
                                             String raisonPerte, Long proprietaireId) {
        Opportunite o = charger(id, proprietaireId);

        o.setStatut(nouveauStatut);
        if (nouveauStatut == StatutOpportunite.PERDUE && raisonPerte != null) {
            o.setRaisonPerte(raisonPerte);
        }

        o = opportuniteRepository.save(o);

        TypeActivite type = switch (nouveauStatut) {
            case GAGNEE -> TypeActivite.OPPORTUNITE_GAGNEE;
            case PERDUE -> TypeActivite.OPPORTUNITE_PERDUE;
            default -> TypeActivite.OPPORTUNITE_STATUT_CHANGE;
        };

        activiteService.enregistrer(
                type, "Statut : " + nouveauStatut.name(), o.getTitre(),
                o.getId(), "OPPORTUNITE", null, o.getProprietaire());

        return enrichir(o);
    }

    @Override
    public DevisResponse genererDevis(Long opportuniteId, Long proprietaireId) {
        Opportunite o = charger(opportuniteId, proprietaireId);
        var proprietaire = o.getProprietaire();

        String numero = genererNumeroDevis(proprietaireId);

        Devis devis = Devis.builder()
                .numero(numero)
                .statut(StatutDevis.BROUILLON)
                .client(o.getClient())
                .opportunite(o)
                .proprietaire(proprietaire)
                .build();

        devis = devisRepository.save(devis);
        log.info("[DEVIS] Généré depuis opportunité — devisId={} opId={}", devis.getId(), opportuniteId);

        activiteService.enregistrer(
                TypeActivite.DEVIS_CREE, "Devis créé", devis.getNumero(),
                devis.getId(), "DEVIS", null, proprietaire);

        return enrichirDevis(devis);
    }

    @Override
    public void supprimer(Long id, Long proprietaireId) {
        Opportunite o = charger(id, proprietaireId);
        opportuniteRepository.deleteById(o.getId());
        log.info("[OPPORTUNITE] Supprimée — id={}", id);
    }

    // ─────────────────────────────────────────────────────────

    private Opportunite charger(Long id, Long proprietaireId) {
        return opportuniteRepository.findByIdAndProprietaireId(id, proprietaireId)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunité introuvable"));
    }

    private String genererNumeroDevis(Long proprietaireId) {
        String annee = String.valueOf(Year.now().getValue());
        String prefixe = "DV-" + annee + "-";
        return devisRepository
                .findTopByProprietaireIdAndNumeroStartingWithOrderByNumeroDesc(proprietaireId, prefixe)
                .map(d -> {
                    int seq = Integer.parseInt(d.getNumero().substring(d.getNumero().length() - 4)) + 1;
                    return String.format("DV-%s-%04d", annee, seq);
                })
                .orElse(String.format("DV-%s-0001", annee));
    }

    private OpportuniteResponse enrichir(Opportunite o) {
        OpportuniteResponse r = opportuniteMapper.toResponse(o);
        r.setDateRelative(dateRelative(o.getDateCreation()));
        return r;
    }

    private DevisResponse enrichirDevis(Devis d) {
        DevisResponse r = devisMapper.toResponse(d);
        r.setDateRelative(dateRelative(d.getDateCreation()));
        r.setLignes(List.of());
        return r;
    }

    private String dateRelative(LocalDateTime date) {
        if (date == null) return "";
        Duration d = Duration.between(date, LocalDateTime.now());
        long min = d.toMinutes();
        if (min < 1) return "à l'instant";
        if (min < 60) return "il y a " + min + " min";
        long h = d.toHours();
        if (h < 24) return "il y a " + h + "h";
        return "il y a " + d.toDays() + "j";
    }
}