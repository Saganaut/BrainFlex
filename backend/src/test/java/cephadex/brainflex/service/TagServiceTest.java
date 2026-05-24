/**
 * Unit tests for TagService. Repository is mocked so no MongoDB instance is
 * required. Covers slug derivation, validation, parent-existence checks,
 * delete protection, and recompute.
 */
package cephadex.brainflex.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import cephadex.brainflex.dto.deck.CreateTagRequest;
import cephadex.brainflex.dto.deck.UpdateTagRequest;
import cephadex.brainflex.model.deck.Deck;
import cephadex.brainflex.model.deck.Tag;
import cephadex.brainflex.repository.TagRepository;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock
    private TagRepository tagRepository;

    @InjectMocks
    private TagService tagService;

    private Tag math;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        math = new Tag();
        math.setId("math");
        math.setDisplayName("Math");
        math.setCurated(true);
    }

    // ---- slugify ----

    @Test
    void slugify_LowercasesAndHyphenates() {
        assertEquals("history-ww2", TagService.slugify("History WW2"));
        assertEquals("pop-culture", TagService.slugify("Pop  Culture!"));
        assertEquals("general-knowledge", TagService.slugify("General/Knowledge"));
    }

    @Test
    void slugify_StripsLeadingAndTrailingHyphens() {
        assertEquals("trivia", TagService.slugify("--trivia--"));
        assertEquals("trivia", TagService.slugify("???trivia???"));
    }

    // ---- create ----

    @Test
    void create_WhenIdMissing_SlugifiesDisplayName() {
        when(tagRepository.existsById("history-ww2")).thenReturn(false);
        when(tagRepository.save(any(Tag.class))).thenAnswer(inv -> inv.getArgument(0));

        Tag created = tagService.create(new CreateTagRequest(
                null, "History WW2", null, null, null, true));

        assertEquals("history-ww2", created.getId());
        assertEquals("History WW2", created.getDisplayName());
        assertTrue(created.isCurated());
    }

    @Test
    void create_WhenIdExplicit_UsesProvidedSlug() {
        when(tagRepository.existsById("custom-id")).thenReturn(false);
        when(tagRepository.save(any(Tag.class))).thenAnswer(inv -> inv.getArgument(0));

        Tag created = tagService.create(new CreateTagRequest(
                "custom-id", "Anything", null, null, null, false));

        assertEquals("custom-id", created.getId());
    }

    @Test
    void create_WhenIdAlreadyExists_ThrowsConflict() {
        when(tagRepository.existsById("math")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> tagService.create(new CreateTagRequest(
                        "math", "Math", null, null, null, true)));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void create_WhenSlugInvalid_ThrowsBadRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> tagService.create(new CreateTagRequest(
                        "Bad ID!", "Whatever", null, null, null, false)));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void create_WhenParentMissing_ThrowsBadRequest() {
        when(tagRepository.existsById("history-ww2")).thenReturn(false);
        when(tagRepository.existsById("history")).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> tagService.create(new CreateTagRequest(
                        null, "History WW2", "history", null, null, false)));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    // ---- update ----

    @Test
    void update_PatchesDisplayNameAndCurated() {
        when(tagRepository.findById("math")).thenReturn(Optional.of(math));
        when(tagRepository.save(any(Tag.class))).thenAnswer(inv -> inv.getArgument(0));

        Tag updated = tagService.update("math", new UpdateTagRequest(
                "Mathematics", null, "Numbers and shapes", null, false));

        assertEquals("Mathematics", updated.getDisplayName());
        assertEquals("Numbers and shapes", updated.getDescription());
        assertEquals(false, updated.isCurated());
    }

    @Test
    void update_WhenParentIsSelf_ThrowsBadRequest() {
        when(tagRepository.findById("math")).thenReturn(Optional.of(math));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> tagService.update("math", new UpdateTagRequest(
                        null, "math", null, null, null)));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void update_WhenParentBlank_ClearsParent() {
        math.setParentTagId("history");
        when(tagRepository.findById("math")).thenReturn(Optional.of(math));
        when(tagRepository.save(any(Tag.class))).thenAnswer(inv -> inv.getArgument(0));

        Tag updated = tagService.update("math", new UpdateTagRequest(
                null, "", null, null, null));

        assertNull(updated.getParentTagId());
    }

    // ---- delete ----

    @Test
    void delete_WhenDeckCountZero_DeletesTag() {
        math.setDeckCount(0);
        when(tagRepository.findById("math")).thenReturn(Optional.of(math));
        when(tagRepository.findByParentTagId("math")).thenReturn(List.of());

        tagService.delete("math");

        verify(tagRepository).delete(math);
    }

    @Test
    void delete_WhenDecksReferenceIt_ThrowsConflict() {
        math.setDeckCount(3);
        when(tagRepository.findById("math")).thenReturn(Optional.of(math));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> tagService.delete("math"));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(tagRepository, never()).delete(any(Tag.class));
    }

    @Test
    void delete_WhenChildrenExist_ThrowsConflict() {
        math.setDeckCount(0);
        Tag child = new Tag();
        child.setId("math-algebra");
        when(tagRepository.findById("math")).thenReturn(Optional.of(math));
        when(tagRepository.findByParentTagId("math")).thenReturn(List.of(child));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> tagService.delete("math"));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(tagRepository, never()).delete(any(Tag.class));
    }

    // ---- requireAllExist ----

    @Test
    void requireAllExist_WhenAllPresent_Passes() {
        Tag a = new Tag();
        a.setId("a");
        Tag b = new Tag();
        b.setId("b");
        when(tagRepository.findAllById(List.of("a", "b"))).thenReturn(List.of(a, b));

        tagService.requireAllExist(List.of("a", "b"));
    }

    @Test
    void requireAllExist_WhenMissing_ThrowsBadRequest() {
        Tag a = new Tag();
        a.setId("a");
        when(tagRepository.findAllById(List.of("a", "missing"))).thenReturn(List.of(a));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> tagService.requireAllExist(List.of("a", "missing")));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("missing"));
    }

    @Test
    void requireAllExist_WhenEmpty_NoLookup() {
        tagService.requireAllExist(List.of());
        tagService.requireAllExist(null);
        verify(tagRepository, never()).findAllById(any(Iterable.class));
    }

    // ---- recompute ----

    @Test
    void recomputeDeckCounts_UpdatesChangedTagsOnly() {
        Tag a = new Tag();
        a.setId("a");
        a.setDeckCount(5);
        Tag b = new Tag();
        b.setId("b");
        b.setDeckCount(0);
        Tag c = new Tag();
        c.setId("c");
        c.setDeckCount(2);
        when(tagRepository.findAll()).thenReturn(List.of(a, b, c));
        when(tagRepository.save(any(Tag.class))).thenAnswer(inv -> inv.getArgument(0));

        Deck d1 = new Deck();
        d1.setTagIds(Set.of("a", "b"));
        Deck d2 = new Deck();
        d2.setTagIds(Set.of("a"));
        Deck d3 = new Deck();
        d3.setTagIds(Set.of("b"));

        int changed = tagService.recomputeDeckCounts(List.of(d1, d2, d3));

        assertEquals(3, changed);
        assertEquals(2, a.getDeckCount());
        assertEquals(2, b.getDeckCount());
        assertEquals(0, c.getDeckCount());
    }

    // ---- findOrCreateFromLegacyTag ----

    @Test
    void findOrCreateFromLegacyTag_WhenSlugExists_ReturnsExisting() {
        Tag history = new Tag();
        history.setId("history");
        history.setDisplayName("History");
        when(tagRepository.findById("history")).thenReturn(Optional.of(history));

        Tag found = tagService.findOrCreateFromLegacyTag("History");

        assertEquals(history, found);
        verify(tagRepository, never()).save(any(Tag.class));
    }

    @Test
    void findOrCreateFromLegacyTag_WhenNew_CreatesUncurated() {
        when(tagRepository.findById("welcome")).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenAnswer(inv -> inv.getArgument(0));

        Tag created = tagService.findOrCreateFromLegacyTag("welcome");

        assertEquals("welcome", created.getId());
        assertEquals("welcome", created.getDisplayName());
        assertEquals(false, created.isCurated());
    }

    @Test
    void findOrCreateFromLegacyTag_WhenBlank_ReturnsNull() {
        assertNull(tagService.findOrCreateFromLegacyTag(""));
        assertNull(tagService.findOrCreateFromLegacyTag(null));
    }
}
