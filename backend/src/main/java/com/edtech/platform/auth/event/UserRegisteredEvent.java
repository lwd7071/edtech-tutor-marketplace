package com.edtech.platform.auth.event;

import com.edtech.platform.auth.domain.Role;
import com.edtech.platform.common.event.AbstractDomainEvent;
import lombok.Getter;

import java.util.UUID;

@Getter
public class UserRegisteredEvent extends AbstractDomainEvent {
    private final UUID userId;
    private final String email;
    private final Role role;
    private final String fullName;

    public UserRegisteredEvent(UUID userId, String email, Role role, String fullName) {
        super();
        this.userId = userId;
        this.role = role;
        this.email = email;
        this.fullName = fullName;
    }

    public String getRoleName() {
        return role != null ? role.name() : null;
    }
}
