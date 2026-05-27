# Unified Reader + Capture Implementation Plan

> **For Hermes:** Use subagent-driven-development skill to implement this plan task-by-task.

**Goal:** Turn paywall-reader into one Material 3 Expressive Android app that can browse/read media through multiple bypass/reader strategies, save clean distraction-free articles, and accept links shared from any app.

**Architecture:** Keep the current paywall-reader look and feel as the product shell. Add modular engines under the UI: a `rule` engine inspired by Periscope/Ladder, a `capture/extraction` engine using Defuddle, and a local `article` library that replaces Afterlight as a separate app. Do not copy GPLv3 Ladder code; reimplement ideas and keep Defuddle/Periscope usage MIT-compliant with attribution.

**Tech Stack:** Android/Kotlin, Jetpack Compose, Material 3 Expressive, Room, WebView, JavaScript bridge, Defuddle JS bundle, optional OkHttp/Jsoup later.

---

## Product Principles

1. **One app, multiple jobs:** paywall-reader is both a media reader/bypass surface and a clean read-later/offline library.
2. **Preserve today's UI:** new surfaces must feel like the existing app: same typography, colors, cards, floating toolbar behavior, and Material 3 Expressive motion/components.
3. **Bookmarks become readings:** avoid treating saved URLs as plain bookmarks. Saved items should become clean, durable articles with metadata and offline content.
4. **Share-sheet first:** any app can share a link into paywall-reader. The app should route it through the same reader/bypass/capture pipeline if needed.
5. **Separation of concerns:** UI should not know whether a URL used removepaywalls, Periscope, Ladder-style headers, archive, or original WebView. That belongs in engines/repositories.
6. **Safe licensing:** Defuddle and Periscope are MIT-friendly. Ladder is GPLv3, so use it as product/architecture inspiration only unless the whole app is intentionally GPLv3.

---

## Target User Flows

### Flow A: Read media and bypass barriers

1. User opens a source or article in the current reader.
2. The app loads the best available reader URL.
3. If the page is blocked, blank, or paywalled, the app tries a configured fallback chain.
4. The user can still open original, share original, refresh, navigate back/forward, or force reader mode.
5. Current Material 3 Expressive reader toolbar remains the main UI pattern.

### Flow B: Save clean article from the reader

1. User taps the current save/bookmark action.
2. The action becomes semantically “Guardar lectura” / “Guardar offline”, not just bookmark.
3. The app extracts the current rendered page with Defuddle.
4. The app saves metadata + clean HTML/Markdown/text locally.
5. The saved item appears in an in-app Library/Readings surface.

### Flow C: Share a link from any app

1. User taps Android Share → PaywallReader.
2. PaywallReader receives `ACTION_SEND text/plain` or URL text.
3. The app opens an import/capture route using the same reader pipeline.
4. If a bypass/reader fallback is needed, it runs before capture.
5. The final clean article is saved to the local library.

---

## Proposed Navigation

Keep existing home/reader feel. Add only one new primary destination at first:

- **Sources / Medios:** current source grid and folders.
- **Lecturas:** saved clean articles, replacing the mental model of bookmarks.
- **Reader:** current immersive WebView surface.

Avoid a full redesign. If bottom navigation is added later, it should use Material 3 navigation components and preserve current spacing/shape/color language.

---

## External Project Integration Notes

### Periscope

Use as inspiration for:

- Domain/path rules.
- Headers per domain.
- User-agent/referer/cookie overrides.
- Optional Periscope fallback backend.

Do not depend blindly on hosted Periscope. Verify URL contract first because the inspected repo appears to parse `/?<url>` rather than `?url=<encoded>`.

### Defuddle

Use directly as the first extraction engine:

- Bundle `defuddle/full` into app assets.
- Inject in WebView after page load or when saving.
- Return title, author, site, published date, image, clean HTML, Markdown, text, word count, and debug info.
- Store extracted content locally.

### Ladder

Use as architecture inspiration only:

- Rulesets.
- Fallback chains.
- Header/url modification ideas.
- Debug surface showing rule/backend used.

Do not copy code or rules verbatim unless license implications are accepted.

### Obsidian Web Clipper

Use as UX/product reference:

- Capture and save durable Markdown.
- Reader-first capture, not bookmark-only.
- Templates/highlights later, not in MVP.

---

## Phase 1: Rename the concept from bookmark to reading

