# MindVault Design System

**Document purpose:** The single source of truth for MindVault's visual
design and interaction patterns. AI coding assistants should read this
file before implementing or modifying UI.

**Product:** MindVault --- a personal knowledge app for saving words and
concepts, searching for new information, organizing a personal Vault,
and revising with flashcards.

**Design direction:** Calm, modern, and distinctive. Use a balanced Sage
& Stone palette with a little more depth and color than a washed-out
pastel interface. Green is the brand identity, not the background color
of every component.

**Platform and UI stack:** Android app built with Kotlin, Jetpack
Compose, and Material 3. Follow the project's existing architecture and
dependencies; do not introduce a new UI framework.

------------------------------------------------------------------------

## 1. Core design principles

1.  **Balanced contrast:** Use warm stone neutrals for most backgrounds,
    pale sage for selected surfaces, medium sage for secondary emphasis,
    and forest green for primary actions and active states.
2.  **Green with restraint:** Do not make the whole app dark green, and
    do not make every card green. Most content surfaces should be
    neutral. Use green where it communicates hierarchy or action.
3.  **Color with purpose:** Category chips and small icons may use muted
    gold, terracotta, slate blue, and muted lilac as accents. These are
    supporting accents within the same design system, not separate
    themes.
4.  **Readable first:** Use clear typography, sufficient text contrast,
    comfortable touch targets, and predictable layouts.
5.  **Consistent navigation:** Keep the same bottom navigation order on
    all main destinations.
6.  **Content over decoration:** Botanical details or illustrations may
    appear sparingly on the Home screen or featured flashcards. Never
    let decoration compete with definitions or lists.
7.  **Reusable components:** Implement shared design tokens and
    components rather than hardcoding styles per screen.
8.  **Local-first MVP:** UI must work with locally persisted data. Never
    imply that data was saved, searched, or synced unless the relevant
    operation actually succeeded.

## 2. Color system

Use these values as the initial canonical palette. Define them centrally
in a Compose theme file, for example `ui/theme/Color.kt`. Do not
duplicate hex values throughout screens.

### 2.1 Brand and neutral colors

  -----------------------------------------------------------------------
  Token                   Hex                     Intended use
  ----------------------- ----------------------- -----------------------
  `Background`            `#F0F1E9`               Main app background;
                                                  warm stone, not pure
                                                  white

  `Surface`               `#FAFAF6`               Standard cards, sheets,
                                                  input fields

  `SurfaceVariant`        `#E6E9DF`               Secondary cards,
                                                  inactive segmented
                                                  controls, grouped areas

  `SageLight`             `#D5E0D2`               Selected category
                                                  backgrounds, welcome
                                                  panels, subtle emphasis

  `Sage`                  `#779782`               Secondary emphasis,
                                                  progress indicators,
                                                  decorative details

  `SageDeep`              `#4F765F`               Secondary buttons,
                                                  selected icon
                                                  containers, medium
                                                  emphasis

  `Forest`                `#286348`               Primary buttons, active
                                                  navigation icon/label,
                                                  selected filters

  `ForestPressed`         `#1E5039`               Pressed state for
                                                  primary green controls

  `TextPrimary`           `#202820`               Main headings and body
                                                  copy

  `TextSecondary`         `#5F685F`               Supporting descriptions
                                                  and metadata

  `TextOnPrimary`         `#FFFFFF`               Text/icons on Forest
                                                  backgrounds

  `Border`                `#D6DCCF`               Subtle outlines and
                                                  separators

  `Disabled`              `#AEB5AA`               Disabled icons,
                                                  controls, and
                                                  supporting text where
                                                  appropriate

  `Scrim`                 `#202820` at 32%        Modal and bottom-sheet
                          opacity                 backdrop
  -----------------------------------------------------------------------

### 2.2 Supporting accent colors

