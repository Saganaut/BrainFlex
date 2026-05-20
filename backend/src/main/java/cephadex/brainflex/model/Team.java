/**
 * A team within a InteractiveSession running in team mode. Embedded in InteractiveSession.teams
 * so teams travel with the session document and dissolve when it ends — they
 * are not promoted to a top-level collection.
 *
 * `score` and `memberCount` are denormalized aggregates. They're recomputed
 * server-side on every answer and on every membership change so clients can
 * render leaderboards and lobby counts without joining InteractiveSessionPlayer.
 *
 * `color` is one of the oklch palette token names ("red", "orange", "yellow",
 * "green", "teal", "blue", "violet", "pink") so the frontend can map a team
 * directly onto its swatch via tokens.css.
 */
package cephadex.brainflex.model;

import lombok.Data;

@Data
public class Team {
    private String id;
    private String name;
    private String color;
    private String captainUserId;
    private int score;
    private int memberCount;
}
