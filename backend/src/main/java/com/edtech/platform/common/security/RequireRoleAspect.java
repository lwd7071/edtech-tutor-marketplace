package com.edtech.platform.common.security;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
public class RequireRoleAspect {

    @Before("@annotation(requireRole)")
    public void checkRole(RequireRole requireRole) {
        String currentRole = SecurityUtils.getCurrentUserRole();
        if (currentRole == null) {
            throw new BusinessException(ErrorCode.ROLE_NOT_ALLOWED);
        }
        
        boolean hasRole = Arrays.asList(requireRole.value()).contains(currentRole);
        if (!hasRole) {
            throw new BusinessException(ErrorCode.ROLE_NOT_ALLOWED);
        }
    }
}
