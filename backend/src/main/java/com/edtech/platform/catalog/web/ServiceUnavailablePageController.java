package com.edtech.platform.catalog.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ServiceUnavailablePageController {
    @RequestMapping("/service-unavailable")
    public String unavailable(HttpServletRequest request, HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setHeader("Cache-Control", "no-store, max-age=0");
        if (request.getAttribute("retryUrl") == null) request.setAttribute("retryUrl", "/");
        return "public/service-unavailable";
    }
}
