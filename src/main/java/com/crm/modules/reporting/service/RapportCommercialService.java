package com.crm.modules.reporting.service;

import com.crm.modules.reporting.dto.ProprietaireResumeResponse;
import com.crm.modules.reporting.dto.RapportCommercialResponse;
import com.crm.modules.reporting.dto.RapportCommercialResponse.Activites;
import com.crm.modules.reporting.dto.RapportCommercialResponse.DevisFactures;
import com.crm.modules.reporting.dto.RapportCommercialResponse.Entreprise;
import com.crm.modules.reporting.dto.RapportCommercialResponse.Periode;
import com.crm.modules.reporting.dto.RapportCommercialResponse.Pipeline;
import com.crm.modules.reporting.dto.RapportCommercialResponse.ResultatCount;
import com.crm.modules.reporting.dto.RapportCommercialResponse.SourceCount;
import com.crm.modules.reporting.dto.RapportCommercialResponse.StatutCount;
import com.crm.modules.reporting.dto.RapportCommercialResponse.Synthese;
import com.crm.modules.reporting.dto.RapportCommercialResponse.TypeCount;
import com.crm.modules.reporting.dto.StatsVentesResponse;
import com.crm.modules.entreprise.entity.EntrepriseCompte;
import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import com.crm.modules.utilisateur.repository.ProprietaireRepository;
import com.crm.modules.vente.entity.Facture;
import com.crm.modules.vente.repository.ActiviteCommercialeRepository;
import com.crm.modules.vente.repository.DevisRepository;
import com.crm.modules.vente.repository.FactureRepository;
import com.crm.modules.vente.repository.LeadRepository;
import com.crm.shared.enums.PeriodeRapport;
import com.crm.shared.enums.ResultatActivite;
import com.crm.shared.enums.SourceLead;
import com.crm.shared.enums.StatutCompte;
import com.crm.shared.enums.StatutDevis;
import com.crm.shared.enums.StatutFacture;
import com.crm.shared.enums.StatutLead;
import com.crm.shared.enums.TypeActiviteCommerciale;
import com.crm.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Stream;

