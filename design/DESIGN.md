# Proctor design system

Used by the docs site and the generated HTML report. Compact, technical look: white canvas, grey
hairlines, black primary actions, one blue for links, monospace for model names and identifiers.
Flat surfaces, one-layer shadows at most. One dark panel per page (the hero) in light mode.

Code form: `design/tokens.css`. Every surface uses the tokens; no ad-hoc colors.

## Color

| Token | Light | Dark | Role |
| --- | --- | --- | --- |
| `canvas` | `#ffffff` | `#0b0f19` | Page background |
| `band` | `#f3f4f6` | `#101828` | Secondary surfaces, tag fills, table stripes |
| `card` | `#ffffff` | `#101828` | Cards, inputs |
| `hairline` | `#e5e7eb` | `#2c3344` | 1px borders and dividers |
| `hairline-strong` | `#d1d5dc` | `#3d4659` | Stronger dividers, placeholders |
| `ink` | `#000000` | `#f9fafb` | Primary text, icons, primary button fill |
| `body` | `#4a5565` | `#d1d5dc` | Secondary text |
| `caption` | `#6a7282` | `#99a1af` | Metadata, helper text, nav links |
| `disabled` | `#99a1af` | `#6a7282` | Tertiary text, disabled controls |
| `link` | `#155dfc` | `#51a2ff` | Active nav, links; one accent per view, never a fill |
| `hero` | `linear-gradient(to right bottom, #2c3344, #0b0f19)` | same | Hero panel and product preview only |

Result states (report only, derived; the source style has none): `pass` `#16794c` / `#4ade80`,
`fail` `#b42318` / `#f87171`, `not-graded` `#a15c07` / `#fbbf24`. Used as text and a 4px-radius
tag with a 12% tint, never as a large fill. "Not graded" is amber, not red: a quota-limited case
is not a prompt failure.

## Typography

Source Sans 3 (SIL Open Font License; npm `@fontsource-variable/source-sans-3`) for text, IBM Plex
Mono (OFL; `@fontsource/ibm-plex-mono`) for model names, versions, file paths and code.
Fallbacks: `Inter, system-ui, sans-serif` and `ui-monospace, monospace`.

| Role | Size / line height | Weight |
| --- | --- | --- |
| display | 48 / 1.2 | 700 |
| heading-lg | 30 / 1.25 | 700 |
| heading | 24 / 1.33 | 600 |
| heading-sm | 20 / 1.37 | 600 |
| subheading | 18 / 1.4 | 600 |
| body-md | 16 / 1.5 | 400 |
| body | 14 / 1.5 | 400 |
| caption | 12 / 1.4 | 400 |
| code | 15 / 1.6 (mono) | 400 |

400 for body, 600 for buttons and section labels, 700 only for the largest headlines. Tabular
numerals for scores and counts.

## Spacing and radius

Scale: 4, 6, 8, 10, 12, 16, 20, 24, 32, 40, 48, 64, 80. Max width 1200, section gap 64, card
padding 16, element gap 8.

| Element | Radius |
| --- | --- |
| Nav, tags | 4 |
| Cards, inputs, buttons | 8 |
| Large containers | 12 |

Border: 1px `hairline`. Card shadow `0 1px 2px rgba(0,0,0,.05)`. No stacked shadows.

## Components

- **Primary button**: `ink` fill, `canvas` text, 8px radius, 10px 20px padding, 14px / 600.
- **Ghost button**: transparent, 1px `hairline` border, `ink` text.
- **Top nav**: `canvas`, 56px, sticky, wordmark 16px / 700, links 14px `caption`, active `link`.
- **Card**: `card` fill, 1px `hairline`, 8px radius, 16px padding. Heading 18 / 600. Identifiers in mono.
- **Tag**: `band` fill, 4px radius, 6px 10px padding, 12px.
- **Report table**: hairline rows, mono model and version cells, state shown as a tag.

## Do / don't

- Do keep `ink` fills for the one primary action per view; links in `link`, text only.
- Do set model names, versions, paths in mono.
- Don't use gradients outside the hero panel.
- Don't mix gradients or brand colors into white sections.
- Don't use red for "not graded".

## Logo

A cartoon bubble "P" whose counter is one big eye: the proctor watching. Blue `#1d4ed8` fill,
navy `#0b1b4d` outline and a 3-unit offset shadow; on dark backgrounds the outline turns white
and the shadow `#1e3a8a`. The palette is independent of the UI palette and of the ecosystem
colors of the language.

Files in `design/brand/`: `mark.svg` (the P alone, switches outline with
`prefers-color-scheme`; docs logo, favicon, README header), `icon.svg` (P on a pale `#eef3ff`
tile), `icon-dark.svg` (on `#0b0f19`), `apple-touch-icon.png` (180 px, from `icon.svg`).