Use accents in small, controlled areas such as category icons, tags, and
status indicators. Keep their backgrounds pale and their foregrounds
dark enough to read.

  --------------------------------------------------------------------------
  Accent token         Foreground        Pale background   Use
  -------------------- ----------------- ----------------- -----------------
  `AccentGold`         `#79571B`         `#F6E8C7`         General
                                                           knowledge,
                                                           featured items

  `AccentTerracotta`   `#984D3E`         `#F4DED7`         Language,
                                                           creativity, or a
                                                           category color

  `AccentBlue`         `#3559A6`         `#DFE7FA`         Technology and
                                                           science
                                                           categories

  `AccentLilac`        `#6247A5`         `#E9E1F8`         Flashcard and
                                                           study-related
                                                           category accents

  `AccentMint`         `#23694D`         `#DCEDE2`         Completed or
                                                           mastered state
  --------------------------------------------------------------------------

Accent colors are optional identifiers, not a second full theme. Do not
use every accent on every screen. Assign category colors consistently
when the app supports user-defined categories.

### 2.3 Color usage proportions

As a visual guideline, not a strict pixel calculation:

-   **60--70%** warm stone and neutral surfaces.
-   **20--30%** pale sage and other subtle surface variations.
-   **5--10%** medium/deep green and supporting accents.

Forest green should be visible and recognizable, but it should not cover
the entire screen. Avoid full-screen dark-green backgrounds. Avoid
pure-white cards against a pure-white background when this removes
useful visual grouping.

### 2.4 Contrast and accessibility

-   Body text must remain clearly readable on all surfaces. Prefer
    `TextPrimary` on light surfaces.
-   Use `TextOnPrimary` on `Forest` and `ForestDeep` controls.
-   Do not place `Sage` or other medium-light colors behind small white
    text unless contrast has been checked.
-   Never communicate meaning through color alone; pair color with text,
    an icon, or another state cue.
-   Provide visible focus, pressed, selected, disabled, and error
    states.
-   Aim for WCAG AA contrast for text where practical: 4.5:1 for normal
    text and 3:1 for large text and meaningful UI boundaries/icons.
-   Check actual rendered contrast before finalizing; palette values are
    starting tokens, not a guarantee that every pairing passes.

## 3. Typography

Use the default Android sans-serif family unless the project already has
a deliberate, available font. Prefer a clean, readable system such as
Roboto. Do not mix decorative serif fonts into standard interface text.
A restrained editorial style can be used for a featured flashcard term
only if it remains legible and consistent.

Use `sp` for text sizes and follow Material 3 typography conventions.

  -----------------------------------------------------------------------
  Style             Size / line       Weight            Usage
                    height                              
  ----------------- ----------------- ----------------- -----------------
  `DisplaySmall`    32 / 38 sp        Semibold          Rare hero message
                                                        only

  `HeadlineSmall`   24 / 30 sp        Semibold          Screen titles
                                                        when space allows

  `TitleLarge`      20 / 26 sp        Semibold          Main section or
                                                        card titles

  `TitleMedium`     16 / 22 sp        Semibold          Item titles,
                                                        dialog titles

  `BodyLarge`       16 / 24 sp        Normal            Definitions and
                                                        primary reading
                                                        content

  `BodyMedium`      14 / 20 sp        Normal            Standard
                                                        descriptions and
                                                        list subtitles

  `BodySmall`       12 / 16 sp        Normal            Metadata only;
                                                        avoid for
                                                        essential
                                                        information

  `LabelLarge`      14 / 20 sp        Medium            Buttons and
                                                        prominent labels

  `LabelMedium`     12 / 16 sp        Medium            Chips and compact
                                                        navigation labels

  `LabelSmall`      11 / 14 sp        Medium            Optional tertiary
                                                        metadata only
  -----------------------------------------------------------------------

Rules: - Use sentence case for headings, buttons, and labels. - Keep
long definitions comfortable to read with line spacing. - Avoid all-caps
labels, excessive bolding, and too many font sizes. - Do not truncate
definitions in the detail view. In preview cards, truncate with a clear
route to the full entry. - Respect Android font scaling and
accessibility settings.

## 4. Spacing, sizing, and layout

