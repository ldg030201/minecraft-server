package com.ldg.mcadmin.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Frame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class DockerService {

    private static final Logger log = LoggerFactory.getLogger(DockerService.class);

    private final DockerClient dockerClient;
    private final String containerName;
    private final String composePath;

    private volatile boolean startInProgress = false;

    public DockerService(DockerClient dockerClient,
                         @Value("${mc.container-name:mc-ms}") String containerName,
                         @Value("${mc.compose-path:}") String composePath) {
        this.dockerClient = dockerClient;
        this.containerName = containerName;
        this.composePath = composePath;
    }

    public boolean isStartInProgress() {
        return startInProgress;
    }

    public String getContainerState() {
        try {
            if (startInProgress) {
                return "creating";
            }
            Container container = findContainer();
            if (container == null) {
                return "not_found";
            }
            return container.getState();
        } catch (Throwable e) {
            log.warn("Docker 상태 조회 실패: {}", e.getMessage());
            return "unknown";
        }
    }

    public String getUptime() {
        try {
            Container container = findContainer();
            if (container == null || !"running".equals(container.getState())) {
                return "";
            }
            long created = container.getCreated();
            Duration uptime = Duration.between(Instant.ofEpochSecond(created), Instant.now());
            long hours = uptime.toHours();
            long minutes = uptime.toMinutesPart();
            if (hours > 0) {
                return hours + "시간 " + minutes + "분";
            }
            return minutes + "분";
        } catch (Throwable e) {
            log.warn("Uptime 조회 실패: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 컨테이너 시작. 리턴값으로 상태를 구분한다.
     * - "already_running": 이미 실행 중
     * - "started": 기존 컨테이너 시작됨
     * - "creating": 컨테이너가 없어서 비동기 생성 시작
     * - "in_progress": 이미 생성 진행 중
     */
    public String startContainer() {
        if (startInProgress) {
            return "in_progress";
        }

        Container container = findContainer();

        if (container != null && "running".equals(container.getState())) {
            log.info("컨테이너가 이미 실행 중: {}", containerName);
            return "already_running";
        }

        if (container != null) {
            try {
                dockerClient.startContainerCmd(container.getId()).exec();
                log.info("기존 컨테이너 시작: {}", containerName);
                return "started";
            } catch (Exception e) {
                log.error("컨테이너 시작 실패: {}", e.getMessage());
                throw new RuntimeException("컨테이너 시작 실패: " + e.getMessage(), e);
            }
        }

        // 컨테이너가 없음 → 비동기로 docker compose 실행
        log.info("컨테이너가 없으므로 docker compose로 생성합니다: {}", containerName);
        startInProgress = true;
        CompletableFuture.runAsync(() -> {
            try {
                runDockerCompose("up", "-d", "minecraft");
                log.info("docker compose로 컨테이너 생성 완료");
            } catch (Exception e) {
                log.error("docker compose 컨테이너 생성 실패: {}", e.getMessage());
            } finally {
                startInProgress = false;
            }
        });
        return "creating";
    }

    public void stopContainer() {
        Container container = findContainer();
        if (container == null) {
            throw new RuntimeException("컨테이너를 찾을 수 없습니다: " + containerName);
        }
        if (!"running".equals(container.getState())) {
            log.info("컨테이너가 이미 중지 상태: {}", containerName);
            return;
        }
        try {
            dockerClient.stopContainerCmd(container.getId()).exec();
            log.info("컨테이너 중지: {}", containerName);
        } catch (Exception e) {
            log.error("컨테이너 중지 실패: {}", e.getMessage());
            throw new RuntimeException("컨테이너 중지 실패: " + e.getMessage(), e);
        }
    }

    public void restartContainer() {
        Container container = findContainer();
        if (container == null) {
            throw new RuntimeException("컨테이너를 찾을 수 없습니다: " + containerName);
        }
        try {
            dockerClient.restartContainerCmd(container.getId()).exec();
            log.info("컨테이너 재시작: {}", containerName);
        } catch (Exception e) {
            log.error("컨테이너 재시작 실패: {}", e.getMessage());
            throw new RuntimeException("컨테이너 재시작 실패: " + e.getMessage(), e);
        }
    }

    public void streamContainerLogs(SseEmitter emitter, int tailCount) {
        Container container = findContainer();
        if (container == null) {
            try {
                emitter.send(SseEmitter.event().data("컨테이너를 찾을 수 없습니다."));
                emitter.complete();
            } catch (IOException e) {
                // ignore
            }
            return;
        }

        ResultCallback.Adapter<Frame> callback = new ResultCallback.Adapter<>() {
            @Override
            public void onNext(Frame frame) {
                try {
                    String line = new String(frame.getPayload(), StandardCharsets.UTF_8).stripTrailing();
                    if (!line.isEmpty()) {
                        emitter.send(SseEmitter.event().data(line));
                    }
                } catch (IOException e) {
                    try {
                        close();
                    } catch (IOException ex) {
                        // ignore
                    }
                }
            }

            @Override
            public void onComplete() {
                emitter.complete();
            }

            @Override
            public void onError(Throwable throwable) {
                emitter.completeWithError(throwable);
            }
        };

        emitter.onCompletion(() -> {
            try {
                callback.close();
            } catch (IOException e) {
                // ignore
            }
        });
        emitter.onTimeout(() -> {
            try {
                callback.close();
            } catch (IOException e) {
                // ignore
            }
        });

        dockerClient.logContainerCmd(container.getId())
                .withStdOut(true)
                .withStdErr(true)
                .withFollowStream(true)
                .withTail(tailCount)
                .withTimestamps(false)
                .exec(callback);
    }

    private Container findContainer() {
        List<Container> containers = dockerClient.listContainersCmd()
                .withShowAll(true)
                .withNameFilter(List.of(containerName))
                .exec();

        return containers.stream()
                .filter(c -> {
                    for (String name : c.getNames()) {
                        if (name.equals("/" + containerName) || name.equals(containerName)) {
                            return true;
                        }
                    }
                    return false;
                })
                .findFirst()
                .orElse(null);
    }

    private void runDockerCompose(String... args) {
        try {
            File composeDir = resolveComposeDirectory();

            List<String> command = new ArrayList<>();
            command.add("docker");
            command.add("compose");
            for (String arg : args) {
                command.add(arg);
            }

            log.info("docker compose 실행: {} (dir: {})", String.join(" ", command), composeDir);

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(composeDir);
            pb.redirectErrorStream(true);

            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("[docker compose] {}", line);
                }
            }

            boolean finished = process.waitFor(300, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("docker compose 실행 시간 초과 (300초)");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new RuntimeException("docker compose 실행 실패 (exit code: " + exitCode + ")");
            }

            log.info("docker compose 실행 완료");
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("docker compose 실행 실패: {}", e.getMessage());
            throw new RuntimeException("docker compose 실행 실패: " + e.getMessage(), e);
        }
    }

    private File resolveComposeDirectory() {
        if (composePath != null && !composePath.isBlank()) {
            File f = new File(composePath);
            if (f.isFile()) {
                return f.getParentFile();
            }
            return f;
        }

        File cwd = new File(System.getProperty("user.dir"));
        if (new File(cwd, "docker-compose.yml").exists()) {
            return cwd;
        }

        File parent = cwd.getParentFile();
        if (parent != null && new File(parent, "docker-compose.yml").exists()) {
            return parent;
        }

        throw new RuntimeException("docker-compose.yml을 찾을 수 없습니다. mc.compose-path 설정을 확인하세요.");
    }
}
