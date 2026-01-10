package com.example.springhotel.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BookRestController {

    @GetMapping("/test")
    public String hello() {
        return "Hello";
    }
}

