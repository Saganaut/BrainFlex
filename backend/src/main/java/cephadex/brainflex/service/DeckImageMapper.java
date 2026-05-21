/**
 * Single source of truth for "which fields on a deck hold an Image". Every
 * code path that needs to transform or visit images (read-time hydration,
 * write-time normalization, future migrations) routes through here so adding
 * a new image slot anywhere in the schema is a one-place change.
 *
 * `map(Deck, UnaryOperator<Image>)` returns a structurally-equal Deck with
 * every Image replaced by `op.apply(image)`. Null images are skipped (the op
 * is never called with null). `forEach` is the read-only equivalent.
 *
 * Chunk 25 — the chrome's {@code background} / {@code image} slots are
 * rebuilt through {@link ElementChrome#withImages(Image, Image)}, so each
 * switch arm only deals with the kind-specific image slots (option images,
 * grid backing image, etc.).
 */
package cephadex.brainflex.service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import cephadex.brainflex.model.Deck;
import cephadex.brainflex.model.element.AllocationQuestion;
import cephadex.brainflex.model.element.DeckElement;
import cephadex.brainflex.model.element.DrawingQuestion;
import cephadex.brainflex.model.element.ElementChrome;
import cephadex.brainflex.model.element.GridCellsConfig;
import cephadex.brainflex.model.element.GridQuestion;
import cephadex.brainflex.model.element.Image;
import cephadex.brainflex.model.element.ImageBlock;
import cephadex.brainflex.model.element.MatchingPair;
import cephadex.brainflex.model.element.MatchingQuestion;
import cephadex.brainflex.model.element.McqOption;
import cephadex.brainflex.model.element.McqQuestion;
import cephadex.brainflex.model.element.NumberQuestion;
import cephadex.brainflex.model.element.PlaceOnImageQuestion;
import cephadex.brainflex.model.element.QAndAQuestion;
import cephadex.brainflex.model.element.RankingItem;
import cephadex.brainflex.model.element.RankingQuestion;
import cephadex.brainflex.model.element.ScalesQuestion;
import cephadex.brainflex.model.element.Slide;
import cephadex.brainflex.model.element.SlideBlock;
import cephadex.brainflex.model.element.TextQuestion;
import cephadex.brainflex.model.element.WordCloudQuestion;

public final class DeckImageMapper {

    private DeckImageMapper() {}

    /** Replace every Image on `deck` (cover, background, every element's image
     *  slots) with `op.apply(image)`. Mutates `deck` in place because callers
     *  (hydration, the controller layer) work against the Deck the repository
     *  returned and want the transformation to land on that instance. */
    public static void map(Deck deck, UnaryOperator<Image> op) {
        if (deck == null) return;
        deck.setCover(applyNullable(deck.getCover(), op));
        deck.setBackground(applyNullable(deck.getBackground(), op));
        List<DeckElement> elements = deck.getElements();
        if (elements == null) return;
        List<DeckElement> mapped = new ArrayList<>(elements.size());
        for (DeckElement element : elements) {
            mapped.add(mapElement(element, op));
        }
        deck.setElements(mapped);
    }

