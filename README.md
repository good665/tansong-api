# tansong-api

시니어라이프(SeniorLife) 모바일 앱([tansong-mobile](https://github.com/good665/tansong-mobile))이 사용하는 Spring Boot REST API 서버입니다.

## 기술 스택

- Spring Boot 3.3.5, Java 17
- MyBatis (XML 매퍼 + 어노테이션 매퍼 혼용)
- MySQL 8.0
- Spring Security + JWT(`io.jsonwebtoken`) 기반 Stateless 인증

## 사전 준비물

- JDK 17
- MySQL 8.0 (로컬에 `reservation_db` 데이터베이스)
- Maven — **로컬에 `mvn`이 설치되어 있지 않다면** 아래 "Maven 없이 실행하기" 참고

## 로컬 실행

1. `src/main/resources/application-dev.yml` 생성 (이 파일은 `.gitignore`에 포함되어 있어 커밋되지 않습니다 — 실제 비밀번호를 담기 때문입니다):

   ```yaml
   spring:
     datasource:
       url: jdbc:mysql://localhost:3306/reservation_db?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
       username: root
       password: <본인의 MySQL 비밀번호>
   mybatis:
     configuration:
       map-underscore-to-camel-case: true
     mapper-locations: classpath:mapper/**/*.xml

   app:
     jwt:
       # 32바이트(영문 32자) 이상
       secret: ${JWT_SECRET:dev-only-secret-change-me-0123456789abcdef}
       expiration-ms: 604800000
     cors:
       allowed-origins: "*"
   ```

2. 서버 실행:

   ```bash
   mvn spring-boot:run
   ```

   기본 포트는 `8080`이며, `tansong-mobile`의 `src/config/api.ts`(웹: `localhost:8080`, Android 에뮬레이터: `10.0.2.2:8080`)와 맞춰져 있습니다.

### Maven 없이 실행하기

시스템에 `mvn`이 없고 Eclipse(m2e)로만 빌드해왔다면, 포터블 Maven을 받아서 쓰면 됩니다:

```bash
curl -o maven.zip https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.zip
unzip maven.zip
./apache-maven-3.9.9/bin/mvn spring-boot:run
```

(처음 한 번은 오프라인(`-o`) 없이 실행해야 `spring-boot-maven-plugin` 등 플러그인 전용 의존성을 내려받습니다. 이후로는 캐시되어 오프라인으로도 동작합니다.)

## 환경 변수 (운영/`application-prod.yml`)

운영 프로필은 기본값이 없고 아래 환경 변수가 필수입니다:

| 변수 | 설명 |
|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | MySQL 접속 정보 |
| `JWT_SECRET` | JWT 서명 키 (32자 이상) |
| `CORS_ALLOWED_ORIGINS` | 허용할 프론트엔드 Origin (쉼표로 구분) |

`SPRING_PROFILES_ACTIVE`로 프로필 전환 (기본값: `dev`).

## 테스트 & CI

```bash
mvn test
```

`main` 브랜치에 push/PR 시 GitHub Actions(`.github/workflows/ci.yml`)가 자동으로 다음을 수행합니다:
- 시크릿(비밀번호/키 등) 커밋 여부 검사 (gitleaks)
- 머지 충돌 마커(`<<<<<<<` 등)가 그대로 커밋됐는지 검사
- `mvn clean test`

### 로컬 pre-commit 훅 (커밋 시점에 미리 검사)

CI는 push된 *이후*에 검사합니다. 커밋하는 순간 바로 막으려면, 클론할 때마다 한 번씩:

```bash
git config core.hooksPath .githooks
```

그리고 [gitleaks](https://github.com/gitleaks/gitleaks/releases)를 설치해 PATH에 추가하면, 커밋할 때마다 스테이징된 변경사항을 자동으로 검사합니다. gitleaks가 설치되어 있지 않으면 경고만 출력하고 커밋은 그대로 진행됩니다 (CI가 최종 방어선).

## 주의

- `application-dev.yml`은 절대 커밋하지 마세요 (이미 `.gitignore`에 포함되어 있습니다).
- 로컬 브랜치와 원격 브랜치의 히스토리가 갈라졌을 때 `git pull`/`git merge`로 합치지 마세요 — 과거에 지워진 민감 정보가 다시 섞여 들어올 수 있습니다. `git fetch` 후 `git rebase origin/main`을 사용하세요.
