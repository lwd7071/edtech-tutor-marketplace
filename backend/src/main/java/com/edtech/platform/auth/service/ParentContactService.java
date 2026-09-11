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

    @Transactional(readOnly = true)
    public ParentContactResponse get(UUID userId) {
        return response(requireStudent(userId));
    }

    @Transactional
    public ParentContactResponse update(UUID userId, UpdateParentContactRequest request) {
        User user = requireStudent(userId);
        user.setParentFullName(normalize(request.parentFullName()));
        user.setParentPhone(normalize(request.parentPhone()));
        user.setParentEmail(normalize(request.parentEmail()));
        user.setNotifyParent(StringUtils.hasText(user.getParentEmail()) && Boolean.TRUE.equals(request.notifyParent()));
        user = users.save(user);
        return response(user);
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private User requireStudent(UUID userId) {
        User user = users.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (user.getRole() != Role.STUDENT) throw new BusinessException(ErrorCode.ROLE_NOT_ALLOWED);
        return user;
    }

    private ParentContactResponse response(User user) {
        return new ParentContactResponse(user.getParentFullName(), user.getParentPhone(), user.getParentEmail(),
                user.getNotifyParent(), user.getUpdatedAt());
    }
}
