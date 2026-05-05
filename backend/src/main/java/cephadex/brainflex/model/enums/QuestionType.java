/**
 * Format of an individual question within a content pack.
 * IMAGE_CHOICE and TEXT_INPUT are reserved for future question types;
 * MULTIPLE_CHOICE is the only type implemented in phase 1.
 */
package cephadex.brainflex.model.enums;

public enum QuestionType {
    MULTIPLE_CHOICE,
    IMAGE_CHOICE,
    TEXT_INPUT
}
