
# EXP Mango — Jules Engineering Contract

## Product identity
Repository: Batmancpu/expmango
Product: EXP Mango
Application ID: com.mangoloads.expmango
Product concept/direction: Batmancpu

EXP Mango is an independent keyboard product built on permissively licensed upstream components. WM Keyboard is MIT licensed. Preserve the upstream copyright/license notice and add clear attribution, but make EXP Mango the user-facing identity.

Do not copy WM logos, banners, screenshots, marketing text, or branding.

## Source ownership
The current project uses WM Keyboard as a git submodule. For substantial product work, do not leave the important implementation trapped inside an immutable submodule pointer.

Preferred end state:
1. vendor the required WM source into this repository as tracked source, or create/use an owner-controlled fork;
2. make EXP Mango code first-class repository code;
3. retain exact upstream provenance and MIT license;
4. do not modify unrelated repositories.

Reuse stable low-level Android input plumbing where useful, but replace the product identity, visible UI, and typing-intelligence layer with EXP Mango implementations.

## Non-negotiable runtime policy
Final EXP Mango APK must be 100 percent offline.

Remove or exclude:
- INTERNET permission;
- cloud AI and LLM;
- API keys;
- network search;
- GIF search;
- sticker networking;
- remote image search;
- cloud translation;
- online dictionary/Wikipedia;
- weather/currency network clients;
- remote addon repositories;
- network update checker;
- analytics;
- crash-reporting SDKs;
- background network activity.

Do not merely hide these controls. Remove production dependencies/source paths where practical and make CI fail if forbidden network access returns.

## Typing hot path
Keypress handling must stay deterministic and low latency.

Never perform synchronously on the keypress path:
- dictionary rebuild;
- database compaction;
- full-dictionary scanning;
- heavy file I/O;
- expensive global edit-distance search;
- model training;
- network access, even accidentally.

Use immutable or snapshot read structures and bounded background workers.

Target normal keypress-to-visible-suggestion work of only a few milliseconds on a mid-range phone such as OPPO Reno10 5G.

## EXP prediction engine
Build a dedicated EXP Mango prediction layer, with English and Hinglish as first-class priorities.

Recommended module boundaries:
- prediction core;
- dictionary/index;
- local learning;
- field policy;
- email domain provider;
- EXP UI/theme;
- runtime coordinator.

Prediction pipeline:
1. normalize current token and context;
2. retrieve bounded candidates from bundled, custom, and learned vocabularies;
3. generate typo candidates using keyboard proximity, omission, insertion, substitution, duplication, transposition, and phonetic similarity;
4. score using context plus personalization;
5. apply field policy;
6. return 3 to 5 candidates quickly;
7. update learning asynchronously.

Use a deterministic score combining:
- bigram/trigram context;
- personal frequency;
- global frequency;
- recency;
- typo similarity;
- keyboard proximity;
- field prior;
- correction risk.

Keep weights configurable and unit-tested.

## Open local learning
Learn locally from:
- committed words;
- accepted suggestions;
- accepted autocorrections;
- manually corrected words;
- word frequency;
- bigram counts;
- trigram counts;
- recent-use timestamps.

Do not learn:
- passwords;
- PINs;
- OTPs;
- secure fields;
- Incognito sessions.

Learning must be queued and processed off the UI/IME hot path.

Use bounded storage, decay, compaction, corruption recovery, and a one-tap reset.

## Field-aware behavior
Create one centralized FieldPolicyResolver.

Normal text:
- suggestions on
- autocorrect on
- learning on

Email:
- email-aware tokenization
- domain suggestions
- local domain learning
- Gmail support
- no ordinary spelling autocorrect in the domain portion

URL:
- conservative behavior
- no ordinary spelling autocorrect

Password/PIN/OTP/secure:
- no learning
- no suggestions exposing prior content
- no autocorrect
- no clipboard learning

Code/terminal:
- conservative correction
- preserve punctuation and identifiers

Per-app:
- app-specific mode/settings
- option to force Incognito in selected apps

## Gmail/domain suggestions
Build a local EmailDomainProvider.

Built-in popular domains should include at least:
gmail.com
outlook.com
yahoo.com
icloud.com
proton.me
protonmail.com
hotmail.com

Behavior:
- username followed by @ immediately shows domain chips;
- typing gma should surface gmail.com;
- selecting a domain commits it cleanly;
- local usage raises frequently used domains;
- no network lookup.

## Incognito/privacy UX
Incognito must be visually obvious.

Suggestion strip should show a compact lock/incognito state when privacy mode is active.

Incognito and secure fields must suppress learning and personalization.

