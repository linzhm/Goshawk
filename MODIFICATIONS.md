# Goshawk modifications

Modified from Sparrow Wallet by Craig Raw and its contributors, licensed under
Apache License 2.0. Original license and attribution notices are preserved.

## 2026-11-16 — Independent identity and branding

Baseline: Sparrow `ddb486dd37ddfa4071e95a81e5c78298433073b6` (2.5.7).
Drongo and Lark remain at the upstream-pinned commits.

- Renamed the product to Goshawk / 苍鹰, with version 0.1.0-dev,
  separate application ID, home/XDG/log directories, lock identity and network override.
- Added an original vector mark, outlined wordmark, light/dark assets,
  tray icons, Linux PNG, Windows ICO and macOS ICNS.
- Isolated resources, installer metadata and product constants in
  `branding/goshawk/`, with deterministic Gradle resource overrides.
- Updated visible names and server client identification through narrow hooks;
  retained Java/module namespaces and external Sparrow wallet-format references.
- Added attribution and local license access to About. Preserved the original
  LICENSE, unmodified upstream README, donation link, dependency licenses,
  source history and original source resources.
- Disabled upstream automatic update checks. The manual upstream download
  verifier is explicitly labelled as a Sparrow verifier.
- Removed Craig Raw's signing identity from Goshawk packaging. Packaging is
  unsigned development output; dependency checks are manual instead of scheduled.
- Added dependency legal-file preservation and a component inventory. This is
  not a completed redistribution audit of every bundled executable.
- Added an isolated upstream synchronization script and maintenance guide.
  No upstream update is automatically accepted or pushed.

This is a branding foundation. BSV transaction rules, signing, SPV services,
WOC conversion and peer-to-peer payment acceptance remain unimplemented.

## Modified source notices

Changed upstream files and copied resource overrides carry a Goshawk modification
notice. New assets and scripts are separately located. Consult Git history for
exact differences and original upstream authorship.
