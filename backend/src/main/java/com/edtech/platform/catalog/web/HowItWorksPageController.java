package com.edtech.platform.catalog.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HowItWorksPageController {
    @GetMapping("/how-it-works")
    public String howItWorks(Model model) {
        model.addAttribute("activeNav", "how-it-works");
        return "public/how-it-works";
    }
}
