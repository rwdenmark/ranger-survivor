package com.rangersurvivor.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Inbound score submission. Separate from the {@link com.rangersurvivor.model.Score}
 * entity so the wire contract and the persistence model can change independently,
 * and so the client can never set id or submittedAt.
 *
 * Absolute caps here are a first gate. The plausibility check in the controller
 * (kills must be reachable in the elapsed time) is the second.
 */
public record ScoreSubmission(
        @NotBlank @Size(max = 24) String name,
        @Min(0) @Max(100_000) int kills,
        @Min(0) @Max(86_400) int durationSeconds
) {
}