Use a consistent 4 dp spacing grid. Define spacing tokens centrally.

  ------------------------------------------------------------------------
  Token                                        Value Typical use
  --------------------- ---------------------------- ---------------------
  `space1`                                      4 dp Icon-to-label micro
                                                     gap

  `space2`                                      8 dp Compact internal
                                                     spacing

  `space3`                                     12 dp Chip gaps, compact
                                                     card padding

  `space4`                                     16 dp Standard screen
                                                     horizontal padding

  `space5`                                     20 dp Section spacing

  `space6`                                     24 dp Major section
                                                     separation

  `space8`                                     32 dp Large separation or
                                                     hero content

  `screenHorizontal`                           16 dp Default horizontal
                                                     screen inset

  `cardPadding`                                16 dp Standard card content
                                                     inset

  `listItemMinHeight`                          64 dp Typical Vault list
                                                     row; may grow for
                                                     wrapping text

  `touchTargetMin`                             48 dp Minimum interactive
                                                     target
  ------------------------------------------------------------------------

Additional rules: - Use 16 dp horizontal padding on typical phone
layouts. Adjust responsively for larger screens. - Prefer consistent
section spacing over arbitrary margins. - Avoid both extremes: do not
cram too many controls into one screen, and do not leave large empty
areas without purpose. - Use scrolling for long lists and content. Keep
primary navigation fixed. - Respect system status and navigation bars,
display cutouts, and keyboard insets.

## 5. Shape, elevation, and component style

### 5.1 Shapes

  Token                        Radius Usage
  ------------------- --------------- ------------------------------------
  `ShapeSmall`                   8 dp Small tags, compact elements
  `ShapeMedium`                 12 dp Inputs, chips, small cards
  `ShapeLarge`                  16 dp Standard cards and buttons
  `ShapeExtraLarge`             24 dp Featured cards and bottom sheets
  `ShapePill`           Fully rounded Filter chips and segmented choices

Use rounded corners consistently but do not make every element
pill-shaped. Use full pills mainly for chips and segmented filters.

### 5.2 Elevation and borders

-   Prefer subtle elevation and a fine `Border` outline to separate
    cards from the stone background.
-   Use low elevation for ordinary cards; reserve more elevation for
    menus, dialogs, and the central Add action.
-   Avoid heavy shadows, glowing effects, and glassmorphism.
-   Do not rely on shadow alone to distinguish a component.
-   Avoid nesting several bordered cards inside each other unless the
    content hierarchy requires it.

### 5.3 Buttons

**Primary button** - Forest background, white label/icon, 16 dp corner
radius. - Used for main actions such as Save, Show Answer, and
confirming changes. - Provide enabled, pressed, loading, and disabled
states. - Disable repeated submissions while an operation is in
progress.

**Secondary button** - Pale sage or `SurfaceVariant` background with
`Forest` or `TextPrimary` content. - Used for supporting actions such as
Cancel or alternate paths.

**Tertiary/text button** - Transparent background with Forest text. -
Used for low-emphasis actions such as See all or Clear.

**Icon button** - Minimum 48 dp touch target, clear accessible
description, visible pressed state. - Destructive actions must use an
appropriate warning color and confirmation where data loss is
significant.

### 5.4 Cards

-   Standard card: `Surface` background, 16 dp radius, 16 dp padding,
    subtle border or elevation.
-   Highlight card: `SageLight` or a restrained sage tint. Use for one
    featured area, not every card.
-   Avoid full forest-green content cards except for small, intentional
    callouts.
-   Keep titles, descriptions, and category tags aligned consistently.
-   Use a small icon container or a category chip for visual variety
    rather than coloring the whole card.

### 5.5 Inputs and search fields

-   Use `Surface` backgrounds, `Border` outlines, 12--16 dp radius, and
    readable hint text.
-   Focused inputs use a Forest outline or indicator.
-   Search fields should have a leading search icon and a clear action
    when non-empty.
-   Labels should remain visible when a field contains text; do not rely
    only on placeholder text.
-   Show inline validation close to the affected field.
-   Multiline definition input should expand enough to make writing
    comfortable.

### 5.6 Chips, filters, and tags

-   Unselected filter: `SurfaceVariant` background and `TextPrimary`
    text.
-   Selected filter: `Forest` background and white text.
-   Category tags: use a restrained accent-tinted background and a
    readable foreground.
-   Keep chip sizes consistent and allow horizontal scrolling when
    necessary.
-   Make selected state obvious through both color and semantics.

### 5.7 Icons and illustrations

-   Prefer a consistent Material Symbols / Material Icons style, using
    either outlined or filled icons according to a clear rule.
