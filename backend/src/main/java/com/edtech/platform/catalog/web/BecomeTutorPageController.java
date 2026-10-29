package com.edtech.platform.catalog.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class BecomeTutorPageController {
    @GetMapping("/become-a-tutor")
    public String becomeTutor(Model model) {
        model.addAttribute("activeNav", "become-a-tutor");
        return "public/become-a-tutor";
    }
}
