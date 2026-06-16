package com.crm.modules.reporting.service;

import com.crm.modules.reporting.dto.RapportCommercialResponse;
import com.crm.modules.reporting.dto.RapportCommercialResponse.Activites;
import com.crm.modules.reporting.dto.RapportCommercialResponse.DevisFactures;
import com.crm.modules.reporting.dto.RapportCommercialResponse.Pipeline;
import com.crm.modules.reporting.dto.RapportCommercialResponse.SourceCount;
import com.crm.modules.reporting.dto.RapportCommercialResponse.Synthese;
import com.crm.modules.reporting.dto.RapportCommercialResponse.TypeCount;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;

import com.crm.modules.reporting.service.RapportPresentation.EtapeFunnel;

import static com.crm.modules.reporting.service.RapportPresentation.echappe;
import static com.crm.modules.reporting.service.RapportPresentation.funnel;
import static com.crm.modules.reporting.service.RapportPresentation.largeurPct;
import static com.crm.modules.reporting.service.RapportPresentation.montant;
import static com.crm.modules.reporting.service.RapportPresentation.pointsDAttention;
import static com.crm.modules.reporting.service.RapportPresentation.pourcent;

/**
 * Génère le rapport commercial sous forme de document PDF (pièce jointe email).
 *
 * <p>Construit un XHTML bien formé puis le rend en PDF via openhtmltopdf
 * (rendu local, aucune donnée ne sort vers un service externe). Pas d'emoji :
 * les polices PDFBox par défaut ne les rendent pas ; les accents français le sont.</p>
 *
 * @author Riahi Dorsaf
 */
@Slf4j
@Service
public class RapportPdfService {

    private static final String APP_NAME     = "Easy Sales CRM";
    private static final String INDIGO       = "#4f46e5";
    private static final String INDIGO_FONCE = "#1e3a8a";
    private static final String AMBRE        = "#b45309";
    private static final String ROUGE = "#dc2626";
    private static final String VERT = "#047857";

