package com.edtech.platform.admin.service;

import com.edtech.platform.admin.dto.response.AdminUserView;
import com.edtech.platform.auth.facade.IdentityDirectoryFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminUserDirectoryService {
    private final IdentityDirectoryFacade identities;

    @Transactional(readOnly = true)
    public Page<AdminUserView> findUsers(String keyword, String role, String status, Pageable pageable) {
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return identities.searchUsers(normalizedKeyword, role, status, pageable).map(AdminUserView::from);
    }
}