-   Use filled icons for active bottom navigation destinations and
    outlined icons for inactive destinations when the chosen icon set
    supports both.
-   Keep icon sizes consistent: usually 20--24 dp for actions and 16--20
    dp for compact metadata.
-   Botanical illustrations are optional, sparse decorative elements for
    Home or selected learning cards only. Do not add leaves to every
    screen or behind dense text.

## 6. Navigation rules

### 6.1 Main bottom navigation

The main navigation order is fixed:

1.  **Search**
2.  **Vault**
3.  **Add** --- prominent centered action, not a normal destination
4.  **Flashcards**
5.  **Home**

Rules: - Keep this exact order on every main screen. - The four
destinations are Search, Vault, Flashcards, and Home. The centered Plus
opens an Add menu. - Active destination: Forest icon and label, with a
clear selected state. - Inactive destinations: neutral/secondary icon
and label. - The Add action uses a Forest circular button, slight
elevation, and a white plus icon. - Use a consistent navigation bar
surface, typically `Surface`, separated subtly from page content. - Keep
labels short and visible; do not rely on icons alone. - Respect system
navigation and gesture insets. - The Add menu may be a modal bottom
sheet with actions such as **Add a concept**, **Search and save**, and
**Create a flashcard**. Only show actions supported by the current MVP.

### 6.2 Navigation behavior

-   Switching between main destinations should preserve each
    destination's sensible state (such as a Vault filter or search
    query) when feasible.
-   Back navigation should return to the previous screen or dismiss the
    current modal before leaving a main destination.
-   Do not show duplicate bottom navigation bars.
-   Detail and form screens may keep the main navigation if that is the
    established app pattern; otherwise use a consistent top app bar and
    back action. Follow the current navigation architecture rather than
    inventing a new pattern per screen.
-   Use clear screen titles and predictable back behavior.

## 7. Screen specifications

### 7.1 Home --- dashboard-first

**Purpose:** Provide an at-a-glance overview and quick access to the
four main activities.

Recommended content order: 1. Top app bar with MindVault identity and a
profile/settings entry if implemented. 2. Prominent search shortcut. 3.
Short welcome or learning prompt in a restrained sage-tinted feature
card. 4. Compact summary cards for total saved items, items to review,
and learning streak/progress if supported by real data. 5. Quick
actions: Add Concept, Search, Flashcards, My Vault. 6. Recently saved
entries with title, short definition preview, and category tag. 7.
Optional See all action to open Vault.

Visual rules: - Background is `Background`. - Keep only one main
featured sage card near the top. - Summary cards should mostly use
`Surface`; use colored icons for differentiation. - Do not invent
progress numbers or streaks. Hide unavailable statistics or show a
meaningful empty state. - If the user has no saved content, explain how
to add the first concept and provide a clear action.

### 7.2 Vault --- library-first

**Purpose:** Browse and manage all saved words and concepts.

Recommended content order: 1. Top app bar with title "My Vault" and
filter/sort actions. 2. Search within saved content. 3. Filter chips
such as All, Words, Concepts, and Tags/categories as supported. 4. Item
count and sort control (alphabetical, recently added, or other
implemented sort). 5. Scrollable list of saved entries. 6. Empty and
no-results states.

List row contents: - Small category/icon container. - Title. - One- or
two-line definition preview. - Category/tag where relevant. - Optional
overflow menu for Edit and Delete.

Visual rules: - List background uses `Background`; rows use `Surface`. -
Use a neutral row surface rather than alternating strong green cards. -
Use category accents sparingly and consistently. - Keep sort/filter
state visible. - Delete must never be implied as successful before
persistence confirms the change. Use confirmation for destructive
actions.

### 7.3 Flashcards --- learning-first

**Purpose:** Help users actively recall saved concepts and manage decks.

Recommended content order: 1. Screen title "Flashcards". 2. Tabs or
segmented controls for Review, All Cards, and Stats if these features
exist. 3. Optional deck selector when decks are supported. 4. A centered
flashcard with progress (for example, card 4 of 12), term/prompt, and
category. 5. Clear Show Answer action. 6. After reveal, show the
definition/answer and available self-assessment controls. 7.
Previous/next controls and session completion state.

