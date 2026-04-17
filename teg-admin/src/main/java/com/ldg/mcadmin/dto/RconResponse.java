package com.ldg.mcadmin.dto;

public class RconResponse {

    private boolean success;
    private String response;

    public RconResponse() {}

    public RconResponse(boolean success, String response) {
        this.success = success;
        this.response = response;
    }

    public static RconResponse success(String response) {
        return new RconResponse(true, response);
    }

    public static RconResponse error(String message) {
        return new RconResponse(false, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }
}
