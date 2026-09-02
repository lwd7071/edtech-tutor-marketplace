package com.edtech.platform.finance.repository;
import com.edtech.platform.finance.domain.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;
import java.util.UUID;
public interface WalletRepository extends JpaRepository<Wallet,UUID>{
    Optional<Wallet> findByTeacherId(UUID teacherId);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select w from Wallet w where w.teacherId=:teacherId")
    Optional<Wallet> findByTeacherIdForUpdate(UUID teacherId);
}
