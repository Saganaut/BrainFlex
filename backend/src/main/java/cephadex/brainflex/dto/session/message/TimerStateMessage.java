/**
 * Broadcast on /topic/interactive-session/{roomCode}/timerState when the host
 * pauses or resumes the round countdown (chunk 25).
 *
 * On pause: {@code paused=true} and {@code remainingMillis} carries the time
 * left so clients can freeze their countdown at the right value. On resume:
 * {@code paused=false}, {@code remainingMillis=null}, and {@code roundStartedAt}
 * is the freshly-shifted origin clients should recompute their countdown from
 * (the server rewinds roundStartedAt by the elapsed-before-pause so the
 * remaining window is preserved).
 */
package cephadex.brainflex.dto.session.message;

import java.time.Instant;

public record TimerStateMessage(int round, boolean paused, Long remainingMillis, Instant roundStartedAt) {
}
