/**
 * Business logic for the curated tag taxonomy.
 *
 * Tag ids are kebab-case slugs derived from the displayName when the admin
 * doesn't supply one explicitly. Slugs are stable: once a tag exists you
 * update its {@code displayName} but its id never changes (deck references
 * would otherwise dangle).
 *
 * {@link #recomputeDeckCounts(java.util.List)} walks every deck and rewrites
 * the denormalized {@code deckCount} on each tag. Called from the seeder
 * after a tag refresh and exposed so admins can re-sync if drift sneaks in.
 *
 * Delete refuses any tag with {@code deckCount > 0}; the admin must move
 * decks off the tag first. Roots with children also stay protected: the
 * frontend would need to display empty branches otherwise.
 */
package cephadex.brainflex.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import cephadex.brainflex.dto.CreateTagRequest;
import cephadex.brainflex.dto.UpdateTagRequest;
import cephadex.brainflex.model.deck.Deck;
import cephadex.brainflex.model.deck.Tag;
import cephadex.brainflex.repository.TagRepository;

@Service
public class TagService {

    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");

    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    // ---- Read ----

    public List<Tag> listAll() {
        return tagRepository.findAll();
    }

    public List<Tag> listCurated() {
        return tagRepository.findByCurated(true);
    }

    public List<Tag> listByParent(String parentTagId) {
        return tagRepository.findByParentTagId(parentTagId);
    }

    /** Chunk 21 — backs the "tags I've created" filter on listTags. */
    public List<Tag> listByCreator(String createdByUserId) {
        return tagRepository.findByCreatedByUserId(createdByUserId);
    }

    public List<Tag> search(String text) {
        if (text == null || text.isBlank())
            return List.of();
        return tagRepository.searchByText(Pattern.quote(text.trim()));
    }

    public Tag get(String id) {
        return tagRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag not found"));
    }

    public List<Tag> children(String parentTagId) {
        return tagRepository.findByParentTagId(parentTagId);
    }

    // ---- Write ----

    public Tag create(CreateTagRequest request) {
        return create(request, null);
    }

    /**
     * Chunk 21 — stamps {@code createdByUserId} on the new tag so the
     * inline-create path can be attributed back to the author. Pass null
     * for system seeds; admin-created tags are stamped too so we can later
     * surface "tags I created" on an admin dashboard.
     */
    public Tag create(CreateTagRequest request, String createdByUserId) {
        String slug = (request.id() != null && !request.id().isBlank())
                ? request.id().trim()
                : slugify(request.displayName());
        validateSlug(slug);
        if (tagRepository.existsById(slug)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tag id already exists: " + slug);
        }
        if (request.parentTagId() != null && !request.parentTagId().isBlank()
                && !tagRepository.existsById(request.parentTagId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Parent tag does not exist: " + request.parentTagId());
        }

        Tag tag = new Tag();
        tag.setId(slug);
        tag.setDisplayName(request.displayName().trim());
        tag.setParentTagId(blankToNull(request.parentTagId()));
        tag.setDescription(request.description());
        tag.setIconUrl(request.iconUrl());
        tag.setCurated(Boolean.TRUE.equals(request.curated()));
        tag.setCreatedByUserId(createdByUserId);
        tag.setDeckCount(0);
        return tagRepository.save(tag);
    }

    public Tag update(String id, UpdateTagRequest request) {
        Tag tag = get(id);
        if (request.displayName() != null && !request.displayName().isBlank()) {
            tag.setDisplayName(request.displayName().trim());
        }
        if (request.parentTagId() != null) {
            String parent = blankToNull(request.parentTagId());
            if (parent != null) {
                if (parent.equals(id)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A tag cannot be its own parent");
                }
                if (!tagRepository.existsById(parent)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Parent tag does not exist: " + parent);
                }
            }
            tag.setParentTagId(parent);
        }
        if (request.description() != null) {
            tag.setDescription(request.description());
        }
        if (request.iconUrl() != null) {
            tag.setIconUrl(request.iconUrl());
        }
        if (request.curated() != null) {
            tag.setCurated(request.curated());
        }
        return tagRepository.save(tag);
    }

    public void delete(String id) {
        Tag tag = get(id);
        if (tag.getDeckCount() > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot delete a tag in use by " + tag.getDeckCount() + " deck(s)");
        }
        if (!tagRepository.findByParentTagId(id).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot delete a tag that still has child tags");
        }
        tagRepository.delete(tag);
    }

    // ---- Helpers exposed to other services ----

    /**
     * Validates that every id in {@code tagIds} maps to an existing tag.
     * Throws 400 on the first missing id. Empty / null lists are allowed.
     */
    public void requireAllExist(List<String> tagIds) {
        if (tagIds == null || tagIds.isEmpty())
            return;
        List<String> distinct = tagIds.stream().filter(s -> s != null && !s.isBlank()).distinct().toList();
        if (distinct.isEmpty())
            return;
        List<Tag> found = tagRepository.findAllById(distinct);
        if (found.size() == distinct.size())
            return;
        Map<String, Boolean> byId = new HashMap<>();
        for (Tag t : found)
            byId.put(t.getId(), true);
        for (String id : distinct) {
            if (!byId.containsKey(id)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown tag id: " + id);
            }
        }
    }

    /**
     * Walks every supplied deck and recomputes each tag's {@code deckCount}.
     * Tags not referenced by any deck land at 0. Called from the seeder so a
     * fresh boot lines up with current data, and exposed for explicit
     * resync requests. Returns the number of tags whose count changed.
     */
    public int recomputeDeckCounts(List<Deck> allDecks) {
        Map<String, Integer> counts = new HashMap<>();
        for (Deck deck : allDecks) {
            java.util.Set<String> ids = deck.getTagIds();
            if (ids == null)
                continue;
            for (String id : ids) {
                if (id == null || id.isBlank())
                    continue;
                counts.merge(id, 1, Integer::sum);
            }
        }
        int changed = 0;
        for (Tag tag : tagRepository.findAll()) {
            int next = counts.getOrDefault(tag.getId(), 0);
            if (tag.getDeckCount() != next) {
                tag.setDeckCount(next);
                tagRepository.save(tag);
                changed++;
            }
        }
        return changed;
    }

    /** Lowercase, hyphen-separated slug used as the Tag id. */
    public static String slugify(String text) {
        if (text == null)
            return "";
        String lowered = text.toLowerCase();
        String collapsed = lowered.replaceAll("[^a-z0-9]+", "-");
        String trimmed = collapsed.replaceAll("(^-)|(-$)", "");
        return trimmed;
    }

    private static void validateSlug(String slug) {
        if (slug == null || slug.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tag id cannot be empty");
        }
        if (!SLUG_PATTERN.matcher(slug).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Tag id must be kebab-case: lowercase letters, digits, single hyphens");
        }
    }

    /** Returns null for null / blank, otherwise the trimmed value. */
    private static String blankToNull(String value) {
        if (value == null)
            return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** Find-or-create used by the one-shot legacy migration. */
    public Tag findOrCreateFromLegacyTag(String legacyTag) {
        String slug = slugify(legacyTag);
        if (slug.isEmpty())
            return null;
        Optional<Tag> existing = tagRepository.findById(slug);
        if (existing.isPresent())
            return existing.get();
        Tag tag = new Tag();
        tag.setId(slug);
        tag.setDisplayName(legacyTag.trim());
        tag.setCurated(false);
        tag.setDeckCount(0);
        return tagRepository.save(tag);
    }
}
