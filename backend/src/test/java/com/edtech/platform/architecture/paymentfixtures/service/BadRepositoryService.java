package com.edtech.platform.architecture.paymentfixtures.service;

import com.edtech.platform.payment.domain.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface BadRepositoryService extends JpaRepository<Invoice, UUID> { }
