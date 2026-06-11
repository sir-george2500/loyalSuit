package com.loyalsuit.common.security;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Locale;

/**
 * RFC 6238 time-based one-time passwords (TOTP), the algorithm behind Google Authenticator /
 * Authy. Hand-rolled (HMAC-SHA1, 6 digits, 30s step) to avoid a dependency; verification
 * tolerates ±1 step of clock skew. Secrets are Base32 (RFC 4648), as authenticator apps expect.
 */
@Component
public class TotpService {

    private static final String BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final int DIGITS = 6;
    private static final int PERIOD_SECONDS = 30;
    private static final int SKEW_STEPS = 1;
    private static final int SECRET_BYTES = 20;

    private final SecureRandom random = new SecureRandom();

    /** A fresh random secret, Base32-encoded, ready to hand to an authenticator app. */
    public String generateSecret() {
        byte[] buffer = new byte[SECRET_BYTES];
        random.nextBytes(buffer);
        return base32Encode(buffer);
    }

    /** True if the code matches the current step or an adjacent one (skew tolerance). */
    public boolean verify(String base32Secret, String code) {
        if (base32Secret == null || code == null || !code.matches("\\d{" + DIGITS + "}")) {
            return false;
        }
        byte[] key = base32Decode(base32Secret);
        long step = System.currentTimeMillis() / 1000L / PERIOD_SECONDS;
        for (int offset = -SKEW_STEPS; offset <= SKEW_STEPS; offset++) {
            if (codeAt(key, step + offset).equals(code)) {
                return true;
            }
        }
        return false;
    }

    /** The code for the current time step — what an authenticator app would show right now. */
    public String currentCode(String base32Secret) {
        long step = System.currentTimeMillis() / 1000L / PERIOD_SECONDS;
        return codeAt(base32Decode(base32Secret), step);
    }

    /** The otpauth:// URI an authenticator app reads from a QR code. */
    public String otpauthUri(String issuer, String accountLabel, String base32Secret) {
        String label = URLEncoder.encode(issuer + ":" + accountLabel, StandardCharsets.UTF_8);
        String enc = URLEncoder.encode(issuer, StandardCharsets.UTF_8);
        return "otpauth://totp/%s?secret=%s&issuer=%s&algorithm=SHA1&digits=%d&period=%d"
                .formatted(label, base32Secret, enc, DIGITS, PERIOD_SECONDS);
    }

    private String codeAt(byte[] key, long step) {
        try {
            byte[] counter = ByteBuffer.allocate(Long.BYTES).putLong(step).array();
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(counter);
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);
            int otp = binary % (int) Math.pow(10, DIGITS);
            return String.format(Locale.ROOT, "%0" + DIGITS + "d", otp);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to compute TOTP", e);
        }
    }

    private static String base32Encode(byte[] data) {
        StringBuilder out = new StringBuilder();
        int buffer = 0;
        int bits = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bits += 8;
            while (bits >= 5) {
                bits -= 5;
                out.append(BASE32.charAt((buffer >> bits) & 0x1F));
            }
        }
        if (bits > 0) {
            out.append(BASE32.charAt((buffer << (5 - bits)) & 0x1F));
        }
        return out.toString();
    }

    private static byte[] base32Decode(String secret) {
        String clean = secret.trim().replace("=", "").toUpperCase(Locale.ROOT);
        ByteBuffer out = ByteBuffer.allocate(clean.length() * 5 / 8);
        int buffer = 0;
        int bits = 0;
        for (char c : clean.toCharArray()) {
            int val = BASE32.indexOf(c);
            if (val < 0) {
                throw new IllegalArgumentException("Invalid Base32 character");
            }
            buffer = (buffer << 5) | val;
            bits += 5;
            if (bits >= 8) {
                bits -= 8;
                out.put((byte) ((buffer >> bits) & 0xFF));
            }
        }
        byte[] result = new byte[out.position()];
        out.flip();
        out.get(result);
        return result;
    }
}
