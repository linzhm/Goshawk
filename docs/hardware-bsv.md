# BSV transactions and hardware signing

Status: implementation plan, 2026-11-16. The branding build does not implement
or certify BSV hardware signing. Preserve Sparrow's UI and transport reuse
where useful, but do not treat Bitcoin hardware support as BSV support.

## Sources and evidence boundaries

- Sparrow baseline: `ddb486dd37ddfa4071e95a81e5c78298433073b6`.
- Drongo: `4d85e20bad80d8f25a8e422d9d1ed8308f7e01b7`.
- Lark: `4eaaf9d2ef242164c801ab21247e616e5c4ef8f7`.
- Reference discussion: Codex chat `01a1e3c5-c0d4-75a0-bb46-e0fdfe671954`,
  “规划新增 Ledger 固件支持”.
- Committed ElectrumSVP reference:
  [`15dcd550f7482dccafa00ce315c7e9fcaa7b46b9`](https://github.com/linzhm/ElectrumSVP/commit/15dcd550f7482dccafa00ce315c7e9fcaa7b46b9).
  This commit covers Nano S Plus discovery and the BCH application setup probe.
- Additional session/GUI/signature-validation changes in the referenced chat
  were still uncommitted when inspected. Their hardware design and validation
  records inform this plan, but must be reviewed at a final commit before porting.

The reference thread reports synthetic P2PKH transaction and message-signing
checks on Nano S Plus / BCH 2.4.14 and Nano X / BCH 2.4.14. Nano S / BCH 2.4.7
had read-only public-key, app-query and device-selection checks; signature tests
were still pending. No funded transaction/broadcast or complete packaged GUI
validation was reported. None of these tests were run from Goshawk.

A prior five-byte app-query failure hypothesis was retracted in that thread:
repeated four- and five-byte queries both succeeded on the Nano S. Do not copy
an invented firmware-version cutoff from an earlier note. Use the final
reviewed session implementation, with bounded reconnect and timeout behavior.

## Three different formats

| Boundary | Goshawk requirement |
| --- | --- |
| Raw transaction | BSV-compatible non-witness serialization, txid, scripts and signatures |
| Signing request | Explicit input outpoints, previous scripts/values, output amounts/scripts, key paths and sighash policy |
| Payment/proof envelope | BEEF/Atomic BEEF plus required payment metadata for peer delivery and SPV verification |

BEEF is a proof-bearing transport format; it does not replace the transaction's
consensus serialization. Likewise, PSBT is a signing coordination container,
not the on-chain format. Existing Sparrow inspection widgets may be reused,
but an internal PSBT representation must not silently select Bitcoin signing
rules or be sent to a device that does not support the required BSV path.

The first signing scope is ordinary P2PKH with `SIGHASH_ALL | FORKID` (`0x41`).
Preserve transaction version, locktime, sequence, all inputs and exact satoshi
amounts. The BSV signature preimage includes input value and previous locking
script. Do not select SegWit serialization merely because part of the signature
hash construction resembles BIP143. Reject unsupported scripts and witness/
Taproot signing explicitly. Do not broadly apply Bitcoin Core standardness,
RBF, fee-weight or dust assumptions to BSV transactions.

## Modules and upstream seams

1. `BsvTransactionCodec` and `BsvSignatureHash`: independently testable transaction
   and digest logic, with published cross-implementation vectors. Drongo's
   transaction model can be adapted, but the BSV signing path must be explicit.
   Its current SigHash enum and Bitcoin signing routes are not sufficient as-is.
2. `BsvSigningRequest`: an immutable transaction plus verified input context,
   key derivations, expected public keys and reviewable outputs. This is the
   boundary shared by software and hardware signers.
3. `HardwareSigner`: per-device/app capabilities, key/address operations and
   signing. Reuse Lark's transport and discovery where appropriate; vendor
   adapters must choose the correct coin application and protocol.
4. `DeviceSession`: selected-device ownership, operation lock, bounded reads,
   app switching, reconnect, and full account-xpub comparison. USB identifiers
   locate a transport; they do not authenticate the wallet account.
5. `PaymentEnvelope`: BEEF parsing/verification and peer exchange, separate from
   device signing. Hardware adapters never broadcast or acknowledge payment
   acceptance on their own.

Keep these modules separate from `branding/goshawk/`. That directory owns
identity only. If Drongo/Lark modifications are needed, use explicit maintained
fork commits and matching source notices, not hidden edits in pinned submodules.

## Ledger adapter

Use the Bitcoin Cash application and its legacy transaction-signing route as
an initial compatibility candidate. Lark currently contains both modern Bitcoin
PSBT and legacy device clients; neither is automatically a BCH/BSV adapter.
Do not infer the active coin app only from a version number or USB model.

The ElectrumSVP setup probe `m/44'/145'/0'` avoids a parent-path restriction
introduced by BCH app 2.4.10. It is a probe only: never migrate an existing
wallet's path or addresses to make initialization pass. Account paths such as
`m/44'/0'/0'` and `m/44'/145'/0'` must remain exactly as originally selected.
Coin type `236'` must not be advertised as supported merely because BSV uses it.

Lark's legacy client requests the root public key for the master fingerprint
and a parent key to serialize an account xpub. Review these separately against
BCH path restrictions. An account fingerprint is not a replacement for a root
fingerprint; preserve the meaning of each field and pair by the full account
xpub. Avoid a fabricated fingerprint used only to satisfy a Bitcoin PSBT flow.

Sign a copy. Verify every returned signature against the original BSV digest,
expected public key and sighash before committing any result to the transaction.
Rejection, timeout or an invalid signature must leave the original unchanged.
Verify message signatures separately; they are not transaction signatures.

The reference's untrusted-input path can produce “unverified inputs” on the
Ledger. Preserve that warning. Host verification of a signature does not prove
that the device verified parent transactions or the fee. Trusted-input handling
requires bounded parent streaming, previous-txid/output-index/value checks,
large-parent limits and separate coinbase-parent fixtures.

## UI and capability gating

The existing hardware-wallet import, keystore, receive-address display and
transaction signing screens are the intended entry points. Gate operations by
(model, OS, app name/version, script type, derivation, operation), not brand name.
Show concrete states: missing, locked, wrong app, busy, waiting for confirmation,
reconnecting, rejected and unsupported. Allow retry and cancel without losing
the prepared transaction. Never silently select the first device after a switch.

A saved hardware wallet must retain watch-only inspection if a device or optional
vendor component is unavailable. New account import stays public-key-only.
Do not prompt for a seed or PIN on the host. Preserve transaction inspection
before and after signing, including device verification limits.

## Acceptance sequence

- Independent raw-transaction, txid and `0x41` vectors; changes to amount, script,
  version, sequence, locktime and outputs must produce the expected digest changes.
- Software sign/verify first, then deterministic hardware protocol fixtures.
- One/multiple P2PKH inputs, with/without change; rejection, disconnect and invalid
  signature must not mutate the original transaction.
- Selected-device switching/reconnect and full account-xpub comparison; old-path
  recovery; missing vendor component must not destroy watch-only access.
- Physical matrix for Nano S, S Plus and X with exact OS/app versions and GUI
  entry points. Reuse synthetic unspendable inputs before any funded testing.
- Keep multisig, data outputs, large scripts and arbitrary contracts as separate
  capabilities. Do not advertise them from a successful ordinary P2PKH test.

No hardware operations were executed as part of this branding change. The
reference thread owns its active device sessions; do not access the same devices
concurrently from Goshawk validation.

## Porting sequence agreed for Goshawk

Wait for the referenced Ledger work to finish and record its final commit and
validation matrix. Reuse Sparrow's existing `Hwi` → Java `Lark` path for device
discovery, public keys, address display, message signing and transaction signing.
Port the reviewed behavior and fixtures into Java/Lark, then connect the existing
Sparrow import, receive and signing UI. The Python/Qt code is a reference; its
GUI and test results do not certify the Java implementation. Preserve per-device
capability evidence, cancellation semantics and post-signature verification.