Visual rules: - Use a pale sage or neutral card with Forest primary
action. - Use small illustrative accents only if they do not compete
with the text. - The term and definition must be readable and visually
distinct. - Do not mark a card as mastered or update statistics unless
the user action and persistence logic support it. - Provide clear states
for no cards, completed session, and unavailable review queue.

### 7.4 Search --- lookup-first

**Purpose:** Search for words/concepts and save useful results.

Recommended content order: 1. Title "Search" and a prominent query
field. 2. Scope filters such as All, Words, Concepts, Saved if
supported. 3. Suggested query prompts when the field is empty. 4. Recent
searches if stored. 5. Search results with title, concise definition,
and source/status where relevant. 6. A clear Save action for results
that are not already in the Vault.

Visual rules: - Keep the page predominantly neutral. - Use Forest for
selected scope filters and primary Save actions. - Loading, empty,
no-results, offline, and error states must be distinguishable. - Do not
fabricate external search results. If network search is not implemented
or unavailable, state that clearly and preserve manual entry as a usable
path. - Prevent duplicate saves or show an appropriate already-saved
state.

### 7.5 Add menu

**Purpose:** Give quick access to supported creation flows.

Open from the centered Plus button. Use a modal bottom sheet with a
scrim and clear dismissal behavior.

Initial options: - **Add a concept** --- manually enter title,
definition, category, and tags. - **Search and save** --- open Search. -
**Create a flashcard** --- create a card or deck if supported by the
MVP.

Additional actions such as scanning text or voice input must not be
shown unless implemented. Do not display decorative options that do
nothing.

### 7.6 Add/Edit concept

**Purpose:** Create or update a saved word or concept.

Recommended fields: - Title (required). - Definition (required). -
Category (optional if the data model allows). - Tags (optional if
supported). - Save and Cancel/back actions.

Rules: - Use persistent field labels and suitable keyboard types. - Show
validation inline. - Preserve entered data if validation fails. -
Prevent accidental double saves. - On successful save, update the local
database and navigate or show feedback according to the established app
flow. - Editing should prefill existing values. - Unsaved changes should
not disappear silently if the user navigates away.

### 7.7 Concept detail

**Purpose:** Read and review a saved entry.

Recommended content: - Title, category, and tags. - Full definition. -
Optional notes, related concepts, or linked flashcards only if
implemented. - Edit and Delete actions. - A clear action to create a
flashcard from the concept if supported.

Rules: - Prioritize readable text and generous line height. - Keep
decorative elements secondary. - Related concepts must come from actual
stored or retrieved data, not placeholders presented as facts.

### 7.8 Profile and Settings

**Purpose:** Provide app preferences and basic management.

-   Access Profile from the Home app bar or established navigation.
-   Settings should be reachable from Profile.
-   Use standard list rows with icons, labels, and supporting text where
    useful.
-   Include only implemented settings. Do not show nonfunctional sync,
    account, notification, or theme toggles.
-   Keep visual styling consistent with the same palette and component
    rules.

## 8. Interaction and component states

Every interactive component must define the following states as
relevant:

  -----------------------------------------------------------------------
  State                   Visual behavior         Functional behavior
  ----------------------- ----------------------- -----------------------
  Default                 Standard token-based    Ready for interaction
                          styling                 

  Pressed                 Subtle darkening or     Responds once to the
                          tonal overlay           interaction

  Focused                 Visible Forest          Keyboard or
                          outline/indicator       accessibility focus is
                                                  clear

  Selected                Forest fill or clear    Selection state is
                          selected treatment      reflected semantically

  Disabled                Muted foreground and    Cannot be activated
                          surface                 

  Loading                 Inline progress         Prevent duplicate
                          indicator or button     submission
                          loading state           

  Success                 Brief confirmation or   Only after the
                          updated saved state     operation succeeds

  Error                   Clear message,          Preserve user input
                          appropriate error       where possible
                          color, recovery action  
                          where possible          

  Empty                   Helpful explanation and No fake or placeholder
                          relevant next action    content presented as
                                                  real

  No results              Explain that nothing    Filters/query remain
                          matched and offer a way editable
                          to revise the query     
  -----------------------------------------------------------------------

