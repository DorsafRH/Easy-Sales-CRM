package com.crm.modules.reporting.service;

import com.crm.modules.reporting.dto.RapportCommercialResponse;
import com.crm.modules.reporting.dto.RapportCommercialResponse.Activites;
import com.crm.modules.reporting.dto.RapportCommercialResponse.DevisFactures;
import com.crm.modules.reporting.dto.RapportCommercialResponse.Pipeline;
import com.crm.modules.reporting.dto.RapportCommercialResponse.SourceCount;
import com.crm.modules.reporting.dto.RapportCommercialResponse.StatutCount;
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

import static com.crm.modules.reporting.service.RapportPresentation.echappe;
import static com.crm.modules.reporting.service.RapportPresentation.largeurPct;
import static com.crm.modules.reporting.service.RapportPresentation.maxStatut;
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

    private static final String BLEU = "#1E3A8A";
    private static final String ROUGE = "#dc2626";
    private static final String VERT = "#16a34a";
    private static final String GRIS = "#475569";

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

    private String construireCorpsHtml(RapportCommercialResponse r) {
        Synthese s = r.getSynthese();
        Pipeline p = r.getPipeline();
        Activites a = r.getActivites();
        DevisFactures d = r.getDevisFactures();

        StringBuilder sb = new StringBuilder();
        sb.append("<div style='font-family:Arial,sans-serif;max-width:640px;margin:0 auto;color:#0f172a;'>");

        // En-tête
        sb.append("<div style='background:").append(BLEU).append(";padding:24px;border-radius:8px 8px 0 0;'>")
                .append("<h1 style='color:#fff;margin:0;font-size:20px;'>📊 Bilan commercial</h1>")
                .append("<p style='color:#cbd5e1;margin:6px 0 0;font-size:14px;'>")
                .append(echappe(r.getEntreprise().getNomEntreprise())).append(" · ")
                .append(echappe(r.getPeriode().getLibelle())).append("</p>")
                .append("</div>");

        sb.append("<div style='background:#f8fafc;padding:20px;border-radius:0 0 8px 8px;'>");

        // Salutation
        sb.append("<p>Bonjour ").append(echappe(r.getEntreprise().getNomProprietaire())).append(",</p>")
                .append("<p>Voici votre bilan d'activité commerciale pour la période.</p>");

        // Synthèse IA
        if (r.getSyntheseIa() != null && !r.getSyntheseIa().isBlank()) {
            sb.append("<div style='background:#eff6ff;border-left:4px solid ").append(BLEU)
                    .append(";padding:14px 16px;border-radius:6px;margin:16px 0;'>")
                    .append("<p style='margin:0;font-style:italic;'>")
                    .append(echappe(r.getSyntheseIa()).replace("\n", "<br/>"))
                    .append("</p></div>");
        }

        // Cartes KPI (2 x 2)
        sb.append("<table width='100%' cellpadding='0' cellspacing='0' style='margin:8px 0;'>")
                .append("<tr>")
                .append(carteKpi("CA encaissé", montant(s.getCaRealise()),
                        variation(s.getVariationCaPct())))
                .append(carteKpi("Nouveaux leads", String.valueOf(s.getNouveauxLeads()), ""))
                .append("</tr><tr>")
                .append(carteKpi("Taux de conversion", pourcent(s.getTauxConversionLeads()), ""))
                .append(carteKpi("Montant impayé", montant(d.getMontantImpaye()),
                        noteImpaye(d)))
                .append("</tr></table>");

        // Points d'attention
        StringBuilder alertes = new StringBuilder("<ul style='margin:0;padding-left:18px;'>");
        for (String pt : pointsDAttention(r)) {
            alertes.append("<li style='margin:4px 0;'>").append(echappe(pt)).append("</li>");
        }
        alertes.append("</ul>");
        sb.append(carte("🚨 Points d'attention", alertes.toString()));

        // Pipeline
        sb.append(carte("Pipeline commercial",
                "<p style='margin:0 0 8px;'>Valeur du pipeline : <strong>" + montant(p.getValeur())
                        + "</strong></p>" + barresStatut(p.getRepartitionParStatut())));

        // Activités
        sb.append(carte("Activités commerciales",
                "<p style='margin:0 0 8px;'>Total : <strong>" + a.getTotal() + "</strong></p>"
                        + barresType(a.getParType())));

        // Devis & factures
        sb.append(carte("Devis &amp; factures",
                ligneKpi("Devis émis", String.valueOf(d.getDevisEmis()))
                        + ligneKpi("Taux d'acceptation des devis", pourcent(d.getTauxAcceptationDevis()))
                        + ligneKpi("Factures impayées", String.valueOf(d.getFacturesImpayees()))
                        + ligneKpi("CA encaissé", montant(d.getCaEncaisse()))));

        // Leads par source
        sb.append(carte("Leads par source", barresSource(r.getLeadsParSource())));

        // Pied de page
        sb.append("<p style='color:#64748b;font-size:12px;margin-top:20px;'>")
                .append("Le détail complet est dans le PDF joint. Rapport généré automatiquement via ")
                .append(echappe(appName)).append(".</p>");

        sb.append("</div></div>");
        return sb.toString();
    }

    // ── Composants email ────────────────────────────────────────────────────────

    private String carte(String titre, String contenuHtml) {
        return "<div style='background:#fff;border:1px solid #e2e8f0;border-radius:8px;"
                + "padding:16px;margin:14px 0;'>"
                + "<h2 style='color:" + BLEU + ";margin:0 0 10px;font-size:16px;'>" + titre + "</h2>"
                + contenuHtml + "</div>";
    }

    private String carteKpi(String label, String valeur, String noteHtml) {
        return "<td width='50%' style='padding:6px;' valign='top'>"
                + "<div style='background:#fff;border:1px solid #e2e8f0;border-radius:8px;"
                + "padding:14px;text-align:center;'>"
                + "<div style='color:#64748b;font-size:11px;text-transform:uppercase;'>" + echappe(label) + "</div>"
                + "<div style='font-size:22px;font-weight:bold;margin:4px 0;'>" + valeur + "</div>"
                + (noteHtml == null || noteHtml.isBlank() ? "" : "<div style='font-size:12px;'>" + noteHtml + "</div>")
                + "</div></td>";
    }

    private String ligneKpi(String label, String valeur) {
        return "<p style='margin:4px 0;font-size:14px;'>"
                + "<span style='color:" + GRIS + ";'>" + echappe(label) + " : </span>"
                + "<strong>" + valeur + "</strong></p>";
    }

    private String barresStatut(List<StatutCount> counts) {
        if (counts == null || counts.isEmpty()) {
            return "<p style='margin:0;color:" + GRIS + ";'>Aucune donnée.</p>";
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
