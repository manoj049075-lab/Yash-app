# Web2APK Studio

Web2APK Studio is a clean, watermark-free, unlimited on-device Android application builder that converts HTML, CSS, JavaScript, and ZIP web assets—or any live Website URL—into standalone Unsigned/Signed APK packages and complete Android Studio source ZIP projects.

## User Review & Critical Decisions

> [!IMPORTANT]
> All core product choices below reflect your confirmed preferences from our clarification step and will be implemented with zero watermarks, zero build quotas, and a clean studio interface.

- **Confirmed Decision 1 — Focused Input Modes (No Cluttered Editor)**: The app provides dedicated, streamlined workflows for uploading single HTML files, multi-file web bundles (HTML, CSS, JS, images, fonts, or `.zip` archives), or entering a live Website URL—keeping the interface fast and uncluttered without a code editor.
- **Confirmed Decision 2 — Full Pro Customization Suite (Watermark-Free & Unlimited)**: Every project unlocks complete manifest, branding, layout, WebView, hardware bridge, offline error handling, and external link routing controls with no watermarks, forced branding, or build limits.
- **Confirmed Decision 3 — Dual Output Builder (APK + Android Studio ZIP)**: Every project can be compiled into an Unsigned APK, a V1/V2 Signed APK (using a built-in release/debug signing engine or custom keystore config), or exported as a complete, build-ready Android Studio Gradle project `.zip`, alongside an instant on-device WebApp Sandbox Runner to test the exact configured app behavior immediately.

---

## 1. Overview & Core Concept

- **What It Does**: Allows developers, creators, and businesses to package local HTML/CSS/JS files, `.zip` web projects, or live websites into native Android applications directly on their device. Users configure every detail of the target app—from package ID, version auto-increment, custom adaptive/monogram icon, and splash screen to full-screen kiosk modes, pull-to-refresh, JavaScript/DOM storage, hardware permissions, native `window.AndroidAPI` bridge, and custom offline pages—and export either an APK or a full Android Studio project archive.
- **Target Audience / Persona**: Web developers, indie makers, students, and agencies who want all the capabilities of `com.basitce.htmltoapk` in a distraction-free, modern interface with zero watermarks and unlimited builds.
- **Key Value**: Eliminates watermarks, paywalls, and artificial limits while delivering both instant APK packaging and clean, transparent Android Studio project generation.

---

## 2. User Experience & Visual Design

- **Key User Flows**:
  1. **Studio Dashboard & Project Hub**: Opens to a clean overview showing quick-start cards (**New HTML/ZIP Project**, **New Website URL App**), build statistics (Total Projects, APKs Built, 0 Watermarks Active badge), and saved project cards with one-tap **Build APK**, **Export Studio ZIP**, **Test App**, or **Edit Config**.
  2. **Step 1 — Source Selection (HTML/CSS/JS/ZIP or Website URL)**:
     - *Local Web Files Mode*: Pick a standalone `.html` file, select multiple `.html`, `.css`, `.js`, and media files, or import a `.zip` bundle via Android's native document picker. The app inspects the bundle, verifies `index.html` (or lets the user designate the entry file), and summarizes total asset size and file count.
     - *Website URL Mode*: Enter any `https://` or `http://` URL with quick protocol toggles, domain validation, and custom User-Agent presets (Mobile, Desktop, or Custom).
  3. **Step 2 — Identity, Icon & Splash Screen**:
     - Configure **App Name**, **Package Name** (e.g., `com.company.myapp` with real-time syntax validation), **Version Name**, **Version Code** (with an **Auto-Increment on Build** toggle), and **Minimum SDK** (API 24–35).
     - Choose an **App Icon**: pick any image from the device (with circular/rounded-square adaptive preview and background color picker) or generate a crisp vector monogram/emoji icon.
     - Configure a **Splash Screen**: toggle on/off, choose background color, centered logo/icon scale, optional tagline, and display duration (500ms–5000ms), explicitly guaranteed 100% free of third-party watermarks.
  4. **Step 3 — Display, Navigation & WebView Engine**:
     - *Layout & Screen*: Screen Orientation (Portrait, Landscape, Sensor Auto), Fullscreen / Immersive Mode (hide status & navigation bars), Keep Screen On (for kiosks/dashboards), Status Bar & Navigation Bar custom hex colors, and Top Toolbar visibility.
     - *WebView & Storage*: Enable/disable JavaScript, DOM Storage (`localStorage` / `sessionStorage`), IndexedDB, Pinch-to-Zoom, Built-in File Upload & Download Manager, Mixed Content (`http` inside `https`), and Native JavaScript Bridge (`window.AndroidAPI` for toast, vibration, clipboard, share, and device info).
     - *Navigation & Offline Rules*: Pull-to-Refresh gesture toggle, Back-Button History navigation (step back through pages before exiting), External Link policy (Open external domains in system browser vs. keep inside app), Custom Deep Link scheme (`myapp://`), Cache Strategy (Default, Cache-First Offline, No-Cache), and a customizable **Offline Error Page** (custom title, message, retry button theme, or uploaded fallback HTML).
     - *Hardware Permissions*: Granular toggles for Internet, Camera, Microphone, Fine Location, and Vibration.
  5. **Step 4 — Build Console, Export & Instant Sandbox Test**:
     - Select target output: **Signed APK (V1/V2)**, **Unsigned APK**, or **Android Studio Project (.zip)**.
     - Watch a step-by-step live build pipeline (Validating Manifest & Package ID → Bundling Web Assets & Splash → Generating Native Bridge & Offline Handler → Packaging Binary / Archive & Signing) with a detailed build log.
     - Save the generated `.apk` or `.zip` directly to device downloads/storage via the system file saver, share it via Android Share Sheet, or tap **Launch Live App Sandbox** to immediately run the configured app (including its custom splash screen, orientation, pull-to-refresh, offline fallback, and `window.AndroidAPI` bridge) on the device.

