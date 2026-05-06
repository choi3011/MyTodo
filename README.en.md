# MyTodo

> A todo app whose mobile and desktop clients share live state through Firestore.

[한국어](README.md) &nbsp;|&nbsp; **English**

A two-platform todo app deliberately steering away from stock Material You — built around an indigo-to-magenta gradient brand. The Android app and the Compose Desktop app share the same Firebase backend, so a todo created on one device shows up on the other within seconds.

## Screenshots

> Coming soon under `docs/screenshots/`. Mobile and desktop share the same visual identity.

## Features

- **Four time scopes** — Today / Week / Month / Year, each with its own accent color
- **Four priority levels** — None / Low / Medium / High, with a sort toggle (recency vs priority)
- **Time ranges** — manual `HH:mm ~ HH:mm` input, no AM/PM dance
- **Calendar dots** — visual marker on dates that have day-scope todos
- **Google sign-in** — Credential Manager on Android, custom OAuth desktop flow on JVM
- **Cross-platform sync** — Android and Desktop share `users/{uid}/todos` in Firestore
- **Session persistence** — refresh tokens cached locally so the desktop app skips the login screen on subsequent launches

## Tech stack

| Layer | Tools |
| --- | --- |
| Mobile | Kotlin 2.0, Jetpack Compose Material 3, AGP 9, minSdk 24 |
| Desktop | Compose Multiplatform 1.7, Kotlin/JVM, JDK 17+ |
| Auth | Firebase Auth, Google OAuth 2.0 (Credential Manager + handwritten PKCE flow) |
| Storage | Firebase Firestore (snapshot listener on mobile / REST `runQuery` polling on desktop) |
| Build | Gradle, jpackage, WiX (Windows MSI) |

## Architecture

```
┌──────────────┐     ┌─────────────────┐     ┌──────────────┐
│  Android     │     │  Firestore      │     │  Compose     │
│  (mobile)    │◄───►│  users/{uid}/   │◄───►│  Desktop     │
│              │     │  todos/{id}     │     │  (JVM)       │
└──────────────┘     └─────────────────┘     └──────────────┘
   snapshot              security rule:         REST runQuery
   listener              auth.uid == uid        + 15s polling
   (push, gRPC)                                 + per-scope cache
```

Both clients sign in with the same Google account, get the same Firebase UID, and read/write the same `todos` subcollection under that UID. Mobile rides the Firestore SDK's gRPC streaming for push-based realtime sync. Desktop goes through pure REST + a cache + polling combo that approximates push-cost.

## Build & run

### Prerequisites

- Android Studio (mobile)
- JDK 17+ for desktop. JetBrains Runtime (bundled with Android Studio) is missing `jpackage`, so use Eclipse Temurin or Microsoft OpenJDK if you want to package an installer.
- A Firebase project with Authentication and Firestore Database enabled.

### Firebase setup

1. Create a new Firebase project.
2. **Authentication** → Sign-in method → enable Google.
3. **Firestore Database** → start, then replace the security rules:
   ```
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       match /users/{uid}/todos/{document=**} {
         allow read, write: if request.auth != null && request.auth.uid == uid;
       }
     }
   }
   ```
4. **Project settings** → add an Android app → download `google-services.json` → place it in `app/`.
5. Note the auto-generated web client_id from Authentication → Sign-in method → Google. Mobile's Credential Manager uses it.

### Android

Open the project in Android Studio and run the `app` configuration. minSdk is 24.

### Desktop

1. **Create a Desktop OAuth client** in Google Cloud Console
   - https://console.cloud.google.com/apis/credentials → Create credentials → OAuth client ID
   - Application type: **Desktop app**
   - Save the Client ID and Client Secret it shows.

2. **Edit `OAuthConfig.kt`** at `desktop/src/main/kotlin/com/example/mytodo/desktop/auth/OAuthConfig.kt`:
   - Replace `GOOGLE_CLIENT_ID` with the new desktop client ID.
   - Replace `FIREBASE_API_KEY` with your project's web API key.
   - Replace the Firestore project ID inside `FirestoreClient.kt` with your project ID too.

3. **Drop the client_secret into `local.properties`** (gitignored, never committed):
   ```
   google.oauth.client_secret=YOUR_DESKTOP_CLIENT_SECRET
   ```

4. **Run during development**:
   ```
   ./gradlew :desktop:run
   ```

5. **Build a Windows installer** (requires a real JDK 17+):
   ```
   ./gradlew :desktop:packageMsi
   ```
   Output: `desktop/build/compose/binaries/main/msi/MyTodo-1.0.0.msi`. Double-clicking installs the app, drops a Start Menu entry, and creates a desktop shortcut.

## Project structure

