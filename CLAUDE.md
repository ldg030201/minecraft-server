# 마인크래프트 RPG 서버 프로젝트 (인프라 + Spring Boot)

## 프로젝트 개요

Docker 기반 마인크래프트 RPG 서버를 운영하기 위한 프로젝트. 이 레포는 **인프라(docker-compose) + Spring Boot 관리 대시보드**를 담당한다.

관련 프로젝트:
- **minecraft-server-side-mod** (별도 레포): Forge 1.16.5 서버사이드 RPG 모드

개발은 내가 하고, 서버는 친구 컴퓨터에서 Docker로 돌아간다. 친구 PC는 항상 켜져 있지 않고, 플레이할 때만 서버를 켠다.

## 기존 GitHub 레포

https://github.com/ldg030201/minecraft-server

기존 docker-compose.yml (업데이트 전):
```yaml
version: "3.8"
services:
  minecraft:
    image: itzg/minecraft-server:java11
    container_name: mc-ms
    ports:
      - "25565:25565"
    environment:
      EULA: "TRUE"
      TYPE: "FORGE"
      VERSION: "1.16.5"
      FORGE_VERSION: "36.2.39"
      MEMORY: "6G"
      TZ: "Asia/Seoul"
    tty: true
    stdin_open: true
    volumes:
      - mc-data:/data
      - ./user/mods:/mods:ro
volumes:
  mc-data:
```

## 서버 성격

- RPG 서버 (PvE 중심, 전투/던전 컨텐츠)
- 소규모: 친구들끼리 ~10명
- 기존에 전투/PvP 관련 모드들이 설치되어 있음
- 친구 PC가 항상 켜져 있지 않음 → 플레이할 때만 실행

---

## 배포 전략 (핵심)

### 원칙
친구 PC가 항상 켜져 있지 않으므로 Watchtower 같은 상시 자동 업데이트 방식은 부적합하다.
대신 **"서버 시작 시점에 최신 이미지를 당겨온다"** 방식을 쓴다.

### 동작 흐름

1. 내가 코드 push → GitHub Actions가 Docker 이미지 빌드 → GHCR에 push
2. 친구가 PC 켜고 `start-server.bat` 더블클릭
3. bat 파일이 `docker compose pull` 실행 → 최신 이미지 자동 다운로드
4. `docker compose up -d` 실행 → 최신 버전으로 서버 시작

### 각 시나리오별 동작

| 상황 | 동작 |
|------|------|
| 친구가 PC 켜고 bat 실행 | 두 이미지 다 최신 확인, 최신 버전으로 시작 |
| 모드만 수정 후 push | 마크 서버 이미지만 새로 빌드/push, 다음 시작 시 적용 |
| Spring Boot만 수정 후 push | 대시보드 이미지만 새로 빌드/push, 다음 시작 시 적용 |
| 둘 다 수정 후 push | 둘 다 새로 빌드/push, 다음 시작 시 둘 다 업데이트 |
| 플레이 중 대시보드만 급히 업데이트 | 대시보드에서 "업데이트 적용" 버튼 → `teg-admin`만 재시작 |

### 친구가 할 일

1. Docker Desktop 설치 (최초 1회)
2. bat 파일 더블클릭해서 서버 켜기 (매번)

---

## 친구 PC의 최종 파일 구조

```
minecraft-server/
├── docker-compose.yml           # 배포용 compose
├── .env                          # RCON 비밀번호 등 (gitignore)
├── start-server.bat             # 서버 시작 (pull + up)
├── stop-server.bat              # 서버 종료 (down)
└── user/
    └── mods/                    # 기존 외부 모드 파일들 (유지)
```

### 친구 PC의 docker-compose.yml

```yaml
version: "3.8"
services:
  minecraft:
    image: ghcr.io/ldg030201/mc-server:latest
    pull_policy: always
    container_name: mc-ms
    ports:
      - "25565:25565"
      - "25575:25575"
    environment:
      EULA: "TRUE"
      TYPE: "FORGE"
      VERSION: "1.16.5"
      FORGE_VERSION: "36.2.39"
      MEMORY: "6G"
      TZ: "Asia/Seoul"
      ENABLE_RCON: "true"
      RCON_PASSWORD: "${RCON_PASSWORD}"
      RCON_PORT: 25575
    tty: true
    stdin_open: true
    volumes:
      - mc-data:/data
      - ./user/mods:/mods:ro

  teg-admin:
    image: ghcr.io/ldg030201/teg-admin:latest
    pull_policy: always
    container_name: teg-admin
    ports:
      - "8080:8080"
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
      - mc-data:/mc-data:ro
    environment:
      RCON_HOST: minecraft
      RCON_PORT: 25575
      RCON_PASSWORD: "${RCON_PASSWORD}"
      MC_CONTAINER_NAME: mc-ms
    depends_on:
      - minecraft

volumes:
  mc-data:
```

### start-server.bat

```batch
@echo off
cd /d "%~dp0"
echo 최신 버전 확인 중...
docker compose pull
echo 서버 시작 중...
docker compose up -d
echo 완료! 서버가 시작되었습니다.
echo 대시보드: http://localhost:8080
pause
```

### stop-server.bat

```batch
@echo off
cd /d "%~dp0"
echo 서버 종료 중...
docker compose down
echo 종료 완료.
pause
```

---

## Spring Boot 앱 (teg-admin)

### 기술 스택
- Spring Boot 3.x (Java 17+)
- Docker로 컨테이너화 (이 레포에 Dockerfile 포함)
- 프론트엔드: Thymeleaf 또는 React

### 구현할 기능

