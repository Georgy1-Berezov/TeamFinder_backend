# TeamFinder API Documentation

## ?? Авторизация

### Регистрация
`POST /auth/register`
```json
{
  "username": "user",
  "email": "user@example.com",
  "password": "Password123"
}
```

### Вход
`POST /auth/login`
```json
{
  "email": "user@example.com",
  "password": "Password123"
}
```

## ?? Проекты

### Создать проект
`POST /projects/`
```json
{
  "title": "Название проекта",
  "description": "Полное описание",
  "briefDescription": "Краткое описание",
  "stage": "IDEA",
  "tags": ["kotlin", "backend"],
  "neededRoles": [
    {
      "title": "Разработчик",
      "description": "Описание вакансии",
      "requiredSkills": ["Kotlin", "Ktor"]
    }
  ]
}
```

### Получить все проекты
``GET /projects?page=1&limit=20`

### Получить проект по ID
`GET /projects/{id}`

## ?? Лайки

### Поставить/убрать лайк
`POST /projects/{id}/like`

## ?? Комментарии

### Добавить комментарий
`POST /projects/{id}/comments`
```json
{
  "content": "Текст комментария"
}
```

### Получить комментарии
`GET /projects/{id}/comments`

## ?? Пользователи

### Получить профиль
`GET /users/{id}`

### Поиск пользователей
`GET /users/search?query=имя`
