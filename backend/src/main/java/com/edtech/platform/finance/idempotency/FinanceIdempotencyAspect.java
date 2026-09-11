package com.edtech.platform.finance.idempotency;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.response.ApiResponse;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.finance.service.FinanceCommandExecutor;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.UUID;

@Aspect
@Component
@RequiredArgsConstructor
public class FinanceIdempotencyAspect {
    private final FinanceCommandExecutor executor;

    @Around("@annotation(idempotent)")
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Object executeOnce(ProceedingJoinPoint point, FinanceIdempotent idempotent) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String rawKey = request.getHeader("Idempotency-Key");
        UUID key;
        try {
            key = rawKey == null || rawKey.isBlank() ? null : UUID.fromString(rawKey.trim());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_REQUIRED);
        }
        UUID actor = Arrays.stream(point.getArgs())
                .filter(AuthenticatedUser.class::isInstance)
                .map(AuthenticatedUser.class::cast)
                .map(AuthenticatedUser::id)
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.IDEMPOTENCY_KEY_REQUIRED));
        Object command = Arrays.stream(point.getArgs())
                .filter(arg -> !(arg instanceof AuthenticatedUser))
                .toList();
        return executor.execute(actor, idempotent.operation(), key, command,
                (Class) idempotent.responseType(), () -> {
                    try {
                        return (ApiResponse) point.proceed();
                    } catch (Throwable ex) {
                        if (ex instanceof RuntimeException runtime) throw runtime;
                        throw new FinanceCommandInvocationException(ex);
                    }
                });
    }

    public static class FinanceCommandInvocationException extends RuntimeException {
        public FinanceCommandInvocationException(Throwable cause) { super(cause); }
    }
}
