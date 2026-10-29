package com.edtech.platform.teacher.web;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice(assignableTypes = TeacherDocumentsPageController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TeacherDocumentUploadExceptionHandler {
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String uploadTooLarge(RedirectAttributes redirect, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        redirect.addFlashAttribute("documentsError", "Tệp vượt quá giới hạn tải lên của máy chủ.");
        return "redirect:/teacher/documents";
    }
}
