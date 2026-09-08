package com.edtech.platform.finance.command;

public record ApproveRefundCommand(int approvedSessions, String adminNote, long version) {
}
