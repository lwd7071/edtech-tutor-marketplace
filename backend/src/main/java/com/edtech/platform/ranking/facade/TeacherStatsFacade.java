package com.edtech.platform.ranking.facade;

import java.util.UUID;

public interface TeacherStatsFacade {
    void recalculateTeacherStats(UUID teacherId);
    void updateAllGlobalRanks();
}
