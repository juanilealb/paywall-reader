# Bookmark-first Markdown Reader Roadmap

> **For Hermes:** Use subagent-driven-development skill to implement this plan PR-by-PR. Keep each PR small enough to review independently, build a debug update APK after every merge, and verify installability with the existing `applicationId` and versionCode bump.

**Goal:** Keep paywall-reader as a two-mode app: (1) read pages with the existing paywall-bypass/intelligent fallback behavior, and (2) save durable bookmarks as beautiful distraction-free markdown/text, including links shared from outside the app.

**Architecture:** Keep the current WebView/paywall-bypass reader as the live reading surface. Add a capture pipeline that can take either the currently-open readable page or an external share URL, run it through the same intelligent reader/paywall fallback path, extract the resulting readable document to markdown, store immutable bookmark records, and render saved markdown in a sober Defuddle-style reader.

**Tech Stack:** Android/Kotlin, Jetpack Compose, Material 3 Expressive, Room, WebView for optional page loading, Defuddle-inspired extraction via a small service/module, existing repository/DAO patterns.

---

## Current-state findings

- The app is **currently using Periscope** for NYTimes URLs: `ReaderScreen.kt` defines `PERISCOPE_HOST = "periscope.corsfix.com"` and routes NYT through `toPeriscopeUrl()`.
- The app is **not using Ladder directly**. Ladder is only conceptually similar to the proxy/fallback approach.
- The app is **not using Defuddle**. Current save uses `ARTICLE_CAPTURE_SCRIPT`, a small custom JS snippet that grabs `article.outerHTML`, `innerText`, basic meta tags, and converts text to very basic markdown via `toBasicMarkdown()`.
- The app still has paywall/fallback behavior: `accessarticlenow.com`, `unwall.app`, `archive.*`, ad cleanup, popup cleanup, and paywall-marker fallback logic. This is **intentional** and should remain the default for live page reading and for bookmark capture.
- Shared URLs currently open the reader immediately via `AppNavigation(sharedUrl)` -> `backStack.add(AppRoute.Reader(...))`; this violates the new product intent.
- Marking read currently deletes the bookmark: `SourceRepository.markRead()` calls `sourceDao.deleteReadingItem(url)`; this violates the “never delete bookmarks” requirement.

## Tool analysis

### Defuddle — use heavily

Repo: `kepano/defuddle`  
License: MIT  
Value: Extracts main content and metadata, returns cleaned HTML or Markdown, created for Obsidian Web Clipper.

Decision:
- Use as the reference/engine for markdown capture.
- Preferred output: markdown + title + author + description + domain + favicon + image + published + wordCount.
- Do not keep the current `innerText -> paragraph markdown` as the primary implementation.

Open question for implementation:
- Android cannot directly import npm TS packages without a bridge. Evaluate in PR2 whether to:
  1. run Defuddle JS inside an invisible/offscreen WebView against loaded HTML/document, or
  2. add a small backend capture endpoint using Defuddle/node, or
  3. port a minimal extraction subset to Kotlin only if JS bridge is too brittle.

### Periscope — maybe as optional HTML fetch fallback only

Repo: `reynaldichernando/periscope`  
License: MIT  
Value: Hosted clean-reader/proxy inspired by Ladder rulesets. Current app already uses hosted `periscope.corsfix.com` for NYT.

Decision:
- Keep Periscope as one of the intelligent reader/fallback tools where it already improves reading, especially for NYTimes.
- It can participate in capture: external share -> reader/fallback pipeline -> Defuddle extraction -> saved markdown.
- It should remain abstracted behind a `ReaderProvider`/fallback layer so it is not hardcoded as the only strategy.

### Ladder — do not embed; optionally borrow architecture ideas

Repo: `everywall/ladder`  
License: GPL-3.0  
Value: Self-hosted proxy with domain rulesets, request/response modification, raw HTML API.

Decision:
- Do **not** copy code into the app because GPL-3.0 would contaminate the Android app licensing unless we want that.
- Do not require a user to run Ladder for the normal app experience.
- Useful ideas to borrow at design level only: per-domain extraction/fetch rules, allowlist, provider ranking, raw HTML endpoint abstraction, and debug visibility for why a provider was chosen.
- Potential future optional integration: user-configurable self-hosted Ladder URL, disabled by default, as one more `ReaderProvider`.

