# Goshawk modifications

Modified from Sparrow Wallet by Craig Raw and its contributors, licensed under
Apache License 2.0. Original license and attribution notices are preserved.

## 2026-11-16 — Independent identity and branding

Baseline: Sparrow `ddb486dd37ddfa4071e95a81e5c78298433073b6` (2.5.7).
At the branding milestone, Drongo and Lark remained at the upstream-pinned commits.

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

At the branding milestone, BSV signing, SPV services, WOC conversion and peer-to-peer
payment acceptance were unimplemented. The subsequent hardware milestone is below.

## Modified source notices

Changed upstream files and copied resource overrides carry a Goshawk modification
notice. New assets and scripts are separately located. Consult Git history for
exact differences and original upstream authorship.

## 2026-11-16 — Explicit BSV Ledger host API

- Pin the maintained public `linzhm/lark` fork with its original Apache-2.0 license.
- Extend the existing Hwi operation lock with BSV discovery, app opening, public
  account import, address display, message signing and transaction-signing methods.
- Keep Bitcoin PSBT/UI/backend operations separate from the BSV request contract.
- Record the final ElectrumSVP reference and Java offline validation separately.
- Drongo, wallet persistence, network services and real device sessions are unchanged.

The BSV wallet UI, chain-state/coinbase eligibility integration, WOC and SPV payment
services remain outstanding. No device or funded transaction certification is claimed.

## 2026-11-16 — BitcoinSV execution path correction

- Replaced the additive hardware-only seam with a BSV software verifier/signer and
  a bridge from the existing hardware button into Lark's BSV API.
- P2PKH address wallets, bare multisig detached templates, ForkID-only signatures;
  reject P2SH/witness inputs and outputs/metadata; disable BIP125 behavior.
- BitcoinSV/BSV UI units; preserve cryptographic constants, licenses and credits.
- Disable Bitcoin networking, fiat and fee-source routes. Online WOC/SPV delivery
  remains unimplemented and is explicitly presented as unavailable.
- Pin maintained Drongo/Lark forks. Do not modify either reference wallet; preserve
  the Ledger custody constraint in the payment compatibility plan.
