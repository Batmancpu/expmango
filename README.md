# expBoard 🥭

A small, local-first Android keyboard experiment focused on **fast Hinglish typing, adaptive vocabulary and privacy**.

## v0.1 capabilities

- Android InputMethodService
- minimal dark/iOS-inspired UI
- liquid-glass-inspired mango app icon
- in-memory prefix trie
- English + Hinglish vocabulary
- personal word learning
- bigram learning
- conservative typo correction
- suggestion strip
- lightweight glide/swipe decoding
- last-correction undo
- app/field-aware privacy rules
- asynchronous persistence
- no network permission
- no persistent background service
- no heavyweight model on the keystroke path

## Performance philosophy

expBoard deliberately chooses **instant deterministic behavior over slower "AI" behavior**.

Typing does not wait for:
- a neural model;
- disk I/O;
- network;
- background analysis.

Vocabulary and phrase learning happen asynchronously while the IME is alive. A future small local model can analyze accumulated text and improve the dictionary in the background, but it must remain off the synchronous key path unless real-device benchmarks prove otherwise.
