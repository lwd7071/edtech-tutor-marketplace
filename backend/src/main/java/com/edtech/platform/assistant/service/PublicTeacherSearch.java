package com.edtech.platform.assistant.service;

import java.util.List;

public interface PublicTeacherSearch {
    List<PublicTeacherMatch> findForQuestion(String question);
}
