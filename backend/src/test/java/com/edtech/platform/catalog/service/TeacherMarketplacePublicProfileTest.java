package com.edtech.platform.catalog.service;

import com.edtech.platform.auth.facade.IdentityFacade;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.ranking.facade.TeacherStatsFacade;
import com.edtech.platform.subject.facade.SubjectFacade;
import com.edtech.platform.teacher.facade.TeacherFacade;
import com.edtech.platform.teacher.facade.dto.TeacherSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TeacherMarketplacePublicProfileTest {

    @Test
    void hidesPendingTeacherProfileFromPublicDetails() {
        assertNotPublic(snapshot("PENDING_APPROVAL", true));
    }

    @Test
    void hidesApprovedButInvisibleTeacherProfileFromPublicDetails() {
        assertNotPublic(snapshot("APPROVED", false));
    }
    @Test
    void returnsFullBioWhenAvailableInSnapshot() {
        UUID teacherId = UUID.randomUUID();
        String fullBio = "Thạc sĩ Toán học ĐH Sư Phạm Hà Nội, 10 năm kinh nghiệm luyện thi ĐH và bồi dưỡng học sinh giỏi. Phương pháp dạy tư duy mạch lạc, dễ nhớ.";
        TeacherSnapshot snapshot = new TeacherSnapshot(
                teacherId, UUID.randomUUID(), "APPROVED", true, true,
                "Thầy Toán", null, "Thạc sĩ Toán...", 10,
                true, false, List.of("Vietnamese"), null, null, null, null, fullBio);

        TeacherFacade teacherFacade = mock(TeacherFacade.class);
        IdentityFacade identityFacade = mock(IdentityFacade.class);
        TeacherStatsFacade statsFacade = mock(TeacherStatsFacade.class);
        SubjectFacade subjectFacade = mock(SubjectFacade.class);
        PricingPackageService packages = mock(PricingPackageService.class);
        TeacherSearchCache searchCache = mock(TeacherSearchCache.class);

        when(teacherFacade.getTeacher(teacherId)).thenReturn(snapshot);
        when(teacherFacade.getSubjectIdsForTeacher(teacherId)).thenReturn(List.of());

        TeacherMarketplaceService service = new TeacherMarketplaceService(
                teacherFacade, identityFacade, statsFacade, subjectFacade, packages, searchCache);

        var detail = service.getTeacherDetail(teacherId);
        org.junit.jupiter.api.Assertions.assertEquals(fullBio, detail.bio());
    }

    private void assertNotPublic(TeacherSnapshot snapshot) {
        TeacherFacade teacherFacade = mock(TeacherFacade.class);
        IdentityFacade identityFacade = mock(IdentityFacade.class);
        TeacherStatsFacade statsFacade = mock(TeacherStatsFacade.class);
        SubjectFacade subjectFacade = mock(SubjectFacade.class);
        PricingPackageService packages = mock(PricingPackageService.class);
        TeacherSearchCache searchCache = mock(TeacherSearchCache.class);
        when(teacherFacade.getTeacher(snapshot.id())).thenReturn(snapshot);

        TeacherMarketplaceService service = new TeacherMarketplaceService(
                teacherFacade, identityFacade, statsFacade, subjectFacade, packages, searchCache);

        assertThatThrownBy(() -> service.getTeacherDetail(snapshot.id()))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        org.assertj.core.api.Assertions.assertThat(ex.getErrorCode())
                                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
        verifyNoInteractions(identityFacade, statsFacade, subjectFacade, packages, searchCache);
    }

    private TeacherSnapshot snapshot(String status, boolean visible) {
        return new TeacherSnapshot(UUID.randomUUID(), UUID.randomUUID(), status, false, visible,
                "Không được công khai", null, "Hồ sơ chưa công khai", 1,
                true, false, List.of("Tiếng Việt"), null, null, null, null);
    }
}
