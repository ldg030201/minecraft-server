package com.ldg.mcadmin.controller;

import com.ldg.mcadmin.service.DockerService;
import com.ldg.mcadmin.service.RconService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class DashboardController {

    private final RconService rconService;
    private final DockerService dockerService;

    public DashboardController(RconService rconService, DockerService dockerService) {
        this.rconService = rconService;
        this.dockerService = dockerService;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        boolean online = rconService.isServerOnline();
        List<String> players = online ? rconService.getPlayerList() : List.of();
        String containerState = dockerService.getContainerState();
        String uptime = dockerService.getUptime();

        model.addAttribute("online", online);
        model.addAttribute("playerCount", players.size());
        model.addAttribute("players", players);
        model.addAttribute("containerState", containerState);
        model.addAttribute("uptime", uptime);

        return "dashboard";
    }
}
