package com.edtech.platform.common.web;

import com.edtech.platform.common.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(basePackages = {
        "com.edtech.platform.catalog.web",
        "com.edtech.platform.subject.web",
        "com.edtech.platform.ranking.web"
})
public class PublicIdentityModelAdvice {
    @ModelAttribute("htmlUser")
    public AuthenticatedUser htmlUser(@AuthenticationPrincipal AuthenticatedUser user) {
        return user;
    }
}
