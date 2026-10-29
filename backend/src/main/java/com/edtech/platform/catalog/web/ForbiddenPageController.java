package com.edtech.platform.catalog.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ForbiddenPageController {
    @RequestMapping("/forbidden")
    public String forbidden() {
        return "public/forbidden";
    }
}
