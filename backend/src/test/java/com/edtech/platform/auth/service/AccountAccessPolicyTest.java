package com.edtech.platform.auth.service;

import com.edtech.platform.auth.domain.Role;
import com.edtech.platform.auth.domain.User;
import com.edtech.platform.auth.domain.UserStatus;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountAccessPolicyTest {
    private final AccountAccessPolicy policy = new AccountAccessPolicy();

    @Test
    void pendingVerificationCannotReceiveSession() {
        User user = User.builder().email("student@example.test").passwordHash("hash").fullName("Student")
                .role(Role.STUDENT).status(UserStatus.PENDING_VERIFICATION).emailVerified(false).build();
        assertThatThrownBy(() -> policy.requireActiveAccess(user))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> org.assertj.core.api.Assertions.assertThat(((BusinessException) error).getErrorCode())
                        .isEqualTo(ErrorCode.AUTH_EMAIL_NOT_VERIFIED));
    }
}
