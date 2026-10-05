# Experimental Mango architecture

## Hot path
Key touch -> in-memory commit -> in-memory prefix lookup -> suggestion update.

The hot path intentionally has no network, no model loading, no disk writes and no synchronous persistence.

## Learning path
Words and bigrams are queued to one background executor and persisted with a short debounce. A future AI model may analyze learned text asynchronously, but it will not be required to render or commit a key.

## Privacy
Password and other sensitive fields disable correction and learning. URI/email/filter fields and known terminal apps disable autocorrection. The app declares no INTERNET permission and has no persistent background service.

## Glide typing
v0.1 uses a deliberately small geometric-to-letter sequence decoder so the keyboard remains self-contained. It is a replaceable module. A stronger open engine can be evaluated later.

## Performance contract
A visible keystroke must never wait for background intelligence. Future neural experiments must be benchmarked on the OPPO Reno10 5G before being allowed on the synchronous path.