---

# PR Plan

## PR1 — Fix external share semantics: capture bookmark, don’t open UI reader

**Goal:** When sharing a URL from another app, paywall-reader starts a bookmark capture in the background and lands on the bookmark list with a confirmation, instead of visibly opening the WebView reader.

**Files:**
- Modify: `app/src/main/java/com/juani/paywallreader/MainActivity.kt`
- Modify: `app/src/main/java/com/juani/paywallreader/ui/navigation/AppNavigation.kt`
- Modify: `app/src/main/java/com/juani/paywallreader/ui/home/HomeViewModel.kt`
- Modify: `app/src/main/java/com/juani/paywallreader/ui/home/HomeScreen.kt`
- Modify: `app/src/main/java/com/juani/paywallreader/data/repository/SourceRepository.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Test: repository/unit tests for shared URL save and duplicate handling.

**Acceptance criteria:**
- Share URL from Chrome/Twitter/etc. creates or updates a bookmark and starts capture.
- App does not navigate to `ReaderRoute` automatically.
- User sees “Guardado en Para leer” / similar.
- Duplicate share does not create duplicates; it updates `addedAt` or shows already saved.
- Shared URL can optionally default to a folder, e.g. “Inbox” or “Sin carpeta”.
- The capture pipeline still uses the reader/paywall fallback stack internally; the user just does not get thrown into the live browser UI.

**Implementation notes:**
- Replace `sharedUrl` navigation effect in `AppNavigation.kt` with `pendingCaptureUrl` handled by Home/ViewModel.
- Add repository method `saveBookmarkFromExternalShare(url, folderIdOrName)` with title fallback to domain until extraction fills metadata.
- After save, clear/consume intent so rotation/recompose does not save repeatedly.

**Verification:**
- Unit test duplicate shared URL.
- Manual: `adb shell am start -a android.intent.action.SEND -t text/plain --es android.intent.extra.TEXT "https://example.com/article" com.juani.paywallreader/.MainActivity`
- Build: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ANDROID_HOME=/opt/android-sdk /home/hermes/.local/gradle/gradle-9.4.1/bin/gradle testDebugUnitTest assembleDebug`

---

## PR2 — Introduce durable bookmark model: never delete on read

**Goal:** Replace `reading_items` as an ephemeral queue with durable bookmarks that have status fields.

**Files:**
- Modify: `ReadingItemEntity.kt` or create `BookmarkEntity.kt`
- Modify: `AppDatabase.kt` migration `7 -> 8`
- Modify: `SourceDao.kt`
- Modify: `SourceRepository.kt`
- Modify: `domain/model/Source.kt` or create `Bookmark.kt`
- Test: DAO/repository migration/status tests.

**Schema additions:**
- `folderName TEXT NOT NULL DEFAULT 'Sin carpeta'`
- `isRead INTEGER NOT NULL DEFAULT 0`
- `readAt INTEGER NULL`
- `isArchived INTEGER NOT NULL DEFAULT 0`
- `archivedAt INTEGER NULL`
- `updatedAt INTEGER NOT NULL DEFAULT current time`
- `captureStatus TEXT NOT NULL DEFAULT 'pending'` (`pending`, `capturing`, `ready`, `failed`)
- Keep `url` unique.

**Acceptance criteria:**
- Mark as read updates `isRead = true`; it does not delete.
- Archive updates `isArchived = true`; it does not delete.
- Existing saved items migrate safely.
- Default list shows unarchived bookmarks; filters can show read/archived later.

**Implementation notes:**
- Replace `SourceRepository.markRead()` implementation; no more `deleteReadingItem()` for read action.
- Keep hard delete either absent from UI or tucked behind explicit destructive menu later.

---

## PR3 — Bookmark folders for saved articles

**Goal:** Bookmarks can be assigned to folders and moved later. Folders are first-class for bookmarks, not just source sites.

**Files:**
- Modify: `FolderEntity.kt` / folder model if reusable.
- Modify: `SourceDao.kt`
- Modify: `SourceRepository.kt`
- Modify: `HomeScreen.kt`
- Modify: `AddSourceSheet.kt` or create `BookmarkFolderPicker.kt`
- Modify: strings.
- Test: repository tests for create/move/filter folder.

