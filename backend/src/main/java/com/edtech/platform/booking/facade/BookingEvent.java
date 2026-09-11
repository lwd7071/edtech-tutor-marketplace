package com.edtech.platform.booking.facade;
import java.util.UUID;
public record BookingEvent(String type, UUID resourceId, UUID studentUserId, UUID teacherProfileId, UUID teacherUserId) {}
