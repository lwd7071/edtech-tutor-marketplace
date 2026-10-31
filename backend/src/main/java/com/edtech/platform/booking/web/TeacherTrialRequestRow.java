package com.edtech.platform.booking.web;

import com.edtech.platform.booking.dto.response.TrialRequestView;

public record TeacherTrialRequestRow(TrialRequestView request, String studentName, String subjectName) {}
