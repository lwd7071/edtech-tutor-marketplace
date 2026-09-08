package com.edtech.platform.finance.command;

public record RejectFinanceCommand(String reason, long version) {
}
