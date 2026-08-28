# Accessibility

How accessibility works in this app, and where to look when changing it. Two things to understand before touching this area: the accessibility bar for all work generated from this template is written down in [`CLAUDE.md`](../CLAUDE.md) ("Accessibility requirements", WCAG 2.x AA with a five-step pre-PR self-check), and every screen is edge-to-edge — so any new view near a screen edge must handle system-bar insets via the extensions in [`EdgeToEdgeExtensions.kt`](../app/src/main/java/com/cube/sprintzerotemplate/lib/extensions/EdgeToEdgeExtensions.kt) or its touch targets can end up under a bar.

## Screen reader (TalkBack)

The template's only interactive surface is the bottom navigation bar, whose items are announced from their menu titles ([`bottom_nav.xml`](../app/src/main/res/menu/bottom_nav.xml)) with labels always visible (`labelVisibilityMode="labeled"`). The placeholder pages contain a single text view each and carry no additional markup. The CLAUDE.md standards define the expectations for real screens (labels on every interactive element, grouped announcements, self-announcing modals, focus order, delegates on custom views).

## Text & display

Text sizes use `sp` (enforced as a standard in CLAUDE.md, which also requires screens to remain usable at 200% font scale and to scroll rather than fix text heights). The placeholder layouts use `wrap_content` for text.

## Colour & contrast

A single light Material 3 theme ([`themes.xml`](../app/src/main/res/values/themes.xml)) — there is no dark theme, so the system dark-mode setting has no effect. Colours are tokenised in [`colors.xml`](../app/src/main/res/values/colors.xml); the bottom navigation uses distinct selected/unselected colours plus the Material 3 active-indicator pill, so the selected state is not conveyed by colour alone.

## Interaction

No screen locks orientation — every screen works in portrait and landscape, with the edge-to-edge extensions keeping content and the tab bar clear of the side navigation bar in landscape. A `min_touch_target` dimension token (48dp, the WCAG 2.5.5 / Material minimum) exists in [`dimens.xml`](../app/src/main/res/values/dimens.xml) for client layouts to reference.

## Known gaps

- No dark or high-contrast theme.
- The placeholder screens carry no `contentDescription` examples, since they contain no interactive elements or images; the standards in CLAUDE.md govern what clients add.