## EXP visual system
Create an original modern visual language inspired by current premium smartphone keyboards, including the general feel of iOS 27, but do not copy Apple assets, trademarks, screenshots, or exact proprietary artwork.

Use:
- rounded or squircle keys;
- clean typography;
- subtle depth;
- restrained translucency;
- light and dark themes;
- compact suggestion strip;
- polished key press animation;
- tasteful haptics;
- smooth state transitions;
- modern bottom-row proportions;
- minimal toolbar.

Suggestion strip should be the main intelligence surface and normally contain only 3 to 5 useful candidates.

Autocorrect state should make the correction clear and offer undo.

Email fields should replace normal suggestions with domain-aware chips when appropriate.

Incognito should show the lock state in the same strip.

## Toolbar
Remove WM's giant toolbox from the default typing surface.

Keep only useful offline controls such as:
- emoji;
- language/globe;
- clipboard;
- settings;
- one optional user shortcut.

No AI, GIF, sticker, web, search or remote-tool controls.

## Offline emoji
Keep local emoji browsing/search if useful. Remove GIF and sticker networking.

## Settings
Keep a focused settings structure:
Typing, Appearance, Privacy, Languages, Per-app, About.

About must contain:
- EXP Mango version;
- EXP Mango identity;
- open-source licenses;
- WM Keyboard attribution;
- third-party notices.

## Branding and attribution
EXP Mango owns its product identity, original UI, product architecture and original EXP code.

Retain WM Keyboard's MIT license and copyright notice in an open-source notices file and in the app's Credits/About UI.

State clearly which parts are derived from or adapted from WM Keyboard.

Do not imply WM Keyboard created EXP Mango.

## Build/release
Exactly one production GitHub Actions workflow.

Production workflow must:
1. build from main;
2. use the permanent EXP Mango signing key when secrets are available;
3. preserve application ID forever;
4. use increasing version codes;
5. verify the APK certificate;
6. publish EXP-Mango.apk to GitHub Releases;
7. never expose signing material;
8. never commit the keystore.

Debug builds are for testing only, never the official release.

## Verification
Every major change needs:
- unit tests;
- Android build;
- permission/dependency audit;
- APK inspection;
- install/update smoke test where possible.

The final APK must:
- install as EXP Mango;
- appear in Android keyboard settings;
- work after reboot;
- work in airplane mode;
- show EXP branding;
- use custom vocabulary;
- use EXP prediction/learning;
- provide Gmail/domain suggestions;
- respect field policies;
- provide privacy/Incognito behavior;
- use the EXP visual system;
- retain third-party attribution;
- update over earlier signed EXP Mango builds.

## Jules workflow
Always read AGENTS.md and docs/EXP_MANGO_MASTER_SPEC.md first.

For each large task:
1. inspect the existing architecture;
2. produce a plan;
3. make small logical commits;
4. run tests/builds;
5. inspect artifacts;
6. report exact remaining issues.

Do not create redundant workflows. Do not delete custom dictionaries. Do not modify unrelated repositories.

Suggested sequential phases:
1. source ownership/vendorization and branding;
2. offline dependency/permission purge;
3. EXP prediction and learning;
4. field policy and email-domain engine;
5. EXP visual system;
6. remove unused WM UI/tooling;
7. performance/tests;
8. signed release and QA.

A build is not "done" merely because WM Keyboard compiles. It is done when the APK can honestly be called EXP Mango.
## No external product/repository dependencies

The final EXP Mango product must not depend on another keyboard/product repository for its core prediction engine, learning algorithm, UI, theme system, toolbar or product behavior.

Do not add HeliBoard, FlorisBoard, FUTO, AnySoftKeyboard, Nboard, another keyboard fork, an iOS clone, or any other competing keyboard/product as a dependency or copied product layer.

Do not make the final app fetch source/assets/models at build time from arbitrary GitHub repositories.

Any third-party library or model that is genuinely necessary must be explicitly reviewed for license, vendored or pinned reproducibly where appropriate, listed in THIRD_PARTY_NOTICES.md, and must not redefine EXP Mango's product identity.

For the UI, implement the EXP Mango visual system directly in the existing Android/Compose codebase. Do not depend on an external "iOS keyboard" GitHub repository. The iOS-27-inspired look is a design target, not a source-code dependency.

For prediction, learning, field policy and email-domain intelligence, implementation must live in EXP Mango source code. Do not solve those requirements by embedding another keyboard's engine.

If a small local model is useful as an optional neural reranker, it must be an explicitly licensed, reproducibly packaged model used only as a local component. It must never be a cloud service, and the deterministic EXP engine must remain the functional fallback.
