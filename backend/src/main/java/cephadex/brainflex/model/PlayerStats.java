package cephadex.brainflex.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlayerStats {
    private int gamesPlayed = 0;
    private int highScore = 0;
    private int totalPoints = 0;
    private int currentStreak = 0;
}