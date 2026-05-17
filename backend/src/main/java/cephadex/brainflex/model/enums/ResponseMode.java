/**
 * Whether an element is currently collecting answers. The host can flip an
 * element to NOT_ACCEPTING_RESPONSES to freeze the answer set mid-round —
 * the runtime then ignores incoming submissions for that element.
 */
package cephadex.brainflex.model.enums;

public enum ResponseMode {
    ACCEPTING_RESPONSES,
    NOT_ACCEPTING_RESPONSES
}
