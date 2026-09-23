package com.edtech.platform.auth.facade;

import com.edtech.platform.auth.facade.dto.UserDirectorySnapshot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IdentityDirectoryFacade {
    Page<UserDirectorySnapshot> searchUsers(String keyword, String role, String status, Pageable pageable);
}
