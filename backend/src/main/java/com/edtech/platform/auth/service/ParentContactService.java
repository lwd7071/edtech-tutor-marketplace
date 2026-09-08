package com.edtech.platform.auth.service;

import com.edtech.platform.auth.domain.Role;
import com.edtech.platform.auth.domain.User;
import com.edtech.platform.auth.dto.request.UpdateParentContactRequest;
import com.edtech.platform.auth.dto.response.ParentContactResponse;
import com.edtech.platform.auth.repository.UserRepository;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ParentContactService {
    private final UserRepository users;

    @Transactional
    public ParentContactResponse update(UUID userId, UpdateParentContactRequest request) {
        User user = users.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (user.getRole() != Role.STUDENT) {
            throw new BusinessException(ErrorCode.ROLE_NOT_ALLOWED);
        }
        user.setParentFullName(request.parentFullName());
        user.setParentPhone(request.parentPhone());
        user.setParentEmail(request.parentEmail());
        boolean hasContact = StringUtils.hasText(request.parentFullName())
                || StringUtils.hasText(request.parentPhone())
                || StringUtils.hasText(request.parentEmail());
        user.setNotifyParent(hasContact && Boolean.TRUE.equals(request.notifyParent()));
        user = users.save(user);
        return new ParentContactResponse(user.getParentFullName(), user.getParentPhone(), user.getParentEmail(),
                user.getNotifyParent(), user.getUpdatedAt());
    }
}
