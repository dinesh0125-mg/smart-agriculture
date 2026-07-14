package com.smartagri.service;

import com.smartagri.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class OtpService {

    private static final int OTP_LENGTH = 6;

    private static final long PHONE_OTP_EXPIRY_SECONDS = 300;
    private static final long EMAIL_OTP_EXPIRY_SECONDS = 600;
    private static final long EMAIL_VERIFIED_WINDOW_SECONDS = 900;

    private static final int MAX_ATTEMPTS = 3;

    private final SecureRandom secureRandom = new SecureRandom();

    private final ConcurrentHashMap<String, OtpEntry> otpStore =
            new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, Instant> verifiedEmails =
            new ConcurrentHashMap<>();

    private record OtpEntry(
            String otp,
            Instant expiresAt,
            int attempts
    ) {

        OtpEntry incrementAttempts() {
            return new OtpEntry(
                    otp,
                    expiresAt,
                    attempts + 1
            );
        }
    }

    public String generatePhoneOtp(String phone) {

        String normalizedPhone = normalizePhone(phone);
        String otp = generateSecureOtp();

        otpStore.put(
                phoneKey(normalizedPhone),
                new OtpEntry(
                        otp,
                        Instant.now().plusSeconds(PHONE_OTP_EXPIRY_SECONDS),
                        0
                )
        );

        log.info("Phone OTP generated for {}", maskPhone(normalizedPhone));

        return otp;
    }

    public String generateEmailOtp(String email) {

        String normalizedEmail = normalizeEmail(email);
        String otp = generateSecureOtp();

        otpStore.put(
                emailKey(normalizedEmail),
                new OtpEntry(
                        otp,
                        Instant.now().plusSeconds(EMAIL_OTP_EXPIRY_SECONDS),
                        0
                )
        );

        log.info("Email OTP generated for {}", maskEmail(normalizedEmail));

        return otp;
    }

    public boolean verifyPhoneOtp(String phone, String otp) {

        String normalizedPhone = normalizePhone(phone);

        return verifyOtp(
                phoneKey(normalizedPhone),
                otp
        );
    }

    public boolean verifyEmailOtp(String email, String otp) {

        String normalizedEmail = normalizeEmail(email);

        boolean verified = verifyOtp(
                emailKey(normalizedEmail),
                otp
        );

        if (verified) {
            verifiedEmails.put(
                    normalizedEmail,
                    Instant.now().plusSeconds(
                            EMAIL_VERIFIED_WINDOW_SECONDS
                    )
            );

            log.info(
                    "Email verified successfully for {}",
                    maskEmail(normalizedEmail)
            );
        }

        return verified;
    }

    public boolean isEmailVerified(String email) {

        String normalizedEmail = normalizeEmail(email);

        Instant expiry = verifiedEmails.get(normalizedEmail);

        if (expiry == null) {
            return false;
        }

        if (expiry.isBefore(Instant.now())) {
            verifiedEmails.remove(normalizedEmail);
            return false;
        }

        return true;
    }

    public void consumeEmailVerification(String email) {

        String normalizedEmail = normalizeEmail(email);

        verifiedEmails.remove(normalizedEmail);

        log.info(
                "Email verification consumed for {}",
                maskEmail(normalizedEmail)
        );
    }

    public void removeEmailOtp(String email) {

        String normalizedEmail = normalizeEmail(email);

        otpStore.remove(emailKey(normalizedEmail));

        log.info(
                "Email OTP removed for {}",
                maskEmail(normalizedEmail)
        );
    }

    public void removePhoneOtp(String phone) {

        String normalizedPhone = normalizePhone(phone);

        otpStore.remove(phoneKey(normalizedPhone));

        log.info(
                "Phone OTP removed for {}",
                maskPhone(normalizedPhone)
        );
    }

    private boolean verifyOtp(String key, String submittedOtp) {

        validateSubmittedOtp(submittedOtp);

        OtpEntry entry = otpStore.get(key);

        if (entry == null) {
            throw new BadRequestException(
                    "No OTP was requested. Please request a new OTP."
            );
        }

        if (entry.expiresAt().isBefore(Instant.now())) {
            otpStore.remove(key);

            throw new BadRequestException(
                    "OTP has expired. Please request a new OTP."
            );
        }

        if (entry.attempts() >= MAX_ATTEMPTS) {
            otpStore.remove(key);

            throw new BadRequestException(
                    "Too many failed attempts. Please request a new OTP."
            );
        }

        String normalizedOtp = submittedOtp.trim();

        if (!entry.otp().equals(normalizedOtp)) {

            int updatedAttempts = entry.attempts() + 1;

            if (updatedAttempts >= MAX_ATTEMPTS) {
                otpStore.remove(key);

                throw new BadRequestException(
                        "Invalid OTP. Maximum attempts reached. " +
                        "Please request a new OTP."
                );
            }

            otpStore.put(
                    key,
                    entry.incrementAttempts()
            );

            int remainingAttempts =
                    MAX_ATTEMPTS - updatedAttempts;

            throw new BadRequestException(
                    "Invalid OTP. " +
                    remainingAttempts +
                    " attempt(s) remaining."
            );
        }

        otpStore.remove(key);

        return true;
    }

    private String generateSecureOtp() {

        int maximumValue = (int) Math.pow(10, OTP_LENGTH);

        int generatedNumber =
                secureRandom.nextInt(maximumValue);

        return String.format(
                "%0" + OTP_LENGTH + "d",
                generatedNumber
        );
    }

    private void validateSubmittedOtp(String otp) {

        if (otp == null || otp.isBlank()) {
            throw new BadRequestException(
                    "OTP is required."
            );
        }

        if (!otp.trim().matches("\\d{6}")) {
            throw new BadRequestException(
                    "OTP must contain exactly 6 digits."
            );
        }
    }

    private String normalizeEmail(String email) {

        if (email == null || email.isBlank()) {
            throw new BadRequestException(
                    "Email address is required."
            );
        }

        String normalizedEmail =
                email.trim().toLowerCase(Locale.ROOT);

        if (!normalizedEmail.matches(
                "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
        )) {
            throw new BadRequestException(
                    "Please provide a valid email address."
            );
        }

        return normalizedEmail;
    }

    private String normalizePhone(String phone) {

        if (phone == null || phone.isBlank()) {
            throw new BadRequestException(
                    "Phone number is required."
            );
        }

        String normalizedPhone =
                phone.replaceAll("[^0-9+]", "");

        if (!normalizedPhone.matches("\\+?[0-9]{10,15}")) {
            throw new BadRequestException(
                    "Please provide a valid phone number."
            );
        }

        return normalizedPhone;
    }

    private String emailKey(String email) {
        return "email:" + email;
    }

    private String phoneKey(String phone) {
        return "phone:" + phone;
    }

    private String maskEmail(String email) {

        int atIndex = email.indexOf("@");

        if (atIndex <= 2) {
            return "***" + email.substring(atIndex);
        }

        return email.substring(0, 2)
                + "***"
                + email.substring(atIndex);
    }

    private String maskPhone(String phone) {

        if (phone.length() <= 4) {
            return "****";
        }

        return "******"
                + phone.substring(phone.length() - 4);
    }

    @Scheduled(fixedRate = 300000)
    public void cleanExpiredEntries() {

        Instant now = Instant.now();

        int otpCountBefore = otpStore.size();
        int verifiedCountBefore = verifiedEmails.size();

        otpStore.entrySet().removeIf(
                entry -> entry.getValue()
                        .expiresAt()
                        .isBefore(now)
        );

        verifiedEmails.entrySet().removeIf(
                entry -> entry.getValue().isBefore(now)
        );

        int removedOtpCount =
                otpCountBefore - otpStore.size();

        int removedVerifiedCount =
                verifiedCountBefore - verifiedEmails.size();

        if (removedOtpCount > 0 ||
                removedVerifiedCount > 0) {

            log.info(
                    "Expired OTP cleanup completed. " +
                    "OTP entries removed: {}, " +
                    "verified email entries removed: {}",
                    removedOtpCount,
                    removedVerifiedCount
            );
        }
    }
}