```
.
├── app/                                # Android module
│   └── src/main/
│       ├── java/com/example/mytodo/
│       │   ├── data/                   # Firestore + Auth repositories
│       │   ├── ui/                     # TodoScreen, LoginScreen, ViewModel
│       │   └── ui/components/          # TodoRow, AddTodoSheet, CalendarPicker, ...
│       └── res/                        # icons, themes, splash
├── desktop/                            # Compose Desktop module
│   ├── src/main/
│   │   ├── kotlin/com/example/mytodo/desktop/
│   │   │   ├── Main.kt                 # window + auth state branching
│   │   │   ├── auth/                   # OAuth desktop flow + Firebase token exchange
│   │   │   ├── data/                   # Firestore REST client + Todo repository
│   │   │   └── ui/                     # mirrors mobile components
│   │   └── resources/                  # icon.png, icon.ico
│   └── build-icon.ps1                  # PowerShell + GDI+ icon generator
├── gradle/libs.versions.toml           # version catalog
└── settings.gradle.kts                 # multi-module setup
```

## Design decisions

### Mobile-first, desktop adopts the phone form factor
The desktop window stays at phone proportions (480×820). Instead of bolting on a desktop-native sidebar, the entire mobile UI is reused so both surfaces share the same visual identity. About 90% of UI components transferred with only an import-line change — Compose Multiplatform makes `androidx.compose.*` namespaces source-compatible across platforms.

### Desktop sync is REST polling, not push
Firestore's realtime Listen channel is bidirectional gRPC streaming — implementing it from scratch in JVM means heavy dependencies and protocol gymnastics. The compromise: the REST `runQuery` endpoint with a server-side `whereEqualTo(scope, ...)` and `whereEqualTo(targetDate, ...)` filter, polled every 15 seconds. A per-`(scope, anchor)` cache plus a 1-second debounce delivers most of the perceived responsiveness of push:

- The currently visible scope is the only one being polled.
- Switching tabs faster than the debounce never triggers a fetch.
- Re-entering a previously visited scope renders cached data instantly while the next poll runs in the background.
- Polling pauses entirely when the window is minimized (`WindowState.isMinimized`).

Net effect: even with 5,000 cumulative todos, one poll = the 5–30 documents currently visible. Comfortable inside Firestore's free tier.

### Desktop OAuth — RFC 8252 implemented by hand
There's no official Google sign-in SDK for Compose Desktop, so the flow is built from primitives:

1. **PKCE** — random 64-byte `code_verifier` → SHA-256 → base64url `code_challenge`.
2. **Loopback redirect** — temporary `HttpServer` (Java SE built-in) on a random `127.0.0.1` port.
3. **System browser** — `java.awt.Desktop.browse()` opens Google's authorization URL.
4. **Code capture** — the redirect callback resolves an `Awaitable<String>` consumed by the suspending caller.
5. **Token exchange** — POST to `oauth2.googleapis.com/token` returns the Google ID token.
6. **Firebase exchange** — POST that ID token to `identitytoolkit.googleapis.com:signInWithIdp` to receive a Firebase ID token + refresh token.
7. **Persistence** — session JSON is written to `~/.mytodo/session.json`; subsequent launches restore it (and refresh through `securetoken.googleapis.com` if the cached token is expired).

A mutex around the refresh path prevents concurrent refreshes when the polling loop and a UI action both bump up against the same expiry boundary.

### Secrets stay out of the repo
- `.gitignore` drops `app/google-services.json`, `local.properties`, `client_secret_*.json`, root-level dev screenshots, and dev-time scratch files.
- Firestore security rules pin reads/writes to the authenticated owner (`request.auth.uid == uid`).
- The OAuth `client_secret` is loaded from `local.properties` at runtime — even though it isn't actually secret per the OAuth spec for desktop apps, keeping it out of the repo makes the public history cleaner.

### Desktop icon is generated from the same vector source
The Android launcher's `ic_launcher_foreground.xml` coordinates feed a PowerShell + GDI+ script (`desktop/build-icon.ps1`) that renders 16 / 32 / 48 / 64 / 128 / 256 PNGs and packs them into a multi-size `.ico`. Mobile and desktop end up pixel-aligned with a single source of truth.

## What I learned

- **Compose Multiplatform compatibility is real** — under Kotlin 2.0 + Compose 1.7, mobile and desktop UI code shares the same package paths almost verbatim. Platform-specific APIs like `LocalHapticFeedback` no-op cleanly on desktop.
- **Firestore billing intuition: list calls = N reads** — naïve polling without server-side filters scales the cost with collection size. Server-side filtering bounds it by visible-scope size, which is what makes long-running clients viable on the free tier.
- **OAuth desktop flow demystified** — RFC 8252's loopback + PKCE pattern is well-defined; building it from scratch made each token's role explicit instead of opaque SDK magic.
- **Session persistence is a tradeoff** — plaintext JSON in `$HOME` is a cheap, robust starting point. OS keychain integration would harden it, but only matters when the threat model includes other local users.
