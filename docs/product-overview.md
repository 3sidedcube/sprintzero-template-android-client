# Product Overview

## What it is

The Sprint Zero Template is 3 Sided Cube's starting point for new native Android client applications. It is not a product in its own right and is never shipped to users; instead, the `bootstrap-android` skill generates a new private client repository from it (interview → rename transform → verify → push), and every generated client inherits whatever the template contains. The template's value is that a new project starts with the team's build system, CI, quality tooling, engineering standards and example code already in place.

## Who it's for

3 Sided Cube Android engineers starting a new client project, and — indirectly — every client whose app begins life as a copy of this repository.

## Core user journeys

- Bootstrap a new client repository from the template and have it build green immediately.
- Replace the placeholder tabs, strings, icons and colours with the client's product.
- Follow the baked-in examples (tests, helpers, standards) as the reference for how the team writes Android code.

## Feature areas (user's view)

The app deliberately ships no product features. What a generated client starts with:

- A splash screen that leads into a five-tab bottom-navigation shell with generic placeholder pages.
- Firebase wiring for crash reporting, analytics and push messaging (the push handler is a stub).
- The team's engineering scaffolding: permissions handling, edge-to-edge utilities, preferences storage, secrets handling and a full example test suite.

## Platforms & release channels

Android only. The template itself is never released; generated clients are distributed through Bitrise (see [ci-cd.md](ci-cd.md)) to their own Play Store / internal channels.