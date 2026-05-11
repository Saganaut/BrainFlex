/**
 * How to score a PlaceOnImageQuestion answer (or future PlaceOnMap variants).
 *   BINARY — full points if within tolerance, zero outside
 *   LINEAR — points scale linearly from full (dead-center) to zero (at tolerance edge)
 */
package cephadex.brainflex.model.enums;

public enum PlaceScoring {
    BINARY,
    LINEAR
}
