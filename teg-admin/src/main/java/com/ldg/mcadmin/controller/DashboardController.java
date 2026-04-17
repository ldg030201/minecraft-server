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
        model.addAttribute("containerState", translateState(containerState));
        model.addAttribute("uptime", uptime);

        return "dashboard";
    }

    private String translateState(String state) {
        if (state == null) return "알 수 없음";
        switch (state) {
            case "running": return "실행 중";
            case "exited": return "중지됨";
            case "creating": return "생성 중...";
            case "not_found": return "없음";
            case "paused": return "일시정지";
            case "restarting": return "재시작 중";
            case "dead": return "중단됨";
            default: return state;
        }
    }
}