    /** Replace every Image on `element` (including nested McqOption.image,
     *  RankingItem.image, GridCellsConfig.backingImage, PlaceOnImage.targetImage,
     *  plus shared chrome image + background) with `op.apply(image)`. */
    public static DeckElement mapElement(DeckElement element, UnaryOperator<Image> op) {
        ElementChrome chrome = element.chrome().withImages(
                applyNullable(element.chrome().background(), op),
                applyNullable(element.chrome().image(), op));
        return switch (element) {
            case Slide s -> new Slide(
                    s.id(), s.slideKind(), s.body(), mapBlocks(s.blocks(), op),
                    s.resultsDisplayType(), s.multipleSelectionsEnabled(),
                    s.selectionsPerParticipant(), s.showResultsAsPercentage(),
                    s.joinType(), s.showJoinInformation(), s.showQrCode(), s.showResponses(),
                    s.heading(), s.participantInformation(),
                    s.autoAdvanceSeconds(),
                    chrome);
            case McqQuestion q -> new McqQuestion(
                    q.id(), q.prompt(), mapOptions(q.options(), op), q.correctOptionIds(),
                    q.pointValue(), q.difficulty(), q.explanation(),
                    q.shuffleOptions(), q.allowMultipleSelect(), q.maxSelections(),
                    chrome);
            case TextQuestion q -> new TextQuestion(
                    q.id(), q.prompt(), q.correctAnswer(), q.acceptedVariants(), q.caseSensitive(),
                    q.pointValue(), q.difficulty(), q.explanation(),
                    q.maxLength(), q.trimWhitespace(), q.fuzzyMatch(), q.fuzzyDistance(),
                    chrome);
            case NumberQuestion q -> new NumberQuestion(
                    q.id(), q.prompt(), q.correctValue(), q.tolerance(), q.unitLabel(), q.decimalPlaces(),
                    q.pointValue(), q.difficulty(), q.explanation(),
                    q.minValue(), q.maxValue(), q.allowNegative(),
                    chrome);
            case RankingQuestion q -> new RankingQuestion(
                    q.id(), q.prompt(), mapItems(q.items(), op), q.correctOrder(), q.scoring(),
                    q.pointValue(), q.difficulty(), q.explanation(),
                    q.shuffleItemsForPresentation(),
                    chrome);
            case ScalesQuestion q -> new ScalesQuestion(
                    q.id(), q.prompt(), q.statements(),
                    q.scaleMin(), q.scaleMax(), q.minLabel(), q.maxLabel(), q.correctRatings(),
                    q.pointValue(), q.difficulty(), q.explanation(),
                    chrome);
            case QAndAQuestion q -> new QAndAQuestion(
                    q.id(), q.prompt(), q.maxSubmissionsPerPlayer(), q.allowVoting(), q.autoApprove(),
                    q.pointValue(), q.difficulty(), q.explanation(),
                    q.anonymousSubmissions(), q.minVotesToShow(),
                    chrome);
            case GridQuestion q -> new GridQuestion(
                    q.id(), q.prompt(), q.rows(), q.cols(), mapCells(q.cells(), op),
                    q.correctCellIndexes(), q.multipleCorrect(),
                    q.pointValue(), q.difficulty(), q.explanation(),
                    chrome);
            case PlaceOnImageQuestion q -> new PlaceOnImageQuestion(
                    q.id(), q.prompt(), applyNullable(q.targetImage(), op),
                    q.correctX(), q.correctY(), q.tolerance(), q.scoring(),
                    q.pointValue(), q.difficulty(), q.explanation(),
                    chrome);
            case WordCloudQuestion q -> new WordCloudQuestion(
                    q.id(), q.prompt(), q.maxSubmissionsPerPlayer(), q.maxWordLength(),
                    q.caseSensitive(), q.profanityFilter(), q.bannedWords(),
                    q.pointValue(), q.difficulty(), q.explanation(),
                    chrome);
            case AllocationQuestion q -> new AllocationQuestion(
                    q.id(), q.prompt(), mapOptions(q.options(), op),
                    q.totalPointsToDistribute(), q.allowZeroOnItem(), q.enforceExactTotal(),
                    q.pointValue(), q.difficulty(), q.explanation(),
                    chrome);
            case MatchingQuestion q -> new MatchingQuestion(
                    q.id(), q.prompt(), mapPairs(q.pairs(), op), q.scoring(),
                    q.pointValue(), q.difficulty(), q.explanation(),
                    chrome);
            case DrawingQuestion q -> new DrawingQuestion(
                    q.id(), q.prompt(), applyNullable(q.backingImage(), op),
                    q.canvasWidth(), q.canvasHeight(),
                    q.maxStrokesPerPlayer(), q.maxPointsPerStroke(), q.palette(),
                    q.pointValue(), q.difficulty(), q.explanation(),
                    chrome);
        };
    }

