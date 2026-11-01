package com.example.FIFA.tournamentgroup.controller;

import com.example.FIFA.security.user.CustomUserDetails;
import com.example.FIFA.tournamentgroup.dto.TournamentGroupResponse;
import com.example.FIFA.tournamentgroup.service.TournamentGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tournament-groups")
@RequiredArgsConstructor
public class TournamentGroupController {

    private final TournamentGroupService tournamentGroupService;

    @GetMapping("/tournament/{tournamentId}")
    public ResponseEntity<List<TournamentGroupResponse>> getGroupsByTournament(@PathVariable UUID tournamentId) {
        return ResponseEntity.ok(tournamentGroupService.getGroupsByTournament(tournamentId));
    }

    @PostMapping("/tournament/{tournamentId}/generate")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<List<TournamentGroupResponse>> generateGroups(
            @PathVariable UUID tournamentId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(tournamentGroupService.generateGroups(tournamentId, currentUser));
    }
}
