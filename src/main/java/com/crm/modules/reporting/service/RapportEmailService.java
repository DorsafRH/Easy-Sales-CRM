package com.crm.modules.reporting.service;

import com.crm.modules.reporting.dto.RapportCommercialResponse;
import com.crm.modules.reporting.dto.RapportCommercialResponse.Activites;
import com.crm.modules.reporting.dto.RapportCommercialResponse.DevisFactures;
import com.crm.modules.reporting.dto.RapportCommercialResponse.Pipeline;
import com.crm.modules.reporting.dto.RapportCommercialResponse.SourceCount;
import com.crm.modules.reporting.dto.RapportCommercialResponse.Synthese;
import com.crm.modules.reporting.dto.RapportCommercialResponse.TypeCount;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.List;

import com.crm.modules.reporting.service.RapportPresentation.EtapeFunnel;

import static com.crm.modules.reporting.service.RapportPresentation.echappe;
import static com.crm.modules.reporting.service.RapportPresentation.funnel;
import static com.crm.modules.reporting.service.RapportPresentation.largeurPct;
import static com.crm.modules.reporting.service.RapportPresentation.montant;
import static com.crm.modules.reporting.service.RapportPresentation.pointsDAttention;
import static com.crm.modules.reporting.service.RapportPresentation.pourcent;

