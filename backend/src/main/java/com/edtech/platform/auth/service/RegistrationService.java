package com.edtech.platform.auth.service;

import com.edtech.platform.auth.domain.Role;
import com.edtech.platform.auth.domain.User;
import com.edtech.platform.auth.domain.UserStatus;
import com.edtech.platform.auth.dto.request.RegisterRequest;
import com.edtech.platform.auth.dto.response.RegistrationResult;
import com.edtech.platform.auth.event.UserRegisteredEvent;
import com.edtech.platform.auth.repository.UserRepository;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.mail.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class RegistrationService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher events;
    private final RedisTokenService oneTimeTokens;
    private final MailService mail;

    @Transactional
    public RegistrationResult register(RegisterRequest request, String ipAddress) {
        if (users.existsByEmailIgnoreCase(request.email())) {
            throw new BusinessException(ErrorCode.AUTH_EMAIL_ALREADY_EXISTS);
        }
        if (request.role() == Role.ADMIN) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Cannot register as ADMIN");
        }

        boolean teacher = request.role() == Role.TEACHER;
        if (teacher && (StringUtils.hasText(request.parentEmail())
                || StringUtils.hasText(request.parentPhone())
                || StringUtils.hasText(request.parentFullName()))) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Teacher cannot have parent contact info");
        }
        boolean notifyParent = !teacher
                && (StringUtils.hasText(request.parentEmail()) || StringUtils.hasText(request.parentPhone()));

        User user = users.save(User.builder()
                .email(request.email().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .role(request.role())
                .status(UserStatus.PENDING_VERIFICATION)
                .emailVerified(false)
                .parentFullName(teacher ? null : request.parentFullName())
                .parentPhone(teacher ? null : request.parentPhone())
                .parentEmail(teacher ? null : request.parentEmail())
                .notifyParent(notifyParent)
                .build());

        events.publishEvent(new UserRegisteredEvent(user.getId(), user.getEmail(), user.getRole(), user.getFullName()));
        String token = oneTimeTokens.issue(RedisTokenService.Purpose.EMAIL_VERIFY, user.getId().toString());
        mail.sendVerificationEmail(user.getEmail(), token);
        return new RegistrationResult(user.getEmail(), true);
    }
}
