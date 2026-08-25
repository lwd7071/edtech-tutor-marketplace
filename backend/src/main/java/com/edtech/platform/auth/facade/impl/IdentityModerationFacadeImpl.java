package com.edtech.platform.auth.facade.impl;

import com.edtech.platform.auth.domain.Role;
import com.edtech.platform.auth.domain.User;
import com.edtech.platform.auth.domain.UserStatus;
import com.edtech.platform.auth.facade.IdentityModerationFacade;
import com.edtech.platform.auth.facade.dto.IdentityModerationChange;
import com.edtech.platform.auth.facade.dto.IdentitySnapshot;
import com.edtech.platform.auth.repository.RefreshTokenRepository;
import com.edtech.platform.auth.repository.UserRepository;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.security.UserStatusCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IdentityModerationFacadeImpl implements IdentityModerationFacade {
    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final UserStatusCacheService statusCache;

    @Override
    @Transactional
    public IdentityModerationChange changeStatus(UUID actorId, UUID targetUserId, String requestedStatus) {
        if (actorId.equals(targetUserId)) throw new BusinessException(ErrorCode.USER_MODERATION_SELF_FORBIDDEN);
        UserStatus next;
        try { next = UserStatus.valueOf(requestedStatus); }
        catch (RuntimeException ex) { throw new BusinessException(ErrorCode.USER_MODERATION_INVALID_STATE); }
        if (next != UserStatus.ACTIVE && next != UserStatus.LOCKED) {
            throw new BusinessException(ErrorCode.USER_MODERATION_INVALID_STATE);
        }
        User user = users.findByIdForUpdate(targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (user.getRole() == Role.ADMIN) throw new BusinessException(ErrorCode.USER_MODERATION_ADMIN_FORBIDDEN);
        if (user.getStatus() == next) throw new BusinessException(ErrorCode.USER_MODERATION_ALREADY_PROCESSED);
        if (!((user.getStatus() == UserStatus.ACTIVE && next == UserStatus.LOCKED)
                || (user.getStatus() == UserStatus.LOCKED && next == UserStatus.ACTIVE))) {
            throw new BusinessException(ErrorCode.USER_MODERATION_INVALID_STATE);
        }
        IdentitySnapshot before = snapshot(user);
        statusCache.beforeStatusChange(user.getId());
        user.setStatus(next);
        if (next == UserStatus.LOCKED) refreshTokens.updateRevokedAtByUserId(user.getId(), Instant.now());
        statusCache.afterStatusChange(user.getId(), next.name());
        return new IdentityModerationChange(before, snapshot(user));
    }

    private IdentitySnapshot snapshot(User user) {
        return new IdentitySnapshot(user.getId(), user.getEmail(), user.getFullName(), user.getRole(),
                user.getStatus(), user.getAvatarUrl(), user.getNotifyParent(), user.getParentEmail());
    }
}
