package com.crm.modules.reporting.service;

import com.crm.modules.reporting.dto.RapportCommercialResponse;
import com.crm.modules.reporting.dto.RapportCommercialResponse.DevisFactures;
import com.crm.modules.reporting.dto.RapportCommercialResponse.StatutCount;
import com.crm.modules.reporting.dto.RapportCommercialResponse.Synthese;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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

    // ── Funnel commercial (entonnoir des opportunités) ───────────────────────────

    /** Étapes ordonnées de l'entonnoir (PERDUE est exclue : montrée à part). */
    private static final List<String> ORDRE_FUNNEL =
            List.of("PROSPECTION", "QUALIFICATION", "PROPOSITION", "NEGOCIATION", "GAGNEE");

    /** Libellés français lisibles pour chaque étape du funnel. */
    private static final Map<String, String> LIBELLES_FUNNEL = Map.of(
            "PROSPECTION", "Prospection",
            "QUALIFICATION", "Qualification",
            "PROPOSITION", "Proposition",
            "NEGOCIATION", "Négociation",
            "GAGNEE", "Gagnée");

    /**
     * Une étape de l'entonnoir commercial.
     *
     * @param libelle    nom lisible de l'étape
     * @param count      nombre d'opportunités à cette étape
     * @param largeurPct largeur de la barre (0..100) proportionnelle au max de l'entonnoir
     * @param conversion taux de passage depuis l'étape précédente en % ({@code null} pour la
     *                   première étape ou si l'étape précédente est vide)
     */
    public record EtapeFunnel(String libelle, long count, int largeurPct, Double conversion) {}

    /**
     * Construit l'entonnoir commercial ordonné à partir de la répartition par statut.
     * La largeur des barres est proportionnelle au plus grand effectif, et chaque étape
     * porte son taux de conversion depuis l'étape précédente (signature des CRM concurrents).
     */
    public static List<EtapeFunnel> funnel(List<StatutCount> repartition) {
        Map<String, Long> parStatut = new LinkedHashMap<>();
        if (repartition != null) {
            for (StatutCount c : repartition) {
                parStatut.put(c.getStatut(), c.getCount());
            }
        }
        long max = ORDRE_FUNNEL.stream().mapToLong(s -> parStatut.getOrDefault(s, 0L)).max().orElse(0);

        List<EtapeFunnel> etapes = new ArrayList<>();
        long precedent = -1;
        for (String statut : ORDRE_FUNNEL) {
            long count = parStatut.getOrDefault(statut, 0L);
            Double conversion = (precedent > 0) ? (count * 100.0) / precedent : null;
            etapes.add(new EtapeFunnel(
                    LIBELLES_FUNNEL.getOrDefault(statut, statut),
                    count, largeurPct(count, max), conversion));
            precedent = count;
        }
        return etapes;
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
