# Obligator — Скринер облигаций Московской биржи (Kotlin Multiplatform)

Мобильное приложение для анализа рублевых облигаций MOEX на базе Kotlin Multiplatform (Android & iOS).

## Архитектура проекта

- `analytics/`: Модуль domain (модели `Bond`, `Payment`, `PortfolioPosition`, `FavoriteItem`, `BondFilter`, интерфейсы репозиториев и функции расчетов). Чистый Kotlin Multiplatform без Compose/Ktor/Room.
- `shared/`: Модуль data & presentation (клиент MOEX ISS API, база данных Room 3, репозитории, ViewModels, Compose UI экраны, Koin DI).
- `androidApp/`: Модуль Android Activity & Application.
- `iosApp/`: Xcode-проект приложения iOS.

## Сборка и запуск

### Требования
- JDK 17+
- Android Studio / IntelliJ IDEA
- Xcode (для запуска iOS приложения)

### Сборка Android
```bash
./gradlew :androidApp:assembleDebug
```

### Запуск iOS
Откройте проект `iosApp/iosApp.xcodeproj` в Xcode и выберите нужный симулятор iOS или устройство.

### Запуск тестов
```bash
./gradlew :analytics:allTests :shared:allTests
```

## Функционал
- **Скринер:** Поиск по названию/ISIN/SECID, фильтрация по группе эмитента, доходности, сроку, типу купона и уровню листинга, сортировка.
- **Карточка выпуска:** Параметры облигации, доходности, график будущих выплат, флаги (амортизация, плавающий купон, оферта, квал).
- **Сравнение:** Сравнительная таблица показателей для 2–3 выбранных выпусков.
- **Избранное:** Сохранение интересующих бумаг на устройстве.
- **Портфель и купонный календарь:** Учет количества облигаций, расчет стоимости портфеля и ежемесячный календарь выплат.
