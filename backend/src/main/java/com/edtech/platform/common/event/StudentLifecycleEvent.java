package com.edtech.platform.common.event;

import java.util.UUID;

public record StudentLifecycleEvent(UUID studentUserId, String type, String title, String content,
                                    String referenceType, UUID resourceId) { }