- **Visual Identity & Theme**:
  - *Aesthetic Direction*: **Precision Developer Studio** — ultra-clean, high-contrast utilitarian elegance inspired by modern build tools (Linear, Vercel, JetBrains Fleet).
  - *Color Palette & Mood*:
    - Dark Studio Surface (`#0B0F17` background, `#131B2E` elevated surface cards, `#1E293B` subtle borders) paired with a crisp Light Mode alternative (`#F8FAFC` background, `#FFFFFF` cards).
    - Primary Accent: Electric Cobalt (`#3B82F6`) for primary build actions and active configuration steps.
    - Secondary Accent: Emerald Mint (`#10B981`) for "Zero Watermark Verified" badges, successful APK builds, and valid package names.
    - Warning / Highlight Accent: Amber (`#F59E0B`) for permission indicators and unsigned APK notices.
  - *Typography & Hierarchy*:
    - Display & Section Headers: **Space Grotesk** (bundled locally) for crisp technical titles and build metrics.
    - Body & Controls: **Plus Jakarta Sans** for clean, scannable setting labels and descriptions.
    - Package IDs, URLs & Build Logs: **JetBrains Mono** for package names (`com.example.app`), URLs, version codes, and terminal build output.
  - *Component Styling & Layout*:
    - Adaptive Material 3 layout with segmented step pills, collapsible configuration accordions, interactive live icon/splash preview cards, and a bottom action bar that respects system navigation bar insets.

- **Interactive Feedback & Motion**:
  - Real-time package name and URL validation badges.
  - Interactive live preview card showing how the App Icon and Splash Screen will look before building.
  - Animated build progress stages with terminal log streaming and haptic/ripple confirmation on export.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Dual Packaging Engine (Real Installable/Exportable APK Binary Structure + Full Android Studio Gradle ZIP + On-Device WebApp Runtime)**
  - *Chosen Approach*: Because Android's OS security sandbox restricts apps from silently compiling arbitrary Java bytecode via `javac` without a multi-hundred-megabyte SDK download, our internal packaging engine generates:
    1. **Signed / Unsigned APK Packages (`.apk`)**: A structured APK archive containing compiled/serialized `AndroidManifest.xml`, `resources.arsc`, `classes.dex` WebView wrapper payload, bundled `assets/` (all user HTML/CSS/JS/ZIP files, generated `app_config.json`, and custom offline page), custom icon PNGs, and `META-INF/` signature block (`CERT.SF`, `CERT.RSA`, `MANIFEST.MF`) for signed builds.
    2. **Complete Android Studio Source Project (`.zip`)**: A 100% ready-to-compile Gradle project (`build.gradle.kts`, `settings.gradle.kts`, `AndroidManifest.xml`, full `MainActivity.kt` implementing every chosen WebView setting, `window.AndroidAPI` JS bridge, splash screen, pull-to-refresh, and bundled `src/main/assets/`), allowing one-click production AAB/APK compilation in Android Studio or CI.
    3. **Instant On-Device WebApp Runner**: Runs the user's exact uploaded HTML/CSS/JS bundle or Website URL inside a full-screen native container honoring every configured setting (splash screen, orientation, status bar colors, pull-to-refresh, JS bridge, and offline screen) right on the phone.
  - *Why*: Gives the user immediate downloadable `.apk` and `.zip` files with zero watermarks plus instant on-device execution of their web app.

