/**
 * Coarse classification used for provider routing and legal/operational gating.
 *
 * The dispatcher consults this when deciding whether to honour the suppression
 * list: TRANSACTIONAL and SYSTEM sends always go through (you can't opt out of
 * your own password reset), while MARKETING is suppression-filtered and must
 * carry an unsubscribe link. Provider mapping (`Map<EmailCategory, EmailProvider>`)
 * lives in the dispatcher so adding a marketing-only provider later is config,
 * not a code change at the call site.
 */
package cephadex.brainflex.service.email;

public enum EmailCategory {
    TRANSACTIONAL,
    SYSTEM,
    MARKETING
}
