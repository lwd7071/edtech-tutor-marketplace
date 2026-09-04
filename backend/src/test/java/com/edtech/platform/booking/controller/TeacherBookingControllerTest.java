package com.edtech.platform.booking.controller;

import com.edtech.platform.booking.domain.BookingStatus;
import com.edtech.platform.booking.domain.CancelInitiatedBy;
import com.edtech.platform.booking.domain.DeliveryMode;
import com.edtech.platform.booking.dto.request.CancelBookingRequest;
import com.edtech.platform.booking.dto.request.CompleteBookingRequest;
import com.edtech.platform.booking.dto.request.CreateBookingRequest;
import com.edtech.platform.booking.dto.request.SessionReportRequest;
import com.edtech.platform.booking.dto.response.BookingDetail;
import com.edtech.platform.booking.service.BookingService;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TeacherBookingControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private TeacherBookingController controller;

    private final UUID teacherUserId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                        return new AuthenticatedUser(teacherUserId, "teacher@example.com", "TEACHER");
                    }
                })
                .build();
    }

    @Test
    void createBooking_shouldReturn201() throws Exception {
        UUID packageId = UUID.randomUUID();
        Instant start = Instant.now().plusSeconds(3600);
        Instant end = start.plusSeconds(3600);
        CreateBookingRequest request = new CreateBookingRequest(packageId, start, end, DeliveryMode.ONLINE, "https://meet.google.com", null);

        BookingDetail detail = new BookingDetail(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), packageId,
                start, end, DeliveryMode.ONLINE, BookingStatus.SCHEDULED, false, false, null, 0L
        );
        when(bookingService.create(eq(teacherUserId), any())).thenReturn(detail);

        mockMvc.perform(post("/api/teacher/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("SCHEDULED"));
    }

    @Test
    void completeBooking_shouldReturn200() throws Exception {
        UUID bookingId = UUID.randomUUID();
        CompleteBookingRequest request = new CompleteBookingRequest(0L, new SessionReportRequest(null, "Good", null, null, 5));

        BookingDetail detail = new BookingDetail(
                bookingId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                Instant.now().minusSeconds(7200), Instant.now().minusSeconds(3600), DeliveryMode.ONLINE,
                BookingStatus.COMPLETED, false, false, null, 1L
        );
        when(bookingService.complete(eq(teacherUserId), eq(bookingId), any())).thenReturn(detail);

        mockMvc.perform(post("/api/teacher/bookings/" + bookingId + "/complete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    void cancelBooking_shouldReturn200() throws Exception {
        UUID bookingId = UUID.randomUUID();
        CancelBookingRequest request = new CancelBookingRequest(0L, "Cancelled by teacher", CancelInitiatedBy.TEACHER);

        BookingDetail detail = new BookingDetail(
                bookingId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                Instant.now().plusSeconds(3600), Instant.now().plusSeconds(7200), DeliveryMode.ONLINE,
                BookingStatus.CANCELLED, false, false, "Cancelled by teacher", 1L
        );
        when(bookingService.cancel(eq(teacherUserId), eq(bookingId), any())).thenReturn(detail);

        mockMvc.perform(post("/api/teacher/bookings/" + bookingId + "/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }
}