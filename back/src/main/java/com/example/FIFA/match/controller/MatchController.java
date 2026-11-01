package com.example.FIFA.match.controller;

import com.example.FIFA.match.dto.MatchScheduleRequest;
import com.example.FIFA.match.entity.Match;
import com.example.FIFA.match.service.MatchService;
import com.example.FIFA.security.user.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    @GetMapping("/tournament/{tournamentId}")
    public ResponseEntity<List<Match>> getGroupMatchesByTournament(@PathVariable UUID tournamentId) {
        return ResponseEntity.ok(matchService.getGroupMatchesByTournament(tournamentId));
    }

    @PostMapping("/tournament/{tournamentId}/group-stage/generate")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<List<Match>> generateGroupMatches(
            @PathVariable UUID tournamentId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(matchService.generateGroupMatches(tournamentId, currentUser));
    }

    @PatchMapping("/{matchId}/schedule")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<Match> scheduleMatch(
            @PathVariable UUID matchId,
            @RequestBody @Valid MatchScheduleRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(matchService.scheduleMatch(matchId, request, currentUser));
    }
}
