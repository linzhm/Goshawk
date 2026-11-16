# Goshawk branding validation — 2026-11-16

Validated on macOS arm64 using Eclipse Temurin Java 25 and the repository's
Gradle 9.1.0 wrapper. The installed Java 27 cannot run this wrapper; no project
dependency was downgraded to work around that environment mismatch.

- `./gradlew classes`: passed.
- `./gradlew :test --tests '*StorageTest' --tests '*SparrowTest'`: 22 tests,
  zero failures/errors. Covers existing wallet storage and file import/export.
- `./gradlew jpackageImage`: passed on the final Java/resource changes.
- Packaged launcher `--version`: `Goshawk Wallet 0.1.0-dev`.
- Packaged launcher `--help`: Goshawk name and home-directory help present.
- All brand resource overlays matched the processed resources byte-for-byte.
- Original LICENSE and archived upstream README matched the baseline byte-for-byte.
- Native bundle contains project license/NOTICE, Drongo/Lark licenses, font OFL,
  upstream README, component inventory and 40 extracted dependency legal files.
- Runtime inventory contains 117 artifacts; 96 have no matching embedded legal
  notice. Their source/license obligations are not certified by extraction.
- XML/FXML/SVG/plist parsing and shell syntax checks passed.
- `upstream-sync.sh preview upstream/master`: passed; no new upstream changes
  since the pinned baseline at the time of this check. Preparation/merge and
  conflict-resolution paths have not been exercised against a real update.
- Light/dark About, welcome and empty main-window FXML were rendered in an
  isolated temporary configuration. Screenshots are in `docs/brand/`. The main
  preview did not start network services or device sessions. This validates
  resource loading/layout, not the complete application interaction flow.

The application image is a local development build with ad-hoc signing, not a
notarized public release. Windows/Linux icons and packaging metadata were
prepared and inspected; installers on those platforms were not built here.

No BSV signing, payment, device operation or real-funds test was performed.
The Ledger work remains a separate reference pending its final reviewed commit.
