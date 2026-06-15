package com.crm.modules.reporting.service;

import com.crm.modules.client.repository.ClientRepository;
import com.crm.modules.reporting.dto.ActiviteResponse;
import com.crm.modules.reporting.dto.CaMensuelDto;
import com.crm.modules.reporting.dto.ReportingKpisResponse;
import com.crm.modules.reporting.dto.ReportingKpisResponse.ActiviteRecenteItem;
import com.crm.modules.reporting.dto.StatsVentesResponse;
import com.crm.modules.reporting.mapper.ActiviteMapper;
import com.crm.modules.reporting.repository.ActiviteRepository;
import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import com.crm.modules.utilisateur.repository.ProprietaireRepository;
import com.crm.modules.vente.entity.Facture;
import com.crm.modules.vente.entity.Opportunite;
import com.crm.modules.vente.repository.DevisRepository;
import com.crm.modules.vente.repository.FactureRepository;
import com.crm.modules.vente.repository.LeadRepository;
import com.crm.modules.vente.repository.OpportuniteRepository;
import com.crm.shared.enums.StatutDevis;
import com.crm.shared.enums.StatutFacture;
import com.crm.shared.enums.StatutLead;
import com.crm.shared.enums.StatutOpportunite;
import com.crm.shared.exception.ResourceNotFoundException;
import com.crm.shared.response.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author Riahi Dorsaf
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportingService implements IReportingService {

    private final ClientRepository      clientRepository;
    private final ProprietaireRepository proprietaireRepository;
    private final ActiviteRepository    activiteRepository;
    private final ActiviteMapper        activiteMapper;
    private final OpportuniteRepository opportuniteRepository;
    private final DevisRepository       devisRepository;
    private final FactureRepository     factureRepository;
    private final LeadRepository        leadRepository;

    // ── Labels mois ──────────────────────────────────────────────────────────
    private static final String[] MOIS_LABELS = {
        "Jan","Fév","Mar","Avr","Mai","Jun","Jul","Aoû","Sep","Oct","Nov","Déc"
    };

    // ── Statuts constants ─────────────────────────────────────────────────────
    private static final List<StatutLead> LEADS_INACTIFS =
            List.of(StatutLead.CONVERTI, StatutLead.PERDU);

    private static final List<StatutOpportunite> OPPORT_ACTIVES =
            List.of(StatutOpportunite.PROSPECTION, StatutOpportunite.QUALIFICATION,
                    StatutOpportunite.PROPOSITION, StatutOpportunite.NEGOCIATION);

    private static final List<StatutDevis> DEVIS_NON_BROUILLON =
            List.of(StatutDevis.ENVOYE, StatutDevis.ACCEPTE,
                    StatutDevis.REFUSE, StatutDevis.EXPIRE);

    // ── Public API ────────────────────────────────────────────────────────────

    @Override
    public ReportingKpisResponse getKpis(String emailProprietaire, String periode) {
        ProprietaireEntreprise proprietaire = chargerProprietaire(emailProprietaire);
        long id = proprietaire.getId();
        LocalDateTime[] fenetre = construireFenetre(periode);

        return ReportingKpisResponse.builder()
                .nbClients(calculerNbClients(id, fenetre))
                .nbOpportunites(calculerNbOpportunites(id, fenetre))
                .nbDevis(calculerNbDevis(id, fenetre))
                .chiffreAffaires(calculerCA(id, fenetre))
                .sparkline(List.of(0, 0, 0, 0, 0, 0, 0))
                .activiteRecente(construireActiviteRecente(id))
                .build();
    }

    @Override
    public BigDecimal getChiffreAffairesMoisPrecedent(String emailProprietaire) {
        Long id = chargerProprietaire(emailProprietaire).getId();
        LocalDateTime[] fenetre = construireFenetreMoisPrecedent();
        return factureRepository.findByProprietaireIdAndStatutAndDatePaiementBetween(
                        id, StatutFacture.PAYEE, fenetre[0], fenetre[1])
                .stream()
                .map(Facture::getMontantTtc)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public StatsVentesResponse getStatsVentes(String emailProprietaire) {
        return getStatsVentes(chargerProprietaire(emailProprietaire).getId());
    }

    @Override
    public StatsVentesResponse getStatsVentes(Long id) {
        return StatsVentesResponse.builder()
                .nbLeadsActifs(calculerNbLeadsActifs(id))
                .tauxConversionLeads(calculerTauxConversionLeads(id))
                .valeurPipeline(calculerValeurPipeline(id))
                .tauxConversionOpportunites(calculerTauxConversionOpportunites(id))
                .tauxAcceptationDevis(calculerTauxAcceptationDevis(id))
                .panierMoyen(calculerPanierMoyen(id))
                .repartitionOpportunites(calculerRepartitionOpportunites(id))
                .top3Opportunites(calculerTop3Opportunites(id))
                .caAnnuel(calculerCaAnnuel(id))
                .build();
    }

    @Override
    public List<CaMensuelDto> getCaParMois(String emailProprietaire) {
        Long id = chargerProprietaire(emailProprietaire).getId();
        LocalDate today = LocalDate.now();
        List<CaMensuelDto> result = new ArrayList<>(12);
        for (int i = 11; i >= 0; i--) {
            LocalDate mois = today.minusMonths(i);
            result.add(caMois(id, mois));
        }
        return result;
    }

    private CaMensuelDto caMois(Long proprietaireId, LocalDate mois) {
        LocalDateTime debut = mois.withDayOfMonth(1).atStartOfDay();
        LocalDateTime fin   = mois.withDayOfMonth(mois.lengthOfMonth()).atTime(23, 59, 59);
        BigDecimal montant  = factureRepository
                .findByProprietaireIdAndStatutAndDatePaiementBetween(
                        proprietaireId, StatutFacture.PAYEE, debut, fin)
                .stream()
                .map(Facture::getMontantTtc)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return CaMensuelDto.builder()
                .mois(mois.getMonthValue())
                .annee(mois.getYear())
                .montant(montant)
                .label(MOIS_LABELS[mois.getMonthValue() - 1])
                .build();
    }

    @Override
    public PageResponse<ActiviteResponse> getActivites(String emailProprietaire, int page, int size) {
        ProprietaireEntreprise proprietaire = chargerProprietaire(emailProprietaire);
        Page<ActiviteResponse> result = activiteRepository
                .findByProprietaireIdOrderByDateCreationDesc(proprietaire.getId(), PageRequest.of(page, size))
                .map(a -> {
                    ActiviteResponse response = activiteMapper.toResponse(a);
                    response.setDateRelative(dateRelative(a.getDateCreation()));
                    return response;
                });
        return PageResponse.from(result);
    }

    // ── Calculs KPIs historiques ──────────────────────────────────────────────

    private long calculerNbClients(Long id, LocalDateTime[] fenetre) {
        if (fenetre == null)
            return clientRepository.countByProprietaireIdAndIsDeletedFalseAndStatut(id, "ACTIF");
        return clientRepository.countByProprietaireIdAndDateCreationBetween(id, fenetre[0], fenetre[1]);
    }

    private long calculerNbOpportunites(Long id, LocalDateTime[] fenetre) {
        if (fenetre == null) return opportuniteRepository.countByProprietaireId(id);
        return opportuniteRepository.countByProprietaireIdAndDateCreationBetween(id, fenetre[0], fenetre[1]);
    }

    private long calculerNbDevis(Long id, LocalDateTime[] fenetre) {
        if (fenetre == null)
            return devisRepository.countByProprietaireIdAndStatut(id, StatutDevis.ENVOYE);
        return devisRepository.countByProprietaireIdAndDateCreationBetween(id, fenetre[0], fenetre[1]);
    }

    private BigDecimal calculerCA(Long id, LocalDateTime[] fenetre) {
        List<Facture> factures = (fenetre == null)
                ? factureRepository.findByProprietaireIdAndStatut(id, StatutFacture.PAYEE)
                : factureRepository.findByProprietaireIdAndStatutAndDatePaiementBetween(
                        id, StatutFacture.PAYEE, fenetre[0], fenetre[1]);
        return factures.stream().map(Facture::getMontantTtc).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<ActiviteRecenteItem> construireActiviteRecente(Long id) {
        return activiteRepository.findTop10ByProprietaireIdOrderByDateCreationDesc(id)
                .stream()
                .map(a -> ActiviteRecenteItem.builder()
                        .id(a.getEntiteId())
                        .type(a.getEntiteType())
                        .typeActivite(a.getType().name())
                        .titre(a.getTitre())
                        .soustitre(a.getDescription())
                        .dateRelative(dateRelative(a.getDateCreation()))
                        .entiteParentId(a.getEntiteParentId())
                        .build())
                .toList();
    }

    // ── Calculs stats ventes ──────────────────────────────────────────────────

    private long calculerNbLeadsActifs(Long id) {
        return leadRepository.countByProprietaireIdAndStatutNotIn(id, LEADS_INACTIFS);
    }

    private double calculerTauxConversionLeads(Long id) {
        long total = leadRepository.countByProprietaireId(id);
        if (total == 0) return 0.0;
        long convertis = leadRepository.countByProprietaireIdAndStatut(id, StatutLead.CONVERTI);
        return arrondir1Dec(convertis * 100.0 / total);
    }

    private BigDecimal calculerValeurPipeline(Long id) {
        return opportuniteRepository.findByProprietaireIdAndStatutIn(id, OPPORT_ACTIVES)
                .stream()
                .map(o -> o.getMontantEstime() != null ? o.getMontantEstime() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private double calculerTauxConversionOpportunites(Long id) {
        long total = opportuniteRepository.countByProprietaireId(id);
        if (total == 0) return 0.0;
        long gagnees = opportuniteRepository.countByProprietaireIdAndStatut(id, StatutOpportunite.GAGNEE);
        return arrondir1Dec(gagnees * 100.0 / total);
    }

    private double calculerTauxAcceptationDevis(Long id) {
        long total = devisRepository.countByProprietaireIdAndStatutIn(id, DEVIS_NON_BROUILLON);
        if (total == 0) return 0.0;
        long acceptes = devisRepository.countByProprietaireIdAndStatut(id, StatutDevis.ACCEPTE);
        return arrondir1Dec(acceptes * 100.0 / total);
    }

    private BigDecimal calculerPanierMoyen(Long id) {
        long nbFactures = factureRepository.countByProprietaireIdAndStatut(id, StatutFacture.PAYEE);
        if (nbFactures == 0) return BigDecimal.ZERO;
        BigDecimal ca = factureRepository.findByProprietaireIdAndStatut(id, StatutFacture.PAYEE)
                .stream().map(Facture::getMontantTtc).reduce(BigDecimal.ZERO, BigDecimal::add);
        return ca.divide(BigDecimal.valueOf(nbFactures), 3, RoundingMode.HALF_UP);
    }

    /**
     * Calcule le CA annuel en filtrant dynamiquement sur l'année civile en cours
     * (du 1er janvier au 31 décembre de {@code LocalDate.now().getYear()}).
     *
     * @param id identifiant du propriétaire
     * @return somme des montantTtc des factures PAYEE pour l'année en cours
     * @author Riahi Dorsaf
     */
    private BigDecimal calculerCaAnnuel(Long id) {
        int annee = LocalDate.now().getYear();
        LocalDateTime debut = LocalDate.of(annee, 1, 1).atStartOfDay();
        LocalDateTime fin   = LocalDate.of(annee, 12, 31).atTime(23, 59, 59);
        return factureRepository
                .findByProprietaireIdAndStatutAndDatePaiementBetween(id, StatutFacture.PAYEE, debut, fin)
                .stream()
                .map(Facture::getMontantTtc)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<StatsVentesResponse.StatutOpportuniteCount> calculerRepartitionOpportunites(Long id) {
        return Stream.of(StatutOpportunite.values())
                .map(s -> StatsVentesResponse.StatutOpportuniteCount.builder()
                        .statut(s.name())
                        .count(opportuniteRepository.countByProprietaireIdAndStatut(id, s))
                        .build())
                .toList();
    }

    private List<StatsVentesResponse.OpportuniteResume> calculerTop3Opportunites(Long id) {
        return opportuniteRepository
                .findTop3ByProprietaireIdAndStatutInOrderByMontantEstimeDesc(id, OPPORT_ACTIVES)
                .stream()
                .map(this::toOpportuniteResume)
                .toList();
    }

    private StatsVentesResponse.OpportuniteResume toOpportuniteResume(Opportunite o) {
        return StatsVentesResponse.OpportuniteResume.builder()
                .id(o.getId())
                .titre(o.getTitre())
                .clientNom(o.getClient().getNomAffichage())
                .montantEstime(o.getMontantEstime())
                .statut(o.getStatut().name())
                .build();
    }

    // ── Fenêtres temporelles ──────────────────────────────────────────────────

    /**
     * Construit la fenêtre [debut, fin] pour la période donnée.
     * Retourne null si periode est null ou non reconnue (pas de filtre date).
     */
    private LocalDateTime[] construireFenetre(String periode) {
        if (periode == null) return null;
        LocalDate today = LocalDate.now();
        return switch (periode) {
            case "AUJOURD_HUI" -> new LocalDateTime[]{
                    today.atStartOfDay(),
                    today.atTime(23, 59, 59)
            };
            case "CE_MOIS" -> new LocalDateTime[]{
                    today.withDayOfMonth(1).atStartOfDay(),
                    today.withDayOfMonth(today.lengthOfMonth()).atTime(23, 59, 59)
            };
            case "CETTE_ANNEE" -> new LocalDateTime[]{
                    today.withDayOfYear(1).atStartOfDay(),
                    LocalDate.of(today.getYear(), 12, 31).atTime(23, 59, 59)
            };
            default -> null;
        };
    }

    private LocalDateTime[] construireFenetreMoisPrecedent() {
        LocalDate moisPrecedent = LocalDate.now().minusMonths(1);
        LocalDateTime debut = moisPrecedent.withDayOfMonth(1).atStartOfDay();
        LocalDateTime fin = moisPrecedent.withDayOfMonth(moisPrecedent.lengthOfMonth()).atTime(23, 59, 59);
        return new LocalDateTime[]{debut, fin};
    }

    // ── Utilitaires ───────────────────────────────────────────────────────────

    private ProprietaireEntreprise chargerProprietaire(String email) {
        return proprietaireRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Propriétaire introuvable."));
    }

    /** Arrondit un taux à 1 décimale. */
    private double arrondir1Dec(double valeur) {
        return Math.round(valeur * 10) / 10.0;
    }

    private String dateRelative(LocalDateTime date) {
        if (date == null) return "";
        Duration d = Duration.between(date, LocalDateTime.now());
        long minutes = d.toMinutes();
        if (minutes < 1) return "a l instant";
        if (minutes < 60) return "il y a " + minutes + " min";
        long heures = d.toHours();
        if (heures < 24) return "il y a " + heures + " h";
        long jours = d.toDays();
        if (jours < 30) return "il y a " + jours + " j";
        return "il y a " + (jours / 30) + " mois";
    }
}
