# EXP Mango Interaction Reference

This file is implementation guidance, not a requirement to copy Apple.

## Core feel
The keyboard should feel immediate, quiet and premium. Every response to a keypress must appear faster than the user can perceive as a delay.

## Key press
- Down state within one animation frame where possible.
- Scale around 0.96.
- Slight surface contrast change.
- Short haptic tick if enabled.
- Release back to 1.0 smoothly.
- Do not animate layout geometry on every keypress.

## Suggestions
- 3 primary candidates normally.
- Up to 5 only when useful.
- Primary candidate gets stronger weight and slightly more visual emphasis.
- Suggestion changes should crossfade or update cleanly rather than jumping.
- No unrelated tool icons in the suggestion row.
- Autocorrect exposes an undo affordance.

## Inline prediction
Where the Android field supports it safely, show the predicted continuation as low-emphasis inline/ghost text.
- Space may accept the prediction.
- Typing another character revises/rejects it naturally.
- Never interfere with cursor movement.
- Disable in passwords, PINs, OTPs, secure fields, code/terminal when unsafe.

## Email
After @, replace ordinary candidate emphasis with domain chips.
Prioritize gmail.com.
Learn local domains.

## Incognito
Use a compact lock/privacy indicator in the suggestion area.
No visual ambiguity about the privacy state.

## One-handed mode
Shift the key grid toward the active thumb.
Suggestion strip remains aligned to the keyboard surface.

## Toolbar
Default:
- emoji
- language
- clipboard
- settings/more

One optional user shortcut is allowed.

Never default to AI, GIF, sticker, web, search or network tools.

## Visual behavior
- Rounded/squircle keys.
- restrained translucency/blur only when cheap.
- strong contrast in light and dark.
- no expensive particles or effects in the typing hot path.
- reduced-motion system preference must be respected.

## Apple-inspired, not copied
Use the interaction principles users expect from current premium smartphone keyboards, including predictive suggestions, inline prediction, glide input, responsive keys and clean bottom-row ergonomics.

Do not copy Apple source, proprietary artwork, logos, screenshots or exact UI assets.
