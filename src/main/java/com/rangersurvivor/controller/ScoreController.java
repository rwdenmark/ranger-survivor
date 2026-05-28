package com.rangersurvivor.controller;

import com.rangersurvivor.model.Score;
import com.rangersurvivor.repository.ScoreRepository;
import com.rangersurvivor.service.ProfanityFilter;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/scores")
public class ScoreController {

    private final ScoreRepository scoreRepository;
    private final ProfanityFilter profanityFilter;

    public ScoreController(ScoreRepository scoreRepository, ProfanityFilter profanityFilter) {
        this.scoreRepository = scoreRepository;
        this.profanityFilter = profanityFilter;
    }

    @PostMapping
    public ResponseEntity<?> submit(@Valid @RequestBody Score score) {
        if (profanityFilter.isProfane(score.getName())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Name not allowed"));
        }
        score.setSubmittedAt(Instant.now());
        Score saved = scoreRepository.save(score);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/top")
    public List<Score> top(@RequestParam(defaultValue = "10") int limit) {
        int capped = Math.min(Math.max(limit, 1), 100);
        return scoreRepository.findAllByOrderByKillsDescDurationSecondsAsc(PageRequest.of(0, capped));
    }

    @GetMapping("/me")
    public List<Score> mine(@RequestParam String name,
                            @RequestParam(defaultValue = "10") int limit) {
        int capped = Math.min(Math.max(limit, 1), 100);
        return scoreRepository.findAllByNameIgnoreCaseOrderBySubmittedAtDesc(name, PageRequest.of(0, capped));
    }
}
