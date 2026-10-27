[English](readme.md) | Русский

## CISTierTagger
Официальный мод отображения тиров для [CISTiers](https://cistiers.com)

### О модификации
CISTierTagger - мод для отображения тира игрока в разных режимах игры на основе рейтинга [cistiers.com](https://cistiers.com) прямо в игре.

<p align="center"><img src="docs/assets/cistiertagger.png" width="100%"></p>

### Конфигурация
Отображение можно настроить, открыв конфигурацию мода через `ModMenu` или командой `/cistier`.

<p align="center"><img src="docs/assets/config.png" width="100%"></p>
<p align="center">
  <img src="docs/assets/config_gamemodes.png" width="49%">
  <img src="docs/assets/config_priority.png" width="49%">
</p>

### Лидерборд и профили
Мод также позволяет искать игроков в рейтинге через поле поиска в меню `/cistier` или открытием профиля конкретного игрока командой `/cistier <ник>`.

<p align="center">
  <img src="docs/assets/search.png" width="49%">
  <img src="docs/assets/profile.png" width="49%">
</p>

### Возможности

- Бейдж тира в неймтеге, таб-листе и чате - каждый переключается независимо.
- Отображаемый тир выбирается по порядку приоритета или по самому высокому тиру игрока среди всех режимов.
- Переключатели для каждого режима из рейтинга [cistiers.com](https://cistiers.com).
- Экран поиска с превью - `/cistier <ник>` открывает полный профиль (ранг, очки, тиры, история).
- Серверы могут скрыть бейджи через plugin message - см. [restriction.md](docs/restriction_ru.md).

### Сборка

Все поддерживаемые версии Minecraft используют одну ветку благодаря [Stonecutter](https://stonecutter.kikugie.dev/).
Версии сборки и версии зависимостей заданы в [stonecutter.properties.toml](stonecutter.properties.toml).

Собрать все версии:
```sh
./gradlew clean build
```

Собрать одну или несколько заданных версий:
```sh
./gradlew :26.2:build
./gradlew :1.20.1:build :26.2:build
```

Запустить клиент конкретной версии:
```sh
./gradlew :26.2:runClient
```

Запустить клиенты нескольких версий параллельно:
```sh
./gradlew :1.20.1:runClient :26.2:runClient --parallel
```

Готовые JAR находятся в `versions/<minecraft>/build/libs/`, данные клиентов - в `runs/<minecraft>/`.

### Лицензия
Проект распространяется под лицензией [GPL-3.0](license).
