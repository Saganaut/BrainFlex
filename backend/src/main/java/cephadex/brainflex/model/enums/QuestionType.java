/**
 * Format of an individual question within a content pack.
 * IMAGE_CHOICE and TEXT_INPUT are reserved for future question types;
 * MULTIPLE_CHOICE is the only type implemented in phase 1.
 * TEXT_INPUT the user has to to type in some text
 * NUMBER_INPUT same as text input but they type in a number
 * SCALES sliding scales (1 or more) where users can give feedback (not suitable for game)
* RANKING organize a bunch of elements in order
* Q_AND_A users are asked to post their questions, admin/presenter can then address them or transform them into actual questions on the go (not suitable for game)
* GRID users are presented with an x y grid and place a point  

*/
package cephadex.brainflex.model.enums;

public enum QuestionType {
    MULTIPLE_CHOICE,
    IMAGE_CHOICE,
    TEXT_INPUT,
    SCALES,
    RANKING,
    Q_AND_A,
    NUMBER_INPUT,
    GRID,
    PLACE_ON_IMAGE,
}
