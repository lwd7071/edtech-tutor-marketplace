package com.edtech.platform.booking.web;

import com.edtech.platform.booking.dto.response.BookingRequestView;

public record TeacherBookingRequestRow(BookingRequestView request, String studentName, String subjectName) {}
