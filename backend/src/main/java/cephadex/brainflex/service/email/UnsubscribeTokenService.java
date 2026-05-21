/**
 * Mints and verifies opaque tokens for the {@code /api/public/email/unsubscribe/{token}}
 * endpoint. A token is a self-contained {@code email|category|hmac} triplet
 * encoded url-safe — no server-side row, no extra Mongo lookup, the HMAC is
 * what proves the link came from us.
 *
 * Why HMAC instead of a random token + Mongo lookup: marketing emails go out
 * in large batches, and we'd rather not write N rows per blast. The token is
 * still single-purpose (carries the email + category it was minted for) so a
 * leaked link can only unsubscribe the address it was issued to, from the
 * category it was issued for.
 *
 * The secret comes from {@code brainflex.email.unsubscribe-secret}; if unset
 * the bean refuses to start so we don't silently mint tokens with an empty
 * secret in prod. Local dev uses a default supplied via application.properties.
 */
package cephadex.brainflex.service.email;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class UnsubscribeTokenService {

    private static final String HMAC_ALG = "HmacSHA256";
    private static final String SEP = "|";

    private final byte[] secret;

    public UnsubscribeTokenService(
            @Value("${brainflex.email.unsubscribe-secret:dev-unsubscribe-secret-change-me}") String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("brainflex.email.unsubscribe-secret must be set");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    /** Build a token that, when handed to {@link #verify(String)}, recovers the same (email, category). */
    public String mint(String email, EmailCategory category) {
        String payload = normalise(email) + SEP + category.name();
        String sig = sign(payload);
        String full = payload + SEP + sig;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(full.getBytes(StandardCharsets.UTF_8));
    }

    /** Returns the {@link Decoded} pair iff the token was minted by us, else empty. */
    public Optional<Decoded> verify(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        String decoded;
        try {
            decoded = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        String[] parts = decoded.split("\\" + SEP);
        if (parts.length != 3) return Optional.empty();
        String payload = parts[0] + SEP + parts[1];
        String expected = sign(payload);
        if (!constantTimeEquals(expected, parts[2])) return Optional.empty();
        EmailCategory category;
        try {
            category = EmailCategory.valueOf(parts[1]);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        return Optional.of(new Decoded(parts[0], category));
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALG);
            mac.init(new SecretKeySpec(secret, HMAC_ALG));
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        } catch (Exception e) {
            // HmacSHA256 is mandatory in every JRE; this is unreachable.
            throw new IllegalStateException("HMAC init failed", e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        byte[] ba = a.getBytes(StandardCharsets.UTF_8);
        byte[] bb = b.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(ba, bb);
    }

    private static String normalise(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public record Decoded(String email, EmailCategory category) {}
}
