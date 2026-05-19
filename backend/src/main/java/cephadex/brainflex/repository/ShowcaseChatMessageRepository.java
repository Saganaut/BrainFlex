/**
 * Persistent store for showcase chat messages. The hot read is the chat panel
 * pagination ("latest N messages, newest first"); host moderation does a
 * lookup-by-id then flips the moderated flag.
 */
package cephadex.brainflex.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import cephadex.brainflex.model.ShowcaseChatMessage;

public interface ShowcaseChatMessageRepository extends MongoRepository<ShowcaseChatMessage, String> {

    Page<ShowcaseChatMessage> findAllByShowcaseIdOrderBySentAtDesc(
            String showcaseId, Pageable pageable);

    void deleteByShowcaseId(String showcaseId);
}
