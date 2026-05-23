/**
 * Spring Data repository for {@link cephadex.brainflex.model.deck.DeckAnalytics}.
 *
 * The document id is the deck id (1:1 relationship), so the default
 * {@code findById(deckId)} / {@code save} methods are all the analytics
 * service needs — no custom finders.
 */
package cephadex.brainflex.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import cephadex.brainflex.model.deck.DeckAnalytics;
public interface DeckAnalyticsRepository extends MongoRepository<DeckAnalytics, String> {
}
