package com.synapsys.api.infrastructure.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaFallbackController {

    @GetMapping(value = {
        "/{path:(?!api(?:/|$))[^\\.]*}",
        "/{path:(?!api(?:/|$))[^\\.]*}/**"
    })
    public String spa() {
        return "forward:/index.html";
    }
}