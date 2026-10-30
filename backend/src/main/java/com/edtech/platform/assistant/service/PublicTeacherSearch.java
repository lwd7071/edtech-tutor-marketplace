package com.edtech.platform.assistant.service;

import com.edtech.platform.catalog.dto.TeacherCard;

import java.util.List;

public interface PublicTeacherSearch {
    List<TeacherCard> findForQuestion(String question);
}