#### 서버 상태 조회
- 서버 온라인/오프라인 상태
- 현재 접속자 수 및 접속 중인 플레이어 목록
- 서버 TPS, 메모리 사용량
- 구현 방법: Minecraft Query 프로토콜 (UDP 25565) 또는 RCON `/list`

#### RCON 명령어 실행
- 웹 UI에서 서버 명령어 원격 실행
- 화이트리스트 관리 (추가/제거)
- 플레이어 밴/언밴
- 아이템 지급 (/give)
- 커스텀 명령어 입력창
- 구현 방법: RCON 프로토콜 (TCP 25575), `rkon-core` 라이브러리

#### 서버 컨테이너 제어
- 웹 UI 버튼으로 마크 서버 컨테이너 on/off/restart
- 업데이트 적용 버튼 (각 서비스별 개별 업데이트 가능)
  - `teg-admin`만 업데이트: `docker compose pull teg-admin && docker compose up -d teg-admin`
  - `minecraft`만 업데이트: 공지 → 카운트다운 → pull → restart
- 구현 방법: Docker Java Client, `/var/run/docker.sock` 마운트

#### 재시작 플로우 (플레이어 배려)
마크 서버 재시작 시 플레이어가 갑자기 튕기지 않도록:
1. RCON으로 공지: `/say 30초 후 서버 재시작`
2. 카운트다운 중간 공지 (15초, 5초)
3. `/save-all` 후 `/stop`
4. `docker compose pull minecraft` → `docker compose up -d minecraft`
5. 시작 완료 감지 후 Discord 웹훅 등으로 재개 알림

#### 플레이어 관리 (모드 연동)
- 플레이어별 레벨, 직업, 장비 현황 조회
- 접속 기록 (접속/퇴장 시간, 플레이타임)
- 랭킹 (레벨, 보스 킬 수, 총 데미지 등)
- 던전 클리어 기록/통계
- 구현 방법: 마크 서버 볼륨(`mc-data`)을 읽기 전용으로 마운트해서 모드의 JSON 데이터 직접 읽기

#### 웹 대시보드 UI
- 메인 페이지: 서버 상태 + 접속자 + 요약 통계
- 플레이어 페이지: 개별 플레이어 상세 정보
- 관리 페이지: RCON 콘솔, 서버 제어, 업데이트 적용
- 랭킹 페이지: 각종 랭킹 보드

### 플레이어 데이터 읽기 경로

모드가 저장한 데이터를 `teg-admin`에서 읽는다:
- 플레이어 데이터: `/mc-data/world/data/rpg/players/*.json`
- 이벤트 로그: `/mc-data/world/data/rpg/logs/YYYY-MM-DD.json`

형식은 **minecraft-server-side-mod 프로젝트의 CLAUDE.md**에 정의되어 있음. 변경 시 양쪽 동기화 필요.

---

## CI/CD 파이프라인

### teg-admin 이미지 빌드 (이 레포)

`.github/workflows/build-admin.yml`:

```yaml
name: Build and Push teg-admin
on:
  push:
    branches: [main]
    paths:
      - 'teg-admin/**'
      - '.github/workflows/build-admin.yml'

jobs:
  build:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'

      - name: Build with Gradle
        working-directory: ./teg-admin
        run: ./gradlew build -x test

      - name: Login to GHCR
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Build and push
        uses: docker/build-push-action@v5
        with:
          context: ./teg-admin
          push: true
          tags: ghcr.io/ldg030201/teg-admin:latest
```

### mc-server 이미지 빌드

모드 레포(`minecraft-server-side-mod`)에서 jar 빌드 후, 그 레포의 워크플로우가 커스텀 MC 서버 이미지까지 빌드해서 GHCR에 push한다. 이 이미지는 `itzg/minecraft-server:java11`을 베이스로 하고 모드 jar를 포함한다.

상세 내용은 모드 레포의 CLAUDE.md 참고.

---

## 이 레포 디렉토리 구조

```
minecraft-server/
├── CLAUDE.md                    # 이 파일
├── docker-compose.yml           # 친구 PC에 배포될 최종 compose
├── start-server.bat             # 서버 시작 bat
├── stop-server.bat              # 서버 종료 bat
├── .env.example                 # 환경변수 템플릿
├── .github/
│   └── workflows/
│       └── build-admin.yml      # teg-admin 이미지 빌드/푸시
├── teg-admin/                    # Spring Boot 프로젝트 (teg-admin)
│   ├── Dockerfile
│   ├── build.gradle
│   ├── src/
│   └── ...
└── user/
    └── mods/                    # 기존 외부 모드 파일들
```

---

## 개발 우선순위

1. **Spring Boot 기본 세팅** — 프로젝트 초기화, Dockerfile
2. **RCON 연동** — rkon-core로 명령어 실행, 서버 상태 조회
3. **Docker 컨테이너 제어** — Docker Java Client로 start/stop/restart
4. **웹 대시보드 UI** — 메인 페이지 (서버 상태 + RCON 콘솔 + 제어 버튼)
5. **CI/CD 파이프라인** — GitHub Actions + GHCR
6. **docker-compose.yml + bat 파일 작성**
7. **모드 연동** — 모드 프로젝트 완성 후, JSON 데이터 읽어서 대시보드 표시
8. **업데이트 적용 버튼** — 서비스별 개별 업데이트 + 재시작 플로우

---

## 참고

- Docker Java Client: https://github.com/docker-java/docker-java
- RCON 라이브러리: https://github.com/Kronos666/rkon-core
- itzg/minecraft-server 이미지: https://github.com/itzg/docker-minecraft-server
