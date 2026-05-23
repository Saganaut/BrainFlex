/**
 * Curated taxonomy node used to categorize decks (and, later, individual
 * elements). Tags form a shallow tree via {@link #parentTagId}: roots act as
 * "Subjects" (Math, History) and children act as sub-topics
 * (history-ww2, math-algebra).
 *
 * {@link #id} is the slug — stable, lowercase, hyphenated — so URLs and
 * deduplication work without an opaque ObjectId.
 *
 * {@link #deckCount} is denormalized: it's recomputed periodically by the
 * seeder and incrementally on deck save. Reads must tolerate it being stale
 * by a few seconds.
 */
package cephadex.brainflex.model.deck;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;
import cephadex.brainflex.model.shared.Auditable;

@Data
@Document(collection = "tags")
@CompoundIndex(name = "explore_default_idx", def = "{'curated': -1, 'deckCount': -1}")
public class Tag extends Auditable {

    @Id
    private String id;

    private String displayName;

    @Indexed
    private String parentTagId;

    private String description;
    private String iconUrl;

    private int deckCount;

    @Indexed
    private boolean curated;

    /**
     * Chunk 21 — user who first created this tag. Stamped on the user-inline-
     * create path so we can attribute non-curated tags and (later) GC ones
     * that fall out of use. Null for legacy/curated rows authored before this
     * field existed and for system seeds.
     */
    @Indexed
    private String createdByUserId;
}
