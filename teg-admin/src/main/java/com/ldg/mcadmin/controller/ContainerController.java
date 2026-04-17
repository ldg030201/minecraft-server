package com.ldg.mcadmin.controller;

import com.ldg.mcadmin.service.DockerService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/container")
public class ContainerController {

    private final DockerService dockerService;

    public ContainerController(DockerService dockerService) {
        this.dockerService = dockerService;
    }

    @PostMapping("/start")
    public Map<String, Object> start() {
        try {
            String result = dockerService.startContainer();
            String message = switch (result) {
                case "already_running" -> "서버가 이미 실행 중입니다.";
                case "started" -> "서버를 시작했습니다.";
                case "creating" -> "서버 컨테이너를 생성하고 있습니다. 잠시 후 상태를 확인해주세요.";
                case "in_progress" -> "서버 컨테이너 생성이 이미 진행 중입니다.";
                default -> "서버 시작 요청 완료.";
            };
            return Map.of("success", true, "message", message);
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    @PostMapping("/stop")
    public Map<String, Object> stop() {
        try {
            dockerService.stopContainer();
            return Map.of("success", true, "message", "서버를 중지했습니다.");
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    @PostMapping("/restart")
    public Map<String, Object> restart() {
        try {
            dockerService.restartContainer();
            return Map.of("success", true, "message", "서버를 재시작했습니다.");
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }
}
