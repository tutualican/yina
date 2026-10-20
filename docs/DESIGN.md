# Design

> A calm surface, one job at a time, the feel of a physical object.

Yina does not try to be a decorative dashboard. It answers one question the
moment it opens: *what should I do now?* Everything else is secondary.

## Principles

1. **One primary action per screen.** On the home screen that is "start
   studying". Import and about are quiet, secondary controls.
2. **Hide complexity, keep capability.** Anki's options are powerful, but they
   are not a first-run experience. The default path is import → study.
3. **Physical motion.** A pressed surface compresses and springs back; a swiped
   row follows the finger and either completes or returns. Nothing loops or
   floats without a reason.
4. **Quiet visuals.** No gradients except a very low-contrast focus panel, no
   decorative shadows, no colour without meaning. Depth comes from tone
   differences between surfaces.
5. **Trust.** Answers are written immediately by Anki's engine. The last answer
   can be undone. Nothing is hidden behind an account.

## Colour

The palette is derived from the launcher artwork: ink, paper and pine, with a
single warm amber accent.

| Token | Light | Dark | Use |
|---|---|---|---|
| `background` | `#F4F2EC` | `#14171A` | Page |
| `surface` | `#FBFAF6` | `#191D21` | Cards, dialogs |
| `surfaceVariant` | `#E7E3D9` | `#262B30` | Secondary surfaces |
| `primary` | `#2E5B4C` | `#9FCDB8` | Primary action, Good |
| `tertiary` | `#B4763C` | `#E0A96D` | Rare emphasis, Hard |
| `error` | red family | red family | Errors, Again |

Rules:

- at most one saturated colour area per screen;
- red only for errors and the "Again" answer;
- colour never carries meaning alone; a label always accompanies it.

## Type

System font (Roboto on Android). No custom font in this release.

- Large due count: display size, semibold, tight tracking.
- Screen titles: headline small / title medium.
- Body: 14–16sp, generous line height.
- Labels: 12–14sp, medium weight.
- Turkish `İ/ı` must render correctly; avoid locale-sensitive `uppercase()`
  without an explicit locale.

## Space and shape

4dp grid. Page margin 20dp. Card interiors 18–26dp. Touch targets at least 44dp
(preferably 48dp). Rounded rectangles: 14dp for chips, 20dp for rows and action
buttons, 28dp for the card surface and focus panel.

## Motion

| Name | Spec | Where |
|---|---|---|
| Press | `spring(damping = 0.62, stiffness = 900)` → 0.975 scale | Every pressable surface |
| Settle | `spring(0.82, 380)` | Release, swipe return |
| Enter | `spring(0.88, 260)` | Sheets, large surfaces |
| Quick | `tween(180 ms)` | Counts and short text swaps |
| Fade | `tween(240 ms)` | Cross-fades |

Rules:

- no animation longer than 400ms;
- no infinite/looping motion except a loading indicator;
- animation never blocks input;
- respect the system "reduce motion" setting by falling back to opacity;
- haptics only for the primary action and swipe completion, not every tap.

## Screens

### Home

A small `Yina` wordmark and today's date, an import button, then one focus panel
with the number of cards due and a single "start studying" action. Below it, the
top-level decks with their card and due counts. Tapping or swiping a deck row
starts that deck. Empty collections explain how to import a package instead of
showing fake decks.

### Study

A compact top bar with back, deck name, remaining count and undo. The card fills
the available space inside a soft surface. Before revealing, there is one wide
"show answer" button. After revealing, four equal answer buttons show Anki's own
interval labels for Again, Hard, Good and Easy. A finished session shows a check,
the number of cards studied, and a way back to the decks.

The card HTML is Anki's own rendered template and notetype CSS, so it may look
different from the rest of the app. That is intentional: the content belongs to
the deck author. Yina only supplies the surrounding shell, dark-mode class and
media base URL.
