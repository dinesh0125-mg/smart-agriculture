package com.smartagri.service;

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
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String mailFrom;

    @Value("${app.name:Smart Agriculture Marketplace}")
    private String applicationName;

    public void sendVerificationCode(
            String recipientEmail,
            String otp
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

            helper.setSubject(
                    applicationName + " - Email Verification Code"
            );

            helper.setText(
                    buildVerificationEmailBody(otp),
                    true
            );

            mailSender.send(mimeMessage);

            log.info(
                    "Verification email sent successfully to {}",
                    maskEmail(recipientEmail)
            );

        } catch (MailAuthenticationException exception) {

            log.error(
                    "Gmail SMTP authentication failed. " +
                    "Check MAIL_USERNAME and MAIL_PASSWORD.",
                    exception
            );

            throw new IllegalStateException(
                    "Email service authentication failed. " +
                    "Please contact the administrator."
            );

        } catch (MailSendException exception) {

            log.error(
                    "SMTP server failed to send verification email to {}",
                    maskEmail(recipientEmail),
                    exception
            );

            throw new IllegalStateException(
                    "Unable to send verification email. " +
                    "Please try again later."
            );

        } catch (MessagingException | MailException exception) {

            log.error(
                    "Failed to send verification email to {}. Reason: {}",
                    maskEmail(recipientEmail),
                    exception.getMessage(),
                    exception
            );

            throw new IllegalStateException(
                    "Failed to send verification code. " +
                    "Please try again."
            );

        } catch (Exception exception) {

            log.error(
                    "Unexpected error while sending verification email to {}",
                    maskEmail(recipientEmail),
                    exception
            );

            throw new IllegalStateException(
                    "Unexpected email service error."
            );
        }
    }

    private String buildVerificationEmailBody(String otp) {

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
                ">

                <table width="100%"
                       cellpadding="0"
                       cellspacing="0"
                       style="padding:30px 15px;">

                    <tr>
                        <td align="center">

                            <table width="100%"
                                   cellpadding="0"
                                   cellspacing="0"
                                   style="
                                       max-width:560px;
                                       background:#ffffff;
                                       border-radius:14px;
                                       overflow:hidden;
                                       box-shadow:
                                       0 8px 30px rgba(0,0,0,0.08);
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
                                            font-size:24px;
                                        ">
                                            Smart Agriculture Marketplace
                                        </h1>

                                        <p style="
                                            margin:8px 0 0;
                                            font-size:14px;
                                        ">
                                            Connecting Farmers Directly to Buyers
                                        </p>
                                    </td>
                                </tr>

                                <tr>
                                    <td style="
                                        padding:32px;
                                        color:#222222;
                                    ">

                                        <h2 style="
                                            margin-top:0;
                                            font-size:21px;
                                        ">
                                            Verify your email address
                                        </h2>

                                        <p style="
                                            font-size:15px;
                                            line-height:1.6;
                                        ">
                                            Use the verification code below
                                            to complete your registration.
                                        </p>

                                        <div style="
                                            margin:28px 0;
                                            text-align:center;
                                        ">

                                            <span style="
                                                display:inline-block;
                                                background:#eff8f1;
                                                color:#1f7a3f;
                                                font-size:32px;
                                                font-weight:bold;
                                                letter-spacing:8px;
                                                padding:16px 24px;
                                                border-radius:10px;
                                                border:1px solid #cfe7d5;
                                            ">
                                                %s
                                            </span>

                                        </div>

                                        <p style="
                                            font-size:14px;
                                            line-height:1.6;
                                            color:#555555;
                                        ">
                                            This verification code is valid
                                            for 10 minutes.
                                        </p>

                                        <p style="
                                            font-size:14px;
                                            line-height:1.6;
                                            color:#555555;
                                        ">
                                            Do not share this code with anyone.
                                            If you did not request this code,
                                            you can ignore this email.
                                        </p>

                                    </td>
                                </tr>

                                <tr>
                                    <td style="
                                        background:#f7faf7;
                                        padding:18px;
                                        text-align:center;
                                        color:#777777;
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
                """.formatted(otp);
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
}
