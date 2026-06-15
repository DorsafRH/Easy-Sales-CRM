package com.crm.modules.reporting.service;

import com.crm.modules.reporting.dto.RapportCommercialResponse;
import com.crm.modules.reporting.dto.RapportCommercialResponse.Activites;
import com.crm.modules.reporting.dto.RapportCommercialResponse.DevisFactures;
import com.crm.modules.reporting.dto.RapportCommercialResponse.Pipeline;
import com.crm.modules.reporting.dto.RapportCommercialResponse.SourceCount;
import com.crm.modules.reporting.dto.RapportCommercialResponse.StatutCount;
import com.crm.modules.reporting.dto.RapportCommercialResponse.Synthese;
import com.crm.modules.reporting.dto.RapportCommercialResponse.TypeCount;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;

import static com.crm.modules.reporting.service.RapportPresentation.echappe;
import static com.crm.modules.reporting.service.RapportPresentation.largeurPct;
import static com.crm.modules.reporting.service.RapportPresentation.maxStatut;
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

    private static final String BLEU = "#1E3A8A";
    private static final String ROUGE = "#dc2626";
    private static final String VERT = "#16a34a";

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

        // En-tête
        b.append("<div class=\"entete\">")
                .append("<div class=\"titre\">Bilan commercial</div>")
                .append("<div class=\"sous-titre\">")
                .append(echappe(r.getEntreprise().getNomEntreprise())).append(" · ")
                .append(echappe(r.getPeriode().getLibelle()))
                .append("</div></div>");

        // Synthèse IA
        if (r.getSyntheseIa() != null && !r.getSyntheseIa().isBlank()) {
            b.append("<div class=\"ia\"><strong>Synthèse</strong><br/>")
                    .append(echappe(r.getSyntheseIa()).replace("\n", "<br/>"))
                    .append("</div>");
        }

        // Cartes KPI
        b.append("<table class=\"kpis\"><tr>")
                .append(carteKpi("CA encaissé", montant(s.getCaRealise()), variationTexte(s.getVariationCaPct()),
                        couleurVariation(s.getVariationCaPct())))
                .append(carteKpi("Nouveaux leads", String.valueOf(s.getNouveauxLeads()), "", "#475569"))
                .append(carteKpi("Taux de conversion", pourcent(s.getTauxConversionLeads()), "", "#475569"))
                .append(carteKpi("Montant impayé", montant(d.getMontantImpaye()),
                        d.getFacturesImpayees() + " facture(s)", d.getFacturesImpayees() > 0 ? ROUGE : VERT))
                .append("</tr></table>");

        // Points d'attention
        b.append("<div class=\"carte\"><h2>Points d'attention</h2><ul class=\"alertes\">");
        for (String alerte : pointsDAttention(r)) {
            b.append("<li>").append(echappe(alerte)).append("</li>");
        }
        b.append("</ul></div>");

        // Pipeline
        b.append("<div class=\"carte\"><h2>Pipeline commercial</h2>")
                .append("<p class=\"info\">Valeur du pipeline : <strong>")
                .append(montant(p.getValeur())).append("</strong></p>")
                .append(barresStatut(p.getRepartitionParStatut()))
                .append("</div>");

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

    private String carteKpi(String label, String valeur, String note, String couleurNote) {
        StringBuilder c = new StringBuilder("<td class=\"kpi\">");
        c.append("<div class=\"kpi-label\">").append(echappe(label)).append("</div>");
        c.append("<div class=\"kpi-valeur\">").append(valeur).append("</div>");
        if (note != null && !note.isBlank()) {
            c.append("<div class=\"kpi-note\" style=\"color:").append(couleurNote).append(";\">")
                    .append(echappe(note)).append("</div>");
        }
        return c.append("</td>").toString();
    }

    private String barresStatut(List<StatutCount> counts) {
        if (counts == null || counts.isEmpty()) {
            return "<p class=\"info\">Aucune donnée.</p>";
        }
        long max = maxStatut(counts);
        StringBuilder b = new StringBuilder();
        for (StatutCount c : counts) {
            b.append(barre(c.getStatut(), c.getCount(), largeurPct(c.getCount(), max)));
        }
        return b.toString();
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
                body { margin: 0; color: #0f172a; font-size: 11px; }
                .entete { background: %s; color: #fff; padding: 18px 24px; }
                .entete .titre { font-size: 22px; font-weight: bold; }
                .entete .sous-titre { font-size: 12px; color: #cbd5e1; margin-top: 4px; }
                .ia { background: #eff6ff; border-left: 4px solid %s; padding: 10px 14px;
                      margin: 16px 24px; font-style: italic; }
                .kpis { width: 100%%; border-collapse: separate; border-spacing: 8px;
                        margin: 8px 16px; }
                .kpi { width: 25%%; background: #f8fafc; border: 1px solid #e2e8f0;
                       border-radius: 6px; padding: 10px; text-align: center; }
                .kpi-label { color: #64748b; font-size: 10px; text-transform: uppercase; }
                .kpi-valeur { font-size: 18px; font-weight: bold; margin: 4px 0; }
                .kpi-note { font-size: 10px; font-weight: bold; }
                .carte { background: #fff; border: 1px solid #e2e8f0; border-radius: 6px;
                         padding: 12px 16px; margin: 12px 24px; }
                .carte h2 { color: %s; font-size: 14px; margin: 0 0 8px; }
                .info { margin: 4px 0; }
                .alertes { margin: 0; padding-left: 18px; }
                .alertes li { margin: 3px 0; }
                .tableau { width: 100%%; border-collapse: collapse; }
                .t-label { color: #64748b; padding: 4px 0; }
                .t-val { text-align: right; font-weight: bold; padding: 4px 0; }
                .barre-l { width: 100%%; border-collapse: collapse; margin: 3px 0; }
                .barre-label { width: 28%%; color: #475569; }
                .barre-zone { width: 60%%; background: #e2e8f0; border-radius: 3px; }
                .barre-fill { background: %s; height: 12px; border-radius: 3px; }
                .barre-val { width: 12%%; text-align: right; font-weight: bold; }
                .pied { color: #64748b; font-size: 10px; text-align: center; margin: 16px 24px; }
                """.formatted(BLEU, BLEU, BLEU, BLEU);
    }
}
