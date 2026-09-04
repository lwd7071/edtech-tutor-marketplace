package com.edtech.platform.finance.repository;
import com.edtech.platform.finance.domain.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;
public interface WalletRepository extends JpaRepository<Wallet,UUID>{
    @Modifying
    @Query(value = "insert into wallets (teacher_id) values (:teacherId) on conflict (teacher_id) do nothing", nativeQuery = true)
    int ensureForTeacher(@Param("teacherId") UUID teacherId);
    Optional<Wallet> findByTeacherId(UUID teacherId);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select w from Wallet w where w.teacherId=:teacherId")
    Optional<Wallet> findByTeacherIdForUpdate(UUID teacherId);
}
