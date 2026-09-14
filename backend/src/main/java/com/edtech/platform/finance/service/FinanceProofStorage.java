package com.edtech.platform.finance.service;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.storage.FileStoragePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class FinanceProofStorage {
    private static final long MAX_SIZE = 10 * 1024 * 1024;
    private static final List<String> ALLOWED_MIME = List.of("application/pdf", "image/jpeg", "image/png");
    private final FileStoragePort storage;
    private final Tika tika = new Tika();

    public FileStoragePort.UploadResult upload(MultipartFile file, String kind, UUID requestId) {
        if (file == null || file.isEmpty()) throw new BusinessException(
                kind.equals("refund") ? ErrorCode.REFUND_PROOF_REQUIRED : ErrorCode.PAYOUT_PROOF_REQUIRED);
        if (file.getSize() > MAX_SIZE) throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        try {
            String detected = tika.detect(file.getInputStream());
            if (!ALLOWED_MIME.contains(detected) || !extensionMatches(file.getOriginalFilename(), detected)) {
                throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
            }
            FileStoragePort.UploadResult result = storage.upload(file, "finance/" + kind + "/" + requestId);
            if (result == null || result.publicId() == null || result.publicId().isBlank()
                    || result.secureUrl() == null || result.secureUrl().isBlank()) {
                throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
            }
            registerRollbackCleanup(result.publicId());
            return result;
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    private boolean extensionMatches(String name, String mime) {
        if (name == null || name.lastIndexOf('.') < 0) return false;
        String ext = name.substring(name.lastIndexOf('.') + 1).toLowerCase(java.util.Locale.ROOT);
        return (mime.equals("application/pdf") && ext.equals("pdf"))
                || (mime.equals("image/jpeg") && (ext.equals("jpg") || ext.equals("jpeg")))
                || (mime.equals("image/png") && ext.equals("png"));
    }

    private void registerRollbackCleanup(String publicId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    try { storage.delete(publicId); }
                    catch (IOException ex) { log.warn("Unable to cleanup finance proof after rollback", ex); }
                }
            }
        });
    }
}