### Task 1: Update user-facing save copy without changing storage yet

**Objective:** Preserve current behavior while making the product intent clear.

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/java/com/juani/paywallreader/ui/reader/ReaderScreen.kt`

**Steps:**
1. Find strings and content descriptions for bookmark/save.
2. Rename user-facing labels to “Guardar lectura” or “Guardar offline”.
3. Keep internal `ReadingItemEntity` unchanged in this task.
4. Run: `./gradlew testDebugUnitTest assembleDebug`.

**Acceptance:** The toolbar action still works, but the app no longer communicates “bookmark” as a plain URL bookmark.

### Task 2: Add a product-level comment/KDoc for the save action

**Objective:** Prevent future regressions where save becomes URL-only again.

**Files:**
- Modify: `app/src/main/java/com/juani/paywallreader/ui/reader/ReaderScreen.kt`

**Steps:**
1. Add KDoc or inline comment near `saveCurrentForLater` explaining the intended evolution: save clean article locally, not just URL.
2. Keep code behavior unchanged.
3. Run unit tests.

**Acceptance:** Future implementers see the product direction at the current save seam.

---

## Phase 2: Add local article model beside current reading items

### Task 3: Create `ArticleEntity`

**Objective:** Add the durable saved-article data model without changing UI yet.

**Files:**
- Create: `app/src/main/java/com/juani/paywallreader/data/local/ArticleEntity.kt`
- Modify: `app/src/main/java/com/juani/paywallreader/data/local/AppDatabase.kt`

**Suggested fields:**

```kotlin
@Entity(
    tableName = "articles",
    indices = [Index(value = ["originalUrl"], unique = true)]
)
data class ArticleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val originalUrl: String,
    val resolvedUrl: String,
    val title: String,
    val sourceName: String,
    val author: String? = null,
    val siteName: String? = null,
    val publishedAt: String? = null,
    val description: String? = null,
    val imageUrl: String? = null,
    val faviconUrl: String? = null,
    val markdownContent: String? = null,
    val cleanHtmlContent: String? = null,
    val textContent: String? = null,
    val wordCount: Int = 0,
    val extractionEngine: String? = null,
    val extractionDebug: String? = null,
    val savedAt: Long = System.currentTimeMillis(),
    val lastReadAt: Long? = null,
    val readingProgress: Float = 0f,
)
```

**Steps:**
1. Create entity.
2. Add entity to `@Database`.
3. Increment database version.
4. Add migration creating `articles` table.
5. Run Room compile via `./gradlew testDebugUnitTest`.

**Acceptance:** Database compiles and existing tests pass.

### Task 4: Create `ArticleDao`

**Objective:** Support saving and reading clean articles locally.

**Files:**
- Create: `app/src/main/java/com/juani/paywallreader/data/local/ArticleDao.kt`
- Modify: `app/src/main/java/com/juani/paywallreader/data/local/AppDatabase.kt`

**Methods:**

```kotlin
@Dao
interface ArticleDao {
    @Query("SELECT * FROM articles ORDER BY savedAt DESC")
    fun observeArticles(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE id = :id LIMIT 1")
    suspend fun getArticle(id: Long): ArticleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(article: ArticleEntity): Long

    @Query("DELETE FROM articles WHERE id = :id")
    suspend fun delete(id: Long)
}
```

**Acceptance:** DAO compiles and database exposes `articleDao()`.

### Task 5: Add `ArticleRepository`

**Objective:** Separate saved-article persistence from `SourceRepository`.

**Files:**
- Create: `app/src/main/java/com/juani/paywallreader/data/repository/ArticleRepository.kt`

**Steps:**
1. Add wrapper around `ArticleDao`.
2. Provide `saveArticle(...)`, `observeArticles()`, `getArticle(id)`.
3. Keep existing `SourceRepository.saveForLater` untouched.

**Acceptance:** Repository compiles and no current UI behavior changes.

---

## Phase 3: Defuddle extraction in WebView

### Task 6: Add Defuddle bundle as an asset

**Objective:** Make the extraction engine available to the Android WebView.

**Files:**
- Create: `app/src/main/assets/defuddle.full.js`
- Add attribution: `app/src/main/assets/THIRD_PARTY_NOTICES.md` or existing notices file if present.

**Steps:**
1. Pin a Defuddle version/commit.
2. Build or download the browser/full bundle.
3. Save under assets.
4. Add MIT attribution.
5. Run `./gradlew assembleDebug`.

**Acceptance:** APK builds with Defuddle asset included.

### Task 7: Create extraction payload model

**Objective:** Parse Defuddle output safely in Kotlin.

**Files:**
- Create: `app/src/main/java/com/juani/paywallreader/domain/model/ExtractedArticle.kt`

**Fields:**
- title
- originalUrl
- resolvedUrl
- author
- siteName
- publishedAt
- description
- imageUrl
- faviconUrl
- markdownContent
- cleanHtmlContent
- textContent
- wordCount
- debugJson

**Acceptance:** Model has sane defaults and can represent partial extraction.

### Task 8: Replace Afterlight capture script with internal extraction seam

**Objective:** Keep the same toolbar button but route the captured payload internally.

**Files:**
- Modify: `app/src/main/java/com/juani/paywallreader/ui/reader/ReaderScreen.kt`
- Modify: `app/src/main/java/com/juani/paywallreader/ui/reader/ReaderViewModel.kt`

**Steps:**
1. Keep current `AFTERLIGHT_CAPTURE_SCRIPT` behavior as fallback only.
2. Add `DEFUDDLE_CAPTURE_SCRIPT` that loads/expects Defuddle and returns JSON.
3. Add `ReaderViewModel.saveExtractedArticle(...)`.
4. On save: call Defuddle extraction → save `ArticleEntity`.
5. If Defuddle fails: save basic article with title/url/text fallback.
6. Keep the existing Afterlight intent only behind temporary fallback/compatibility if desired.

**Acceptance:** Tapping save creates an `ArticleEntity` even if Afterlight is not installed.

### Task 9: Chunk large JS payloads if needed

**Objective:** Avoid losing large Markdown/HTML payloads through `evaluateJavascript` limits/escaping.

**Files:**
- Modify: `ReaderScreen.kt`

**Steps:**
1. Start with direct JSON result.
2. If tests/manual capture show truncation, add `JavascriptInterface` chunking.
3. Keep JS bridge methods narrow and validate source state.

**Acceptance:** Long articles save reliably.

---

## Phase 4: In-app readings/library surface

### Task 10: Add Library route shell

**Objective:** Add a saved-article UI without redesigning the app.

**Files:**
- Modify: `app/src/main/java/com/juani/paywallreader/ui/navigation/AppNavigation.kt`
- Create: `app/src/main/java/com/juani/paywallreader/ui/library/LibraryScreen.kt`
- Create: `app/src/main/java/com/juani/paywallreader/ui/library/LibraryViewModel.kt`

**Material 3 Expressive requirements:**
- Use existing `PaywallReaderTheme`.
- Use current card shapes/elevations where possible.
- Prefer expressive Material components already in use: cards, floating toolbars, wavy progress where appropriate.
- No visual reset/rebrand.

**Acceptance:** Library screen lists saved articles with title/source/date and opens detail placeholder.

### Task 11: Add offline article reader screen

**Objective:** Read clean saved content inside paywall-reader.

**Files:**
- Create: `app/src/main/java/com/juani/paywallreader/ui/library/ArticleReaderScreen.kt`

**MVP:**
- Render clean HTML in a local WebView with JavaScript disabled, or render Markdown in Compose if dependency is already available.
- Show title, source, and original URL action.
- Preserve reader typography and calm visual style.

**Acceptance:** Saved articles can be opened offline from Library.

---

## Phase 5: Android share-sheet capture

### Task 12: Add share intent filters

**Objective:** Let PaywallReader receive links from any app.

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/java/com/juani/paywallreader/MainActivity.kt`

**Intent filters:**
- `ACTION_SEND` with `text/plain`
- Optional `ACTION_VIEW` with `http`/`https` later, if product wants direct open links.

**Acceptance:** PaywallReader appears in Android share sheet for links/text.

### Task 13: Parse shared URL and route to capture

**Objective:** Shared links enter the same pipeline as reader saves.

**Files:**
- Modify: `MainActivity.kt`
- Modify: `AppNavigation.kt`

**Steps:**
1. Extract first URL from shared text.
2. Open Reader route with source name inferred from host.
3. Show a banner/state: “Listo para guardar como lectura limpia”.
4. User can inspect or save.
5. Later iteration can auto-save after extraction if desired.

**Acceptance:** Sharing a URL opens it in paywall-reader and allows saving clean content.

---

## Phase 6: Rule engine inspired by Periscope/Ladder

### Task 14: Create `SiteRule` and `RuleMatcher`

**Objective:** Stop hardcoding site behavior in `ReaderScreen.kt`.

**Files:**
- Create: `app/src/main/java/com/juani/paywallreader/domain/rules/SiteRule.kt`
- Create: `app/src/main/java/com/juani/paywallreader/domain/rules/RuleMatcher.kt`
- Test: `app/src/test/java/com/juani/paywallreader/domain/rules/RuleMatcherTest.kt`

**Core model:**

```kotlin
data class SiteRule(
    val id: String,
    val domains: List<String>,
    val paths: List<String> = emptyList(),
    val preferredBackend: ReaderBackend = ReaderBackend.Original,
    val fallbackChain: List<ReaderBackend> = emptyList(),
    val headers: Map<String, String> = emptyMap(),
    val cleanupSelectors: List<String> = emptyList(),
    val paywallMarkers: List<String> = emptyList(),
)

enum class ReaderBackend {
    Original,
    RemovePaywalls,
    Periscope,
    AccessArticleNow,
    Unwall,
    Archive,
}
```

**Acceptance:** Matching handles exact host and subdomains safely; `evilnytimes.com` must not match `nytimes.com`.

### Task 15: Move existing fallback constants into rules

**Objective:** Preserve behavior but make it declarative.

**Files:**
- Modify: `ReaderScreen.kt`
- Create/Modify: rules registry file.

**Steps:**
1. Move NYTimes/Periscope preference into a rule.
2. Move Wired/Medium/Substack fallback behavior into rules.
3. Move paywall marker strings into rules or `PaywallDetector`.
4. Keep UI unchanged.

**Acceptance:** Existing default sources still open as before.

### Task 16: Add debug metadata for captures

**Objective:** Know which rule/backend produced a saved article.

**Files:**
- Modify: `ArticleEntity.kt`
- Modify: extraction/save path.

**Fields:**
- `ruleId`
- `backendUsed`
- `extractionEngine`
- `extractionDebug`

**Acceptance:** Saved articles store enough debug info to diagnose bad captures.

---

## Phase 7: Deprecate Afterlight cleanly

### Task 17: Stop sending new captures to Afterlight by default

**Objective:** Make paywall-reader the source of truth for saved readings.

**Files:**
- Modify: `ReaderScreen.kt`
- Modify: `AndroidManifest.xml`

**Steps:**
1. Save internally first.
2. Remove automatic `sendToAfterlight` call.
3. Keep optional “Export/share” later if needed.
4. Remove `<queries>` for Afterlight only after no code references it.

**Acceptance:** PaywallReader works fully without Afterlight installed.

### Task 18: Update naming and docs

**Objective:** Make deprecation explicit.

**Files:**
- Modify: README if present.
- Add release note/changelog if project uses one.

**Acceptance:** The product direction is clear: Afterlight functionality is integrated into paywall-reader.

---

## Validation Commands

Use local Android environment:

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export ANDROID_HOME=/opt/android-sdk
export PATH=/home/hermes/.local/gradle/gradle-9.4.1/bin:$PATH
./gradlew testDebugUnitTest assembleDebug
```

For every PR:

```bash
git diff --check
git status --short
```

For APK handoff:

```bash
cp app/build/outputs/apk/debug/app-debug.apk /home/hermes/.hermes/media/apks/paywall-reader-debug-<date>.apk
sha256sum /home/hermes/.hermes/media/apks/paywall-reader-debug-<date>.apk
/opt/android-sdk/build-tools/37.0.0/aapt dump badging /home/hermes/.hermes/media/apks/paywall-reader-debug-<date>.apk | sed -n '1,3p'
```

---

## First PR Recommendation

Start with a small PR that does **not** attempt the full architecture:

1. Rename save affordance to reading/offline language.
2. Add `ArticleEntity`, `ArticleDao`, `ArticleRepository`.
3. Save a basic article internally when the toolbar save action is tapped.
4. Keep Afterlight compatibility only as fallback.
5. Preserve UI exactly except copy change.

Then the second PR can add Defuddle extraction.

---

## Non-goals for MVP

- No full redesign.
- No background clipboard scraping.
- No FlareSolverr local integration.
- No copying Ladder GPLv3 code/rules.
- No template/highlight system until clean saving and reading works.
- No cloud backend unless explicitly requested later.
