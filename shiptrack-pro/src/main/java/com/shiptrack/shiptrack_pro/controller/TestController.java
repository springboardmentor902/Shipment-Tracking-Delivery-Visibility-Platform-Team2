package com.shiptrack.shiptrack_pro.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/secure")
    public Map<String, Object> secureEndpoint() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        return Map.of(
                "message", "You reached a protected endpoint!",
                "loggedInAs", authentication.getName(),
                "authorities", authentication.getAuthorities().toString()
        );
    }
}