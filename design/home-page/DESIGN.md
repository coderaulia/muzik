---
name: Libadwaita Slate
colors:
  surface: '#131313'
  surface-dim: '#131313'
  surface-bright: '#393939'
  surface-container-lowest: '#0e0e0e'
  surface-container-low: '#1b1c1c'
  surface-container: '#1f2020'
  surface-container-high: '#2a2a2a'
  surface-container-highest: '#353535'
  on-surface: '#e4e2e1'
  on-surface-variant: '#c1c6d4'
  inverse-surface: '#e4e2e1'
  inverse-on-surface: '#303030'
  outline: '#8b919e'
  outline-variant: '#414752'
  surface-tint: '#a7c8ff'
  primary: '#a7c8ff'
  on-primary: '#003061'
  primary-container: '#4691f2'
  on-primary-container: '#002a55'
  inverse-primary: '#005eb2'
  secondary: '#cabeff'
  on-secondary: '#31009a'
  secondary-container: '#4a16d1'
  on-secondary-container: '#bcaeff'
  tertiary: '#48e087'
  on-tertiary: '#00391b'
  tertiary-container: '#00a65b'
  on-tertiary-container: '#003117'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#d5e3ff'
  primary-fixed-dim: '#a7c8ff'
  on-primary-fixed: '#001b3c'
  on-primary-fixed-variant: '#004788'
  secondary-fixed: '#e6deff'
  secondary-fixed-dim: '#cabeff'
  on-secondary-fixed: '#1c0062'
  on-secondary-fixed-variant: '#480fcf'
  tertiary-fixed: '#69fea1'
  tertiary-fixed-dim: '#48e087'
  on-tertiary-fixed: '#00210d'
  on-tertiary-fixed-variant: '#00522a'
  background: '#131313'
  on-background: '#e4e2e1'
  surface-variant: '#353535'
typography:
  display-lg:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.02em
  display-lg-mobile:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '700'
    lineHeight: 32px
    letterSpacing: -0.01em
  headline-md:
    fontFamily: Inter
    fontSize: 22px
    fontWeight: '600'
    lineHeight: 28px
    letterSpacing: -0.01em
  headline-sm:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
    letterSpacing: 0em
  title-md:
    fontFamily: Inter
    fontSize: 15px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0em
  body-default:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
    letterSpacing: 0em
  body-subtle:
    fontFamily: Inter
    fontSize: 13px
    fontWeight: '400'
    lineHeight: 18px
    letterSpacing: 0.01em
  label-ui:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.02em
  label-mono:
    fontFamily: JetBrains Mono
    fontSize: 11px
    fontWeight: '500'
    lineHeight: 14px
    letterSpacing: 0.04em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  space-2xs: 0.125rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 0.75rem
  space-lg: 1rem
  space-xl: 1.5rem
  space-2xl: 2rem
  space-3xl: 3rem
  header-height: 3rem
  transport-height: 4.5rem
  sidebar-width: 15rem
---

## Brand & Style

This design system channels the refined, purpose-driven ethos of modern GNOME and Libadwaita human interface guidelines. It treats the local music library as a first-class citizen of the operating system: native, responsive, tactile, and free from telemetry clutter or commercial intrusion.

The emotional core is quiet craftsmanship—delivering the balance between utility and visual restraint typical of contemporary Linux desktop environments. Interfaces are defined by structured window topology, purposeful contrast, high-information density without visual noise, and physical affordance delivered through flat-layer stacking rather than skeuomorphic excess.

The design movement combines **Modern Linux Desktop (Libadwaita)** with **Tonal Minimalism**:
- Structured titlebars ("HeaderBars") housing integrated controls and contextual window interactions.
- Low-contrast surface stacking over deep matte backdrops (`#1e1e1e` base).
- Unified, pill-based navigation tabs and view switchers.
- Vivid symbolic accents cutting through neutral dark tones to communicate active states and audio telemetry.

## Colors

The color palette is built strictly around authentic Libadwaita dark scheme tokens, utilizing warm-tinted dark slates and crisp border separations rather than pure blacks.

