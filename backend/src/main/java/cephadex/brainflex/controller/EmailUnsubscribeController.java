/**
 * Public endpoint that lets a recipient unsubscribe from a category via the
 * link rendered into marketing email footers. The token is the credential;
 * the endpoint is mounted under {@code /api/public/} so {@code SecurityConfig}
 * already permits it without further allow-listing.
 *
 * Verb choice: GET works because the link is the only credential and the
 * action is the user's own (no CSRF surface to protect — there's nothing the
 * caller could trick the user into clicking that the link itself didn't
 * already grant). The frontend renders a follow-up page after the GET
 * confirming the change.
 */
package cephadex.brainflex.controller;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cephadex.brainflex.model.EmailSuppression;
import cephadex.brainflex.service.email.EmailSuppressionService;
import cephadex.brainflex.service.email.UnsubscribeTokenService;
import cephadex.brainflex.service.email.UnsubscribeTokenService.Decoded;

@RestController
@RequestMapping("/api/public/email")
public class EmailUnsubscribeController {

    private final EmailSuppressionService suppression;
    private final UnsubscribeTokenService tokens;

    public EmailUnsubscribeController(EmailSuppressionService suppression,
                                      UnsubscribeTokenService tokens) {
        this.suppression = suppression;
        this.tokens = tokens;
    }

    @PostMapping("/unsubscribe/{token}")
    public ResponseEntity<UnsubscribeResponse> unsubscribe(@PathVariable String token) {
        return resolve(token);
    }

    /** GET form so the marketing footer link works straight from the inbox. */
    @GetMapping("/unsubscribe/{token}")
    public ResponseEntity<UnsubscribeResponse> unsubscribeGet(@PathVariable String token) {
        return resolve(token);
    }

    private ResponseEntity<UnsubscribeResponse> resolve(String token) {
        Optional<Decoded> decoded = tokens.verify(token);
        if (decoded.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        Decoded d = decoded.get();
        suppression.suppress(d.email(), d.category(), EmailSuppression.Reason.UNSUBSCRIBE);
        return ResponseEntity.ok(new UnsubscribeResponse(d.email(), d.category().name()));
    }

    public record UnsubscribeResponse(String email, String category) {}
}
