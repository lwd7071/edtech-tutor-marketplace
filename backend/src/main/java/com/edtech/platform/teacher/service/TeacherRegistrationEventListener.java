package com.edtech.platform.teacher.service;

import com.edtech.platform.auth.domain.Role;
import com.edtech.platform.auth.domain.User;
import com.edtech.platform.auth.event.UserRegisteredEvent;
import com.edtech.platform.teacher.domain.TeacherProfile;
import com.edtech.platform.teacher.repository.TeacherProfileRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class TeacherRegistrationEventListener {

    private final TeacherProfileRepository teacherProfileRepository;
    private final EntityManager entityManager;

    @EventListener
    @Transactional
    public void handleUserRegisteredEvent(UserRegisteredEvent event) {
        if (event.getRole() == Role.TEACHER) {
            log.info("Handling UserRegisteredEvent for TEACHER user: {}", event.getUserId());

            User userProxy = entityManager.getReference(User.class, event.getUserId());

            TeacherProfile profile = TeacherProfile.builder()
                    .user(userProxy)
                    .build();

            teacherProfileRepository.save(profile);
            log.info("Successfully created DRAFT TeacherProfile for user: {}", event.getUserId());
        }
    }
}
