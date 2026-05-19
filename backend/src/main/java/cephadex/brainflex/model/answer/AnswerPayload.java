/**
 * Sealed polymorphic answer body, one shape per question kind.
 *
 * `kind` mirrors ElementKind for routing the response to the right scorer.
 * TIMEOUT is the sentinel for "didn't submit before the round ended".
 *
 * Stored on PlayerAnswer.payload; serialized for REST via the `kind` discriminator.
 */
package cephadex.brainflex.model.answer;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "kind")
@JsonSubTypes({
        @JsonSubTypes.Type(value = McqAnswer.class),
        @JsonSubTypes.Type(value = TextAnswer.class),
        @JsonSubTypes.Type(value = NumberAnswer.class),
        @JsonSubTypes.Type(value = RankingAnswer.class),
        @JsonSubTypes.Type(value = ScalesAnswer.class),
        @JsonSubTypes.Type(value = GridAnswer.class),
        @JsonSubTypes.Type(value = PlaceOnImageAnswer.class),
        @JsonSubTypes.Type(value = WordCloudAnswer.class),
        @JsonSubTypes.Type(value = AllocationAnswer.class),
        @JsonSubTypes.Type(value = MatchingAnswer.class),
        @JsonSubTypes.Type(value = DrawingAnswer.class),
        @JsonSubTypes.Type(value = TimeoutAnswer.class)
})
public sealed interface AnswerPayload
        permits McqAnswer, TextAnswer, NumberAnswer,
                RankingAnswer, ScalesAnswer, GridAnswer, PlaceOnImageAnswer,
                WordCloudAnswer, AllocationAnswer, MatchingAnswer, DrawingAnswer, TimeoutAnswer {
}
