package com.crm.modules.reporting.dto;

import com.crm.shared.enums.PeriodeRapport;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Rapport commercial agrégé pour une période complète écoulée (semaine / mois / année).
 *
 * <p>Construit par {@code RapportCommercialService} en réutilisant le module reporting
 * existant. Consommé par l'orchestration n8n (un seul appel) qui y ajoutera la synthèse IA
 * avant diffusion (email + notification).</p>
 *
 * @author Riahi Dorsaf
 */
@Data
@Builder
public class RapportCommercialResponse {

    /** Entreprise et propriétaire destinataire du rapport. */
    private Entreprise entreprise;

    /** Bornes de la période couverte. */
    private Periode periode;

    /** Indicateurs de synthèse (CA, variation, leads). */
    private Synthese synthese;

    /** État du pipeline (snapshot courant). */
    private Pipeline pipeline;

    /** Activités commerciales réalisées sur la période. */
    private Activites activites;

    /** Devis & factures. */
    private DevisFactures devisFactures;

    /** Répartition des nouveaux leads par source sur la période (dont FACEBOOK). */
    private List<SourceCount> leadsParSource;

    /**
     * Synthèse rédigée par l'IA (Groq). Restera {@code null} en Phase A : elle est
     * remplie par n8n avant la diffusion.
     */
    private String syntheseIa;

    // ── Sous-structures ───────────────────────────────────────────────────────

    @Data
    @Builder
    public static class Entreprise {
        private Long   proprietaireId;
        private String email;
        private String nomProprietaire;
        private String nomEntreprise;
    }

    @Data
    @Builder
    public static class Periode {
        /** Type de période (SEMAINE / MOIS / ANNEE). */
        private PeriodeRapport type;
        /** Libellé lisible, ex. « Semaine du 08/06 au 14/06/2026 ». */
        private String    libelle;
        private LocalDate debut;
        private LocalDate fin;
    }

    @Data
    @Builder
    public static class Synthese {
        /** CA encaissé (factures PAYEE) sur la période. */
        private BigDecimal caRealise;
        /** CA encaissé sur la période précédente (base de la variation). */
        private BigDecimal caPeriodePrecedente;
        /** Variation du CA en % vs période précédente (null si base à 0). */
        private BigDecimal variationCaPct;
        /** Nombre de leads créés sur la période. */
        private long nouveauxLeads;
        /** Parmi les leads créés sur la période, ceux désormais CONVERTI. */
        private long leadsConvertis;
        /** Taux de conversion des nouveaux leads de la période (en %). */
        private double tauxConversionLeads;
        /** Panier moyen = CA encaissé / nombre de factures payées (deal size). */
        private BigDecimal panierMoyen;
    }

    @Data
    @Builder
    public static class Pipeline {
        /** Valeur du pipeline = somme montantEstime des opportunités actives. */
        private BigDecimal valeur;
        /** Taux de victoire = GAGNEE / (GAGNEE + PERDUE), en % (win rate). */
        private double winRate;
        /** Répartition du nombre d'opportunités par StatutOpportunite. */
        private List<StatutCount> repartitionParStatut;
        /** Top opportunités actives par montant estimé décroissant. */
        private List<StatsVentesResponse.OpportuniteResume> topOpportunites;
    }

    @Data
    @Builder
    public static class Activites {
        /** Total d'activités commerciales sur la période. */
        private long total;
        /** Répartition par TypeActiviteCommerciale (APPEL/EMAIL/REUNION/VISITE/TACHE). */
        private List<TypeCount> parType;
        /** Répartition par ResultatActivite (POSITIF/NEGATIF/EN_ATTENTE/SANS_REPONSE). */
        private List<ResultatCount> parResultat;
    }

    @Data
    @Builder
    public static class DevisFactures {
        /** Nombre de devis créés sur la période. */
        private long devisEmis;
        /** Taux d'acceptation des devis créés sur la période (en %). */
        private double tauxAcceptationDevis;
        /** Nombre de factures non soldées (EMISE / LIVREE / EN_RETARD). */
        private long facturesImpayees;
        /** Montant TTC total des factures impayées. */
        private BigDecimal montantImpaye;
        /** CA encaissé (= caRealise), exposé ici pour la lisibilité du bloc. */
        private BigDecimal caEncaisse;
    }

    // ── Compteurs génériques ──────────────────────────────────────────────────

    @Data
    @Builder
    public static class StatutCount {
        private String statut;
        private long   count;
    }

    @Data
    @Builder
    public static class TypeCount {
        private String type;
        private long   count;
    }

    @Data
    @Builder
    public static class ResultatCount {
        private String resultat;
        private long   count;
    }

    @Data
    @Builder
    public static class SourceCount {
        private String source;
        private long   count;
    }
}
