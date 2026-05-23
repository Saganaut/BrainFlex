/**
 * Authored grouping of decks. The Kahoot "folder" / Mentimeter "Series" analog
 * — an ordered list of deck ids the owner can curate independently of each
 * deck's own visibility. A deck can appear in many collections; collections
 * themselves are owned by a single user and optionally org-shared.
 *
 * The list of deck ids is stored inline on the collection document because
 * reads and reorders always work on the entire ordered list — there is no
 * use case for paging through it. Reorder is therefore a single document
 * write, not a join-row shuffle. Missing deck ids (deck deleted, deck moved
 * out of caller's visibility) are filtered out at read time rather than
 * trigger-deleted; the {@link DeckCollection} stays the source of truth.
 */
package cephadex.brainflex.model.deck;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import cephadex.brainflex.model.image.Image;
import cephadex.brainflex.model.enums.DeckVisibility;
import lombok.Data;
import cephadex.brainflex.model.shared.Auditable;

@Data
@Document(collection = "deck_collections")
// Public browsing of collections sorts newest-first within a visibility
// bucket, mirroring the deck explore feed.
@CompoundIndex(name = "collection_explore_idx", def = "{'visibility': 1, 'updatedAt': -1}")
public class DeckCollection extends Auditable {

    @Id
    private String id;

    @Indexed
    private String ownerUserId;

    @Indexed
    private String organizationId;

    private String name;
    private String description;

    private Image cover;

    private List<String> deckIds = new ArrayList<>();

    private DeckVisibility visibility = DeckVisibility.PRIVATE;

    private int viewCount;
}
