package com.edtech.platform.auth.facade.impl;

import com.edtech.platform.auth.domain.Role;
import com.edtech.platform.auth.domain.User;
import com.edtech.platform.auth.domain.UserStatus;
import com.edtech.platform.auth.repository.RefreshTokenRepository;
import com.edtech.platform.auth.repository.UserRepository;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.security.UserStatusCacheService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class IdentityModerationFacadeImplTest {
    @Mock UserRepository users;
    @Mock RefreshTokenRepository refreshTokens;
    @Mock UserStatusCacheService statusCache;

    @Test
    void adminCannotModerateOwnAccount() {
        UUID actorId = UUID.randomUUID();
        IdentityModerationFacadeImpl facade = new IdentityModerationFacadeImpl(users, refreshTokens, statusCache);

        assertThatThrownBy(() -> facade.changeStatus(actorId, actorId, "LOCKED"))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_MODERATION_SELF_FORBIDDEN));
    }
}
