package com.synapsys.api.infrastructure.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class SpaFallbackController {

    @GetMapping(value = {
        "/{path:[^\\.]*}",
        "/{path:[^\\.]*}/**"
    })
    public String spa(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri.equals("/api") || uri.startsWith("/api/")) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return "forward:/index.html";
    }
}