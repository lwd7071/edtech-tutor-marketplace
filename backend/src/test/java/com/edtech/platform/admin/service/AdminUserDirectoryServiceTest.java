package com.edtech.platform.admin.service;

import com.edtech.platform.auth.facade.IdentityDirectoryFacade;
import com.edtech.platform.auth.facade.dto.UserDirectorySnapshot;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AdminUserDirectoryServiceTest {
    @Test
    void trimsSearchTermAndMapsOnlyDirectoryFields() {
        IdentityDirectoryFacade identities = mock(IdentityDirectoryFacade.class);
        AdminUserDirectoryService service = new AdminUserDirectoryService(identities);
        UUID id = UUID.randomUUID();
        var page = PageRequest.of(0, 20);
        var snapshot = new UserDirectorySnapshot(id, "Learner", "learner@example.test", "STUDENT",
                "ACTIVE", Instant.parse("2026-09-01T00:00:00Z"), null);
        when(identities.searchUsers("learner", "STUDENT", "ACTIVE", page))
                .thenReturn(new PageImpl<>(List.of(snapshot), page, 1));

        var result = service.findUsers(" learner ", "STUDENT", "ACTIVE", page);

        assertThat(result.getContent()).containsExactly(new com.edtech.platform.admin.dto.response.AdminUserView(
                id, "Learner", "learner@example.test", "STUDENT", "ACTIVE",
                Instant.parse("2026-09-01T00:00:00Z"), null));
        verify(identities).searchUsers("learner", "STUDENT", "ACTIVE", page);
    }
}
