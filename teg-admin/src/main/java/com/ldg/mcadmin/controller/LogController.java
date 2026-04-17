package com.ldg.mcadmin.controller;

import com.ldg.mcadmin.service.DockerService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/logs")
public class LogController {

    private final DockerService dockerService;

    public LogController(DockerService dockerService) {
        this.dockerService = dockerService;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamLogs(@RequestParam(defaultValue = "100") int tail) {
        SseEmitter emitter = new SseEmitter(0L); // 타임아웃 없음
        dockerService.streamContainerLogs(emitter, tail);
        return emitter;
    }
}