### System Palette Roles
- **Primary Accent (`#3584e4`)**: GNOME Blue. Used for primary call-to-actions, track progress bars, active view indicators, volume sliders, and selection states.
- **Secondary Accent (`#7856ff`)**: Purple / Violet. Reserved for playlist tagging, Hi-Res/lossless audio format badges (FLAC, DSD), and active mood filters.
- **Tertiary Status (`#33d17a`)**: GNOME Green. Signifies bit-perfect playback status, local sync completion, and audio engine health.
- **Surface Hierarchy**:
  - **Base Canvas (`#1e1e1e`)**: Root window canvas, playlist tracklists, and view viewports.
  - **Sidebar Surface (`#242424`)**: Left structural navigation and source hierarchy.
  - **HeaderBar & Bottom Transport Bar (`#2a2a2a`)**: Dominant horizontal anchors framing content.
  - **Card / Popover Surface (`#323232`)**: Floating inspector panels, context menus, and elevated cards.
- **Borders & Dividers (`#383838` / `#2d2d2d`)**: Subtle 1px structural lines establishing clear window-part borders without jarring contrast.
- **Text & Foreground**:
  - **Primary (`#ffffff`, 95% opacity)**: Track titles, modal headings, transport controls.
  - **Secondary (`#c0bfbc`, 70% opacity)**: Artist names, album titles, durations, column headers.
  - **Tertiary / Disabled (`#77767b`, 45% opacity)**: Inactive track numbers, empty states, scrubber background tracks.

## Typography

Typography prioritizes system native feel, legibility in dense lists, and clear tabular layout. `Inter` acts as the primary sans-serif body and display font (providing identical optical proportions to Cantarell and modern system fonts), while `JetBrains Mono` handles all audio metrics, sample rates, track numbers, and playback timers.

### Hierarchy Guidelines
- **Album / Artist Headers (`display-lg`)**: Bold, tightened kerning for hero headers in the collection inspector.
- **Track & Section Titles (`title-md`)**: Direct, clear visual weight used for song titles in list views and sidebar section titles.
- **Body & Metadata (`body-default`, `body-subtle`)**: Optimized for vertical row alignment across dense library grids and multi-column metadata views (Artist, Album, Year, Genre).
- **Timecodes & Bitrates (`label-mono`)**: Fixed-width digits guarantee zero layout jitter during scrubbing, playback increments (`02:45 / 04:12`), and format metrics (`FLAC 24-bit / 96kHz`).

## Layout & Spacing

The layout mirrors standard desktop window architecture:

1. **HeaderBar (Top, fixed 48px / `header-height`)**: Contains window control buttons, path navigation/view switchers, search entry, and window menu triggers.
2. **Body Workspace (Flexible)**:
   - **Source Sidebar (Left, 240px / `sidebar-width`)**: Fixed-width navigation tree (Playlists, Artists, Albums, Local Folders, Audio Devices).
   - **Content Viewport (Fluid)**: Dynamic grid (for album covers) or virtualized tabular list (for large libraries exceeding 50,000 tracks).
3. **Now-Playing Transport Bar (Bottom, fixed 72px / `transport-height`)**: Unbroken, persistent deck docked along the bottom edge, spanning edge-to-edge.

### Responsive Breakpoints
- **Desktop Wide (>1024px)**: 3-column split view (Permanent sidebar, dynamic grid/list, optional right collapsible track inspector/queue panel).
- **Desktop Compact / Tablet (640px – 1024px)**: Collapsible sidebar toggled via a HeaderBar burger/sidebar button; transport bar compresses metadata to single-line marquees.
- **Mobile Handheld (<640px)**: Sidebar converted to bottom-sheet navigation; window HeaderBar condenses to title and search; persistent transport bar simplifies to mini-player with swipe gestures.

## Elevation & Depth

This system avoids heavy drop shadows in favor of **Tonal Layers and Crisp Internal Dividers**, mirroring native GTK4/Adwaita rendering:

- **Level 0 (Base Canvas, `#1e1e1e`)**: Ground level. Holds table items, playlist rows, and grid backdrops.
- **Level 1 (Structural Anchors, `#242424` & `#2a2a2a`)**: The HeaderBar and Bottom Transport Bar sit on this level. Separated from Level 0 by a 1px solid border (`#2d2d2d`).
- **Level 2 (In-Window Floating Elements, `#323232`)**: Segmented pill selectors, text inputs, search entries, and inset cards. They sit flush inside panels with a subtle inset contrast or 1px border (`#3c3c3c`).
- **Level 3 (Overlays & Menus, `#383838`)**: Context popovers, track detail dropdowns, volume flyouts, and dialog windows. Raised with a soft perimeter ring (`1px solid rgba(255, 255, 255, 0.08)`) and an ambient shadow: `0 8px 20px rgba(0, 0, 0, 0.45)`.

