/**
 * Persistent store for interactiveSession chat messages. The hot read is the chat panel
 * pagination ("latest N messages, newest first"); host moderation does a
 * lookup-by-id then flips the moderated flag.
 */
package cephadex.brainflex.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import cephadex.brainflex.model.InteractiveSessionChatMessage;

public interface InteractiveSessionChatMessageRepository extends MongoRepository<InteractiveSessionChatMessage, String> {

    Page<InteractiveSessionChatMessage> findAllByInteractiveSessionIdOrderBySentAtDesc(
            String interactiveSessionId, Pageable pageable);

    long countByInteractiveSessionId(String interactiveSessionId);

    void deleteByInteractiveSessionId(String interactiveSessionId);
}
