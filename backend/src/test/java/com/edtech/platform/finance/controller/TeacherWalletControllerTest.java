package com.edtech.platform.finance.controller;

import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.finance.domain.BalanceBucket;
import com.edtech.platform.finance.domain.LedgerDirection;
import com.edtech.platform.finance.domain.LedgerEntryType;
import com.edtech.platform.finance.dto.response.LedgerEntryView;
import com.edtech.platform.finance.dto.response.WalletView;
import com.edtech.platform.finance.service.WalletQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TeacherWalletControllerTest {

    private MockMvc mockMvc;

    @Mock
    private WalletQueryService walletQueryService;

    @InjectMocks
    private TeacherWalletController controller;

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
    void getWallet_shouldReturnWalletView() throws Exception {
        WalletView walletView = new WalletView(UUID.randomUUID(), teacherUserId, 500_000L, 1_000_000L, 200_000L, 1L);
        when(walletQueryService.getOrCreateWallet(teacherUserId)).thenReturn(walletView);

        mockMvc.perform(get("/api/teacher/wallet")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.pendingBalanceVnd").value(500000))
                .andExpect(jsonPath("$.data.availableBalanceVnd").value(1000000))
                .andExpect(jsonPath("$.data.reservedBalanceVnd").value(200000));
    }

    @Test
    void getLedger_shouldReturnLedgerEntriesPage() throws Exception {
        LedgerEntryView entry = new LedgerEntryView(
                UUID.randomUUID(),
                UUID.randomUUID(),
                LedgerEntryType.SESSION_CREDIT_AVAILABLE,
                200_000L,
                BalanceBucket.AVAILABLE,
                LedgerDirection.CREDIT,
                "SESSION_REPORT",
                UUID.randomUUID(),
                "Session completed",
                Instant.now()
        );
        when(walletQueryService.getLedgerEntries(eq(teacherUserId), any()))
                .thenReturn(new PageImpl<>(List.of(entry), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/teacher/wallet/ledger")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].amountVnd").value(200000))
                .andExpect(jsonPath("$.data[0].entryType").value("SESSION_CREDIT_AVAILABLE"))
                .andExpect(jsonPath("$.meta.totalElements").value(1));
    }
}