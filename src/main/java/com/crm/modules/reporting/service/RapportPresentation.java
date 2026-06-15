package com.crm.modules.reporting.service;

import com.crm.modules.reporting.dto.RapportCommercialResponse;
import com.crm.modules.reporting.dto.RapportCommercialResponse.DevisFactures;
import com.crm.modules.reporting.dto.RapportCommercialResponse.StatutCount;
import com.crm.modules.reporting.dto.RapportCommercialResponse.Synthese;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Présentation du rapport commercial : formatage des valeurs, calcul des barres
 * et génération des « points d'attention ». Logique partagée entre l'email
 * ({@code RapportEmailService}) et le PDF ({@code RapportPdfService}) pour éviter
 * toute duplication.
 *
 * <p>Méthodes statiques sans état : aucune dépendance Spring nécessaire.</p>
 *
 * @author Riahi Dorsaf
 */
public final class RapportPresentation {

    /** Seuil en dessous duquel le taux de conversion des leads est signalé. */
    private static final double SEUIL_CONVERSION_FAIBLE = 20.0;

    /** Nombre d'opportunités en négociation à partir duquel on alerte. */
    private static final long SEUIL_OPPORTUNITES_NEGO = 3;

    private RapportPresentation() {
    }

    // ── Formatage ───────────────────────────────────────────────────────────────

    /** Montant formaté à la française avec devise, ex. « 431 084,64 DT ». */
    public static String montant(BigDecimal valeur) {
        if (valeur == null) {
            return "0,00 DT";
        }
        return String.format(Locale.FRANCE, "%,.2f DT", valeur);
    }

    /** Pourcentage à une décimale, ex. « 63,6 % ». */
    public static String pourcent(double valeur) {
        return String.format(Locale.FRANCE, "%.1f %%", valeur);
    }

    /** Pourcentage à partir d'un BigDecimal (variation), ex. « 12,0 % ». */
    public static String pourcent(BigDecimal valeur) {
        if (valeur == null) {
            return "n/a";
        }
        return String.format(Locale.FRANCE, "%.1f %%", valeur);
    }

    /**
     * Largeur d'une barre (0..100) proportionnelle à la valeur par rapport au max.
     * Retourne 0 si le max est nul.
     */
    public static int largeurPct(long valeur, long max) {
        if (max <= 0) {
            return 0;
        }
        return (int) Math.round((valeur * 100.0) / max);
    }

    /** Plus grand compteur d'une liste de {@link StatutCount} (pour l'échelle des barres). */
    public static long maxStatut(List<StatutCount> counts) {
        return counts == null ? 0
                : counts.stream().mapToLong(StatutCount::getCount).max().orElse(0);
    }

    /** Échappe le HTML/XML pour éviter toute injection depuis des données saisies. */
    public static String echappe(String valeur) {
        if (valeur == null) {
            return "";
        }
        return valeur.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    // ── Points d'attention ───────────────────────────────────────────────────────

    /**
     * Génère les alertes décisionnelles à partir du rapport (texte brut, sans HTML).
     * Toujours au moins un élément : message positif si rien à signaler.
     */
    public static List<String> pointsDAttention(RapportCommercialResponse r) {
        List<String> alertes = new ArrayList<>();

        DevisFactures d = r.getDevisFactures();
        if (d != null && d.getFacturesImpayees() > 0) {
            alertes.add(d.getFacturesImpayees() + " facture(s) impayée(s) : "
                    + montant(d.getMontantImpaye()) + " à relancer.");
        }

        Synthese s = r.getSynthese();
        if (s != null) {
            if (s.getVariationCaPct() != null && s.getVariationCaPct().signum() < 0) {
                alertes.add("Chiffre d'affaires en baisse de "
                        + pourcent(s.getVariationCaPct().abs()) + " vs période précédente.");
            }
            if (s.getNouveauxLeads() > 0 && s.getTauxConversionLeads() < SEUIL_CONVERSION_FAIBLE) {
                alertes.add("Taux de conversion des leads faible ("
                        + pourcent(s.getTauxConversionLeads()) + ") : à surveiller.");
            }
        }

        long enNego = enNegociation(r);
        if (enNego >= SEUIL_OPPORTUNITES_NEGO) {
            alertes.add(enNego + " opportunités en négociation : relancer pour les conclure.");
        }

        if (alertes.isEmpty()) {
            alertes.add("Aucun point d'attention majeur sur la période. Continuez ainsi.");
        }
        return alertes;
    }

    /** Nombre d'opportunités au statut NEGOCIATION dans la répartition du pipeline. */
    private static long enNegociation(RapportCommercialResponse r) {
        if (r.getPipeline() == null || r.getPipeline().getRepartitionParStatut() == null) {
            return 0;
        }
        return r.getPipeline().getRepartitionParStatut().stream()
                .filter(c -> "NEGOCIATION".equals(c.getStatut()))
                .mapToLong(StatutCount::getCount)
                .sum();
    }
}