**Acceptance criteria:**
- User can save a bookmark into a folder.
- User can move an existing bookmark to another folder.
- List can filter/group bookmarks by folder.
- Folders with bookmarks are not deleted just because no source site uses them.

**Implementation notes:**
- Avoid overloading “source folders” if that causes weird coupling. If needed, introduce `bookmark_folders` or add a folder `type`.
- External shares should use last selected/default folder and allow changing later.

---

## PR4 — Reader-provider pipeline + Defuddle capture to markdown

**Goal:** Every saved bookmark, whether saved from inside or shared from outside, gets captured as clean markdown with metadata, similar to Obsidian Web Clipper reader output, **after** passing through the app’s intelligent reader/paywall fallback chain when needed.

**Files:**
- Create: `data/capture/ArticleCaptureService.kt`
- Create: `data/capture/ArticleCaptureResult.kt`
- Create: `data/capture/DefuddleBridge.kt` or equivalent.
- Create: `data/reader/ReaderProvider.kt`
- Create: `data/reader/ReaderProviderResolver.kt`
- Modify: `ReaderScreen.kt` to call capture service instead of `ARTICLE_CAPTURE_SCRIPT` for saves.
- Modify: `SourceRepository.kt` to update capture fields/status.
- Modify: `AppDatabase.kt` migration if additional fields needed.
- Test: capture result parsing + repository update tests.

**Acceptance criteria:**
- Saving from inside and outside both run the same capture path.
- External share capture tries the same effective reading path as live reading: original page, Periscope where useful, archive/unwall/accessarticlenow fallbacks where existing logic chooses them, then Defuddle extraction from the best readable result.
- Stored markdown includes title, source URL, metadata/frontmatter or consistent header, and clean body.
- Capture failure still saves bookmark with `captureStatus = failed` and allows retry.
- Capture records the provider used (`original`, `periscope`, `unwall`, `archive`, etc.) for debugging.

**Preferred markdown shape:**

```markdown
---
title: "Article title"
url: "https://example.com/article"
site: "Example"
author: "Author"
published: "2026-05-27"
saved: "2026-05-27T..."
tags: []
---

# Article title

Clean article markdown...
```

**Defuddle integration spike inside this PR:**
- Try an Android WebView JS bridge first: load URL/HTML, inject bundled Defuddle browser/full JS, call `new Defuddle(document).parse({ markdown: true })` or equivalent.
- If bundling Defuddle in Android is painful, carve out PR4a as a small capture service endpoint using `defuddle/node`, and keep Android client API generic.
- Document the chosen path and why.

---

## PR5 — Add saved-bookmark markdown reader, keep live WebView reader

**Goal:** Add a beautiful saved-bookmark markdown reader without removing the current live page reader.

**Files:**
- Create: `ui/bookmark/BookmarkReaderScreen.kt`
- Create: `ui/bookmark/MarkdownRenderer.kt` or integrate a markdown Compose renderer.
- Modify: `AppNavigation.kt`
- Modify: `HomeScreen.kt`
- Keep: `ReaderScreen.kt` as the live page/paywall reader.
- Test: UI/state tests where practical.

**Acceptance criteria:**
- Tapping a saved bookmark opens distraction-free markdown if capture is ready.
- If capture pending/failed, show useful state: retry capture, open original, or view saved metadata.
- The reader is clean, fast, dark-mode-friendly, and works offline for captured content.
- Live “Leer páginas” continues to use the current WebView/paywall-bypass reader.

**Visual target from Juani’s screenshot:**
- Dark, sober, almost Obsidian-like canvas: near-black background, low-contrast texture/tonal depth, no bright cards except media.
- Narrow centered reading column, roughly 560–680dp max width on large screens, comfortable horizontal margin on phone.
- Title is compact, bold, white/off-white; metadata below is small and muted.
- Body typography is readable but not huge; muted off-white text, generous paragraph spacing, no visual clutter.
- Hero image is large, centered, rounded corners, with subtle overlay/open icon if needed.
- Top-right actions are tiny/quiet: edit/copy/text settings/share style controls, not a heavy toolbar.
- Overall mood: calm, premium, distraction-free, archive/reader rather than browser.

