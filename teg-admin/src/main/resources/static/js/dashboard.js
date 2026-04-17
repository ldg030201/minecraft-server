document.addEventListener('DOMContentLoaded', function () {
    var form = document.getElementById('rcon-form');
    var input = document.getElementById('rcon-input');
    var output = document.getElementById('console-output');
    var pollInterval = null;

    // RCON Console
    form.addEventListener('submit', function (e) {
        e.preventDefault();
        var command = input.value.trim();
        if (!command) return;

        appendLine('> ' + command, 'command');
        input.value = '';

        fetch('/api/rcon/command', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({command: command})
        })
            .then(function (res) { return res.json(); })
            .then(function (data) {
                if (data.success) {
                    appendLine(data.response || '(출력 없음)', 'response');
                } else {
                    appendLine(data.response, 'error');
                }
            })
            .catch(function (err) {
                appendLine('오류: ' + err.message, 'error');
            });
    });

    function appendLine(text, type) {
        var line = document.createElement('div');
        line.className = 'console-line ' + type;
        line.textContent = text;
        output.appendChild(line);
        output.scrollTop = output.scrollHeight;
    }

    // Status polling
    var previousContainerState = null;

    function refreshStatus() {
        fetch('/api/status')
            .then(function (res) { return res.json(); })
            .then(function (status) {
                var badge = document.querySelector('.status-badge');
                if (badge) {
                    if (status.startInProgress) {
                        badge.textContent = '생성 중...';
                        badge.className = 'status-badge creating';
                    } else if (status.online) {
                        badge.textContent = '온라인';
                        badge.className = 'status-badge online';
                    } else {
                        badge.textContent = '오프라인';
                        badge.className = 'status-badge offline';
                    }
                }

                var values = document.querySelectorAll('.status-value');
                if (values.length >= 3) {
                    var stateText = status.containerState;
                    if (stateText === 'creating') stateText = '생성 중...';
                    else if (stateText === 'not_found') stateText = '없음';
                    else if (stateText === 'running') stateText = '실행 중';
                    else if (stateText === 'exited') stateText = '중지됨';
                    values[0].textContent = stateText || '알 수 없음';
                    values[1].textContent = status.uptime || '-';
                    values[2].textContent = status.playerCount || '0';
                }

                // 컨테이너가 생성 완료되어 running 상태가 되면 자동으로 로그 연결
                if (previousContainerState !== 'running' && status.containerState === 'running' && !eventSource) {
                    toggleLogStream();
                }
                previousContainerState = status.containerState;
            })
            .catch(function () {
                // silently fail on poll
            });
    }

    function startFastPolling() {
        stopPolling();
        pollInterval = setInterval(refreshStatus, 3000);
    }

    function startNormalPolling() {
        stopPolling();
        pollInterval = setInterval(refreshStatus, 10000);
    }

    function stopPolling() {
        if (pollInterval) {
            clearInterval(pollInterval);
            pollInterval = null;
        }
    }

    // 기본 10초 폴링
    startNormalPolling();

    // Server control
    window.controlServer = function (action) {
        var labels = {start: '시작', stop: '중지', restart: '재시작'};
        var msg = document.getElementById('control-message');

        var buttons = document.querySelectorAll('.control-buttons .btn');
        buttons.forEach(function (b) { b.disabled = true; });
        msg.textContent = '서버 ' + labels[action] + ' 중...';
        msg.className = 'control-message';

        fetch('/api/container/' + action, {method: 'POST'})
            .then(function (res) { return res.json(); })
            .then(function (data) {
                msg.textContent = data.message;
                msg.className = 'control-message ' + (data.success ? 'success' : 'error');
                if (data.success) {
                    refreshStatus();
                    // 서버 시작/재시작 시 빠른 폴링으로 전환
                    if (action === 'start' || action === 'restart') {
                        startFastPolling();
                        // 30초 후 일반 폴링으로 복귀
                        setTimeout(function () { startNormalPolling(); }, 30000);
                    }
                }
            })
            .catch(function (err) {
                msg.textContent = '오류: ' + err.message;
                msg.className = 'control-message error';
            })
            .finally(function () {
                buttons.forEach(function (b) { b.disabled = false; });
            });
    };

    // Server log streaming
    var logOutput = document.getElementById('server-log-output');
    var eventSource = null;

    window.toggleLogStream = function () {
        var btn = document.getElementById('btn-log-connect');
        if (eventSource) {
            eventSource.close();
            eventSource = null;
            btn.textContent = '로그 연결';
            appendLogLine('[연결 해제됨]', 'info');
            return;
        }

        eventSource = new EventSource('/api/logs/stream?tail=100');
        btn.textContent = '로그 중지';
        appendLogLine('[로그 스트림 연결 중...]', 'info');

        eventSource.onmessage = function (event) {
            appendLogLine(event.data, 'log');
        };

        eventSource.onerror = function () {
            if (eventSource.readyState === EventSource.CLOSED) {
                appendLogLine('[연결 종료됨]', 'info');
                eventSource = null;
                btn.textContent = '로그 연결';
            }
        };
    };

    window.clearLogs = function () {
        logOutput.innerHTML = '';
    };

    function appendLogLine(text, type) {
        var line = document.createElement('div');
        line.className = 'console-line ' + type;
        line.textContent = text;
        logOutput.appendChild(line);
        while (logOutput.children.length > 1000) {
            logOutput.removeChild(logOutput.firstChild);
        }
        logOutput.scrollTop = logOutput.scrollHeight;
    }
});