## Shapes

Shapes emulate standard Libadwaita geometry: rounded rectangles with disciplined corner uniformity.

- **Window Framing**: Window canvas features `rounded-xl` (12px) corners on unmaximized windows, flattening to `0px` when snapped or maximized.
- **Pill Containers & View Switchers**: Completely circular caps (`9999px`) for segmented switches, category chips, and filter tags.
- **Standard Controls (Buttons, Inputs, Cards)**: Standardized at `rounded-md` to `rounded-lg` (6px to 8px) to balance modern softness with structural precision.
- **Album Artwork**: Standardized at 6px radius (`rounded-md`) with a faint 1px inner stroke to prevent bright album covers from visually bleeding into dark backgrounds.

## Components

### HeaderBars & Window Controls
- **Structure**: Continuous surface (`#2a2a2a`), height 48px, horizontal layout.
- **Window Controls**: Window close, minimize, and maximize buttons aligned to the configured desktop side (default right for standard GNOME). Circular interactive hit-boxes (24px diameter) with soft hover states (`rgba(255, 255, 255, 0.08)`).
- **View Switcher**: Centered pill group toggling between *Albums*, *Artists*, *Tracks*, and *Genres*. Active item displays a raised `#3a3a3a` background with `#ffffff` text; inactive tabs use `#c0bfbc`.

### Persistent Transport Bar (Bottom)
- **Container**: Docked bottom-edge horizontal bar (`#2a2a2a`), 72px tall, top border 1px solid `#383838`.
- **Left Zone (Track Info)**: 48px album thumb with 4px corner radius, stacked Track Title (`title-md`) and Artist (`body-subtle`).
- **Center Zone (Transport Controls & Progress)**:
  - Flat symbolic buttons (Shuffle, Previous, Play/Pause, Next, Repeat). Play/Pause button is a 36px filled circle (`#ffffff` icon on `#3584e4` circular surface).
  - Continuous scrub bar: 4px default height, expands to 6px on hover. Track fill is `#3584e4`; unfilled trough is `#3c3c3c`. Scrub handle is an 12px circle visible on hover.
  - Flanking timestamps using `label-mono` (`#c0bfbc`).
- **Right Zone (Utilities)**: Lossless quality badge (`secondary` purple pill), queue toggle, volume slider with speaker status icon.

### Buttons & Interactive Controls
- **Standard Push Button**: `#383838` background, 1px border `rgba(255,255,255,0.06)`, 32px height, 6px radius. Hover shifts to `#424242`; active/press shifts to `#2f2f2f`.
- **Suggested Action (Primary)**: Solid `#3584e4` fill with white text. Hover brightens to `#4a90e8`.
- **Destructive Action**: `#e01b24` background with white text.

### Search & Text Inputs
- Pill or rounded-rect input (`#222222`), 32px height, interior search glyph at left.
- Subtle inner border (`#333333`). Focus switches border to 2px solid `#3584e4` with zero glow/shadow bleed.

### Tracklist Table
- Alternating subtle row states: Default row transparent, hover state `#2a2a2a` with 4px border radius.
- Playing row: `#3584e4` text accent with a 3px vertical pill indicator along the extreme left border.
- Columns: Index Number (`label-mono`), Play State Icon, Title & Artist stacked or separated, Album, Duration (`label-mono`).

### Badges & Chips
- **Codec / Audio Metric Tags**: 18px height, `label-mono`, 4px radius, padded 4px 6px.
  - FLAC/Hi-Res: Background `rgba(120, 86, 255, 0.15)`, text `#a991ff`, border 1px solid `rgba(120, 86, 255, 0.3)`.
  - Bit-Perfect: Background `rgba(51, 209, 122, 0.15)`, text `#33d17a`, border 1px solid `rgba(51, 209, 122, 0.3)`.