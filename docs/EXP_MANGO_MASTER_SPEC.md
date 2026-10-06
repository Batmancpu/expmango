
# EXP Mango Master Product Specification

## Goal
Build a focused Android keyboard that is unmistakably EXP Mango rather than a renamed WM Keyboard build.

Priorities:
1. English and Indian spoken-English/Hinglish typing quality;
2. local adaptive prediction;
3. low latency;
4. field-aware behavior;
5. Gmail/domain suggestions;
6. polished iOS-27-inspired interaction quality;
7. strict offline privacy.

## Prediction engine
Use layered vocabularies:
- bundled English frequency list;
- app custom vocabulary;
- locally learned words;
- user-imported local words.

Preserve the existing repository files:
- app/src/main/assets/dictionary.txt
- app/src/main/assets/hinglish.txt

Candidate generation should use indexes, not a full dictionary scan:
- prefix trie;
- normalized lookup;
- phonetic key;
- keyboard-neighbor substitutions;
- common typo rules.

Handle:
- omission;
- insertion;
- substitution;
- transposition;
- duplication;
- repeated spaces;
- phonetic spelling;
- Indian-English spelling variation.

Context model:
- unigram;
- bigram;
- trigram;
- sentence boundary;
- casing;
- field type;
- accepted/rejected history.

Suggested conceptual ranking:

finalScore =
0.35 context
+ 0.20 personalFrequency
+ 0.15 globalFrequency
+ 0.10 recency
+ 0.10 typoSimilarity
+ 0.05 keyboardProximity
+ 0.05 fieldPrior
- correctionRisk

These are starting weights, not permanent constants.

Autocorrect must be confidence gated. Intentional slang and repeatedly accepted personal spellings should become less likely to be corrected.

## Learning
Use a bounded local learning queue.

Events:
WORD_COMMITTED
WORD_LEARNED
SUGGESTION_ACCEPTED
SUGGESTION_REJECTED
AUTOCORRECT_ACCEPTED
AUTOCORRECT_UNDONE

Persist updates in background batches.

Use:
- frequency counters;
- timestamps;
- decay;
- bounded sizes;
- compaction;
- corruption recovery;
- reset learned data.

No learning in:
- password;
- PIN;
- OTP;
- secure;
- Incognito.

## Field policies
Centralize behavior in one resolver.

NORMAL:
prediction, autocorrect, learning.

EMAIL:
email tokenizer, domain suggestions, local domain learning, Gmail priority, no domain autocorrect.

URI:
conservative mode.

PASSWORD/PIN/OTP:
no learning, no suggestions, no autocorrect, no sensitive history.

CODE/TERMINAL:
conservative mode, preserve identifiers and punctuation.

INCognito:
privacy behavior identical to secure mode.

PER-APP:
allow stored per-app mode and always-Incognito option.

## Email domain provider
Built-in domains:
gmail.com
outlook.com
yahoo.com
icloud.com
proton.me
protonmail.com
hotmail.com

Examples:
rahul@ -> gmail.com first
rahul@gma -> gmail.com
learned company.in becomes higher-ranked if used repeatedly

No network calls.

## Visual design
Create an original visual system inspired by the clean, rounded, translucent and responsive qualities of modern iPhone keyboards, including iOS 27.

Do not copy Apple source, artwork, screenshots or trademarks.

Keys:
- rounded/squircle geometry;
- subtle pressed feedback;
- tiny scale/opacity transition;
- consistent height and spacing;
- restrained shadows/highlights;
- light/dark palettes;
- optional low-cost translucency/blur.

Suggestion strip:
- 3 to 5 items;
- strong primary candidate;
- autocorrect and undo state;
- email domain chips;
- Incognito lock state;
- compact height;
- no unrelated tool clutter.

Toolbar:
emoji, language, clipboard, settings, and at most one user shortcut.

Remove AI/GIF/sticker/search controls.

## Offline rules
Final APK must contain no INTERNET permission.

Remove production access to:
- cloud AI;
- LLM APIs;
- GIF services;
- sticker services;
- web/image search;
- translation services;
- weather/currency APIs;
- remote dictionaries;
- online update services;
- remote addon repositories.

Do not just hide buttons. Remove/exclude production code and dependencies.

Create CI checks for:
- INTERNET permission;
- forbidden networking dependencies;
- API URL/key constants.

## Product identity
Every user-facing location should say EXP Mango:
- launcher/settings;
- IME picker;
- About;
- version;
- screenshots;
- release title;
- README.

Do not use WM Keyboard as the product name.

Retain an Open Source Licenses/Credits screen:
- WM Keyboard / Wasi Master, MIT License;
- any additional third-party libraries actually shipped.

## Source architecture
Prefer a dedicated EXP module layer rather than rewriting stable input plumbing.

Recommended logical modules:
- prediction core;
- dictionary/index;
- learning;
- field policy;
- email;
- theme/UI;
- privacy;
- runtime coordinator.

Keep algorithmic components JVM-testable wherever possible.

## Performance
No expensive work on the typing hot path.

Background tasks must be cancellable and bounded.

Measure:
- prefix retrieval;
- typo generation;
- contextual ranking;
- learning ingestion;
- cold-start latency.

Use benchmarks where practical.

## Test matrix
Unit:
- prefix retrieval;
- typo correction;
- bigram/trigram ranking;
- learning;
- decay;
- Incognito;
- secure fields;
- email domains;
- URL policy;
- Hinglish;
- dictionary loading;
- storage recovery.

Manual:
- English;
- Indian English;
- Hinglish;
- slang;
- fast typo-heavy typing;
- email;
- URL;
- password;
- OTP;
- code editor;
- Incognito;
- forced Incognito per app;
- airplane mode;
- reboot;
- update installation.

## Release
One workflow.

Final release must:
- use stable signing key;
- preserve com.mangoloads.expmango;
- verify certificate;
- publish EXP-Mango.apk;
- include checksum;
- contain no debug artifacts.

A debug APK is acceptable only as a development artifact and must never be called the official release.

## Definition of done
The APK should feel like a new keyboard product.

A rebranded WM Keyboard with unchanged prediction, unchanged UI, unchanged toolbar and unchanged toolset is not an acceptable final milestone.
