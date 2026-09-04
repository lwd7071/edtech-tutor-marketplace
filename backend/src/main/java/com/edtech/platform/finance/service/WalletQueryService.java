package com.edtech.platform.finance.service;

import com.edtech.platform.finance.domain.Wallet;
import com.edtech.platform.finance.dto.response.LedgerEntryView;
import com.edtech.platform.finance.dto.response.WalletView;
import com.edtech.platform.finance.repository.LedgerEntryRepository;
import com.edtech.platform.finance.repository.WalletRepository;
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

    @Transactional
    public WalletView getOrCreateWallet(UUID teacherId) {
        walletRepository.ensureForTeacher(teacherId);
        Wallet wallet = walletRepository.findByTeacherId(teacherId)
                .orElseGet(() -> walletRepository.save(Wallet.forTeacher(teacherId)));
        return WalletView.from(wallet);
    }

    public Page<LedgerEntryView> getLedgerEntries(UUID teacherId, Pageable pageable) {
        return walletRepository.findByTeacherId(teacherId)
                .map(wallet -> ledgerEntryRepository.findByWalletId(wallet.getId(), pageable)
                        .map(LedgerEntryView::from))
                .orElseGet(() -> new PageImpl<>(List.of(), pageable, 0));
    }
}
