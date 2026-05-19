/**
 * Read-time hydrator that rewrites the legacy {@code Deck.tags} array as a
 * snapshot of the display names referenced by {@code Deck.tagIds}.
 *
 * Pre-migration decks carry meaningful {@code tags} and an empty
 * {@code tagIds}; those are left untouched so legacy free-form labels keep
 * rendering. Post-migration decks have {@code tagIds} populated, and we
 * batch-fetch the Tag documents to produce a fresh display-name list every
 * read. This avoids stale labels when an admin renames a tag.
 *
 * Mutates the Deck instance in place — DTO construction happens after.
 */
package cephadex.brainflex.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import cephadex.brainflex.model.Deck;
import cephadex.brainflex.model.Tag;
import cephadex.brainflex.repository.TagRepository;

@Service
public class DeckTagHydrationService {

    private final TagRepository tagRepository;

    public DeckTagHydrationService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    public void hydrate(Deck deck) {
        if (deck == null) return;
        Set<String> referenced = collect(List.of(deck));
        if (referenced.isEmpty()) return;
        Map<String, String> displayNames = fetchDisplayNames(referenced);
        applyDisplayNames(deck, displayNames);
    }

    /**
     * Batched hydration for list endpoints. One Mongo round-trip resolves
     * every tag id touched by the supplied decks.
     */
    public void hydrate(Collection<Deck> decks) {
        if (decks == null || decks.isEmpty()) return;
        Set<String> referenced = collect(decks);
        if (referenced.isEmpty()) return;
        Map<String, String> displayNames = fetchDisplayNames(referenced);
        for (Deck deck : decks) {
            applyDisplayNames(deck, displayNames);
        }
    }

    private static Set<String> collect(Collection<Deck> decks) {
        Set<String> ids = new HashSet<>();
        for (Deck deck : decks) {
            if (deck == null) continue;
            List<String> tagIds = deck.getTagIds();
            if (tagIds == null) continue;
            for (String id : tagIds) {
                if (id != null && !id.isBlank()) ids.add(id);
            }
        }
        return ids;
    }

    private Map<String, String> fetchDisplayNames(Set<String> ids) {
        Map<String, String> result = new HashMap<>();
        for (Tag tag : tagRepository.findAllById(ids)) {
            result.put(tag.getId(), tag.getDisplayName());
        }
        return result;
    }

    /**
     * Only overrides the legacy {@code tags} list when this deck has a
     * non-empty {@code tagIds}. Pre-migration decks keep their original
     * free-form strings until the migration script runs.
     */
    private static void applyDisplayNames(Deck deck, Map<String, String> displayNames) {
        List<String> tagIds = deck.getTagIds();
        if (tagIds == null || tagIds.isEmpty()) return;
        List<String> snapshot = new ArrayList<>(tagIds.size());
        for (String id : tagIds) {
            String name = displayNames.get(id);
            snapshot.add(name != null ? name : id);
        }
        deck.setTags(snapshot);
    }
}
