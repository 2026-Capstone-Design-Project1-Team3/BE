# 🗣️ TalkUp Backend

TalkUp 서비스의 백엔드 리포지토리입니다.
사용자 시선 캘리브레이션 및 실시간 AI 데이터 처리를 지원하는 API 서버입니다.

## 🛠️ Tech Stack

- **Language:** Java 17
- **Framework:** Spring Boot 3.x
- **Database:** MySQL 8.x, Redis
- **ORM:** Spring Data JPA, Hibernate
- **Security:** Spring Security, JWT (RS256 RSA Key)

---

## 📁 Folder Structure

```text
src/main/java/com/server/talkup_be/
├── config/           # Security, JWT, Redis, Swagger 등 환경 설정
├── controller/       # API 엔드포인트 (UserController 등)
├── dto/              # 클라이언트 요청/응답 데이터 전송 객체 (UserDto 등)
├── entity/           # JPA DB 엔티티 (User, EyeCalibration 등)
├── exception/        # 커스텀 예외 처리 (MissingCalibrationException 등)
├── repo/             # DB 접근 계층 (UserRepo 등)
└── service/          # 핵심 비즈니스 로직 (UserService, RedisBlacklistService 등)
```

## ⚙️ Getting Started (초기 세팅 및 실행 방법)
1. Prerequisites (사전 준비)
프로젝트를 실행하기 전 로컬 환경에 아래 항목들이 설치되어 있어야 합니다.

- **Java 17 (JDK)**

- **MySQL (Port: 3306)**

- **Redis (Port: 6379)**

### 2. Database Setup
MySQL에 접속하여 로컬 테스트용 데이터베이스를 생성합니다.

```sql
CREATE DATABASE talkUp CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3. Environment Variables (환경 변수 및 키 설정)
보안 상의 이유로 JWT 서명에 사용되는 RSA Key 및 DB 비밀번호는 깃허브에 업로드되지 않습니다.
프로젝트 최상단 `src/main/resources/` 경로에 아래 파일들을 직접 추가해야 합니다.

* **`application.yml`**
  * MySQL DB 접속 정보 (username, password)
  * Redis 접속 정보
* **JWT RSA Keys (`.pem` 파일)**
  * `private_key.pem`
  * `public_key.pem`

### 4. Build & Run

```bash
# 프로젝트 클론
$ git clone https://github.com/2026-Capstone-Design-Project1-Team3/BE.git

# 폴더 이동
$ cd TalkUp_BE

# 빌드
$ ./gradlew build

# 실행
$ ./gradlew bootRun
```
