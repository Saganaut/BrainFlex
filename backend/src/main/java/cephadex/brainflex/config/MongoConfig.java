/**
 * Mongo customizations. Currently registers read converters that bridge
 * pre-rename enum values in existing documents to their renamed equivalents
 * so legacy data deserializes cleanly without a one-shot migration.
 *
 * Why a converter (and not just `valueOf`): Spring Data MongoDB uses the
 * default enum reader (`Enum.valueOf`) which throws on unknown names.
 * Documents persisted before chunk 24 may carry {@code recommendedPreset:
 * "PULSE"}; the read converter rewrites that to {@code PRESENTATION} (a
 * Pulse-shaped deck is just an unscored presentation). The next save writes
 * the renamed value back, so this is a soft migration.
 */
package cephadex.brainflex.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import cephadex.brainflex.model.enums.SessionFormat;

// Mongo auditing is enabled once, on BrainflexApplication (see Auditable's
// javadoc). Declaring @EnableMongoAuditing here too registered a second
// `mongoAuditingHandler` bean, which fails context startup under Spring Boot's
// default no-bean-override policy — so it intentionally lives only there.
@Configuration
public class MongoConfig {

    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(List.of(new SessionFormatReadingConverter()));
    }

    /**
     * Reads {@code Deck.recommendedPreset} / {@code Deck.defaultSessionFormat}
     * etc. Translates legacy {@code PULSE} → {@code PRESENTATION}; everything
     * else falls back to {@link SessionFormat#valueOf}.
     */
    @ReadingConverter
    static class SessionFormatReadingConverter implements Converter<String, SessionFormat> {
        @Override
        public SessionFormat convert(String source) {
            if (source == null) {
                return null;
            }
            return SessionFormat.fromValue(source);
        }
    }
}
