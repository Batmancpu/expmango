#!/usr/bin/env bash
set -euo pipefail

export EXP_MANGO_VERSION_CODE="${GITHUB_RUN_NUMBER:-1}"
export EXP_MANGO_VERSION_NAME="1.0.${EXP_MANGO_VERSION_CODE}"

python3 - <<'PY'
from pathlib import Path

app = Path("wmkeyboard/app/build.gradle.kts")
text = app.read_text()
text = text.replace(
    'applicationId = "com.wasimaster.wmkeyboard"',
    'applicationId = "com.mangoloads.expmango"',
    1,
)
app.write_text(text)

strings = Path("wmkeyboard/app/src/main/res/values/strings.xml")
text = strings.read_text()
text = text.replace(
    '<string name="app_name">WM Keyboard</string>',
    '<string name="app_name">EXP Mango</string>',
    1,
)
text = text.replace(
    '<string name="app_name_short" translatable="false">WMK</string>',
    '<string name="app_name_short" translatable="false">EXP Mango</string>',
    1,
)
strings.write_text(text)

props = Path("wmkeyboard/gradle.properties")
lines = []
for line in props.read_text().splitlines():
    if line.startswith("wmkb.versionName="):
        line = "wmkb.versionName=" + __import__("os").environ["EXP_MANGO_VERSION_NAME"]
    elif line.startswith("wmkb.versionCode="):
        line = "wmkb.versionCode=" + __import__("os").environ["EXP_MANGO_VERSION_CODE"]
    lines.append(line)
props.write_text("\n".join(lines) + "\n")

dictionary = Path("wmkeyboard/app/dictionaries-src/en.txt")
existing = dictionary.read_text().splitlines()
seen = set()
for line in existing:
    if line and not line.lstrip().startswith("#"):
        seen.add(line.split()[0].lower())

custom_sources = [
    Path("app/src/main/assets/dictionary.txt"),
    Path("app/src/main/assets/hinglish.txt"),
]
extra = []
for source in custom_sources:
    for raw in source.read_text().splitlines():
        word = raw.strip().lower()
        if not word or word.startswith("#") or any(ch.isspace() for ch in word):
            continue
        if word not in seen:
            seen.add(word)
            extra.append(f"{word} 5000")

if extra:
    with dictionary.open("a") as out:
        out.write("\n# EXP Mango custom English + Hinglish vocabulary\n")
        out.write("\n".join(extra))
        out.write("\n")

# The pinned WM upstream commit currently has a one-line class-brace regression
# around phoneticCandidateListFor(). Repair it in the build workspace only so
# we can build against the pinned source without maintaining a forked submodule.
settings = Path("wmkeyboard/core/settings/src/main/java/com/wasimaster/wmkeyboard/core/settings/SettingsRepository.kt")
s = settings.read_text()
broken = """    fun wordPairsEnabledFor(langId: String): Boolean = langId !in wordPairsOffLangs
}
    /** Where [langId]'s phonetic layout shows its candidate list; OFF for no phonetic layout. */
    fun phoneticCandidateListFor(langId: String?): PhoneticCandidateList =
        langId?.let { phoneticCandidateLists[it] } ?: PhoneticCandidateList.OFF


"""
fixed = """    fun wordPairsEnabledFor(langId: String): Boolean = langId !in wordPairsOffLangs

    /** Where [langId]'s phonetic layout shows its candidate list; OFF for no phonetic layout. */
    fun phoneticCandidateListFor(langId: String?): PhoneticCandidateList =
        langId?.let { phoneticCandidateLists[it] } ?: PhoneticCandidateList.OFF

}

"""
if broken in s:
    settings.write_text(s.replace(broken, fixed, 1))
PY
