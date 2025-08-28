package com.leethublog.controller.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api/github")
public class GithubAPIController {

    @GetMapping("/repo")
    public String getRepo() {
        return "redirect:https://api.github.com/user/repos";

    }
    @PostMapping("/create")
    public String createRepo() {
        return "redirect:https://api.github.com/user/repos";

    }
}
