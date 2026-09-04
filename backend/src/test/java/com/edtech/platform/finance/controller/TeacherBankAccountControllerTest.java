package com.edtech.platform.finance.controller;

import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.finance.dto.request.UpsertBankAccountRequest;
import com.edtech.platform.finance.dto.response.BankAccountView;
import com.edtech.platform.finance.service.BankAccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TeacherBankAccountControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private BankAccountService bankAccountService;

    @InjectMocks
    private TeacherBankAccountController controller;

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
    void listBankAccounts_shouldReturn200() throws Exception {
        BankAccountView view = new BankAccountView(
                UUID.randomUUID(), "970436", "Vietcombank", "******6789", "NGUYEN VAN A", false, true, Instant.now()
        );
        when(bankAccountService.findTeacherAccounts(teacherUserId)).thenReturn(List.of(view));

        mockMvc.perform(get("/api/teacher/bank-accounts")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].bankName").value("Vietcombank"))
                .andExpect(jsonPath("$.data[0].accountNumberMasked").value("******6789"));
    }

    @Test
    void createBankAccount_shouldReturn201() throws Exception {
        UpsertBankAccountRequest req = new UpsertBankAccountRequest(
                "970436", "Vietcombank", "0123456789", "NGUYEN VAN A", true
        );
        BankAccountView view = new BankAccountView(
                UUID.randomUUID(), "970436", "Vietcombank", "******6789", "NGUYEN VAN A", false, true, Instant.now()
        );
        when(bankAccountService.createAccount(eq(teacherUserId), any())).thenReturn(view);

        mockMvc.perform(post("/api/teacher/bank-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accountNumberMasked").value("******6789"));
    }

    @Test
    void deleteBankAccount_shouldReturn204() throws Exception {
        UUID accountId = UUID.randomUUID();

        mockMvc.perform(delete("/api/teacher/bank-accounts/" + accountId))
                .andExpect(status().isNoContent());
    }
}