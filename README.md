## JCooldowns

**Ядро**: Paper/Spigot  
**Версия**: 1.16.5 - 1.21.11  
**Java**: 16+  
**Зависимости**: LuckPerms  

### Основные возможности

- Установка кулдаунов на команды для конкретных групп
- Поддержка форматов времени: `10s`, `5m`, `2h`, `3d`
- Алиасы команд (например `/cmi home` → кулдаун от `/home`)
- Уведомления о кулдауне: чат, title, actionbar, bossbar, звук
- Сохранение активных кулдаунов игроков между перезапусками (`data.yml`)
- Команды администрирования: установка, удаление, просмотр, перезагрузка
- Полная поддержка HEX-цветов (`&#RRGGBB`) и простых градиентов (`<#RRGGBB>текст</#AABBCC>`)

### Установка
1. Скачай `.jar` плагина
2. Положи в папку `plugins/`
3. Перезапусти сервер
4. Настрой `config.yml`

### Команды
> Все команды начинаются с `/jcd` и требуют права `jcooldowns.admin`

| Команда                               | Описание                                  | Пример                  |
|---------------------------------------|-------------------------------------------|-------------------------|
| `/jcd reload`                         | Перезагрузить конфигурацию                | `/jcd reload`           |
| `/jcd list`                           | Показать список всех кулдаунов по группам | `/jcd list`             |
| `/jcd set <команда> <группа> <время>` | Установить кулдаун для команды в группе   | `/jcd set home vip 30m` |
| `/jcd unset <команда> <группа>`       | Удалить кулдаун для команды в группе      | `/jcd unset home vip`   |
| `/jcd` (без аргументов)               | Показать помощь по командам               | `/jcd`                  |

### Права (permissions)

| Право                        | Описание                                      | По умолчанию |
|------------------------------|-----------------------------------------------|--------------|
| `jcooldowns.admin`           | Доступ ко всем командам `/jcd`                | op           |
| `jcooldowns.bypass`          | Игнорировать все кулдауны (обход)             | false        |

### Конфигурация (config.yml)
```yaml
# Сообщения плагина
messages:
  no_permission: "&cНет прав."
  reload_success: "&aКонфигурация перезагружена."
  invalid_time: "&cНеверный формат времени. Примеры: 10s, 5m, 2h, 3d"
  set_success: "&aКулдаун для &e%command% &aв группе &e%group% &aустановлен на &f%time%"
  unset_success: "&aКулдаун для &e%command% &aв группе &e%group% &aудалён"
  unset_not_found: "&cВ группе &e%group% &cнет кулдауна для команды &e%command%"
  help:
    - ""
    - "  &eПомощь по командам"
    - ""
    - "  &6/jcd set <команда> <группа> <время> &7- установить кулдаун"
    - "  &6/jcd unset <команда> <группа> &7- удалить кулдаун"
    - "  &6/jcd list &7- список кулдаунов"
    - "  &6/jcd reload &7- перезагрузить конфиг"
    - "  "

# Отправка сообщения задержки по центру екрана
cooldown_title:
  enabled: false
  title: "&cЗадержка!"
  subtitle: "&fИспользовать &c/%command% &fможно через: &c%cooldown_timer%"
  display_time: 5s

# Отправка сообщения задержки в екшнбар
cooldown_actionbar:
  enabled: false
  message: "&cЗадержка! &fИспользовать &c/%command% &fможно через: &c%cooldown_timer%"
  display_time: 5s

# Отправка сообщения задержки в боссбар
cooldown_bossbar:
  enabled: false
  bossbar:
    color: "RED"
    style: "SOLID"
    message: "&cЗадержка! &fИспользовать &c/%command% &fможно через: &c%cooldown_timer%"
    display_time: 5s

# Отправка сообщения задержки в чат
cooldown_chat:
  enabled: true
  message:
    - ""
    - "  &cЗадержка!"
    - "  &fИспользовать &c/%command% &fможно через: &c%cooldown_timer%"
    - ""

# Звук при задержке использования команды
cooldown_sound:
  enabled: true
  sound: BLOCK_ANVIL_PLACE
  volume: 1.0
  pitch: 1.5

# Формат отображения списка кулдаунов по группам и их времени
cooldowns_list:
  header:
    - ""
    - "  &6Список установленных кулдаунов"
    - ""
  group: "  &6%group%:"
  commands: "    &6- &e%command% &7: &f%cooldown_time%"
  empty: "  &cНет установленных задержек"
  empty-groups: "  &cНет ни одной группы с кулдаунами"
  footer:
    - ""
    - "  &fПраво &ejcooldowns.bypass &fпозволяет"
    - "  &fобойти задержку на использование команд"
    - ""

# Алиасы для команд
# Назначение: объединяет разные варианты одной команды под один кулдаун
aliases:
  "cmi home": "home"
  "essentials:home": "home"
  "cmi warp": "warp"
  "essentials:warp": "warp"
```
### Пример сохранения задержек в cooldowns.yml
```yaml
cooldowns:
  default:
    - home:10m
    - tpa:30s
  vip:
    - home:5m
    - tpa:15s
    - fly:1h
  premium:
    - home:2m
    - fly:30m
```
### Рекомендации
- После любого изменения в `cooldowns.yml` выполняй `/jcd reload`
- Если используешь плагины с алиасами (CMI, Essentials и т.д.) — обязательно добавляй их в раздел `aliases:` в `config.yml`

<img width="922" height="499" alt="Screenshot_2" src="https://github.com/user-attachments/assets/b72b7c42-2f7b-4167-8377-e47299156dfe" />
<img width="828" height="337" alt="Screenshot_4" src="https://github.com/user-attachments/assets/cd13a93e-2eb0-485f-97fd-3847dd4b12c8" />
<img width="1081" height="358" alt="Screenshot_5" src="https://github.com/user-attachments/assets/852aed92-0d88-415b-8d73-67de4eb3a7bc" />
<img width="1025" height="198" alt="Screenshot_7" src="https://github.com/user-attachments/assets/d1c25ed9-3017-42bc-a5f4-49d0d2256fcc" />
<img width="1919" height="1007" alt="Screenshot_8" src="https://github.com/user-attachments/assets/e1353196-f21c-4910-8ea8-dfba69db39d0" />
<img width="708" height="111" alt="Screenshot_3" src="https://github.com/user-attachments/assets/d4b9ea68-8a4d-41a2-8b5c-91a49a5152a4" />
<img width="563" height="106" alt="Screenshot_1" src="https://github.com/user-attachments/assets/f49ceb5e-1b49-495c-bb56-c431e03c1f2b" />

