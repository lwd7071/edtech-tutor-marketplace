package com.edtech.platform.finance.service;

import com.edtech.platform.finance.domain.Wallet;
import com.edtech.platform.finance.dto.response.LedgerEntryView;
import com.edtech.platform.finance.dto.response.WalletView;
import com.edtech.platform.finance.repository.LedgerEntryRepository;
import com.edtech.platform.finance.repository.PlatformLedgerEntryRepository;
import com.edtech.platform.finance.repository.WalletRepository;
import com.edtech.platform.teacher.facade.TeacherFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WalletQueryService {

    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final PlatformLedgerEntryRepository platformLedgerEntryRepository;
    private final TeacherFacade teachers;

    @Transactional
    public WalletView getOrCreateWallet(UUID teacherUserId) {
        UUID teacherId = teachers.getTeacherByUserId(teacherUserId).id();
        walletRepository.ensureForTeacher(teacherId);
        Wallet wallet = walletRepository.findByTeacherId(teacherId)
                .orElseGet(() -> walletRepository.save(Wallet.forTeacher(teacherId)));
        return WalletView.from(wallet, platformLedgerEntryRepository.heldBalanceForTeacher(teacherId));
    }

    public Page<LedgerEntryView> getLedgerEntries(UUID teacherUserId, Pageable pageable) {
        UUID teacherId = teachers.getTeacherByUserId(teacherUserId).id();
        return walletRepository.findByTeacherId(teacherId)
                .map(wallet -> ledgerEntryRepository.findByWalletId(wallet.getId(), pageable)
                        .map(LedgerEntryView::from))
                .orElseGet(() -> new PageImpl<>(List.of(), pageable, 0));
    }
}