    /**
     * Construit le PDF du rapport.
     *
     * @param r rapport commercial agrégé (synthèse IA éventuellement incluse)
     * @return contenu binaire du PDF
     */
    public byte[] genererPdf(RapportCommercialResponse r) {
        String xhtml = construireXhtml(r);
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(xhtml, null);
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        } catch (Exception e) {
            log.error("[RAPPORT PDF] Échec de génération du PDF : {}", e.getMessage(), e);
            throw new IllegalStateException("Échec de la génération du PDF du rapport", e);
        }
    }

    // ── Document ────────────────────────────────────────────────────────────────

    private String construireXhtml(RapportCommercialResponse r) {
        Synthese s = r.getSynthese();
        Pipeline p = r.getPipeline();
        Activites a = r.getActivites();
        DevisFactures d = r.getDevisFactures();

        StringBuilder b = new StringBuilder();
        b.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"/>");
        b.append("<style>").append(css()).append("</style></head><body>");

        // En-tête premium
        b.append("<div class=\"entete\">")
                .append("<div class=\"marque\">").append(echappe(APP_NAME)).append("</div>")
                .append("<div class=\"titre\">Bilan commercial</div>")
                .append("<div class=\"periode-pill\">").append(echappe(r.getPeriode().getLibelle())).append("</div>")
                .append("<span class=\"entreprise\">").append(echappe(r.getEntreprise().getNomEntreprise()))
                .append("</span>")
                .append("</div>");

        // Synthèse IA
        if (r.getSyntheseIa() != null && !r.getSyntheseIa().isBlank()) {
            b.append("<div class=\"ia\"><div class=\"ia-titre\">Synthese IA</div>")
                    .append(echappe(r.getSyntheseIa()).replace("\n", "<br/>"))
                    .append("</div>");
        }

        // Cartes KPI — CA, Win rate, Panier moyen, Conversion leads
        b.append("<table class=\"kpis\"><tr>")
                .append(carteKpi("CA encaissé", montant(s.getCaRealise()), INDIGO,
                        variationTexte(s.getVariationCaPct()), couleurVariation(s.getVariationCaPct())))
                .append(carteKpi("Taux de victoire", pourcent(p.getWinRate()), VERT,
                        "opportunites gagnees", "#64748b"))
                .append(carteKpi("Panier moyen", montant(s.getPanierMoyen()), AMBRE,
                        "par affaire", "#64748b"))
                .append(carteKpi("Conversion leads", pourcent(s.getTauxConversionLeads()), INDIGO_FONCE,
                        s.getLeadsConvertis() + "/" + s.getNouveauxLeads() + " leads", "#64748b"))
                .append("</tr></table>");

        // Win rate hero
        b.append(blocWinRate(p.getWinRate()));

        // Funnel commercial
        b.append("<div class=\"carte\"><h2>Funnel commercial</h2>")
                .append("<p class=\"info\">Valeur du pipeline : <strong>")
                .append(montant(p.getValeur())).append("</strong></p>")
                .append(blocFunnel(funnel(p.getRepartitionParStatut())))
                .append("</div>");

        // Points d'attention
        b.append("<div class=\"carte\"><h2>Points d'attention</h2>");
        for (String alerte : pointsDAttention(r)) {
            boolean positif = alerte.toLowerCase().contains("aucun point");
            b.append("<div class=\"alerte ").append(positif ? "alerte-ok" : "alerte-warn").append("\">")
                    .append(echappe(alerte)).append("</div>");
        }
        b.append("</div>");

        // Activités
        b.append("<div class=\"carte\"><h2>Activités commerciales</h2>")
                .append("<p class=\"info\">Total : <strong>").append(a.getTotal()).append("</strong></p>")
                .append(barresType(a.getParType()))
                .append("</div>");

        // Devis & factures
        b.append("<div class=\"carte\"><h2>Devis &amp; factures</h2><table class=\"tableau\">")
                .append(ligneTableau("Devis émis", String.valueOf(d.getDevisEmis())))
                .append(ligneTableau("Taux d'acceptation des devis", pourcent(d.getTauxAcceptationDevis())))
                .append(ligneTableau("Factures impayées", String.valueOf(d.getFacturesImpayees())))
                .append(ligneTableau("Montant impayé", montant(d.getMontantImpaye())))
                .append(ligneTableau("CA encaissé", montant(d.getCaEncaisse())))
                .append("</table></div>");

        // Leads par source
        b.append("<div class=\"carte\"><h2>Leads par source</h2>")
                .append(barresSource(r.getLeadsParSource()))
                .append("</div>");

        b.append("<div class=\"pied\">Rapport généré automatiquement par Easy Sales CRM.</div>");
        b.append("</body></html>");
        return b.toString();
    }

    // ── Composants ────────────────────────────────────────────────────────────────

    private String carteKpi(String label, String valeur, String accent, String note, String couleurNote) {
        StringBuilder c = new StringBuilder("<td class=\"kpi\" style=\"border-top:3px solid ")
                .append(accent).append(";\">");
        c.append("<div class=\"kpi-label\">").append(echappe(label)).append("</div>");
        c.append("<div class=\"kpi-valeur\">").append(valeur).append("</div>");
        if (note != null && !note.isBlank()) {
            c.append("<div class=\"kpi-note\" style=\"color:").append(couleurNote).append(";\">")
                    .append(echappe(note)).append("</div>");
        }
        return c.append("</td>").toString();
    }

    /** Bloc « win rate » mis en avant : grande valeur + jauge de progression. */
    private String blocWinRate(double winRate) {
        int largeur = (int) Math.max(0, Math.min(100, Math.round(winRate)));
        return "<div class=\"winrate\">"
                + "<table class=\"wr-head\"><tr>"
                + "<td><div class=\"wr-label\">Taux de victoire</div>"
                + "<div class=\"wr-sub\">part des opportunites gagnees</div></td>"
                + "<td class=\"wr-val\">" + pourcent(winRate) + "</td></tr></table>"
                + "<div class=\"wr-track\"><div class=\"wr-fill\" style=\"width:" + largeur + "%;\"></div></div>"
                + "</div>";
    }

    /** Entonnoir commercial : barres décroissantes + taux de conversion par étape. */
    private String blocFunnel(List<EtapeFunnel> etapes) {
        if (etapes == null || etapes.isEmpty()) {
            return "<p class=\"info\">Aucune donnée.</p>";
        }
        StringBuilder b = new StringBuilder();
        for (EtapeFunnel e : etapes) {
            int largeur = Math.max(e.largeurPct(), e.count() > 0 ? 6 : 0);
            b.append("<table class=\"funnel-l\"><tr>")
                    .append("<td class=\"funnel-label\">").append(echappe(e.libelle())).append("</td>")
                    .append("<td class=\"funnel-zone\"><div class=\"funnel-fill\" style=\"width:")
                    .append(largeur).append("%;\"></div></td>")
                    .append("<td class=\"funnel-val\">").append(e.count()).append("</td>")
                    .append("<td class=\"funnel-conv\">").append(chipConversion(e.conversion())).append("</td>")
                    .append("</tr></table>");
        }
        return b.toString();
    }

    private String chipConversion(Double conversion) {
        if (conversion == null) {
            return "";
        }
        String classe = conversion >= 50 ? "chip-ok" : "chip-warn";
        return "<span class=\"chip " + classe + "\">" + pourcent(conversion) + "</span>";
    }

    private String barresType(List<TypeCount> counts) {
        if (counts == null || counts.isEmpty()) {
            return "<p class=\"info\">Aucune donnée.</p>";
        }
        long max = counts.stream().mapToLong(TypeCount::getCount).max().orElse(0);
        StringBuilder b = new StringBuilder();
        for (TypeCount c : counts) {
            b.append(barre(c.getType(), c.getCount(), largeurPct(c.getCount(), max)));
        }
        return b.toString();
    }

    private String barresSource(List<SourceCount> counts) {
        if (counts == null || counts.isEmpty()) {
            return "<p class=\"info\">Aucune donnée.</p>";
        }
        long max = counts.stream().mapToLong(SourceCount::getCount).max().orElse(0);
        StringBuilder b = new StringBuilder();
        for (SourceCount c : counts) {
            b.append(barre(c.getSource(), c.getCount(), largeurPct(c.getCount(), max)));
        }
        return b.toString();
    }

    /** Une ligne « label + barre proportionnelle + valeur ». */
    private String barre(String label, long valeur, int largeur) {
        return "<table class=\"barre-l\"><tr>"
                + "<td class=\"barre-label\">" + echappe(label) + "</td>"
                + "<td class=\"barre-zone\"><div class=\"barre-fill\" style=\"width:" + largeur + "%;\"></div></td>"
                + "<td class=\"barre-val\">" + valeur + "</td>"
                + "</tr></table>";
    }

    private String ligneTableau(String label, String valeur) {
        return "<tr><td class=\"t-label\">" + echappe(label) + "</td>"
                + "<td class=\"t-val\">" + valeur + "</td></tr>";
    }

    private String variationTexte(BigDecimal pct) {
        if (pct == null) {
            return "vs n/a";
        }
        String signe = pct.signum() >= 0 ? "+" : "-";
        return signe + pourcent(pct.abs());
    }

    private String couleurVariation(BigDecimal pct) {
        if (pct == null) {
            return "#475569";
        }
        return pct.signum() >= 0 ? VERT : ROUGE;
    }

    // ── Feuille de style (PDF) ─────────────────────────────────────────────────────

    private String css() {
        return """
                * { font-family: Helvetica, Arial, sans-serif; }
                body { margin: 0; color: #0f172a; font-size: 11px; background: #ffffff; }
                .entete { background: %1$s; color: #fff; padding: 22px 26px; }
                .entete .marque { font-size: 10px; letter-spacing: 2px; text-transform: uppercase;
                                  color: #c7d2fe; font-weight: bold; }
                .entete .titre { font-size: 24px; font-weight: bold; margin-top: 4px; }
                .periode-pill { display: inline-block; background: #4f46e5; color: #fff;
                                font-size: 11px; font-weight: bold; padding: 4px 12px;
                                border-radius: 10px; margin-top: 10px; }
                .entreprise { color: #c7d2fe; font-size: 11px; margin-left: 8px; }
                .ia { background: #eef2ff; border-left: 4px solid %2$s; padding: 12px 14px;
                      margin: 16px 24px; border-radius: 8px; font-style: italic; }
                .ia-titre { font-style: normal; font-size: 10px; letter-spacing: 1px;
                            text-transform: uppercase; color: %2$s; font-weight: bold; margin-bottom: 4px; }
                .kpis { width: 100%%; table-layout: fixed; border-collapse: separate;
                        border-spacing: 8px; margin: 8px 16px; }
                .kpi { width: 25%%; background: #f8fafc; border: 1px solid #e2e8f0;
                       border-radius: 8px; padding: 12px 10px; text-align: center; }
                .kpi-label { color: #64748b; font-size: 9px; letter-spacing: 0.5px;
                             text-transform: uppercase; font-weight: bold; }
                .kpi-valeur { font-size: 18px; font-weight: bold; margin: 5px 0 3px; }
                .kpi-note { font-size: 9px; font-weight: bold; }
                .winrate { background: #ecfdf5; border: 1px solid #a7f3d0; border-radius: 10px;
                           padding: 14px 16px; margin: 12px 24px; }
                .wr-head { width: 100%%; border-collapse: collapse; }
                .wr-label { font-size: 11px; letter-spacing: 0.5px; text-transform: uppercase;
                            color: #047857; font-weight: bold; }
                .wr-sub { color: #64748b; font-size: 10px; }
                .wr-val { text-align: right; font-size: 26px; font-weight: bold; color: #047857; }
                .wr-track { background: #d1fae5; border-radius: 10px; margin-top: 10px; }
                .wr-fill { background: #10b981; height: 12px; border-radius: 10px; }
                .carte { background: #fff; border: 1px solid #e2e8f0; border-radius: 10px;
                         padding: 14px 16px; margin: 12px 24px; }
                .carte h2 { color: #0f172a; font-size: 14px; margin: 0 0 10px; }
                .info { margin: 4px 0; color: #64748b; }
                .alerte { padding: 8px 12px; margin: 5px 0; border-radius: 6px; }
                .alerte-ok { background: #ecfdf5; border-left: 3px solid #10b981; }
                .alerte-warn { background: #fffbeb; border-left: 3px solid #f59e0b; }
                .funnel-l { width: 100%%; border-collapse: collapse; margin: 5px 0; }
                .funnel-label { width: 26%%; color: #0f172a; font-weight: bold; }
                .funnel-zone { width: 48%%; background: #eef2f7; border-radius: 5px; }
                .funnel-fill { background: %1$s; height: 16px; border-radius: 5px; }
                .funnel-val { width: 8%%; text-align: right; font-weight: bold; }
                .funnel-conv { width: 18%%; text-align: right; }
                .chip { font-size: 9px; font-weight: bold; padding: 2px 7px; border-radius: 10px; }
                .chip-ok { background: #d1fae5; color: #047857; }
                .chip-warn { background: #fef3c7; color: #b45309; }
                .tableau { width: 100%%; border-collapse: collapse; }
                .t-label { color: #64748b; padding: 5px 0; }
                .t-val { text-align: right; font-weight: bold; padding: 5px 0; }
                .barre-l { width: 100%%; border-collapse: collapse; margin: 3px 0; }
                .barre-label { width: 28%%; color: #475569; }
                .barre-zone { width: 60%%; background: #e2e8f0; border-radius: 3px; }
                .barre-fill { background: %3$s; height: 12px; border-radius: 3px; }
                .barre-val { width: 12%%; text-align: right; font-weight: bold; }
                .pied { color: #64748b; font-size: 10px; text-align: center; margin: 16px 24px; }
                """.formatted(INDIGO_FONCE, INDIGO, INDIGO);
    }
}
