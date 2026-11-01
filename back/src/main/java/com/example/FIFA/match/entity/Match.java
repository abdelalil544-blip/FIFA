package com.example.FIFA.match.entity;

import com.example.FIFA.match.enums.MatchResultSource;
import com.example.FIFA.match.enums.MatchStage;
import com.example.FIFA.match.enums.MatchStatus;
import com.example.FIFA.match.enums.NextMatchSlot;
import com.example.FIFA.player.entity.Player;
import com.example.FIFA.tournament.entity.Tournament;
import com.example.FIFA.tournamentgroup.entity.TournamentGroup;
import com.example.FIFA.user.entity.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "matches",
    indexes = {
        @Index(name = "idx_matches_tournament", columnList = "tournament_id"),
        @Index(name = "idx_matches_group", columnList = "group_id"),
        @Index(name = "idx_matches_status", columnList = "status"),
        @Index(name = "idx_matches_stage", columnList = "stage"),
        @Index(name = "idx_matches_player1", columnList = "player1_id"),
        @Index(name = "idx_matches_player2", columnList = "player2_id"),
        @Index(name = "idx_matches_winner", columnList = "winner_id"),
        @Index(name = "idx_matches_next_match", columnList = "next_match_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private TournamentGroup group;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false, length = 20)
    private MatchStage stage;

    @Column(name = "round_number", nullable = false)
    private Integer roundNumber;

    @Column(name = "match_number", nullable = false)
    private Integer matchNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player1_id")
    private Player player1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player2_id")
    private Player player2;

    @Column(name = "score_player1")
    private Integer scorePlayer1;

    @Column(name = "score_player2")
    private Integer scorePlayer2;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    private Player winner;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 25)
    private MatchStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by_id")
    private User submittedBy;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by_id")
    private User confirmedBy;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disputed_by_id")
    private User disputedBy;

    @Column(name = "disputed_at")
    private LocalDateTime disputedAt;

    @Column(name = "dispute_reason", length = 500)
    private String disputeReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_source", length = 25)
    private MatchResultSource resultSource;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "played_at")
    private LocalDateTime playedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "next_match_id")
    private Match nextMatch;

    @Enumerated(EnumType.STRING)
    @Column(name = "next_match_slot", length = 10)
    private NextMatchSlot nextMatchSlot;
}
