package vn.iotstar.controller;

import org.springframework.stereotype.Controller;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
public class HomeController {
    @GetMapping("/")
    String home() { return "home"; }

    // Minimal destination for the successful-login URL in Example 1, page 7.
    @GetMapping("/dashboard")
    String dashboard() { return "home"; }

    // Handle both direct requests and Security's access-denied forwards.
    @RequestMapping("/access-denied")
    @ResponseStatus(HttpStatus.FORBIDDEN)
    String accessDenied() { return "access-denied"; }
}
