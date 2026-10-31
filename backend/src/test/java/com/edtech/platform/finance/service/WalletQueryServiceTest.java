package com.edtech.platform.finance.service;

import com.edtech.platform.finance.domain.Wallet;
import com.edtech.platform.finance.repository.LedgerEntryRepository;
import com.edtech.platform.finance.repository.PlatformLedgerEntryRepository;
import com.edtech.platform.finance.repository.WalletRepository;
import com.edtech.platform.teacher.facade.TeacherFacade;
import com.edtech.platform.teacher.facade.dto.TeacherSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletQueryServiceTest {
    @Mock private WalletRepository wallets;
    @Mock private LedgerEntryRepository ledgerEntries;
    @Mock private PlatformLedgerEntryRepository platformLedgerEntries;
    @Mock private TeacherFacade teachers;
    @InjectMocks private WalletQueryService service;

    @Test
    void walletAndLedgerResolvePrincipalUserIdToTeacherProfileId() {
        UUID userId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        when(teachers.getTeacherByUserId(userId)).thenReturn(new TeacherSnapshot(teacherId, userId,
                "APPROVED", false, true, null, null, null, null, true, false, List.of(), null, null));
        when(wallets.findByTeacherId(teacherId)).thenReturn(Optional.of(Wallet.forTeacher(teacherId)));
        when(platformLedgerEntries.heldBalanceForTeacher(teacherId)).thenReturn(0L);
        var pageable = PageRequest.of(0, 20);
        when(ledgerEntries.findByWalletId(null, pageable)).thenReturn(org.springframework.data.domain.Page.empty(pageable));

        assertEquals(teacherId, service.getOrCreateWallet(userId).teacherId());
        service.getLedgerEntries(userId, pageable);

        verify(wallets).ensureForTeacher(teacherId);
        verify(wallets, org.mockito.Mockito.times(2)).findByTeacherId(teacherId);
        verify(teachers, org.mockito.Mockito.times(2)).getTeacherByUserId(userId);
    }
}
