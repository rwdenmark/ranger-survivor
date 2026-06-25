package com.rangersurvivor.dto;

import com.rangersurvivor.model.Score;

import java.time.Instant;

/**
 * Outbound score representation. Keeps the response shape decoupled from the
 * entity so adding an internal column doesn't silently change the API.
 */
public record ScoreView(
        Long id,
        String name,
        int kills,
        int durationSeconds,
        Instant submittedAt
) {
    public static ScoreView from(Score score) {
        return new ScoreView(
                score.getId(),
                score.getName(),
                score.getKills(),
                score.getDurationSeconds(),
                score.getSubmittedAt()
        );
    }
}