---

## PR6 — Preserve paywall-bypass by default and reuse it for bookmark capture

**Goal:** Keep paywall bypass as a first-class default capability for live reading and make it reusable by bookmark capture. This PR must **not** remove or disable automatic paywall-bypass behavior.

**Files:**
- Modify: `ReaderScreen.kt`
- Modify: strings and UI labels.
- Create/modify reader provider abstractions around helpers/constants: `REMOVE_PAYWALLS_HOST`, `PERISCOPE_HOST`, `ARTICLE_READER_HOST`, `UNWALL_HOST`, archive fallback, paywall marker scripts.
- Test: URL normalization tests.

**Acceptance criteria:**
- Automatic routing/fallback through Periscope/accessarticlenow/unwall/archive remains available for normal reading where current behavior uses it.
- Bookmark capture can invoke the same provider chain headlessly/offscreen.
- Externally shared URLs go through the same intelligent reader/paywall path before text extraction, then save the cleaned markdown/text bookmark without visibly opening the reader UI.
- The app has enough intelligence to try the best provider for the domain and fall back if the result is blank/paywalled.
- Original URL is always preserved.

**Implementation notes:**
- Move hardcoded fallback logic out of `ReaderScreen.kt` into testable provider/resolver classes where practical.
- Preserve ad/popup cleanup if it improves live reading or capture quality.
- Add debug metadata so saved bookmark details can say which provider produced the text.

---

## PR7 — Modern bookmark list interactions: read/archive with Material 3 Expressive motion

**Goal:** Add swipe gestures and modern animations. Swiping should mark read/archive, never delete.

**Files:**
- Modify: `HomeScreen.kt`
- Modify/create: bookmark row/card components.
- Modify: `ui/theme/Motion.kt`
- Modify: repository status methods.
- Test: repository status methods; manual UI verification.

**Acceptance criteria:**
- Swipe one direction: archive bookmark.
- Optional opposite direction: mark read/unread.
- Gesture uses Material motion principles: emphasized easing/spring, clear state layer, icon reveal, smooth item placement animation.
- Undo snackbar for archive/read.
- No destructive delete attached to swipe.

**Material 3 Expressive motion guidance:**
- Use existing `PaywallReaderMotion` tokens and expand them where needed.
- Prefer `animateItemPlacement`, `AnimatedContent`, `AnimatedVisibility`, spring-based scale/offset for swipe background/icon reveal.
- Keep motion purposeful: spatial continuity, responsive feedback, no gratuitous bounce.

---

## PR8 — Capture queue, retries, and background polish

**Goal:** Make capture robust enough for real daily use.

**Files:**
- Create/modify capture queue components.
- Add WorkManager dependency if needed.
- Modify repository to expose capture states.
- Modify UI badges/progress.
- Test: failed capture retry, duplicate queue coalescing.

**Acceptance criteria:**
- External share returns quickly after saving placeholder bookmark.
- Capture can continue/retry in background.
- Failed items show “Reintentar extracción”.
- Multiple shares do not block the UI.

---

# Recommended order

1. PR1 immediately fixes the confusing share behavior.
2. PR2 prevents data loss by making bookmarks durable.
3. PR3 adds folder organization.
4. PR4 makes capture markdown-first with Defuddle after reader/paywall fallback.
5. PR5 adds the saved markdown reader while keeping live page reading.
6. PR6 formalizes the paywall-bypass provider chain and makes it reusable by capture.
7. PR7 adds swipe/archive/read with expressive motion.
8. PR8 makes capture reliable.

# Non-goals

- No removing the current paywall-bypass/live reader capability.
- No default dependency on self-hosted Ladder.
- No destructive swipe-to-delete.
- No lossy conversion where original URL/metadata disappear.
- No opening externally shared URLs immediately in the app.
- No Obsidian export/import work for now; it is explicitly out of scope.

# Release workflow per PR

After every merged PR:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 \
ANDROID_HOME=/opt/android-sdk \
/home/hermes/.local/gradle/gradle-9.4.1/bin/gradle testDebugUnitTest assembleDebug
```

Then bump `versionCode`/`versionName`, create a GitHub Release asset APK, and verify the direct download with `curl -I -L` before sending it to Juani.
