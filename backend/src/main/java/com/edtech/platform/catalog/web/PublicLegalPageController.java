package com.edtech.platform.catalog.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PublicLegalPageController {
    @GetMapping("/terms")
    public String terms(Model model) {
        model.addAttribute("activeNav", "terms");
        return "public/terms";
    }

    @GetMapping("/privacy")
    public String privacy(Model model) {
        model.addAttribute("activeNav", "privacy");
        return "public/privacy";
    }
}
