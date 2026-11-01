package com.example.FIFA.match.controller;

import com.example.FIFA.match.dto.MatchForfeitRequest;
import com.example.FIFA.match.dto.MatchResultDisputeRequest;
import com.example.FIFA.match.dto.MatchResultResolutionRequest;
import com.example.FIFA.match.dto.MatchResultSubmissionRequest;
import com.example.FIFA.match.dto.MatchResponse;
import com.example.FIFA.match.dto.MatchResultHistoryResponse;
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
@RequestMapping("/api")
@RequiredArgsConstructor
public class MatchResultController {

    private final MatchService matchService;

    @PostMapping("/tournaments/{tournamentId}/matches/{matchId}/result")
    @PreAuthorize("hasAnyRole('PLAYER', 'ORGANIZER', 'ADMIN')")
    public ResponseEntity<MatchResponse> submitMatchResult(
            @PathVariable UUID tournamentId,
            @PathVariable UUID matchId,
            @RequestBody @Valid MatchResultSubmissionRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(matchService.submitMatchResult(tournamentId, matchId, request, currentUser));
    }

    @PatchMapping("/tournaments/{tournamentId}/matches/{matchId}/result/confirm")
    @PreAuthorize("hasAnyRole('PLAYER', 'ORGANIZER', 'ADMIN')")
    public ResponseEntity<MatchResponse> confirmMatchResult(
            @PathVariable UUID tournamentId,
            @PathVariable UUID matchId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(matchService.confirmMatchResult(tournamentId, matchId, currentUser));
    }

    @PatchMapping("/tournaments/{tournamentId}/matches/{matchId}/result/dispute")
    @PreAuthorize("hasAnyRole('PLAYER', 'ORGANIZER', 'ADMIN')")
    public ResponseEntity<MatchResponse> disputeMatchResult(
            @PathVariable UUID tournamentId,
            @PathVariable UUID matchId,
            @RequestBody @Valid MatchResultDisputeRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(matchService.disputeMatchResult(tournamentId, matchId, request, currentUser));
    }

    @PatchMapping("/tournaments/{tournamentId}/matches/{matchId}/result/dispute/resolve")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<MatchResponse> resolveMatchDispute(
            @PathVariable UUID tournamentId,
            @PathVariable UUID matchId,
            @RequestBody @Valid MatchResultResolutionRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(matchService.resolveMatchDispute(tournamentId, matchId, request, currentUser));
    }

    @PostMapping("/tournaments/{tournamentId}/matches/{matchId}/forfeit")
    @PreAuthorize("hasAnyRole('PLAYER', 'ORGANIZER', 'ADMIN')")
    public ResponseEntity<MatchResponse> submitForfeit(
            @PathVariable UUID tournamentId,
            @PathVariable UUID matchId,
            @RequestBody @Valid MatchForfeitRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(matchService.submitForfeit(tournamentId, matchId, request, currentUser));
    }

    @GetMapping("/tournaments/{tournamentId}/matches/{matchId}/result/history")
    @PreAuthorize("hasAnyRole('PLAYER', 'ORGANIZER', 'ADMIN')")
    public ResponseEntity<List<MatchResultHistoryResponse>> getMatchResultHistory(
            @PathVariable UUID tournamentId,
            @PathVariable UUID matchId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(matchService.getMatchResultHistory(tournamentId, matchId, currentUser));
    }
}
