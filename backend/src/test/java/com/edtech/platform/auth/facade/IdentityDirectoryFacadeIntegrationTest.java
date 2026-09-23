package com.edtech.platform.auth.facade;

import com.edtech.platform.auth.domain.Role;
import com.edtech.platform.auth.domain.UserStatus;
import com.edtech.platform.common.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class IdentityDirectoryFacadeIntegrationTest extends AbstractIntegrationTest {
    @Autowired IdentityDirectoryFacade directory;
    @Autowired JdbcTemplate jdbc;

    @Test
    @Transactional
    void directorySearchIncludesOnlyActiveOrLockedStudentsAndTeachersAndSearchesNameOrEmail() {
        UUID student = user("student-match@example.test", "Nguyễn Minh An", "STUDENT", "ACTIVE", false);
        UUID teacher = user("teacher@example.test", "Gia sư khác", "TEACHER", "LOCKED", false);
        user("admin-match@example.test", "Minh Admin", "ADMIN", "ACTIVE", false);
        user("pending-match@example.test", "Minh Pending", "STUDENT", "PENDING_VERIFICATION", false);
        user("deleted-match@example.test", "Minh Deleted", "TEACHER", "ACTIVE", true);

        var byName = directory.searchUsers("minh", null, null, PageRequest.of(0, 20, Sort.by("email", "id")));
        assertThat(byName.getContent()).extracting(snapshot -> snapshot.id()).containsExactly(student);

        var byEmail = directory.searchUsers("TEACHER@EXAMPLE.TEST", "TEACHER", "LOCKED",
                PageRequest.of(0, 20, Sort.by("createdAt", "id")));
        assertThat(byEmail.getContent()).extracting(snapshot -> snapshot.id()).containsExactly(teacher);
        assertThat(byEmail.getContent().getFirst().status()).isEqualTo("LOCKED");
    }

    private UUID user(String email, String name, String role, String status, boolean deleted) {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into users(id,email,password_hash,full_name,role,status,is_deleted) values(?,?,?,?,?,?,?)",
                id, email, "hash", name, role, status, deleted);
        return id;
    }
}
