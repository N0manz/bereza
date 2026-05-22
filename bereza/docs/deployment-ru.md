# Деплой на российские серверы и соответствие ФЗ-152

## 1. Где разворачивать

Подходящие провайдеры (хостинг ПДн граждан РФ):

| Провайдер           | Тип        | Особенности                                         |
|---------------------|------------|------------------------------------------------------|
| Yandex Cloud        | IaaS/PaaS  | Managed PostgreSQL, Object Storage S3, Compute Cloud |
| VK Cloud (MCS)      | IaaS/PaaS  | Managed PostgreSQL, k8s, S3-совместимое              |
| Selectel            | IaaS       | Bare-metal + cloud, российские ДЦ                    |
| Cloud.ru (sbercloud)| IaaS/PaaS  | Регулируемый провайдер ПДн                           |
| Rusonyx, Timeweb    | IaaS       | Бюджетные VPS                                        |

Минимальная топология (≤ 10k MAU):

```
[ Yandex Compute VM 4vCPU / 8GB RAM ] — backend + nginx
[ Yandex Managed PostgreSQL Single ]   — БД (бэкапы, репликация — встроены)
[ Yandex Object Storage (RU) ]         — файловое хранилище (через S3-адаптер)
```

Боевая (50k+ MAU): k8s-кластер Yandex Managed K8s + Managed PostgreSQL HA + Redis.

## 2. ФЗ-152: что обязательно

1. **Локализация ПДн.** Все базы данных и файлы лежат в датацентрах РФ
   (PostgreSQL `ru-central1` / Object Storage region `ru-central1`).
   Без CDN-узлов за рубежом для контента с ПДн.
2. **Уведомление в Роскомнадзор.** Подайте уведомление об обработке ПДн
   через [pd.rkn.gov.ru](https://pd.rkn.gov.ru). Категория данных: общие, цели:
   «оказание услуг информационного сервиса».
3. **Политика обработки ПДн** — публичный документ (`/legal/privacy`),
   ссылка с любой формы регистрации.
4. **Согласие на обработку.** Регистрация хранит чекбокс согласия (запишите в `users.consent_at`
   при будущей миграции `V4__consent.sql`). Шаблон согласия — в `legal/consent.md`.
5. **Право на отзыв/удаление.** Endpoint `DELETE /api/users/me` (план — этап 2)
   делает hard-delete или анонимизацию (см. п. 7).
6. **Срок хранения.** Срок обработки персональных данных в политике должен
   совпадать с реальным; настройте джоб anonymization после inactivity > 3 лет.
7. **Анонимизация.** При удалении замените `username/email/phone/displayName`
   на детерминированные хеши; сохраните `id`, чтобы не нарушить FK в чатах
   (сообщения остаются — без идентифицируемой связи).

## 3. Шифрование и сертификаты

- TLS-сертификат — Let's Encrypt (через certbot) **или** аккредитованный УЦ
  Минцифры (для гос-сегмента и интеграций с госуслугами).
- Шифры — TLS 1.2/1.3, поддержка ГОСТ TLS (через CryptoPro CSP) — опционально,
  включается в Nginx через `--with-openssl=/opt/cryptopro/openssl-gost`.

## 4. Бэкапы и DR

- **PostgreSQL.** Managed Postgres делает PITR (point-in-time recovery) 7 дней.
  Дополнительно — еженедельный `pg_basebackup` в Object Storage.
- **Файлы.** Бэкенд пишет в S3 с включённой версионностью; lifecycle-rule —
  удалять «delete-markers» старше 90 дней.
- **Restore-drill.** Раз в квартал восстановите БД на staging и проверьте
  целостность миграций + чек-сумма дамп файлового хранилища.

## 5. Чек-лист перед прод-релизом

- [ ] `.env` сгенерирован, `REMEMBER_ME_KEY` уникален, не лежит в git.
- [ ] `COOKIE_SECURE=true` и сертификат TLS установлен.
- [ ] `ALLOWED_ORIGINS` содержит только домены вашего фронта (без `*`).
- [ ] Spring profile = `prod` (`SPRING_PROFILES_ACTIVE=prod`).
- [ ] Миграция `V3__seed_demo_data.sql` отключена (`-Dflyway.target=2`)
      или демо-пользователи удалены вручную.
- [ ] Брандмауэр пропускает только 80/443 наружу; 5432/8080 — закрыты.
- [ ] `/actuator/*` доступен только из приватной сети (см. `nginx/nginx.conf`).
- [ ] Логи отправляются в централизованный store (ELK / Yandex Cloud Logging).
- [ ] Включены метрики и алерты (CPU > 80% > 10мин, error rate > 1%).

## 6. Команды первичного деплоя

```bash
# на проде:
ssh deploy@prod-host
sudo mkdir -p /opt/bereza && sudo chown deploy:deploy /opt/bereza
cd /opt/bereza
git clone https://gitlab.example.ru/bereza/app.git .
cp .env.example .env && nano .env       # выставить секреты
sudo docker compose pull
sudo docker compose up -d
sudo docker compose logs -f backend     # убедиться, что Flyway отработал
```

## 7. Контроль доступа

- SSH — только по ключу, root-логин запрещён.
- Доступ к PostgreSQL — только из VPC (security group / private subnet).
- `ADMIN`-пользователи создаются вручную через миграцию `Vx__create_admin.sql`,
  публичная регистрация роли `ADMIN` запрещена кодом (`AuthService`).

## 8. Аудит

- Логи входа/выхода — пишутся как INFO в logback (`messenger.bereza.service.AuthService`).
- Будущая миграция: таблица `audit_log` (action, user_id, ip, user_agent, payload).
- ELK или Yandex Cloud Logging хранит логи ≥ 6 месяцев.
