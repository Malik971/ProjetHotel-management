package com.example.springhotel.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginPageController {

    @GetMapping("/login")
    public String showLoginPage() {
        return "login.html"; // Spring Boot servira automatiquement login.html depuis resources/static/
    }

    @GetMapping("/register")
    public String showRegisterPage() {
        return "register.html"; // Spring Boot servira automatiquement register.html depuis resources/static/
    }

    @GetMapping("/home")
    public String homePage() {
        return "home.html"; // Spring Boot servira automatiquement home.html depuis resources/static/
    }
}