/**
 * Service de reporting commercial automatisé (multi-tenant, machine-à-machine).
 *
 * <p>Agrège, pour une période complète écoulée, les données du module vente. Réutilise
 * {@link IReportingService#getStatsVentes(Long)} pour le snapshot du pipeline et calcule
 * les métriques fenêtrées (CA, leads, activités, devis) directement via les repositories.</p>
 *
 * @author Riahi Dorsaf
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RapportCommercialService implements IRapportCommercialService {

    /** Factures considérées comme impayées (non soldées). */
    private static final List<StatutFacture> FACTURES_IMPAYEES =
            List.of(StatutFacture.EMISE, StatutFacture.LIVREE, StatutFacture.EN_RETARD);

    private static final String[] MOIS_LABELS = {
            "janvier", "février", "mars", "avril", "mai", "juin",
            "juillet", "août", "septembre", "octobre", "novembre", "décembre"
    };

    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ProprietaireRepository          proprietaireRepository;
    private final IReportingService               reportingService;
    private final RapportEmailService             rapportEmailService;
    private final RapportSyntheseService          rapportSyntheseService;
    private final FactureRepository               factureRepository;
    private final LeadRepository                  leadRepository;
    private final DevisRepository                 devisRepository;
    private final ActiviteCommercialeRepository   activiteCommercialeRepository;

    // ── API ───────────────────────────────────────────────────────────────────

    @Override
    public List<ProprietaireResumeResponse> listerProprietairesActifs() {
        return proprietaireRepository
                .findByIsActiveTrueAndEntrepriseCompte_IsDeletedFalseAndEntrepriseCompte_StatutCompteOrderById(
                        StatutCompte.ACTIVE)
                .stream()
                .map(p -> ProprietaireResumeResponse.builder()
                        .id(p.getId())
                        .email(p.getEmail())
                        .build())
                .toList();
    }

    @Override
    public RapportCommercialResponse genererRapport(Long proprietaireId, PeriodeRapport periode) {
        ProprietaireEntreprise proprietaire = proprietaireRepository.findById(proprietaireId)
                .orElseThrow(() -> new ResourceNotFoundException("Propriétaire introuvable."));

        Fenetre f = calculerFenetre(periode);

        // Snapshot pipeline calculé une seule fois (réutilisé par le pipeline ET la synthèse).
        StatsVentesResponse stats = reportingService.getStatsVentes(proprietaireId);

        BigDecimal caRealise   = caEncaisse(proprietaireId, f.debut(), f.fin());
        BigDecimal caPrecedent = caEncaisse(proprietaireId, f.debutPrec(), f.finPrec());

        return RapportCommercialResponse.builder()
                .entreprise(construireEntreprise(proprietaire))
                .periode(construirePeriode(periode, f))
                .synthese(construireSynthese(proprietaireId, f, caRealise, caPrecedent, stats))
                .pipeline(construirePipeline(stats))
                .activites(construireActivites(proprietaireId, f))
                .devisFactures(construireDevisFactures(proprietaireId, f, caRealise))
                .leadsParSource(construireLeadsParSource(proprietaireId, f))
                .syntheseIa(null) // rempli par n8n (Groq) avant diffusion
                .build();
    }

    @Override
    public RapportCommercialResponse envoyerRapport(Long proprietaireId, PeriodeRapport periode,
                                                    String syntheseIa) {
        RapportCommercialResponse rapport = genererRapport(proprietaireId, periode);
        // Si l'appelant ne fournit pas de synthèse, on la génère via l'IA (Groq/Ollama).
        String synthese = (syntheseIa != null && !syntheseIa.isBlank())
                ? syntheseIa
                : rapportSyntheseService.genererSynthese(rapport);
        rapport.setSyntheseIa(synthese);
        rapportEmailService.envoyerRapport(rapport);
        return rapport;
    }

    @Override
    public int envoyerTousLesRapports(PeriodeRapport periode) {
        List<ProprietaireResumeResponse> proprietaires = listerProprietairesActifs();
        int envoyes = 0;
        for (ProprietaireResumeResponse p : proprietaires) {
            try {
                envoyerRapport(p.getId(), periode, null);
                envoyes++;
            } catch (Exception e) {
                log.error("[REPORTING AUTO] Échec rapport {} pour propriétaire id={} ({}) : {}",
                        periode, p.getId(), p.getEmail(), e.getMessage());
            }
        }
        log.info("[REPORTING AUTO] Rapports {} envoyés : {}/{}",
                periode, envoyes, proprietaires.size());
        return envoyes;
    }

    // ── Construction des sections ───────────────────────────────────────────────

    private Entreprise construireEntreprise(ProprietaireEntreprise p) {
        EntrepriseCompte compte = p.getEntrepriseCompte();
        return Entreprise.builder()
                .proprietaireId(p.getId())
                .email(p.getEmail())
                .nomProprietaire(((p.getPrenom() != null ? p.getPrenom() + " " : "") + p.getNom()).trim())
                .nomEntreprise(compte != null ? compte.getNomEntreprise() : null)
                .build();
    }

    private Periode construirePeriode(PeriodeRapport periode, Fenetre f) {
        return Periode.builder()
                .type(periode)
                .libelle(f.libelle())
                .debut(f.debut())
                .fin(f.fin())
                .build();
    }

    private Synthese construireSynthese(Long id, Fenetre f, BigDecimal caRealise,
                                        BigDecimal caPrecedent, StatsVentesResponse stats) {
        LocalDateTime debut = f.debut().atStartOfDay();
        LocalDateTime fin   = f.fin().atTime(23, 59, 59);

        long nouveauxLeads  = leadRepository.countByProprietaireIdAndDateCreationBetween(id, debut, fin);
        long leadsConvertis = leadRepository.countByProprietaireIdAndStatutAndDateCreationBetween(
                id, StatutLead.CONVERTI, debut, fin);

        return Synthese.builder()
                .caRealise(caRealise)
                .caPeriodePrecedente(caPrecedent)
                .variationCaPct(variationPct(caRealise, caPrecedent))
                .nouveauxLeads(nouveauxLeads)
                .leadsConvertis(leadsConvertis)
                .tauxConversionLeads(pourcentage(leadsConvertis, nouveauxLeads))
                .panierMoyen(stats.getPanierMoyen())
                .build();
    }

    private Pipeline construirePipeline(StatsVentesResponse stats) {
        // Snapshot courant — réutilise le calcul existant du dashboard.
        List<StatutCount> repartition = stats.getRepartitionOpportunites().stream()
                .map(r -> StatutCount.builder().statut(r.getStatut()).count(r.getCount()).build())
                .toList();
        return Pipeline.builder()
                .valeur(stats.getValeurPipeline())
                .winRate(stats.getTauxConversionOpportunites())
                .repartitionParStatut(repartition)
                .topOpportunites(stats.getTop3Opportunites())
                .build();
    }

    private Activites construireActivites(Long id, Fenetre f) {
        LocalDateTime debut = f.debut().atStartOfDay();
        LocalDateTime fin   = f.fin().atTime(23, 59, 59);

        List<TypeCount> parType = Stream.of(TypeActiviteCommerciale.values())
                .map(t -> TypeCount.builder()
                        .type(t.name())
                        .count(activiteCommercialeRepository
                                .countByProprietaireIdAndTypeAndDateActiviteBetween(id, t, debut, fin))
                        .build())
                .toList();

        List<ResultatCount> parResultat = Stream.of(ResultatActivite.values())
                .map(r -> ResultatCount.builder()
                        .resultat(r.name())
                        .count(activiteCommercialeRepository
                                .countByProprietaireIdAndResultatAndDateActiviteBetween(id, r, debut, fin))
                        .build())
                .toList();

        return Activites.builder()
                .total(activiteCommercialeRepository
                        .countByProprietaireIdAndDateActiviteBetween(id, debut, fin))
                .parType(parType)
                .parResultat(parResultat)
                .build();
    }

    private DevisFactures construireDevisFactures(Long id, Fenetre f, BigDecimal caRealise) {
        LocalDateTime debut = f.debut().atStartOfDay();
        LocalDateTime fin   = f.fin().atTime(23, 59, 59);

        long devisEmis = devisRepository.countByProprietaireIdAndDateCreationBetween(id, debut, fin);
        long devisAcceptes = devisRepository.countByProprietaireIdAndStatutAndDateCreationBetween(
                id, StatutDevis.ACCEPTE, debut, fin);

        List<Facture> impayees = factureRepository.findByProprietaireIdAndStatutIn(id, FACTURES_IMPAYEES);
        BigDecimal montantImpaye = impayees.stream()
                .map(Facture::getMontantTtc)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return DevisFactures.builder()
                .devisEmis(devisEmis)
                .tauxAcceptationDevis(pourcentage(devisAcceptes, devisEmis))
                .facturesImpayees(impayees.size())
                .montantImpaye(montantImpaye)
                .caEncaisse(caRealise)
                .build();
    }

    private List<SourceCount> construireLeadsParSource(Long id, Fenetre f) {
        LocalDateTime debut = f.debut().atStartOfDay();
        LocalDateTime fin   = f.fin().atTime(23, 59, 59);
        return Stream.of(SourceLead.values())
                .map(s -> SourceCount.builder()
                        .source(s.name())
                        .count(leadRepository
                                .countByProprietaireIdAndSourceAndDateCreationBetween(id, s, debut, fin))
                        .build())
                .toList();
    }

    // ── Fenêtres temporelles (période complète écoulée + précédente) ────────────

    /**
     * Fenêtre d'un rapport : période écoulée [debut, fin] + période précédente
     * [debutPrec, finPrec] (base de la variation) + libellé lisible.
     */
    private record Fenetre(LocalDate debut, LocalDate fin,
                           LocalDate debutPrec, LocalDate finPrec,
                           String libelle) {}

    private Fenetre calculerFenetre(PeriodeRapport periode) {
        LocalDate today = LocalDate.now();
        return switch (periode) {
            case SEMAINE -> {
                LocalDate debut     = today.with(DayOfWeek.MONDAY).minusWeeks(1);
                LocalDate fin       = debut.plusDays(6);
                LocalDate debutPrec = debut.minusWeeks(1);
                LocalDate finPrec   = debutPrec.plusDays(6);
                String libelle = "Semaine du " + debut.format(JOUR) + " au " + fin.format(JOUR);
                yield new Fenetre(debut, fin, debutPrec, finPrec, libelle);
            }
            case MOIS -> {
                LocalDate moisPrec     = today.minusMonths(1);
                LocalDate debut        = moisPrec.withDayOfMonth(1);
                LocalDate fin          = moisPrec.withDayOfMonth(moisPrec.lengthOfMonth());
                LocalDate moisPrecPrec = today.minusMonths(2);
                LocalDate debutPrec    = moisPrecPrec.withDayOfMonth(1);
                LocalDate finPrec      = moisPrecPrec.withDayOfMonth(moisPrecPrec.lengthOfMonth());
                String libelle = "Mois de " + MOIS_LABELS[debut.getMonthValue() - 1] + " " + debut.getYear();
                yield new Fenetre(debut, fin, debutPrec, finPrec, libelle);
            }
            case ANNEE -> {
                int annee = today.getYear() - 1;
                LocalDate debut     = LocalDate.of(annee, 1, 1);
                LocalDate fin       = LocalDate.of(annee, 12, 31);
                LocalDate debutPrec = LocalDate.of(annee - 1, 1, 1);
                LocalDate finPrec   = LocalDate.of(annee - 1, 12, 31);
                yield new Fenetre(debut, fin, debutPrec, finPrec, "Année " + annee);
            }
        };
    }

    // ── Utilitaires ─────────────────────────────────────────────────────────────

    private BigDecimal caEncaisse(Long id, LocalDate debut, LocalDate fin) {
        return factureRepository.findByProprietaireIdAndStatutAndDatePaiementBetween(
                        id, StatutFacture.PAYEE, debut.atStartOfDay(), fin.atTime(23, 59, 59))
                .stream()
                .map(Facture::getMontantTtc)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Variation en % entre période courante et précédente.
     * Retourne {@code null} si la base (période précédente) est nulle (variation indéfinie).
     */
    private BigDecimal variationPct(BigDecimal courant, BigDecimal precedent) {
        if (precedent == null || precedent.signum() == 0) return null;
        return courant.subtract(precedent)
                .multiply(BigDecimal.valueOf(100))
                .divide(precedent, 1, RoundingMode.HALF_UP);
    }

    /** Pourcentage arrondi à 1 décimale (0 si dénominateur nul). */
    private double pourcentage(long numerateur, long denominateur) {
        if (denominateur == 0) return 0.0;
        return Math.round(numerateur * 1000.0 / denominateur) / 10.0;
    }
}
