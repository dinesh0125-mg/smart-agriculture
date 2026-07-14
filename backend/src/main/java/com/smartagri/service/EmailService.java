package com.smartagri.service;

import com.smartagri.dto.request.ContactMessageRequest;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:${MAIL_FROM:${MAIL_USERNAME}}}")
    private String mailFrom;

    @Value("${app.name:Smart Agriculture Marketplace}")
    private String applicationName;

    @Value("${app.frontend-url:${FRONTEND_URL:http://localhost:5173}}")
    private String frontendUrl;

    /**
     * Send OTP verification code.
     *
     * Existing AuthController expects:
     * sendOtpEmail(String email, String fullName, String otp)
     */
    public void sendOtpEmail(
            String recipientEmail,
            String recipientName,
            String otp
    ) {
        validateEmail(recipientEmail);

        String safeName = defaultName(recipientName);

        String subject = applicationName + " - Verification Code";

        String body = """
                <h2>Email Verification</h2>
                <p>Hello %s,</p>
                <p>Use the following verification code to continue:</p>

                <div style="
                    margin:24px 0;
                    text-align:center;
                ">
                    <span style="
                        display:inline-block;
                        padding:16px 24px;
                        background:#edf8f0;
                        color:#1f7a3f;
                        border:1px solid #cde6d3;
                        border-radius:10px;
                        font-size:30px;
                        font-weight:bold;
                        letter-spacing:7px;
                    ">
                        %s
                    </span>
                </div>

                <p>This verification code is valid for 10 minutes.</p>
                <p>Do not share this code with anyone.</p>
                """.formatted(
                escapeHtml(safeName),
                escapeHtml(otp)
        );

        sendHtmlEmail(recipientEmail, subject, body);
    }

    /**
     * Alternative OTP method in case another service uses this name.
     */
    public void sendVerificationCode(
            String recipientEmail,
            String otp
    ) {
        sendOtpEmail(
                recipientEmail,
                "User",
                otp
        );
    }

    /**
     * AuthService expects:
     * sendWelcomeEmail(String email, String fullName, String role)
     */
    @Async
    public void sendWelcomeEmail(
            String recipientEmail,
            String fullName,
            String role
    ) {
        validateEmail(recipientEmail);

        String safeName = defaultName(fullName);
        String safeRole = role == null || role.isBlank()
                ? "User"
                : role;

        String subject = "Welcome to " + applicationName;

        String body = """
                <h2>Welcome to %s</h2>

                <p>Hello %s,</p>

                <p>Your account has been created successfully.</p>

                <p>
                    <strong>Account role:</strong> %s
                </p>

                <p>
                    You can now explore products, manage orders and use
                    the marketplace services available for your account.
                </p>

                <div style="margin-top:24px;">
                    <a href="%s"
                       style="
                           display:inline-block;
                           padding:12px 22px;
                           background:#1f7a3f;
                           color:#ffffff;
                           text-decoration:none;
                           border-radius:7px;
                       ">
                        Open Marketplace
                    </a>
                </div>
                """.formatted(
                escapeHtml(applicationName),
                escapeHtml(safeName),
                escapeHtml(safeRole),
                escapeHtml(frontendUrl)
        );

        sendHtmlEmail(recipientEmail, subject, body);
    }

    /**
     * AuthService expects:
     * sendVerificationEmail(String email, String fullName, String token)
     */
    @Async
    public void sendVerificationEmail(
            String recipientEmail,
            String fullName,
            String verificationToken
    ) {
        validateEmail(recipientEmail);

        String safeName = defaultName(fullName);

        String verificationUrl =
                normalizeFrontendUrl(frontendUrl)
                        + "/verify-email?token="
                        + verificationToken;

        String subject = applicationName + " - Verify Your Email";

        String body = """
                <h2>Verify your email address</h2>

                <p>Hello %s,</p>

                <p>
                    Click the button below to verify your email address.
                </p>

                <div style="margin:24px 0;">
                    <a href="%s"
                       style="
                           display:inline-block;
                           padding:12px 22px;
                           background:#1f7a3f;
                           color:#ffffff;
                           text-decoration:none;
                           border-radius:7px;
                       ">
                        Verify Email
                    </a>
                </div>

                <p>
                    If the button does not work, copy and open this link:
                </p>

                <p style="word-break:break-all;">
                    %s
                </p>

                <p>
                    Ignore this email if you did not create this account.
                </p>
                """.formatted(
                escapeHtml(safeName),
                escapeHtml(verificationUrl),
                escapeHtml(verificationUrl)
        );

        sendHtmlEmail(recipientEmail, subject, body);
    }

    /**
     * AuthService expects:
     * sendPasswordResetEmail(String email, String fullName, String token)
     */
    public void sendPasswordResetEmail(
            String recipientEmail,
            String fullName,
            String resetToken
    ) {
        validateEmail(recipientEmail);

        String safeName = defaultName(fullName);

        String resetUrl =
                normalizeFrontendUrl(frontendUrl)
                        + "/reset-password?token="
                        + resetToken;

        String subject = applicationName + " - Password Reset";

        String body = """
                <h2>Password Reset Request</h2>

                <p>Hello %s,</p>

                <p>
                    We received a request to reset your account password.
                </p>

                <div style="margin:24px 0;">
                    <a href="%s"
                       style="
                           display:inline-block;
                           padding:12px 22px;
                           background:#1f7a3f;
                           color:#ffffff;
                           text-decoration:none;
                           border-radius:7px;
                       ">
                        Reset Password
                    </a>
                </div>

                <p>
                    If the button does not work, copy and open this link:
                </p>

                <p style="word-break:break-all;">
                    %s
                </p>

                <p>
                    Ignore this email if you did not request a password reset.
                </p>
                """.formatted(
                escapeHtml(safeName),
                escapeHtml(resetUrl),
                escapeHtml(resetUrl)
        );

        sendHtmlEmail(recipientEmail, subject, body);
    }

    /**
     * OrderService expects:
     * sendOrderStatusUpdateEmail(
     *     String email,
     *     String fullName,
     *     Long orderId,
     *     String status
     * )
     */
    @Async
    public void sendOrderStatusUpdateEmail(
            String recipientEmail,
            String fullName,
            Long orderId,
            String orderStatus
    ) {
        validateEmail(recipientEmail);

        String safeName = defaultName(fullName);

        String safeStatus = orderStatus == null || orderStatus.isBlank()
                ? "UPDATED"
                : orderStatus;

        String orderUrl =
                normalizeFrontendUrl(frontendUrl)
                        + "/orders/"
                        + orderId;

        String subject =
                "Order #" + orderId + " Status Updated";

        String body = """
                <h2>Order Status Update</h2>

                <p>Hello %s,</p>

                <p>
                    The status of your order has been updated.
                </p>

                <table style="
                    width:100%%;
                    border-collapse:collapse;
                    margin:20px 0;
                ">
                    <tr>
                        <td style="
                            padding:10px;
                            border:1px solid #dddddd;
                        ">
                            <strong>Order ID</strong>
                        </td>

                        <td style="
                            padding:10px;
                            border:1px solid #dddddd;
                        ">
                            #%s
                        </td>
                    </tr>

                    <tr>
                        <td style="
                            padding:10px;
                            border:1px solid #dddddd;
                        ">
                            <strong>Status</strong>
                        </td>

                        <td style="
                            padding:10px;
                            border:1px solid #dddddd;
                        ">
                            %s
                        </td>
                    </tr>
                </table>

                <div style="margin-top:24px;">
                    <a href="%s"
                       style="
                           display:inline-block;
                           padding:12px 22px;
                           background:#1f7a3f;
                           color:#ffffff;
                           text-decoration:none;
                           border-radius:7px;
                       ">
                        View Order
                    </a>
                </div>
                """.formatted(
                escapeHtml(safeName),
                orderId,
                escapeHtml(safeStatus),
                escapeHtml(orderUrl)
        );

        sendHtmlEmail(recipientEmail, subject, body);
    }

    /**
     * PaymentService expects:
     * sendPaymentSuccessEmail(
     *     String email,
     *     String fullName,
     *     Long orderId,
     *     BigDecimal amount
     * )
     */
    @Async
    public void sendPaymentSuccessEmail(
            String recipientEmail,
            String fullName,
            Long orderId,
            BigDecimal amount
    ) {
        validateEmail(recipientEmail);

        String safeName = defaultName(fullName);
        String formattedAmount = formatCurrency(amount);

        String orderUrl =
                normalizeFrontendUrl(frontendUrl)
                        + "/orders/"
                        + orderId;

        String subject =
                "Payment Successful - Order #" + orderId;

        String body = """
                <h2>Payment Successful</h2>

                <p>Hello %s,</p>

                <p>
                    Your payment has been received successfully.
                </p>

                <table style="
                    width:100%%;
                    border-collapse:collapse;
                    margin:20px 0;
                ">
                    <tr>
                        <td style="
                            padding:10px;
                            border:1px solid #dddddd;
                        ">
                            <strong>Order ID</strong>
                        </td>

                        <td style="
                            padding:10px;
                            border:1px solid #dddddd;
                        ">
                            #%s
                        </td>
                    </tr>

                    <tr>
                        <td style="
                            padding:10px;
                            border:1px solid #dddddd;
                        ">
                            <strong>Amount Paid</strong>
                        </td>

                        <td style="
                            padding:10px;
                            border:1px solid #dddddd;
                        ">
                            %s
                        </td>
                    </tr>
                </table>

                <div style="margin-top:24px;">
                    <a href="%s"
                       style="
                           display:inline-block;
                           padding:12px 22px;
                           background:#1f7a3f;
                           color:#ffffff;
                           text-decoration:none;
                           border-radius:7px;
                       ">
                        View Order
                    </a>
                </div>
                """.formatted(
                escapeHtml(safeName),
                orderId,
                escapeHtml(formattedAmount),
                escapeHtml(orderUrl)
        );

        sendHtmlEmail(recipientEmail, subject, body);
    }

    /**
     * ContactService expects:
     * sendContactNotification(
     *     String adminEmail,
     *     ContactMessageRequest request
     * )
     *
     * Adjust getter names only when your ContactMessageRequest
     * uses different property names.
     */
    @Async
    public void sendContactNotification(
            String recipientEmail,
            ContactMessageRequest request
    ) {
        validateEmail(recipientEmail);

        if (request == null) {
            throw new IllegalArgumentException(
                    "Contact request cannot be null."
            );
        }

        String senderName = safeValue(
                readContactName(request)
        );

        String senderEmail = safeValue(
                readContactEmail(request)
        );

        String contactSubject = safeValue(
                readContactSubject(request)
        );

        String contactMessage = safeValue(
                readContactMessage(request)
        );

        String subject =
                "New Contact Message - " + contactSubject;

        String body = """
                <h2>New Contact Message</h2>

                <table style="
                    width:100%%;
                    border-collapse:collapse;
                    margin:20px 0;
                ">
                    <tr>
                        <td style="
                            padding:10px;
                            border:1px solid #dddddd;
                            width:130px;
                        ">
                            <strong>Name</strong>
                        </td>

                        <td style="
                            padding:10px;
                            border:1px solid #dddddd;
                        ">
                            %s
                        </td>
                    </tr>

                    <tr>
                        <td style="
                            padding:10px;
                            border:1px solid #dddddd;
                        ">
                            <strong>Email</strong>
                        </td>

                        <td style="
                            padding:10px;
                            border:1px solid #dddddd;
                        ">
                            %s
                        </td>
                    </tr>

                    <tr>
                        <td style="
                            padding:10px;
                            border:1px solid #dddddd;
                        ">
                            <strong>Subject</strong>
                        </td>

                        <td style="
                            padding:10px;
                            border:1px solid #dddddd;
                        ">
                            %s
                        </td>
                    </tr>

                    <tr>
                        <td style="
                            padding:10px;
                            border:1px solid #dddddd;
                            vertical-align:top;
                        ">
                            <strong>Message</strong>
                        </td>

                        <td style="
                            padding:10px;
                            border:1px solid #dddddd;
                            white-space:pre-wrap;
                        ">
                            %s
                        </td>
                    </tr>
                </table>
                """.formatted(
                escapeHtml(senderName),
                escapeHtml(senderEmail),
                escapeHtml(contactSubject),
                escapeHtml(contactMessage)
        );

        sendHtmlEmail(recipientEmail, subject, body);
    }

    /**
     * Generic plain text email helper.
     */
    public void sendSimpleEmail(
            String recipientEmail,
            String subject,
            String message
    ) {
        validateEmail(recipientEmail);

        String body = """
                <p>%s</p>
                """.formatted(
                escapeHtml(message)
                        .replace("\n", "<br>")
        );

        sendHtmlEmail(recipientEmail, subject, body);
    }

    private void sendHtmlEmail(
            String recipientEmail,
            String subject,
            String content
    ) {
        try {
            MimeMessage mimeMessage =
                    mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(
                            mimeMessage,
                            false,
                            StandardCharsets.UTF_8.name()
                    );

            helper.setFrom(
                    mailFrom,
                    applicationName
            );

            helper.setTo(recipientEmail);
            helper.setSubject(subject);

            helper.setText(
                    wrapHtmlTemplate(content),
                    true
            );

            mailSender.send(mimeMessage);

            log.info(
                    "Email sent successfully to {} with subject: {}",
                    maskEmail(recipientEmail),
                    subject
            );

        } catch (MailAuthenticationException exception) {

            log.error(
                    "SMTP authentication failed. " +
                    "Check MAIL_USERNAME and MAIL_PASSWORD.",
                    exception
            );

            throw new IllegalStateException(
                    "Email service authentication failed.",
                    exception
            );

        } catch (MailSendException exception) {

            log.error(
                    "SMTP failed to send email to {}. Reason: {}",
                    maskEmail(recipientEmail),
                    exception.getMessage(),
                    exception
            );

            throw new IllegalStateException(
                    "Unable to send email. Please try again later.",
                    exception
            );

        } catch (MessagingException | MailException exception) {

            log.error(
                    "Email sending failed for {}. Reason: {}",
                    maskEmail(recipientEmail),
                    exception.getMessage(),
                    exception
            );

            throw new IllegalStateException(
                    "Failed to send email.",
                    exception
            );

        } catch (Exception exception) {

            log.error(
                    "Unexpected email error for {}. Reason: {}",
                    maskEmail(recipientEmail),
                    exception.getMessage(),
                    exception
            );

            throw new IllegalStateException(
                    "Unexpected email service error.",
                    exception
            );
        }
    }

    private String wrapHtmlTemplate(String content) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport"
                          content="width=device-width, initial-scale=1.0">
                </head>

                <body style="
                    margin:0;
                    padding:0;
                    background:#f4f7f4;
                    font-family:Arial,Helvetica,sans-serif;
                    color:#222222;
                ">

                    <table width="100%%"
                           cellpadding="0"
                           cellspacing="0"
                           style="padding:30px 15px;">

                        <tr>
                            <td align="center">

                                <table width="100%%"
                                       cellpadding="0"
                                       cellspacing="0"
                                       style="
                                           max-width:600px;
                                           background:#ffffff;
                                           border-radius:14px;
                                           overflow:hidden;
                                           box-shadow:
                                           0 8px 28px rgba(0,0,0,0.08);
                                       ">

                                    <tr>
                                        <td style="
                                            background:#1f7a3f;
                                            color:#ffffff;
                                            padding:24px;
                                            text-align:center;
                                        ">
                                            <h1 style="
                                                margin:0;
                                                font-size:23px;
                                            ">
                                                %s
                                            </h1>

                                            <p style="
                                                margin:7px 0 0;
                                                font-size:14px;
                                            ">
                                                Connecting Farmers Directly
                                                to Buyers
                                            </p>
                                        </td>
                                    </tr>

                                    <tr>
                                        <td style="
                                            padding:30px;
                                            font-size:15px;
                                            line-height:1.6;
                                        ">
                                            %s
                                        </td>
                                    </tr>

                                    <tr>
                                        <td style="
                                            background:#f7faf7;
                                            color:#777777;
                                            padding:18px;
                                            text-align:center;
                                            font-size:12px;
                                        ">
                                            This is an automated email.
                                            Please do not reply.
                                        </td>
                                    </tr>

                                </table>

                            </td>
                        </tr>

                    </table>

                </body>
                </html>
                """.formatted(
                escapeHtml(applicationName),
                content
        );
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "₹0.00";
        }

        NumberFormat formatter =
                NumberFormat.getCurrencyInstance(
                        new Locale("en", "IN")
                );

        return formatter.format(amount);
    }

    private void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "Recipient email is required."
            );
        }
    }

    private String defaultName(String name) {
        if (name == null || name.isBlank()) {
            return "User";
        }

        return name.trim();
    }

    private String normalizeFrontendUrl(String url) {
        if (url == null || url.isBlank()) {
            return "http://localhost:5173";
        }

        String normalizedUrl = url.trim();

        while (normalizedUrl.endsWith("/")) {
            normalizedUrl = normalizedUrl.substring(
                    0,
                    normalizedUrl.length() - 1
            );
        }

        return normalizedUrl;
    }

    private String safeValue(String value) {
        if (value == null || value.isBlank()) {
            return "Not provided";
        }

        return value.trim();
    }

    /*
     * The methods below use reflection so this EmailService can compile
     * even if ContactMessageRequest has getter names such as:
     *
     * getName() / getFullName()
     * getEmail()
     * getSubject()
     * getMessage()
     *
     * It avoids hard dependency on one exact DTO field naming.
     */

    private String readContactName(ContactMessageRequest request) {
        return readStringProperty(
                request,
                "getName",
                "getFullName",
                "name",
                "fullName"
        );
    }

    private String readContactEmail(ContactMessageRequest request) {
        return readStringProperty(
                request,
                "getEmail",
                "email"
        );
    }

    private String readContactSubject(ContactMessageRequest request) {
        return readStringProperty(
                request,
                "getSubject",
                "subject"
        );
    }

    private String readContactMessage(ContactMessageRequest request) {
        return readStringProperty(
                request,
                "getMessage",
                "getContent",
                "message",
                "content"
        );
    }

    private String readStringProperty(
            Object object,
            String... methodNames
    ) {
        for (String methodName : methodNames) {
            try {
                Object result = object
                        .getClass()
                        .getMethod(methodName)
                        .invoke(object);

                if (result != null) {
                    return result.toString();
                }

            } catch (ReflectiveOperationException ignored) {
                // Try the next method name.
            }
        }

        return "Not provided";
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }

        int atIndex = email.indexOf("@");

        if (atIndex <= 2) {
            return "***" + email.substring(atIndex);
        }

        return email.substring(0, 2)
                + "***"
                + email.substring(atIndex);
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
