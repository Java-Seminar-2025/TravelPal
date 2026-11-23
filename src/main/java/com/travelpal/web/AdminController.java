package com.travelpal.web;

import com.travelpal.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Only users with role ADMIN can open /admin (see SecurityConfig). */
@Controller
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;

    @GetMapping("/admin")
    public String admin(Model model) {
        model.addAttribute("users", userService.findAllUsers());
        return "admin";
    }
}