Further interaction rules: - Add content should provide immediate
feedback only after the local save succeeds. - Search results should
distinguish loading, successful results, no results, and failure. - A
selected filter must visibly differ from unselected filters. - A
flashcard should clearly distinguish prompt from revealed answer. -
Destructive actions require an intentional user action; deletion should
not occur from an accidental tap on a tiny target. - Support keyboard
navigation and screen-reader descriptions where applicable. - Use
haptics only if already part of the project conventions and appropriate
to the action; do not add unnecessary motion.

## 9. Motion and feedback

-   Use short, restrained transitions for bottom sheets, tab changes,
    and card reveal.
-   Motion should clarify state, not decorate every tap.
-   Avoid bouncing, glowing, parallax, and excessive animated gradients.
-   Respect Android animation settings and reduced-motion expectations
    where possible.
-   Use snackbars or inline feedback for save/update/delete outcomes.
    Avoid stacking multiple transient messages.

## 10. Accessibility and responsive behavior

-   Keep important interactive targets at least 48 dp.
-   Support font scaling and content wrapping.
-   Ensure labels do not clip on narrow screens or larger font settings.
-   Do not encode category or completion status using color alone.
-   Add meaningful content descriptions to non-decorative icons; mark
    decorative images as decorative.
-   Keep keyboard and bottom-sheet behavior usable when entering a
    concept.
-   For larger screens, expand content width and spacing sensibly rather
    than stretching text across the entire screen.
-   Test light surfaces, accent tags, buttons, and active/inactive
    navigation for contrast.

## 11. Compose implementation guidance

-   Keep design tokens in shared theme files, e.g. `Color.kt`,
    `Type.kt`, `Shape.kt`, and `Theme.kt`.
-   Prefer Material 3 components and theme values where practical.
-   Build reusable composables for primary/secondary buttons, search
    field, category chip, concept list row, empty state, section header,
    and the bottom navigation.
-   Use one shared bottom navigation implementation for all main
    destinations.
-   Keep business logic and persistence outside composables according to
    the project's MVVM architecture.
-   Represent loading, success, empty, and error states explicitly in UI
    state.
-   Avoid hardcoded colors, text sizes, corner radii, or spacing in
    individual screens when a token exists.
-   Do not add dependencies or replace architectural patterns without a
    clear need and explicit project-level agreement.
-   Preserve the existing MVP scope and data model unless a requirement
    explicitly changes.

## 12. Do and don't checklist

### Do

-   Use stone neutrals for the majority of each screen.
-   Use sage for selected surfaces and secondary emphasis.
-   Use forest green for primary actions and active navigation.
-   Use small accent colors for category identity and a few meaningful
    indicators.
-   Keep Home dashboard-first, Vault library-first, and Flashcards
    learning-first.
-   Keep the bottom navigation order: Search, Vault, Add, Flashcards,
    Home.
-   Reuse components and design tokens.
-   Show real data and honest operation states.

### Don't

-   Do not turn every screen or every card dark green.
-   Do not use a stark black-and-white theme.
-   Do not make the interface washed-out, low-contrast, or excessively
    pastel.
-   Do not introduce a separate color palette or a theme switcher unless
    explicitly requested.
-   Do not decorate every screen with leaves or illustrations.
-   Do not show controls for unimplemented features.
-   Do not fake search results, saved states, streaks, review counts, or
    mastery statistics.
-   Do not change the agreed navigation order.
-   Do not silently discard user input or claim a failed operation
    succeeded.

## 13. Final design acceptance checklist

Before marking a screen complete, verify:

-   [ ] It uses the shared palette and typography tokens.
-   [ ] Stone neutrals dominate; green adds hierarchy without
    overwhelming the screen.
-   [ ] Text and controls have sufficient contrast.
-   [ ] Spacing, corners, borders, and elevation match shared component
    rules.
-   [ ] The bottom navigation order and selected state are correct where
    applicable.
-   [ ] Loading, empty, error, disabled, and success states are handled
    as needed.
-   [ ] Every visible action works and provides honest feedback.
-   [ ] The layout handles small screens, long definitions, and font
    scaling.
-   [ ] No unsupported feature or fake data is presented as functional.
-   [ ] The screen matches the intended purpose: dashboard-first Home,
    library-first Vault, learning-first Flashcards.
