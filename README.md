# Lumen Addons (Fabric 1.21.11)

Отдельный мод-аддон к Lumen Visuals: Item swap, Trajectory, Shulker preview, Weapon trails, Custom hand, Low fire, Saturation HUD, Hit range, Inventory HUD.
Модули появляются в меню Lumen (Utility / Visuals), сохраняются в lumen.json, у каждого есть бинд вкл/выкл.

## Сборка
1. Нужен JDK 21 и интернет.
2. Открой https://fabricmc.net/develop, выбери 1.21.11 и впиши актуальные
   `yarn_mappings` и `fabric_api_version` в gradle.properties.
3. В папке проекта: `gradle wrapper --gradle-version 9.2.1`, затем `./gradlew build`
   (на Windows: `gradlew.bat build`). Либо просто открой папку в IntelliJ IDEA.
4. Готовый файл: build/libs/lumen-addons-1.0.0.jar — кидай в mods рядом с lumen-visuals и Fabric API.

## Управление
Настройки → Управление → «Lumen Addons» → назначь клавишу «Item swap: swap now».
Остальное — в меню Lumen.

## Если сборка ругается
Код написан по Yarn-именам 1.21.11 по памяти, без компиляции. Самые вероятные места:
- ItemSwap: `inv.getSelectedSlot()`
- ShulkerPreview: `ContainerComponent.copyTo`, `ctx.drawStackOverlay`
- Trajectory: `ChargedProjectilesComponent.contains/isEmpty`, `tick.getTickProgress(false)`
- LumenAddons: `KeyBinding.Category.create`
Пришли текст ошибки — поправлю.

## Сборка без установки Java (GitHub Actions)
Загрузи проект в репозиторий GitHub (вместе с папкой .github). Вкладка Actions → последний запуск → Artifacts → lumen-addons.

## Новые модули (визуал)
Custom hand (минимальный) + Hand editor, Hit color, Hit particles, Crit particles, Hitbox customizer,
Kill effect, Block overlay, China hat, Body glow, Trails, World particles, Arrow nametag, Mace helper, Totem tracker.
Все они по умолчанию выключены. Эффекты рисуются поверх экрана (без шейдеров).

## Конфиги
`/lumencfg download <ссылка> [имя]` — скачать конфиг (https, github blob и pastebin конвертируются сами).
`/lumencfg paste [имя]` — конфиг из буфера (ссылка или JSON), `/lumencfg export` — скопировать свой конфиг в буфер,
`/lumencfg list`, `/lumencfg load <имя>`. Скачанное попадает в config/lumen и видно во вкладке «Конфиги».
