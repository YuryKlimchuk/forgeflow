# ForgeFlow — фронтенд (React)

Интерфейс каталога деталей. Общается с Java-бэкендом через REST `/api/v1/parts`.

## Команды

```bash
npm install      # установить зависимости
npm run dev      # dev-сервер (http://localhost:5173), /api проксируется на :8080
npm run test     # прогон Vitest
npm run build    # типизация + продакшн-сборка
npm run preview  # предпросмотр собранного бандла
```

## Связка с бэкендом

1. Подними бэкенд: `cd .. && ./mvnw spring-boot:run` (порт 8080).
2. В отдельном терминале: `npm run dev`.
3. Открой `http://localhost:5173`.

Запросы `/api/*` проксируются Vite на `http://localhost:8080`, поэтому CORS не нужен.

## Структура

```
src/
├── api.ts            — типы DTO, fetch-клиент, нормализация ошибок
├── App.tsx           — корневой экран (состояние, загрузка, ошибки)
├── PartsTable.tsx    — таблица деталей (rename/delete)
├── CreatePartForm.tsx — форма создания
├── App.css           — стили (RAL 7024/7032, светлая тема)
└── App.test.tsx      — тесты рендера/формы/ошибок (Vitest + RTL)
```
