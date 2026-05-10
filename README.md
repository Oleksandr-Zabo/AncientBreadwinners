# Ancient Breadwinners

Симуляція життя українських хліборобів. JavaFX додаток з мікро-об'єктами (хлібороби) та макро-об'єктами (церква, млин, пшеничне поле).

## Особливості

- Текстовий формат збереження (.txt)
- Білі підписи для кращої читабельності на фоні гри
- Діалогове вікно керування з усіма клавішами
- Оновлене меню: Файл, Редагувати, Керування, Вікна, Про гру

## Architecture

### Micro-objects (Farmers)

Abstract base class `Farmer` implements `Cloneable` and `Comparable<Farmer>`:

| Property | Type | Description |
|----------|------|-------------|
| `name` | `String` | Farmer's name |
| `motivation` | `int` | Motivation level (0-100) |
| `speed` | `double` | Movement speed |
| `maxLoad` | `int` | Maximum load capacity |
| `tool` | `Tool` | Tool (reference type for deep cloning) |
| `x`, `y` | `double` | Position coordinates |
| `active` | `boolean` | Active state |

**Implementations:**
- `Gardener` — gardener with knife
- `FreePeasant` — free peasant
- `MasterFarmer` — master farmer with scythe

### Macro-objects (MacroObject)

Abstract class `MacroObject` contains:

| Property | Type | Description |
|----------|------|-------------|
| `name` | `String` | Macro-object name |
| `imageAsset` | `String` | Image path |
| `x`, `y` | `double` | Coordinates |
| `width`, `height` | `double` | Dimensions |
| `members` | `List<Farmer>` | List of members |

**Concrete implementations:**
- `Church` — church (red border)
- `Mill` — mill (gray border)
- `WheatField` — wheat field (turquoise border)

## Micro-Macro Interaction

### 1. Entering Macro-objects

**Unselected (inactive) farmers:**
- Freely enter macro-object on touch
- Teleport to free position with spacing within macro
- Freely exit when leaving macro bounds

**Selected (active) farmers:**
- Enter only via `Enter` key on collision
- Positioned with 20px spacing within macro-object
- Exit only via `Q` key (ejection)

### 2. Visual Membership Indication

Active micro-object border color depends on macro-object:
- `ORANGE` — belongs to no macro-object
- `RED` — belongs to `Church`
- `GRAY` — belongs to `Mill`
- `TURQUOISE` — belongs to `WheatField`

### 3. Exiting Macro-objects

**Unselected farmers:**
- Freely exit when leaving macro-object bounds (automatic)

**Selected farmers:**
- Exit only on `Q` key press
- Ejected outside macro-object bounds (`ejectFromMacro()`)

### 4. Hotkeys for Macro-object Interaction

| Key | Action |
|-----|--------|
| `Enter` | Enter macro-object (selected, on collision) |
| `Q` | Exit macro-object (selected) |
| `M` | List members in `Mill` |
| `E` | List members in `WheatField` |
| `J` | List members in `Church` |

### 5. Membership Management via Menu

- **Edit → Detach from macro-object** — removes from `members` list
- When deleting a farmer — automatically removed from all macro-objects

## Збереження гри

Гра зберігається у текстовий формат (.txt):
- Позиція камери (cameraX, cameraY)
- Кількість монет
- Макро-об'єкти (тип, x, y)
- Хлібороби (тип, ім'я, x, y, мотивація, швидкість, вантаж, інструмент, поточний вантаж)

## Структура меню

| Меню | Опис |
|------|------|
| Файл | Створити, Зберегти (Ctrl+S), Завантажити (Ctrl+O), Вихід |
| Редагувати | Копіювати (Ctrl+C), Видалити (Delete), Скасувати (Esc), Вилучити з макро |
| Керування | Діалогове вікно з усіма клавішами |
| Вікна | Всі діалоги: створення, редагування, покупка, пошук, списки, статистика, сортування |
| Про гру | Інформація про гру |

## Implemented Behavior

- Active and inactive micro-objects are visually different (border)
- Left mouse button toggles active state
- Multiple selection of micro-objects is supported
- Rendering includes reference state (`Tool`) for deep cloning
- `Ctrl+C` clones all active micro-objects with unique names (`_copyN`)
- Status line shows names and count of active objects
- Arrow keys move active micro-objects
- `Insert` opens create dialog with `TextField`, `CheckBox`, `ListView`, `RadioButton`
- Right click opens edit dialog
- `Delete` removes active micro-objects
- `Esc` clears selection
- Macro-objects show member count inside
- Three macro-objects added at startup
- **Unselected farmers**: freely enter/exit macro-objects (teleport into macro on touch)
- **Selected farmers**: enter via `Enter`, exit via `Q`
- Maximum 4 farmers per macro-object (2x2 grid with spacing)
- If macro is full, new farmers wait outside (queue system)
- Farmers positioned with spacing within macro-object bounds
- On `U` upgrade farmer type (tool remains unchanged)
- Tool: can only buy more expensive models (NoTool → Knife → Sickle/Scythe → GoldenScythe)
- After upgrade, new tool models become available in buy menu (I)
- Church positioned lower, wheat field higher on the map

## Controls

| Key/Action | Result |
|------------|--------|
| Left click on micro-object | Activate/deactivate |
| Right click on micro-object | Edit |
| `Ctrl+C` | Clone active |
| Arrow keys | Move active |
| `Enter` | Enter macro-object (selected, on collision) |
| `Q` | Exit macro-object (selected) |
| `M` | List in mill |
| `E` | List in wheat field |
| `J` | List in church |
| `Insert` | Create micro-object |
| `Delete` | Delete active |
| `Esc` | Cancel selection |

## Build and Run

Use Maven wrapper:

```powershell
.\mvnw.cmd -DskipTests clean compile
.\mvnw.cmd javafx:run
```

If Maven reports `JAVA_HOME not found`, first configure JDK path:

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-26"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd -DskipTests clean compile
```