    /** Visit every Image on `deck` without mutating anything. Nulls skipped. */
    public static void forEach(Deck deck, Consumer<Image> visitor) {
        if (deck == null) return;
        visitNullable(deck.getCover(), visitor);
        visitNullable(deck.getBackground(), visitor);
        List<DeckElement> elements = deck.getElements();
        if (elements == null) return;
        for (DeckElement element : elements) {
            forEachOnElement(element, visitor);
        }
    }

    /** Visit every Image on a single element. */
    public static void forEachOnElement(DeckElement element, Consumer<Image> visitor) {
        visitNullable(element.image(), visitor);
        visitNullable(element.background(), visitor);
        switch (element) {
            case Slide s -> {
                if (s.blocks() != null) {
                    for (SlideBlock block : s.blocks()) {
                        if (block instanceof ImageBlock ib) visitNullable(ib.image(), visitor);
                    }
                }
            }
            case McqQuestion q -> {
                if (q.options() != null) {
                    for (McqOption opt : q.options()) visitNullable(opt.image(), visitor);
                }
            }
            case RankingQuestion q -> {
                if (q.items() != null) {
                    for (RankingItem item : q.items()) visitNullable(item.image(), visitor);
                }
            }
            case GridQuestion q -> {
                if (q.cells() != null) visitNullable(q.cells().backingImage(), visitor);
            }
            case PlaceOnImageQuestion q -> visitNullable(q.targetImage(), visitor);
            case AllocationQuestion q -> {
                if (q.options() != null) {
                    for (McqOption opt : q.options()) visitNullable(opt.image(), visitor);
                }
            }
            case MatchingQuestion q -> {
                if (q.pairs() != null) {
                    for (MatchingPair pair : q.pairs()) {
                        visitNullable(pair.leftImage(), visitor);
                        visitNullable(pair.rightImage(), visitor);
                    }
                }
            }
            case DrawingQuestion q -> visitNullable(q.backingImage(), visitor);
            default -> { /* nothing extra */ }
        }
    }

    private static List<McqOption> mapOptions(List<McqOption> options, UnaryOperator<Image> op) {
        if (options == null) return null;
        List<McqOption> mapped = new ArrayList<>(options.size());
        for (McqOption opt : options) {
            mapped.add(opt.withImage(applyNullable(opt.image(), op)));
        }
        return mapped;
    }

    private static List<RankingItem> mapItems(List<RankingItem> items, UnaryOperator<Image> op) {
        if (items == null) return null;
        List<RankingItem> mapped = new ArrayList<>(items.size());
        for (RankingItem item : items) {
            mapped.add(item.withImage(applyNullable(item.image(), op)));
        }
        return mapped;
    }

    private static List<MatchingPair> mapPairs(List<MatchingPair> pairs, UnaryOperator<Image> op) {
        if (pairs == null) return null;
        List<MatchingPair> mapped = new ArrayList<>(pairs.size());
        for (MatchingPair pair : pairs) {
            mapped.add(new MatchingPair(
                    pair.id(), pair.leftLabel(), pair.rightLabel(),
                    applyNullable(pair.leftImage(), op),
                    applyNullable(pair.rightImage(), op)));
        }
        return mapped;
    }

    private static GridCellsConfig mapCells(GridCellsConfig cells, UnaryOperator<Image> op) {
        if (cells == null) return null;
        return cells.withBackingImage(applyNullable(cells.backingImage(), op));
    }

    /** Re-emit each SlideBlock with its embedded image (if any) transformed.
     *  Non-ImageBlock kinds pass through unchanged. */
    private static List<SlideBlock> mapBlocks(List<SlideBlock> blocks, UnaryOperator<Image> op) {
        if (blocks == null) return null;
        List<SlideBlock> mapped = new ArrayList<>(blocks.size());
        for (SlideBlock block : blocks) {
            if (block instanceof ImageBlock ib) {
                mapped.add(ib.withImage(applyNullable(ib.image(), op)));
            } else {
                mapped.add(block);
            }
        }
        return mapped;
    }

    private static Image applyNullable(Image image, UnaryOperator<Image> op) {
        return image == null ? null : op.apply(image);
    }

    private static void visitNullable(Image image, Consumer<Image> visitor) {
        if (image != null) visitor.accept(image);
    }
}
