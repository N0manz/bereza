# CI/CD

## Принципы

1. **Trunk-based development.** `main` всегда зелёный, релиз — git-tag.
2. **Каждый PR — собственный pipeline:** lint → test → build → security scan.
3. **Артефакты идентичны** для всех окружений (один docker-образ, разные env).
4. **Откат — мгновенный** (предыдущий образ остаётся в registry).

## Пример GitHub Actions

`.github/workflows/ci.yml`:

```yaml
name: ci
on:
  push:
    branches: [main]
  pull_request:

jobs:
  backend:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:16-alpine
        env:
          POSTGRES_DB: bereza
          POSTGRES_USER: bereza
          POSTGRES_PASSWORD: bereza
        ports: ['5432:5432']
        options: >-
          --health-cmd="pg_isready -U bereza"
          --health-interval=5s
          --health-timeout=3s
          --health-retries=10
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: '21', cache: maven }
      - run: ./mvnw -B verify
      - uses: actions/upload-artifact@v4
        with: { name: bereza-jar, path: target/bereza.jar }

  frontend:
    runs-on: ubuntu-latest
    defaults: { run: { working-directory: frontend } }
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with: { node-version: '20', cache: npm, cache-dependency-path: frontend/package-lock.json }
      - run: npm ci
      - run: npm run build
      - uses: actions/upload-artifact@v4
        with: { name: bereza-frontend, path: frontend/dist }

  docker:
    needs: [backend, frontend]
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: docker/setup-buildx-action@v3
      - uses: docker/login-action@v3
        with:
          registry: ${{ secrets.REGISTRY_URL }}
          username: ${{ secrets.REGISTRY_USER }}
          password: ${{ secrets.REGISTRY_PASSWORD }}
      - name: Build & push backend
        uses: docker/build-push-action@v6
        with:
          context: .
          push: true
          tags: ${{ secrets.REGISTRY_URL }}/bereza/backend:${{ github.sha }}
      - name: Build & push frontend
        uses: docker/build-push-action@v6
        with:
          context: ./frontend
          push: true
          tags: ${{ secrets.REGISTRY_URL }}/bereza/frontend:${{ github.sha }}

  deploy:
    needs: docker
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    steps:
      - name: SSH deploy
        uses: appleboy/ssh-action@v1
        with:
          host: ${{ secrets.PROD_HOST }}
          username: ${{ secrets.PROD_USER }}
          key: ${{ secrets.PROD_SSH_KEY }}
          script: |
            cd /opt/bereza
            sed -i "s|backend:.*|backend: ${{ secrets.REGISTRY_URL }}/bereza/backend:${{ github.sha }}|" docker-compose.yml
            sed -i "s|frontend:.*|frontend: ${{ secrets.REGISTRY_URL }}/bereza/frontend:${{ github.sha }}|" docker-compose.yml
            docker compose pull
            docker compose up -d --remove-orphans
```

## GitLab CI вариант (для приватного gitlab.yandexcloud)

```yaml
stages: [test, build, deploy]
backend-test:
  stage: test
  image: maven:3.9.9-eclipse-temurin-21
  services: [postgres:16-alpine]
  variables:
    POSTGRES_DB: bereza
    POSTGRES_USER: bereza
    POSTGRES_PASSWORD: bereza
    DB_URL: jdbc:postgresql://postgres:5432/bereza
  script: mvn -B verify
```

## Security

- **OWASP Dependency-Check / Snyk** — на стадии test.
- **Trivy** для образов: `trivy image bereza/backend:<sha>`.
- **Gitleaks** на pre-commit и в CI — ловит секреты в diff.

## Гайдлайны

- Миграции **только forward**. Откат — отдельная миграция `Vn__revert_xxx.sql`.
- Образ имеет фиксированную версию (sha + semver tag), `latest` не используется в проде.
- Конфиги — через env, никаких секретов в репозитории.
- Перед `docker compose up -d` в проде — `docker compose pull` (атомарность скачивания).
