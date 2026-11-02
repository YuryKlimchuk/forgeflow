# Tasks

## 1. Backend security foundation

- [x] 1.1 Добавить Spring Security OAuth2 Resource Server зависимости и конфигурацию issuer URL для `forgeflow-users` и `forgeflow-admins`; проверить успешную сборку Maven.
- [x] 1.2 Настроить SecurityFilterChain для Bearer JWT и публичные/защищённые маршруты; проверить тестами HTTP 401 для отсутствующего и недействительного токена.
- [x] 1.3 Реализовать выбор JWT decoder по `iss` и mapping issuer в `Realms`; проверить тестами успешную аутентификацию обоих realm и отказ неизвестного issuer.
- [x] 1.4 Реализовать инфраструктурный `AuthenticatedUserProvider` из `SecurityContext`, mapping JWT `sub` в `UserId`; проверить тестом значение `user-123`.
- [x] 1.5 Переключить runtime-конфигурацию с `StubAuthenticatedUserProvider` на Spring Security provider, удалить runtime-использование `MutableSubjectProvider` и stub-пользователя; проверить `ApplicationContextTest` и `ModuleVerificationTest`.

## 2. Организационный lifecycle

- [x] 2.1 Добавить идемпотентную регистрацию `Owner` без организации для нового пользователя realm `forgeflow-users`; проверить unit-тестом отсутствие дубликата при повторном запросе.
- [x] 2.2 Подключить ленивую регистрацию к проверенному authenticated request до разрешения organization context; проверить сценарии нового пользователя без организации и существующего `Owner`.
- [x] 2.3 Обновить тесты `OrganizationContextResolver` и lifecycle для JWT-derived user, включая запрет контекста для `forgeflow-admins`; проверить `organizations` domain purity.

## 3. Keycloak development infrastructure

- [x] 3.1 Добавить `docker-compose.yml` настройки Keycloak с импортом realm-файлов и конфигурацией issuer URL; проверить запуск контейнера и доступность обоих realm.
- [x] 3.2 Создать JSON realm imports для `forgeflow-users` и `forgeflow-admins` с password flow, Google IdP placeholder/env configuration и frontend clients; проверить импорт без repository secrets.
- [x] 3.3 Добавить Testcontainers Keycloak fixture, импортирующий реальные realm-файлы и тестовых пользователей/клиентов; проверить получение JWT из каждого realm.

## 4. Frontend authentication

- [x] 4.1 Добавить `keycloak-js`, конфигурацию двух issuer/client пар и инициализацию Authorization Code + PKCE; проверить frontend unit-тестами выбор соответствующего realm.
- [x] 4.2 Добавить отдельные entry points «войти как пользователь» и «войти в админку»; проверить вызов login с `forgeflow-users` и `forgeflow-admins`.
- [x] 4.3 Обновить общий API fetch wrapper для добавления `Authorization: Bearer` ко всем `/api` запросам и обновления access token; проверить `frontend/src/api.test.ts` для GET и mutating requests.
- [x] 4.4 Добавить обработку logout/session expiry и состояния загрузки входа; проверить, что просроченная сессия не отправляет старый токен.

## 5. Интеграционная проверка

- [x] 5.1 Выполнить интеграционные тесты с реальным Keycloak для user/admin JWT, mapping `sub`, HTTP 401 и issuer rejection; проверить командой Maven test profile.
- [x] 5.2 Проверить сквозной пользовательский flow: login → Bearer API request → разрешение организации или пустой context → cleanup; зафиксировать результат автоматизированным тестом.
- [x] 5.3 Запустить полный backend и frontend test suite и проверить `openspec validate --change "20-keycloak-authentication"` без ошибок.
