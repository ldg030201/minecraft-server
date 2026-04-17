package com.ldg.mcadmin.service;

import com.ldg.mcadmin.config.RconConfig;
import com.ldg.mcadmin.dto.RconResponse;
import com.github.t9t.minecraftrconclient.RconClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RconService {

    private static final Logger log = LoggerFactory.getLogger(RconService.class);
    private static final Pattern PLAYER_LIST_PATTERN =
            Pattern.compile("There are (\\d+) of a max of \\d+ players online:(.*)");

    private final RconConfig rconConfig;

    public RconService(RconConfig rconConfig) {
        this.rconConfig = rconConfig;
    }

    public RconResponse sendCommand(String command) {
        try (RconClient client = RconClient.open(
                rconConfig.getHost(), rconConfig.getPort(), rconConfig.getPassword())) {
            String response = client.sendCommand(command);
            return RconResponse.success(response);
        } catch (Exception e) {
            log.warn("RCON command failed: {}", e.getMessage());
            return RconResponse.error("서버에 연결할 수 없습니다: " + e.getMessage());
        }
    }

    public boolean isServerOnline() {
        try (RconClient client = RconClient.open(
                rconConfig.getHost(), rconConfig.getPort(), rconConfig.getPassword())) {
            client.sendCommand("list");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public List<String> getPlayerList() {
        RconResponse response = sendCommand("list");
        if (!response.isSuccess()) {
            return List.of();
        }

        Matcher matcher = PLAYER_LIST_PATTERN.matcher(response.getResponse());
        if (matcher.find()) {
            String playersPart = matcher.group(2).trim();
            if (playersPart.isEmpty()) {
                return List.of();
            }
            List<String> players = new ArrayList<>();
            for (String name : playersPart.split(",")) {
                String trimmed = name.trim();
                if (!trimmed.isEmpty()) {
                    players.add(trimmed);
                }
            }
            return players;
        }
        return List.of();
    }

    public int getPlayerCount() {
        RconResponse response = sendCommand("list");
        if (!response.isSuccess()) {
            return 0;
        }

        Matcher matcher = PLAYER_LIST_PATTERN.matcher(response.getResponse());
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 0;
    }
}
