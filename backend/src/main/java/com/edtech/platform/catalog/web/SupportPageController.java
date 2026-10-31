package com.edtech.platform.catalog.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SupportPageController {
    @GetMapping("/support")
    public String support(Model model) {
        model.addAttribute("activeNav", "support");
        return "public/support";
    }
}
