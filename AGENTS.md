# AI Agent Instructions for Nosved YouTube Downloader Development

This document is the absolute source of truth for any AI agent or LLM assisting with the development of the **Nosved** project. You must strictly adhere to these rules, architectural guidelines, and communication protocols.

---

## 1. Communication & Chat Protocol

- **Zero Blabbering / Conversational Fluff:** Avoid long explanations, explanations of standard APIs, or conversational pleasantries.
- **Concise Chat Explanations:** Explain what you did in **1-2 sentences maximum** before/after a code block. Focus entirely on the code.
- **Direct Code Delivery:** Present complete, functional code blocks directly rather than describing step-by-step how to write it.

---

## 2. Core Development Philosophy & Code Fidelity

- **Project Context:** Nosved is a modern, material design Android video/audio downloader based on yt-dlp, using the `youtubedl-android`, `ffmpeg`, and `aria2c` libraries.
- **Zero-Error Code Implementation:**
  - **No Placeholders:** Never use placeholders (`// TODO`, `...`, etc.) in modified or new files. All code must be fully implemented, syntactically valid, and ready for production.
  - **Verified Imports & Package Name:** Always use the correct package (`com.devson.nosved`) and verify that all imported classes actually exist in the codebase.
  - **Type Safety:** Never use the not-null assertion operator (`!!`). Always use safe calls (`?.`), Elvis operator fallbacks (`?:`), or smart casts.
- **Zero-Crash Tolerance:**
  - Downloading and media processing (muxing, converting, extracting audio) are highly error-prone.
  - All download-related calls must be wrapped in safe try-catch blocks handling `YoutubeDLException` and generic `Exception`.
  - Failures must update the UI state gracefully (e.g. displaying error messages to the user) and clean up temporary files/notifications, rather than crashing the application.

---

## 3. Performance & Concurrency Guidelines

- **Main Thread Isolation:**
  - Never execute file system checks, yt-dlp initialization, network queries, format extraction, or database writes on the main thread.
  - Always dispatch background tasks to `Dispatchers.IO`.
- **Lifecycle & Services:**
  - Long-running downloads must be managed via `DownloadService` or `WorkManager` to prevent termination when the app goes into the background.
  - Handle progress and speed callback updates from yt-dlp efficiently to prevent excessive UI thread bottlenecking (e.g., throttle progress updates if needed).

---

## 4. UI & Jetpack Compose Guidelines

- **Framework:** Jetpack Compose with Material Design 3 (M3) components is the exclusive UI standard.
- **Touch & Mobile Ergonomics:** Prioritize mobile-first design, ensuring accurate touch targets (minimum 48dp), smooth animations, and proper padding.
- **State Separation:** Keep Composables stateless by hoisting states to the `MainViewModel`. Ensure recomposition counts are minimized by using targeted StateFlow updates.

---

## 5. Documentation & Update Tracking

You must actively maintain the project's changelog. After every completed task, bug fix, or feature addition, you must append an entry to the `update_details.md` file.

**Format and Rules for `update_details.md`:**

- Do NOT rewrite or read the entire file. Simply append the new data at the very end.
- Use the following format for each entry:
  - **Date:** (Current date and time)
  - **Issue:** (Brief description of the issue, bug, or feature request)
  - **Type:** (e.g., Error, Bug, UI, Performance, Feature, Architecture)
  - **Solution:** (How it was solved/implemented. Max 10 lines.)
  - Followed by exactly `---` on a new line.

---

## 6. Version Control Protocol

- **Do not commit or push** any changes to the git repository until explicitly instructed to do so by the developer.
