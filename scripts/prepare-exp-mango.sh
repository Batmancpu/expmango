#!/usr/bin/env bash
set -euo pipefail

python3 - <<'PY'
from pathlib import Path

app = Path("wmkeyboard/app/build.gradle.kts")
text = app.read_text()
text = text.replace('applicationId = "com.wasimaster.wmkeyboard"', 'applicationId = "com.mangoloads.expmango"', 1)
app.write_text(text)

strings = Path("wmkeyboard/app/src/main/res/values/strings.xml")
text = strings.read_text()
text = text.replace('<string name="app_name">WM Keyboard</string>', '<string name="app_name">EXP Mango</string>', 1)
text = text.replace('<string name="app_name_short" translatable="false">WMK</string>', '<string name="app_name_short" translatable="false">EXP Mango</string>', 1)
strings.write_text(text)

props = Path("wmkeyboard/gradle.properties")
text = props.read_text()
text = text.replace("wmkb.versionName=0.5.13", "wmkb.versionName=1.0.0", 1)
text = text.replace("wmkb.versionCode=29", "wmkb.versionCode=1", 1)
props.write_text(text)
PY
