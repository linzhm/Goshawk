<!-- Modified for Goshawk on 2026-11-16; see MODIFICATIONS.md. Original notices retained. -->
# Goshawk

![Goshawk](docs/brand/goshawk-lockup.svg)

**苍鹰 · A clear view of your money.**

Goshawk is an independent desktop wallet project for BSV, built on
[Sparrow Wallet](https://sparrowwallet.com). The first product focuses on
payments, UTXO control, transaction inspection, and backup and recovery.

**Current status: development preview, offline signing only.** Wallet creation and
address entry use BitcoinSV P2PKH. Software signing, transaction verification and
the existing hardware-signing button now use BSV ForkID rules; the Ledger route
uses the Bitcoin Cash app through Java/Lark. P2SH, SegWit, Taproot and BIP125
replacement are unavailable. The UI and currency unit use BitcoinSV / BSV.

The former Bitcoin server, price, fee-source and broadcast routes are disabled.
WOC address discovery and the end-to-end SPV service are not connected yet, so
this is **not a usable online BSV release**. No Java physical-device, funded
transaction or external-wallet interoperability result is claimed.

## Development

```sh
git clone --recurse-submodules https://github.com/linzhm/Goshawk.git
cd Goshawk
./gradlew :test --tests '*BsvWalletSigningTest' :lark:test --tests '*Bsv*Test' --tests '*HIDTransportTest'
./goshawk
```

Use Java 25. The current Gradle wrapper does not run on Java 27.
The upstream build targets Java 25. `./goshawk.bat` is the Windows launcher.
Create a native application image with `./gradlew jpackageImage`; installer
tooling is platform-specific. Goshawk does not inherit Sparrow's release
signature, signing identity or claims of reproducible released binaries.

Goshawk uses `~/.goshawk` on macOS/Linux and `%APPDATA%/Goshawk` on Windows.
Its optional XDG directories use `goshawk`, its log is `goshawk.log`, and its
network override is `GOSHAWK_NETWORK`. Use `./goshawk -h` for CLI options.
Existing Sparrow directories are not migrated or opened automatically.

## Product and upstream boundaries

- [Architecture and BSV integration plan](docs/goshawk-architecture.md)
- [BSV transactions and hardware signing](docs/hardware-bsv.md)
- [Payment compatibility and service boundaries](docs/payment-compatibility.md)
- [Brand assets and visual identity](docs/brand/README.md)
- [Upstream synchronization](docs/upstream-maintenance.md)
- [Modification record](MODIFICATIONS.md)
- [Branding validation](docs/validation-branding.md)

Brand resources, packaging resources and product configuration live in
`branding/goshawk/`. Original Java package names and upstream resource files
remain in place to keep the adaptation small and future merges reviewable.

## Acknowledgements and license

Goshawk is based on Sparrow Wallet by **Craig Raw and its contributors**.
We thank Sparrow, Drongo, Lark and the wider open-source community for the
foundation, and BSV Desktop for informing the planned payment workflows.
Goshawk is independently maintained and is not an official Sparrow release.

The original [Apache License 2.0](LICENSE) is retained unchanged. See
[NOTICE](NOTICE), [third-party notices](THIRD_PARTY.md), and the
[unaltered upstream README](docs/upstream/SPARROW-README.md), including its
YourKit credit. Third-party components retain their own licenses.

[Sparrow source](https://github.com/sparrowwallet/sparrow) ·
[Support Sparrow](https://sparrowwallet.com/donate)
