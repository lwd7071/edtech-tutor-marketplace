package com.edtech.platform.ranking.facade.impl;

import com.edtech.platform.ranking.domain.TeacherStats;
import com.edtech.platform.ranking.repository.TeacherStatsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeacherStatsFacadeImplTest {

    @Mock
    private TeacherStatsRepository teacherStatsRepository;

    @InjectMocks
    private TeacherStatsFacadeImpl teacherStatsFacade;

    private TeacherStats statA;
    private TeacherStats statB;
    private TeacherStats statC;

    @BeforeEach
    void setUp() {
        // A: highest Bayesian rating
        statA = TeacherStats.builder()
                .teacherId(UUID.randomUUID())
                .bayesianRating(new BigDecimal("4.8"))
                .completedSessionCount(100)
                .completionRate(new BigDecimal("0.95"))
                .build();

        // B: medium rating
        statB = TeacherStats.builder()
                .teacherId(UUID.randomUUID())
                .bayesianRating(new BigDecimal("4.2"))
                .completedSessionCount(50)
                .completionRate(new BigDecimal("0.80"))
                .build();

        // C: lowest rating
        statC = TeacherStats.builder()
                .teacherId(UUID.randomUUID())
                .bayesianRating(new BigDecimal("3.5"))
                .completedSessionCount(20)
                .completionRate(new BigDecimal("0.70"))
                .build();
    }

    // ── Slice 1: ranks are assigned in correct order ──────────────────────────

    @Test
    void updateAllGlobalRanks_assignsRank1ToHighestBayesian() {
        when(teacherStatsRepository.findAll()).thenReturn(List.of(statB, statC, statA)); // shuffled

        teacherStatsFacade.updateAllGlobalRanks();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TeacherStats>> captor = ArgumentCaptor.forClass(List.class);
        verify(teacherStatsRepository).saveAll(captor.capture());

        List<TeacherStats> saved = captor.getValue();
        // Find rank of each
        TeacherStats rank1 = saved.stream().filter(s -> s.getGlobalRank() != null && s.getGlobalRank() == 1).findFirst().orElseThrow();
        TeacherStats rank3 = saved.stream().filter(s -> s.getGlobalRank() != null && s.getGlobalRank() == 3).findFirst().orElseThrow();

        assertEquals(statA.getTeacherId(), rank1.getTeacherId());
        assertEquals(statC.getTeacherId(), rank3.getTeacherId());
    }

    // ── Slice 2: empty list → no error ────────────────────────────────────────

    @Test
    void updateAllGlobalRanks_handlesEmptyList() {
        when(teacherStatsRepository.findAll()).thenReturn(List.of());

        assertDoesNotThrow(() -> teacherStatsFacade.updateAllGlobalRanks());
        verify(teacherStatsRepository).saveAll(List.of());
    }

    // ── Slice 3: tie in Bayesian → higher session count wins ─────────────────

    @Test
    void updateAllGlobalRanks_tieBreaksBySessionCount() {
        TeacherStats tie1 = TeacherStats.builder()
                .teacherId(UUID.randomUUID())
                .bayesianRating(new BigDecimal("4.5"))
                .completedSessionCount(200)
                .completionRate(new BigDecimal("0.90"))
                .build();

        TeacherStats tie2 = TeacherStats.builder()
                .teacherId(UUID.randomUUID())
                .bayesianRating(new BigDecimal("4.5"))
                .completedSessionCount(50)
                .completionRate(new BigDecimal("0.90"))
                .build();

        when(teacherStatsRepository.findAll()).thenReturn(List.of(tie2, tie1));

        teacherStatsFacade.updateAllGlobalRanks();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TeacherStats>> captor = ArgumentCaptor.forClass(List.class);
        verify(teacherStatsRepository).saveAll(captor.capture());

        List<TeacherStats> saved = captor.getValue();
        TeacherStats rank1 = saved.stream().filter(s -> s.getGlobalRank() != null && s.getGlobalRank() == 1).findFirst().orElseThrow();
        assertEquals(tie1.getTeacherId(), rank1.getTeacherId()); // More sessions → rank 1
    }
}
