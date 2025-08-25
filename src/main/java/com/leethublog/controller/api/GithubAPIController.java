package com.leethublog.controller.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@GetMapping("/api/github")
public class GithubAPIController {

    @GetMapping("/repo")
    public String getRepo() {
        return "redirect:https://api.github.com/user/repos";

    }
}
