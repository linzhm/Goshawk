# BitcoinSV transactions and hardware signing

Updated 2026-11-16. This supersedes the earlier additive BSV API proposal: Goshawk
is a BitcoinSV product, not a Bitcoin wallet with an optional BSV hardware API.
The online service remains unconnected; do not treat this as a usable funds wallet.

## Implemented boundaries

- New address wallets and address parsing accept P2PKH only. Existing incompatible
  wallets are refused, never silently reinterpreted with a different script type.
- `BsvTransaction` in the maintained Drongo fork implements ForkID-zero digests,
  amount/script binding, ordinary serialization and transaction-template policy.
  Software signing and verification of signed wallet spends use this path.
- Default signing is `0x41`; host checks support `41/42/43/c1/c2/c3`. Ledger BCH
  supports `0x41` only. Bitcoin `0x01` is rejected by the Goshawk signing path.
- `Hwi.signPSBT`, the entry used by the existing signing button, now adapts the
  transaction through `BsvWalletSigning` into Lark's BSV request/result API.
  PSBT remains an internal editor container, with BSV signatures and strict
  rejection of redeem/witness/Taproot metadata. This does not certify third-party
  PSBT interoperability. No Bitcoin device-signing fallback remains in Hwi.
- Address/message/device discovery and account import use the BSV Ledger route.
  Only Nano S, Nano S Plus and Nano X are exposed. Physical Java validation is pending.
- Detached requests support P2PKH and bare multisig inputs. P2SH is rejected,
  including historical recovery; it is not an offered template. The HD wallet UI
  remains single-signature P2PKH. Ledger output review remains P2PKH-only; software
  template validation also accepts bare multisig and OP_RETURN data outputs.
- BIP125 replacement is not exposed. Bitcoin public servers, Core connections,
  fiat quotes, external fee estimates and direct broadcast cannot be used by
  Goshawk. WOC and the SPV exchange service require subsequent integration.

## Keys and protocol strings

Ledger account pairing compares the complete account xpub and exact original
path, not a short USB identifier or a fingerprint alone. Original paths such as
`m/44'/0'/0'` and `m/44'/145'/0'` are preserved; changing the chain does not permit
changing a restored wallet's keys. Coin type `236'` is not advertised as supported
by this BCH application route.

The current Sparrow keystore format requires a root fingerprint on import. The
adapter requests the actual fingerprint from the selected, paired device. If the
application refuses that root-key query, import fails; it must never substitute
an account fingerprint. This is a remaining physical-device acceptance point.

`Bitcoin seed` (BIP32), `Bitcoin Signed Message` and the actual device app name
`Bitcoin Cash` remain protocol constants. Product copy and units are BitcoinSV /
BSV. Payment URIs use ElectrumSVP's existing `bitcoin:<address>?sv` convention;
unmarked Bitcoin payment URIs are refused. Original licenses and upstream credits
retain their actual names.

## Sources and licensing

Sparrow baseline `ddb486dd37ddfa4071e95a81e5c78298433073b6`; Drongo upstream
`4d85e20bad80d8f25a8e422d9d1ed8308f7e01b7`; Lark upstream
`4eaaf9d2ef242164c801ab21247e616e5c4ef8f7`. Goshawk pins maintained public
Drongo and Lark forks through submodule commits.

Behavioral reference: [ElectrumSVP hardware branch](https://github.com/linzhm/ElectrumSVP/tree/codex/hardware-wallet-support)
at `fff115ea3233ad7045c7a67a9479a9d1e1310c9e`; hardware work at
`1dd2a8aa91f996b684dd347528d28bf56b88b48a`. Its Open BSV source was not copied
into this Apache implementation. BitcoinX is an offline oracle only. BSV Desktop
and ElectrumSVP were read, not modified.

See [validation](hardware-bsv-validation.md), [Lark API](../lark/docs/bsv-ledger.md)
and [payment compatibility](payment-compatibility.md). A trusted parent on Ledger
is not proof of chain membership, current unspentness or coinbase maturity.
