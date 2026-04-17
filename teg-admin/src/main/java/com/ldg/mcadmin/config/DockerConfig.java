package com.ldg.mcadmin.config;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;

@Configuration
public class DockerConfig {

    @Value("${docker.host:#{null}}")
    private String dockerHost;

    @Bean
    public DockerClient dockerClient() {
        String host = resolveDockerHost();

        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(host)
                .build();

        ApacheDockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(URI.create(host))
                .build();

        return DockerClientImpl.getInstance(config, httpClient);
    }

    private String resolveDockerHost() {
        if (dockerHost != null && !dockerHost.isBlank()) {
            return dockerHost;
        }
        boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
        return isWindows ? "npipe:////./pipe/docker_engine" : "unix:///var/run/docker.sock";
    }
}
