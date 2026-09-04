package com.edtech.platform.booking.facade;
import java.util.UUID;
public record BookingEvent(String type, UUID bookingId, UUID studentId, UUID teacherId) {}
