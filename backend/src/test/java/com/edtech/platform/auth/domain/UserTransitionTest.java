package com.edtech.platform.auth.domain;

import com.edtech.platform.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserTransitionTest {
    @Test
    void moderationOnlyAllowsActiveAndLockedTransitions() {
        User user = User.builder().status(UserStatus.ACTIVE).build();
        user.lock();
        assertEquals(UserStatus.LOCKED, user.getStatus());
        user.unlock();
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertThrows(BusinessException.class, user::unlock);
        User pending = User.builder().status(UserStatus.PENDING_VERIFICATION).build();
        assertThrows(BusinessException.class, pending::lock);
    }

    @Test
    void verificationActivatesOnlyPendingUsers() {
        User pending = User.builder().status(UserStatus.PENDING_VERIFICATION).build();
        pending.verifyEmail();
        assertEquals(UserStatus.ACTIVE, pending.getStatus());
        assertTrue(pending.getEmailVerified());
        User locked = User.builder().status(UserStatus.LOCKED).build();
        locked.verifyEmail();
        assertEquals(UserStatus.LOCKED, locked.getStatus());
    }
}
