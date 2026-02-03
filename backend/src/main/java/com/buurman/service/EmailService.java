package com.buurman.service;

import com.buurman.domain.Contract;
import com.buurman.domain.Payment;
import com.buurman.domain.Team;
import com.buurman.domain.TeamInvitation;
import com.buurman.domain.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.email.from}")
    private String fromEmail;

    @Value("${app.email.from-name}")
    private String fromName;

    @Value("${app.email.base-url}")
    private String baseUrl;

    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    @Async("emailTaskExecutor")
    public void sendWelcomeEmail(User user) {
        Context context = new Context();
        context.setVariable("userName", user.getFirstName());
        context.setVariable("baseUrl", baseUrl);

        sendEmail(user.getEmail(), "Welcome to Buurman!", "email/welcome", context);
    }

    @Async("emailTaskExecutor")
    public void sendTeamInvitation(TeamInvitation invitation, String inviterName, String teamName) {
        Context context = new Context();
        context.setVariable("inviterName", inviterName);
        context.setVariable("teamName", teamName);
        context.setVariable("role", formatRole(invitation.getRole()));
        context.setVariable("inviteUrl", baseUrl + "/invitation/" + invitation.getToken());
        context.setVariable("expiresAt", formatDate(invitation.getExpiresAt()));

        sendEmail(invitation.getEmail(), "You've been invited to join " + teamName,
                  "email/team-invitation", context);
    }

    @Async("emailTaskExecutor")
    public void sendInvitationAccepted(User inviter, User newMember, Team team) {
        Context context = new Context();
        context.setVariable("inviterName", inviter.getFirstName());
        context.setVariable("memberName", newMember.getFirstName() + " " + newMember.getLastName());
        context.setVariable("memberEmail", newMember.getEmail());
        context.setVariable("teamName", team.getName());
        context.setVariable("baseUrl", baseUrl);

        sendEmail(inviter.getEmail(), newMember.getFirstName() + " joined " + team.getName(),
                  "email/invitation-accepted", context);
    }

    @Async("emailTaskExecutor")
    public void sendPasswordChanged(User user) {
        Context context = new Context();
        context.setVariable("userName", user.getFirstName());
        context.setVariable("baseUrl", baseUrl);

        sendEmail(user.getEmail(), "Your password has been changed", "email/password-changed", context);
    }

    @Async("emailTaskExecutor")
    public void sendPaymentReminder(User user, Payment payment, String propertyName, BigDecimal amount) {
        Context context = new Context();
        context.setVariable("userName", user.getFirstName());
        context.setVariable("propertyName", propertyName);
        context.setVariable("amount", formatCurrency(amount));
        context.setVariable("dueDate", formatDate(payment.getDueDate()));
        context.setVariable("baseUrl", baseUrl);

        sendEmail(user.getEmail(), "Payment reminder for " + propertyName,
                  "email/payment-reminder", context);
    }

    @Async("emailTaskExecutor")
    public void sendContractExpiryAlert(User user, Contract contract, String propertyName, int daysUntilExpiry) {
        Context context = new Context();
        context.setVariable("userName", user.getFirstName());
        context.setVariable("propertyName", propertyName);
        context.setVariable("daysUntilExpiry", daysUntilExpiry);
        context.setVariable("expiryDate", formatDate(contract.getEndDate()));
        context.setVariable("baseUrl", baseUrl);

        sendEmail(user.getEmail(), "Contract expiring soon for " + propertyName,
                  "email/contract-expiry", context);
    }

    private void sendEmail(String to, String subject, String templateName, Context context) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String htmlContent = templateEngine.process(templateName, context);

            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email sent successfully to {} with subject: {}", to, subject);
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("Failed to send email to {} with subject: {}", to, subject, e);
        }
    }

    private String formatRole(String role) {
        return switch (role) {
            case "TEAM_ADMIN" -> "Administrator";
            case "TEAM_EDITOR" -> "Editor";
            case "TEAM_VIEWER" -> "Viewer";
            default -> role;
        };
    }

    private String formatDate(java.time.Instant instant) {
        return instant != null
            ? LocalDate.ofInstant(instant, java.time.ZoneOffset.UTC)
                       .format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))
            : "";
    }

    private String formatDate(LocalDate date) {
        return date != null ? date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")) : "";
    }

    private String formatCurrency(BigDecimal amount) {
        return amount != null ? String.format("€%.2f", amount) : "";
    }
}
