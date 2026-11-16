# BitcoinSV Java/Lark validation — 2026-11-16

Reference: ElectrumSVP `fff115ea3233ad7045c7a67a9479a9d1e1310c9e`, hardware
implementation `1dd2a8aa91f996b684dd347528d28bf56b88b48a`. Goshawk now pins
maintained Drongo and Lark forks. The reference wallets were read, not modified.

## Checks

macOS arm64, Temurin Java 25, Gradle 9.1.0:

```sh
./gradlew :test --tests '*BsvWalletSigningTest' --tests '*StorageTest' \
  --tests '*SparrowTest' :lark:test --tests '*Bsv*Test' \
  --tests '*HIDTransportTest' jpackageImage
```

- **31 application tests passed**, zero failures/errors: 9 BSV flow checks and 22
  storage/import/export checks. Positive import/backup cases now use P2PKH. Original
  encrypted witness fixtures remain unchanged and must be invalid for active BSV
  use, while their original keys, scripts and derivations remain readable intact.
- **29 Lark offline tests passed**, zero failures/errors. P2SH is a rejection case.
- Independent BitcoinX 0.9 vectors validate 24 digest/signature cases: two inputs
  × six ForkID modes × P2PKH/bare multisig. The old third P2SH fixture is retained
  only to prove that even a matching redeem script is rejected.
- Software signing, finalization and recognition of the wallet's own signed BSV
  spend are checked. The editor-to-Lark bridge authenticates signatures, preserves
  the retained request and pairs using the complete account and exact path.
- BIP32 public test-seed/fingerprint/child-key fixtures protect the cryptographic
  protocol strings from branding edits. Device address paths are checked for the
  exact account/branch/index sequence; wildcard descriptors are rejected.
- Missing/mismatched parents, Bitcoin sighash, witness/P2SH/Taproot metadata and
  output templates, changed signatures/intent, malformed bounded JSON, partial
  cosignatures, HID timeouts and ambiguous reconnects have negative coverage.
- Payment URI checks cover `bitcoin:<address>?sv`, optional amount/label/message
  combinations, refusal of unmarked Bitcoin URIs, and BSV currency conversion.
- Modified FXML files parse successfully. This is not an interactive GUI/device test.
- `jpackageImage` builds the macOS arm64 development app successfully. Its packaged
  launcher reports `Goshawk Wallet 0.1.0-dev` with `--version`. The app has an ad-hoc
  development signature; this is not a notarized release or an interactive test.
- Original Sparrow, Drongo and Lark LICENSE files are byte-identical to their baselines.

The physical `EnumerateTest` is excluded; no USB operation, wallet file, real UTXO,
funded transaction or broadcast was used. The full upstream Bitcoin-focused suite
is not a passing BSV acceptance suite and was not run wholesale.

## Physical evidence remains separate

| Combination | Reference project's physical checks | This Java port |
| --- | --- | --- |
| Nano S Plus / OS 1.7.0 / BCH 2.4.14 | Account/address, rejection, simple synthetic transaction and message | Not tested on device |
| Nano X / OS 2.8.0 / BCH 2.4.14 | Account/address, rejection, simple synthetic transaction and message | Not tested on device |
| Nano S / OS 2.1.0 / BCH 2.4.7 | Above plus trusted synthetic coinbase parents, partial P2PKH and bare 2-of-2 multisig | Not tested on device |

S Plus/X trusted-parent and multisig cases were not physically completed even in
the reference. Reference tests were not final packaged-GUI or funded-transaction
certification. No owner's public keys, addresses, USB identities, signatures,
wallet files, real UTXOs or device sessions were used for this Java port. Only
public test keys and deliberately synthetic transaction inputs appear in fixtures.

## Remaining acceptance

- Physical Java tests through the packaged GUI, including actual-root-fingerprint
  import behavior on each BCH app version. Never fabricate a fingerprint on failure.
- WOC ordinary-address discovery, complete history recovery and the self-built
  end-to-end service, including selected-chain proof/UTXO/coinbase eligibility.
- External-wallet interoperability against existing ElectrumSVP/BSV Desktop
  methods; no changes to either peer and no implied BRC29 Ledger signing support.
- Additional scripts, test networks, vendors and external signing-container formats.

This is an offline development preview. The former Bitcoin networking/fee/fiat/
broadcast routes are disabled, rather than presented as a working BSV connection.
