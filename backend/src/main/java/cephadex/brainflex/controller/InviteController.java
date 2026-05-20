/**
 * Single endpoint for invitees to redeem the token from their email. Public —
 * the token itself is the credential; we mark the row redeemed and (if logged
 * in) link it to the resolved user id. If the parent ScheduledInteractiveSession
 * hasn't booted yet, `roomCode` in the response is null and the frontend renders
 * a waiting-room page that polls.
 */
package cephadex.brainflex.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cephadex.brainflex.dto.RedeemInviteResponse;
import cephadex.brainflex.service.ScheduledInteractiveSessionService;
import cephadex.brainflex.service.UserService;

@RestController
@RequestMapping("/api/invites")
public class InviteController {

    private final ScheduledInteractiveSessionService scheduleService;
    private final UserService userService;

    public InviteController(ScheduledInteractiveSessionService scheduleService,
                            UserService userService) {
        this.scheduleService = scheduleService;
        this.userService = userService;
    }

    @PostMapping("/{token}/redeem")
    public ResponseEntity<RedeemInviteResponse> redeemInvite(@PathVariable String token,
                                                             Authentication authentication) {
        String userId = userService.resolveAnyAuthenticatedUser(authentication)
                .map(u -> u.getId())
                .orElse(null);
        return ResponseEntity.ok(scheduleService.redeem(token, userId));
    }
}
