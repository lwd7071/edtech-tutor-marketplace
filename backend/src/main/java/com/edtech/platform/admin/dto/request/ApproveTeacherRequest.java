package com.edtech.platform.admin.dto.request;

import jakarta.validation.constraints.Size;

public record ApproveTeacherRequest(@Size(max = 1000) String note) {
}
