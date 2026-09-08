package com.edtech.platform.finance.command;

import java.time.Instant;

public record ApproveExtensionCommand(Instant approvedExpiryDate, String adminNote) {
}
