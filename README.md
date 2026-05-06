# AncientBreadwinners

JavaFX project with micro-objects (farmers) and macro-objects (church, mill, wheat field).

## Implemented behavior

- Active and inactive micro-objects are visually different.
- Left mouse click toggles active state.
- Multiple active micro-objects are supported.
- Micro-object rendering includes reference-type state (`Tool`) for deep cloning.
- `Ctrl+C` clones all active micro-objects.
- Status line shows active object names and count.
- Arrow keys move active micro-objects.
- `Insert` opens create dialog with `TextField`, `CheckBox`, `ComboBox`, `RadioButton`, and `Button`.
- Right click opens edit dialog.
- `Delete` removes active micro-objects.
- `Esc` clears active selection.
- Macro-objects use multiple primitives and text, and show member count.
- Three macro-objects are added at startup.
- Membership can be assigned or removed via top controls.

## Controls

- Left mouse button on micro-object: activate/deactivate
- Right mouse button on micro-object: edit
- `Ctrl+C`: clone active
- Arrow keys: move active
- `Insert`: create micro-object
- `Delete`: remove active
- `Esc`: clear selection

## Build and run

Use Maven wrapper.

```powershell
.\mvnw.cmd -DskipTests clean compile
.\mvnw.cmd javafx:run
```

If Maven reports `JAVA_HOME not found`, configure JDK path first.

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-26"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd -DskipTests clean compile
```

