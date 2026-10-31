package com.edtech.platform.booking.web;

import com.edtech.platform.auth.service.SessionService;
import com.edtech.platform.booking.domain.BookingStatus;
import com.edtech.platform.booking.domain.SettlementStatus;
import com.edtech.platform.booking.dto.response.BookingSettlementAdminView;
import com.edtech.platform.booking.service.BookingReadService;
import com.edtech.platform.booking.service.BookingService;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.JwtTokenProvider;
import com.edtech.platform.common.security.UserStatusCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AdminBookingSettlementsPageController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminBookingSettlementsPageControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private BookingReadService reads;
    @MockBean private BookingService bookings;
    @MockBean private JwtTokenProvider tokens;
    @MockBean private UserStatusCacheService statuses;
    @MockBean private SessionService sessions;
    @MockBean private Clock clock;

    private final UUID adminId = UUID.randomUUID();
    private final UUID bookingId = UUID.randomUUID();

    @BeforeEach
    void authenticateAdmin() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(adminId, "admin@example.test", "ADMIN"), null, List.of()));
        when(clock.instant()).thenReturn(Instant.parse("2026-10-29T00:00:00Z"));
    }

    @Test
    void rendersSettlementQueueAndPrivatePageHeaders() throws Exception {
        when(reads.adminSettlements(eq(SettlementStatus.DISPUTE_PENDING), any()))
                .thenReturn(new PageImpl<>(List.of(settlement(SettlementStatus.DISPUTE_PENDING))));
        mockMvc.perform(get("/admin/booking-settlements").param("status", "DISPUTE_PENDING"))
                .andExpect(status().isOk())
                .andExpect(view().name("workspace/admin-booking-settlements"))
                .andExpect(header().string("Cache-Control", "no-store, max-age=0"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Xem xác nhận của hai bên")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Lý do khiếu nại")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Mở lại xác nhận")));
    }

    @Test
    void normalizesFilterAndAppliesStablePaging() throws Exception {
        when(reads.adminSettlements(eq((SettlementStatus) null), any())).thenReturn(new PageImpl<>(List.of()));
        mockMvc.perform(get("/admin/booking-settlements").param("status", "unknown").param("page", "-1"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Không có buổi học ở trạng thái này")));
        verify(reads).adminSettlements(eq((SettlementStatus) null), argThat(page -> page.getPageNumber() == 0
                && page.getPageSize() == 20 && page.getSort().getOrderFor("createdAt").getDirection() == Sort.Direction.DESC
                && page.getSort().getOrderFor("id").getDirection() == Sort.Direction.DESC));
    }

    @Test
    void rendersSafeErrorStateWhenReadModelFails() throws Exception {
        when(reads.adminSettlements(any(), any())).thenThrow(new IllegalStateException("private service details"));
        mockMvc.perform(get("/admin/booking-settlements"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa tải được danh sách quyết toán")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("private service details"))));
    }

    @Test
    void blankDecisionNoteIsRejectedBeforeCallingDomainService() throws Exception {
        mockMvc.perform(post("/admin/booking-settlements/{id}/retain", bookingId)
                        .param("version", "3").param("note", "  "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/booking-settlements?status=ALL&page=0"));
        verify(bookings, never()).adminDecision(any(), any(), anyLong(), anyBoolean(), anyString());
    }

    @Test
    void actionsCarryCurrentVersionAndDecisionNote() throws Exception {
        mockMvc.perform(post("/admin/booking-settlements/{id}/reopen", bookingId)
                        .param("version", "9").param("note", "  Đối chiếu lại xác nhận hai bên.  ")
                        .param("status", "DISPUTE_PENDING").param("page", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/booking-settlements?status=DISPUTE_PENDING&page=2"));
        verify(bookings).reopenSettlement(adminId, bookingId, 9L, "Đối chiếu lại xác nhận hai bên.");
    }

    @Test
    void retainsNoteAndOpensActionAfterConcurrentConflict() throws Exception {
        when(bookings.adminDecision(adminId, bookingId, 2L, true, "Ghi chú quyết định"))
                .thenThrow(new BusinessException(ErrorCode.CONCURRENT_MODIFICATION));
        mockMvc.perform(post("/admin/booking-settlements/{id}/retain", bookingId)
                        .param("version", "2").param("note", "Ghi chú quyết định"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/booking-settlements?status=ALL&page=0"));
    }

    private BookingSettlementAdminView settlement(SettlementStatus status) {
        Instant started = Instant.parse("2026-10-28T10:00:00Z");
        return new BookingSettlementAdminView(bookingId, UUID.randomUUID(), "Học viên A", UUID.randomUUID(),
                "Gia sư B", BookingStatus.COMPLETED, started, started.plusSeconds(3600), status,
                null, null, started.plusSeconds(86400), null, 150_000L, "Hai bên đang bất đồng", started,
                4L);
    }
}
