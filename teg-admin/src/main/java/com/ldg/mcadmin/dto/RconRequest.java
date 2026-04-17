package com.ldg.mcadmin.dto;

public class RconRequest {

    private String command;

    public RconRequest() {}

    public RconRequest(String command) {
        this.command = command;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }
}
