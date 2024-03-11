package org.zeniot.server.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Wu.Chunyang
 */
@RequestMapping("/api")
@RestController
public class TestController {

    @GetMapping("test")
    public String test(HttpServletRequest request) {
        return "test ok. " + request.getServerName();
    }
}
