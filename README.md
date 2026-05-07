# TeamFinder Backend

## ?? Требования
- Java 17+
- PostgreSQL 14+

## ?? Установка PostgreSQL
1. Скачайте с https://www.postgresql.org/download/windows/
2. Установите, запомните пароль для пользователя postgres
3. Создайте базу данных:
   psql -U postgres -c "CREATE DATABASE teamfinder;"

## ?? Настройка
Отредактируйте файл backend/src/main/resources/application.conf
Укажите свой пароль от PostgreSQL в поле password.

## ?? Запуск
1. Откройте CMD в папке TeamFinder-export
2. Соберите проект:
   gradlew clean build
3. Запустите сервер:
   gradlew :backend:run

## ? Проверка
Откройте браузер: http://localhost:8080/health
Должен вернуться JSON: {"status":"ok"}

## ?? API Документация
Все эндпоинты описаны в файле API.md
