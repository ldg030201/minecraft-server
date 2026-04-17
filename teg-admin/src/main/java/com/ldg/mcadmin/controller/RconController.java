package com.ldg.mcadmin.controller;

import com.ldg.mcadmin.dto.RconRequest;
import com.ldg.mcadmin.dto.RconResponse;
import com.ldg.mcadmin.service.RconService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rcon")
public class RconController {

    private final RconService rconService;

    public RconController(RconService rconService) {
        this.rconService = rconService;
    }

    @PostMapping("/command")
    public RconResponse sendCommand(@RequestBody RconRequest request) {
        if (request.getCommand() == null || request.getCommand().isBlank()) {
            return RconResponse.error("명령어를 입력해주세요.");
        }
        return rconService.sendCommand(request.getCommand());
    }
}
