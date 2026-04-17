package com.ldg.mcadmin.controller;

import com.ldg.mcadmin.dto.ServerStatus;
import com.ldg.mcadmin.service.DockerService;
import com.ldg.mcadmin.service.RconService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class StatusController {

    private final RconService rconService;
    private final DockerService dockerService;

    public StatusController(RconService rconService, DockerService dockerService) {
        this.rconService = rconService;
        this.dockerService = dockerService;
    }

    @GetMapping("/status")
    public ServerStatus getStatus() {
        String containerState = dockerService.getContainerState();
        boolean inProgress = dockerService.isStartInProgress();
        boolean online = rconService.isServerOnline();

        if (!online) {
            return ServerStatus.offline(containerState, inProgress);
        }

        List<String> players = rconService.getPlayerList();
        String uptime = dockerService.getUptime();

        return new ServerStatus(true, players.size(), players, containerState, uptime, false);
    }
}
