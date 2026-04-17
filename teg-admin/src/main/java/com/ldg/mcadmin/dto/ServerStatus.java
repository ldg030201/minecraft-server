package com.ldg.mcadmin.dto;

import java.util.List;

public class ServerStatus {

    private boolean online;
    private int playerCount;
    private List<String> players;
    private String containerState;
    private String uptime;
    private boolean startInProgress;

    public ServerStatus() {}

    public ServerStatus(boolean online, int playerCount, List<String> players,
                        String containerState, String uptime, boolean startInProgress) {
        this.online = online;
        this.playerCount = playerCount;
        this.players = players;
        this.containerState = containerState;
        this.uptime = uptime;
        this.startInProgress = startInProgress;
    }

    public static ServerStatus offline(String containerState, boolean startInProgress) {
        return new ServerStatus(false, 0, List.of(), containerState, "", startInProgress);
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public int getPlayerCount() {
        return playerCount;
    }

    public void setPlayerCount(int playerCount) {
        this.playerCount = playerCount;
    }

    public List<String> getPlayers() {
        return players;
    }

    public void setPlayers(List<String> players) {
        this.players = players;
    }

    public String getContainerState() {
        return containerState;
    }

    public void setContainerState(String containerState) {
        this.containerState = containerState;
    }

    public String getUptime() {
        return uptime;
    }

    public void setUptime(String uptime) {
        this.uptime = uptime;
    }

    public boolean isStartInProgress() {
        return startInProgress;
    }

    public void setStartInProgress(boolean startInProgress) {
        this.startInProgress = startInProgress;
    }
}