- **Decision 2: Local Persistence with Room Database**
  - *Chosen Approach*: Store all saved projects, imported asset bundles, icon/splash configurations, and build history locally on the device using Room SQLite and internal project workspace directories.
  - *Why*: An on-device APK builder is an offline-capable developer utility; local Room storage ensures instant startup, zero login friction, and complete privacy for the user's source code and assets.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

- **Architecture & Component Diagram**:

```
┌───────────────────────────────────────────────────────────────────────────┐
│                         UI Layer (Jetpack Compose)                        │
│                                                                           │
│  ┌─────────────────────┐  ┌─────────────────────┐  ┌───────────────────┐  │
│  │  Studio Dashboard   │  │ 4-Step Project      │  │ Build Console &   │  │
│  │  & Build History    │  │ Configurator Wizard │  │ Live App Sandbox  │  │
│  └──────────┬──────────┘  └──────────┬──────────┘  └─────────┬─────────┘  │
└─────────────┼────────────────────────┼───────────────────────┼────────────┘
              │                        │                       │
              ▼                        ▼                       ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                     ViewModel & State Layer (MVVM)                        │
│                                                                           │
│  ┌─────────────────────────────────────────────────────────────────────┐  │
│  │ StudioViewModel (StateFlow<StudioUiState>, Form Validation,         │  │
│  │ Asset Import Handler, Build Progress Stream, Sandbox Controller)    │  │
│  └──────────────────┬───────────────────────────────────┬──────────────┘  │
└─────────────────────┼───────────────────────────────────┼─────────────────┘
                      │                                   │
                      ▼                                   ▼
┌─────────────────────────────────────────┐ ┌───────────────────────────────┐
│       Data & Workspace Repository       │ │   Packaging & Build Engine    │
│                                         │ │                               │
│  ┌───────────────────────────────────┐  │ │  ┌─────────────────────────┐  │
│  │ Room Database (WebAppProjectDao,  │  │ │  │ APK Binary & META-INF   │  │
│  │ BuildArtifactDao)                 │  │ │  │ Signer / Packager       │  │
│  └───────────────────────────────────┘  │ │  └─────────────────────────┘  │
│  ┌───────────────────────────────────┐  │ │  ┌─────────────────────────┐  │
│  │ Asset Workspace Manager           │  │ │  │ Android Studio Gradle   │  │
│  │ (HTML/CSS/JS/ZIP & Icon Storage)  │  │ │  │ Project ZIP Generator   │  │
│  └───────────────────────────────────┘  │ │  └─────────────────────────┘  │
└─────────────────────────────────────────┘ └───────────────────────────────┘
```

- **Data Model & State**:
  - `WebAppProjectEntity`: Stores project ID, app name, package name, version name, version code, auto-increment flag, min SDK, source type (`LOCAL_FILES` or `WEBSITE_URL`), entry URL or local workspace path, asset file count/size, icon URI/monogram config, splash screen settings (enabled, background hex, duration, tagline), display settings (orientation, fullscreen, keep screen on, toolbar, status/nav bar colors), WebView settings (JS, DOM storage, zoom, file access, desktop mode, mixed content, JS bridge, cache mode), navigation/offline settings (pull-to-refresh, back history, external link rule, deep link scheme, custom offline title/message), and permissions list.
  - `BuildRecordEntity`: Tracks every generated build artifact (project ID, build timestamp, version code/name, output format `SIGNED_APK`, `UNSIGNED_APK`, or `STUDIO_ZIP`, file size, local file path, and signing mode).

- **Interactive Component & State Mapping**:
  - **Source Picker & Inspector**: Uses `ActivityResultContracts.OpenMultipleDocuments` and `OpenDocument` to import `.html`, `.css`, `.js`, images, or `.zip` archives into a project-specific workspace directory, automatically extracting `.zip` contents and detecting `index.html`.
  - **Configuration Wizard Tabs**: Mutates draft project state with real-time validation (ensuring valid Java package names like `com.domain.app`, valid URLs, and non-empty version strings) and updates the live Icon & Splash preview card immediately.
  - **Build & Export Pipeline**: Runs asynchronously on `Dispatchers.IO`, streaming stage-by-stage progress (`0%` to `100%`) and log lines to the Build Console modal, then unlocks **Download / Save to Device** (`ActivityResultContracts.CreateDocument`), **Share File** (`FileProvider`), and **Run in Sandbox** actions.
  - **Live App Sandbox**: Renders an embedded Android `WebView` configured with the exact `WebSettings`, `WebViewClient` (offline error interceptor + external link routing), `WebChromeClient` (file chooser + permissions), custom splash screen overlay, pull-to-refresh wrapper, and `@JavascriptInterface` (`window.AndroidAPI`) defined in the project.
