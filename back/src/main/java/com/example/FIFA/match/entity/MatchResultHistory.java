package com.example.FIFA.match.entity;

import com.example.FIFA.match.enums.MatchResultActionType;
import com.example.FIFA.match.enums.MatchStatus;
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
import jakarta.persistence.PrePersist;
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
    name = "match_result_histories",
    indexes = {
        @Index(name = "idx_mrh_match", columnList = "match_id"),
        @Index(name = "idx_mrh_changed_by", columnList = "changed_by_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class MatchResultHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @Column(name = "previous_score_player1")
    private Integer previousScorePlayer1;

    @Column(name = "previous_score_player2")
    private Integer previousScorePlayer2;

    @Column(name = "new_score_player1")
    private Integer newScorePlayer1;

    @Column(name = "new_score_player2")
    private Integer newScorePlayer2;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 25)
    private MatchStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", length = 25)
    private MatchStatus newStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 30)
    private MatchResultActionType actionType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "changed_by_id", nullable = false)
    private User changedBy;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @PrePersist
    protected void onCreate() {
        if (this.changedAt == null) {
            this.changedAt = LocalDateTime.now();
        }
        if (this.actionType == null) {
            this.actionType = MatchResultActionType.SUBMISSION;
        }
    }
}