/**
 * Service d'envoi par email du rapport commercial au propriétaire.
 *
 * <p>Réutilise le compte mail applicatif ({@code spring.mail.username} =
 * easycrm4u@gmail.com). Email <b>visuel</b> (cartes KPI, barres CSS, points
 * d'attention) construit en HTML inline (email-safe), avec le <b>rapport PDF en
 * pièce jointe</b> ({@link RapportPdfService}).</p>
 *
 * <p>Envoi <b>synchrone</b> (≠ emails de compte en {@code @Async}) : l'appel vient
 * de n8n, qui doit connaître l'issue réelle (succès / échec) pour tracer et relancer.</p>
 *
 * @author Riahi Dorsaf
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RapportEmailService {

    private final JavaMailSender mailSender;
    private final RapportPdfService rapportPdfService;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.name:Easy Sales CRM}")
    private String appName;

    // ── Design system premium (couleurs partagées avec le PDF) ──────────────────
    private static final String INDIGO       = "#4f46e5";
    private static final String INDIGO_FONCE = "#1e3a8a";
    private static final String EMERAUDE     = "#10b981";
    private static final String ROSE         = "#f43f5e";
    private static final String AMBRE        = "#f59e0b";
    private static final String ENCRE        = "#0f172a";
    private static final String MUTED        = "#64748b";
    private static final String BORD         = "#e2e8f0";

    // Alias conservés pour les composants existants (barres, notes).
    private static final String BLEU = INDIGO;
    private static final String ROUGE = ROSE;
    private static final String VERT = EMERAUDE;
    private static final String GRIS = MUTED;

    /**
     * Envoie le rapport commercial (email visuel + PDF joint) au propriétaire.
     *
     * @param rapport rapport agrégé (avec éventuelle synthèse IA déjà renseignée)
     * @throws IllegalStateException si la génération du PDF ou l'envoi échoue
     */
    public void envoyerRapport(RapportCommercialResponse rapport) {
        String destinataire = rapport.getEntreprise().getEmail();
        try {
            byte[] pdf = rapportPdfService.genererPdf(rapport);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(destinataire);
            helper.setSubject(construireSujet(rapport));
            helper.setText(construireCorpsHtml(rapport), true);
            helper.addAttachment(nomFichierPdf(rapport),
                    new ByteArrayResource(pdf), "application/pdf");

            mailSender.send(message);
            log.info("[RAPPORT EMAIL] Rapport {} envoyé à {} (propriétaire id={}, PDF joint)",
                    rapport.getPeriode().getType(), destinataire,
                    rapport.getEntreprise().getProprietaireId());

        } catch (MessagingException | RuntimeException e) {
            log.error("[RAPPORT EMAIL] Échec envoi à {} : {}", destinataire, e.getMessage());
            throw new IllegalStateException(
                    "Échec de l'envoi du rapport par email à " + destinataire, e);
        }
    }

    // ── Sujet & nom de fichier ──────────────────────────────────────────────────

    private String construireSujet(RapportCommercialResponse r) {
        return switch (r.getPeriode().getType()) {
            case SEMAINE -> "📊 Votre bilan commercial hebdomadaire — " + r.getPeriode().getLibelle();
            case MOIS    -> "📊 Votre bilan commercial mensuel — " + r.getPeriode().getLibelle();
            case ANNEE   -> "📊 Votre bilan commercial annuel — " + r.getPeriode().getLibelle();
        };
    }

    /** Nom de fichier PDF nettoyé (sans accents ni espaces). */
    private String nomFichierPdf(RapportCommercialResponse r) {
        String base = "bilan-" + r.getPeriode().getType().name().toLowerCase() + "-"
                + r.getPeriode().getDebut();
        String sansAccents = Normalizer.normalize(base, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sansAccents.replaceAll("[^a-zA-Z0-9.-]", "-") + ".pdf";
    }

    // ── Corps HTML (email-safe : CSS inline + tables) ────────────────────────────

    private static final String POLICE =
            "-apple-system,'Segoe UI',Roboto,Helvetica,Arial,sans-serif";

    private String construireCorpsHtml(RapportCommercialResponse r) {
        Synthese s = r.getSynthese();
        Pipeline p = r.getPipeline();
        Activites a = r.getActivites();
        DevisFactures d = r.getDevisFactures();

        StringBuilder sb = new StringBuilder();
        sb.append("<div style='background:#eef1f6;padding:24px 12px;'>");
        sb.append("<div style='font-family:").append(POLICE)
                .append(";max-width:660px;margin:0 auto;color:").append(ENCRE)
                .append(";background:#ffffff;border-radius:18px;overflow:hidden;")
                .append("box-shadow:0 10px 30px rgba(15,23,42,0.10);'>");

        // En-tête premium (dégradé indigo, avec repli couleur solide)
        sb.append("<div style='background:").append(INDIGO_FONCE)
                .append(";background-image:linear-gradient(135deg,#1e3a8a 0%,#4338ca 55%,#6366f1 100%);")
                .append("padding:30px 30px 26px;'>")
                .append("<div style='color:#c7d2fe;font-size:11px;letter-spacing:2px;")
                .append("text-transform:uppercase;font-weight:700;'>")
                .append(echappe(appName)).append("</div>")
                .append("<h1 style='color:#fff;margin:8px 0 0;font-size:26px;font-weight:800;")
                .append("letter-spacing:-0.5px;'>Bilan commercial</h1>")
                .append("<div style='margin-top:14px;'>")
                .append("<span style='display:inline-block;background:rgba(255,255,255,0.16);")
                .append("color:#fff;font-size:13px;font-weight:600;padding:6px 14px;border-radius:999px;'>")
                .append(echappe(r.getPeriode().getLibelle())).append("</span>")
                .append("<span style='display:inline-block;color:#c7d2fe;font-size:13px;")
                .append("margin-left:10px;'>")
                .append(echappe(r.getEntreprise().getNomEntreprise())).append("</span>")
                .append("</div></div>");

        sb.append("<div style='padding:24px 26px 28px;'>");

        // Salutation
        sb.append("<p style='margin:0 0 4px;'>Bonjour ")
                .append(echappe(r.getEntreprise().getNomProprietaire())).append(",</p>")
                .append("<p style='margin:0 0 8px;color:").append(MUTED)
                .append(";'>Voici l'essentiel de votre performance commerciale sur la période.</p>");

        // Synthèse IA
        if (r.getSyntheseIa() != null && !r.getSyntheseIa().isBlank()) {
            sb.append("<div style='background:#eef2ff;border-left:4px solid ").append(INDIGO)
                    .append(";padding:14px 16px;border-radius:10px;margin:16px 0;'>")
                    .append("<div style='font-size:11px;letter-spacing:1px;text-transform:uppercase;")
                    .append("color:").append(INDIGO).append(";font-weight:700;margin-bottom:6px;'>")
                    .append("✨ Synthèse IA</div>")
                    .append("<p style='margin:0;font-style:italic;line-height:1.5;'>")
                    .append(echappe(r.getSyntheseIa()).replace("\n", "<br/>"))
                    .append("</p></div>");
        }

        // Cartes KPI (2 x 2) — CA, Win rate, Panier moyen, Conversion leads
        sb.append("<table width='100%' cellpadding='0' cellspacing='0' style='margin:8px 0;'>")
                .append("<tr>")
                .append(carteKpi("CA encaissé", montant(s.getCaRealise()), INDIGO,
                        variation(s.getVariationCaPct())))
                .append(carteKpi("Taux de victoire", pourcent(p.getWinRate()), EMERAUDE,
                        "<span style='color:" + MUTED + ";font-size:12px;'>opportunités gagnées</span>"))
                .append("</tr><tr>")
                .append(carteKpi("Panier moyen", montant(s.getPanierMoyen()), AMBRE,
                        "<span style='color:" + MUTED + ";font-size:12px;'>par affaire</span>"))
                .append(carteKpi("Conversion leads", pourcent(s.getTauxConversionLeads()), INDIGO_FONCE,
                        "<span style='color:" + MUTED + ";font-size:12px;'>"
                                + s.getLeadsConvertis() + "/" + s.getNouveauxLeads() + " leads</span>"))
                .append("</tr></table>");

        // Bloc Win rate hero (jauge de progression)
        sb.append(blocWinRate(p.getWinRate()));

        // Funnel commercial (entonnoir + taux de conversion par étape)
        sb.append(carte("Funnel commercial",
                "<p style='margin:0 0 12px;color:" + MUTED + ";font-size:13px;'>Valeur du pipeline : "
                        + "<strong style='color:" + ENCRE + ";'>" + montant(p.getValeur())
                        + "</strong></p>" + blocFunnel(funnel(p.getRepartitionParStatut()))));

        // Points d'attention
        sb.append(carte("🚨 Points d'attention", blocAlertes(pointsDAttention(r))));

        // Activités
        sb.append(carte("Activités commerciales",
                "<p style='margin:0 0 8px;color:" + MUTED + ";font-size:13px;'>Total : <strong style='color:"
                        + ENCRE + ";'>" + a.getTotal() + "</strong></p>" + barresType(a.getParType())));

        // Devis & factures
        sb.append(carte("Devis &amp; factures",
                ligneKpi("Devis émis", String.valueOf(d.getDevisEmis()))
                        + ligneKpi("Taux d'acceptation des devis", pourcent(d.getTauxAcceptationDevis()))
                        + ligneKpi("Factures impayées", noteImpaye(d))
                        + ligneKpi("Montant impayé", montant(d.getMontantImpaye()))));

        // Leads par source
        sb.append(carte("Leads par source", barresSource(r.getLeadsParSource())));

        // Pied de page
        sb.append("<p style='color:").append(MUTED).append(";font-size:12px;margin-top:22px;")
                .append("border-top:1px solid ").append(BORD).append(";padding-top:14px;'>")
                .append("Le détail complet est dans le PDF joint. Rapport généré automatiquement par ")
                .append(echappe(appName)).append(".</p>");

        sb.append("</div></div></div>");
        return sb.toString();
    }

    // ── Composants email ────────────────────────────────────────────────────────

    private String carte(String titre, String contenuHtml) {
        return "<div style='background:#fff;border:1px solid " + BORD + ";border-radius:14px;"
                + "padding:18px;margin:14px 0;'>"
                + "<h2 style='color:" + ENCRE + ";margin:0 0 12px;font-size:15px;font-weight:700;"
                + "letter-spacing:-0.2px;'>" + titre + "</h2>"
                + contenuHtml + "</div>";
    }

    /** Carte KPI premium : liseré couleur en haut, label majuscule, grande valeur, note. */
    private String carteKpi(String label, String valeur, String accent, String noteHtml) {
        return "<td width='50%' style='padding:6px;' valign='top'>"
                + "<div style='background:#fff;border:1px solid " + BORD + ";border-radius:14px;"
                + "border-top:3px solid " + accent + ";padding:16px;'>"
                + "<div style='color:" + MUTED + ";font-size:11px;letter-spacing:0.5px;"
                + "text-transform:uppercase;font-weight:600;'>" + echappe(label) + "</div>"
                + "<div style='font-size:24px;font-weight:800;margin:6px 0 4px;color:" + ENCRE + ";"
                + "letter-spacing:-0.5px;'>" + valeur + "</div>"
                + (noteHtml == null || noteHtml.isBlank() ? "" : "<div style='font-size:12px;'>" + noteHtml + "</div>")
                + "</div></td>";
    }

    /** Bloc « win rate » mis en avant : grande valeur + jauge de progression. */
    private String blocWinRate(double winRate) {
        int largeur = (int) Math.max(0, Math.min(100, Math.round(winRate)));
        return "<div style='background:#ecfdf5;border:1px solid #a7f3d0;border-radius:14px;"
                + "padding:18px;margin:14px 0;'>"
                + "<table width='100%' cellpadding='0' cellspacing='0'><tr>"
                + "<td><div style='color:#047857;font-size:11px;letter-spacing:0.5px;"
                + "text-transform:uppercase;font-weight:700;'>Taux de victoire</div>"
                + "<div style='color:" + MUTED + ";font-size:12px;'>part des opportunités gagnées</div></td>"
                + "<td align='right' style='font-size:30px;font-weight:800;color:#047857;'>"
                + pourcent(winRate) + "</td></tr></table>"
                + "<div style='background:#d1fae5;border-radius:999px;height:12px;margin-top:12px;'>"
                + "<div style='background:" + EMERAUDE + ";background-image:linear-gradient(90deg,#10b981,#34d399);"
                + "width:" + largeur + "%;height:12px;border-radius:999px;'></div></div>"
                + "</div>";
    }

    /** Entonnoir commercial : barres décroissantes + chips de conversion entre étapes. */
    private String blocFunnel(List<EtapeFunnel> etapes) {
        if (etapes == null || etapes.isEmpty()) {
            return "<p style='margin:0;color:" + MUTED + ";'>Aucune donnée.</p>";
        }
        StringBuilder b = new StringBuilder();
        for (EtapeFunnel e : etapes) {
            b.append("<table width='100%' cellpadding='0' cellspacing='0' style='margin:6px 0;font-size:13px;'><tr>")
                    .append("<td width='28%' style='color:").append(ENCRE).append(";font-weight:600;'>")
                    .append(echappe(e.libelle())).append("</td>")
                    .append("<td width='46%'><div style='background:#eef2f7;border-radius:6px;'>")
                    .append("<div style='background:").append(INDIGO)
                    .append(";background-image:linear-gradient(90deg,#4338ca,#6366f1);width:")
                    .append(Math.max(e.largeurPct(), e.count() > 0 ? 6 : 0))
                    .append("%;height:18px;border-radius:6px;'></div></div></td>")
                    .append("<td width='10%' align='right' style='font-weight:800;color:").append(ENCRE).append(";'>")
                    .append(e.count()).append("</td>")
                    .append("<td width='16%' align='right'>").append(chipConversion(e.conversion())).append("</td>")
                    .append("</tr></table>");
        }
        return b.toString();
    }

    /** Chip de conversion d'une étape à l'autre (vert si ≥ 50 %, ambre sinon). */
    private String chipConversion(Double conversion) {
        if (conversion == null) {
            return "";
        }
        String couleur = conversion >= 50 ? "#047857" : "#b45309";
        String fond    = conversion >= 50 ? "#d1fae5" : "#fef3c7";
        return "<span style='background:" + fond + ";color:" + couleur + ";font-size:11px;"
                + "font-weight:700;padding:3px 8px;border-radius:999px;'>" + pourcent(conversion) + "</span>";
    }

    /** Liste des points d'attention sous forme de cartes à liseré gauche. */
    private String blocAlertes(List<String> alertes) {
        StringBuilder b = new StringBuilder();
        for (String pt : alertes) {
            boolean positif = pt.toLowerCase().contains("aucun point");
            String accent = positif ? EMERAUDE : AMBRE;
            String fond   = positif ? "#ecfdf5" : "#fffbeb";
            b.append("<div style='background:").append(fond).append(";border-left:3px solid ").append(accent)
                    .append(";border-radius:8px;padding:10px 12px;margin:6px 0;font-size:13px;'>")
                    .append(echappe(pt)).append("</div>");
        }
        return b.toString();
    }

    private String ligneKpi(String label, String valeur) {
        return "<p style='margin:4px 0;font-size:14px;'>"
                + "<span style='color:" + GRIS + ";'>" + echappe(label) + " : </span>"
                + "<strong>" + valeur + "</strong></p>";
    }

    private String barresType(List<TypeCount> counts) {
        if (counts == null || counts.isEmpty()) {
            return "<p style='margin:0;color:" + GRIS + ";'>Aucune donnée.</p>";
        }
        long max = counts.stream().mapToLong(TypeCount::getCount).max().orElse(0);
        StringBuilder b = new StringBuilder();
        for (TypeCount c : counts) {
            b.append(barre(c.getType(), c.getCount(), largeurPct(c.getCount(), max)));
        }
        return b.toString();
    }

    private String barresSource(List<SourceCount> counts) {
        if (counts == null) {
            return "<p style='margin:0;color:" + GRIS + ";'>Aucune donnée.</p>";
        }
        long max = counts.stream().mapToLong(SourceCount::getCount).max().orElse(0);
        String corps = counts.stream()
                .filter(c -> c.getCount() > 0)
                .map(c -> barre(c.getSource(), c.getCount(), largeurPct(c.getCount(), max)))
                .reduce("", String::concat);
        return corps.isEmpty() ? "<p style='margin:0;color:" + GRIS + ";'>Aucun lead sur la période.</p>" : corps;
    }

    /** Ligne « label + barre CSS proportionnelle + valeur » (table email-safe). */
    private String barre(String label, long valeur, int largeur) {
        return "<table width='100%' cellpadding='0' cellspacing='0' style='margin:3px 0;font-size:13px;'><tr>"
                + "<td width='35%' style='color:" + GRIS + ";'>" + echappe(label) + "</td>"
                + "<td width='52%'><div style='background:#e2e8f0;border-radius:3px;'>"
                + "<div style='background:" + BLEU + ";width:" + largeur + "%;height:12px;border-radius:3px;'></div>"
                + "</div></td>"
                + "<td width='13%' align='right' style='font-weight:bold;'>" + valeur + "</td>"
                + "</tr></table>";
    }

    /** Variation signée, colorée, avec flèche (Unicode OK en email). */
    private String variation(BigDecimal pct) {
        if (pct == null) {
            return "<span style='color:" + GRIS + ";font-size:12px;'>vs période préc. : n/a</span>";
        }
        boolean positif = pct.signum() >= 0;
        String couleur = positif ? VERT : ROUGE;
        String fleche = positif ? "▲ +" : "▼ ";
        return "<span style='color:" + couleur + ";font-weight:bold;font-size:12px;'>"
                + fleche + pourcent(pct.abs()) + "</span>";
    }

    private String noteImpaye(DevisFactures d) {
        String couleur = d.getFacturesImpayees() > 0 ? ROUGE : VERT;
        return "<span style='color:" + couleur + ";font-weight:bold;'>"
                + d.getFacturesImpayees() + " facture(s)</span>";
    }
}
