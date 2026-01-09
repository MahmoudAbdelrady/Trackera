package com.mdevs.trackera.controller;

import com.mdevs.trackera.shared.annotations.PublicAPI;
import com.mdevs.trackera.shared.annotations.RateLimited;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@PublicAPI
@RateLimited
@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {
    @GetMapping("/hello")
    public ResponseEntity<?> sayHello() {
        return ResponseEntity.ok("Hello, Trackera!");
    }
}
