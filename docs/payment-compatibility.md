# Payment compatibility — current decisions and limits

2026-11-16. Scope: change Goshawk and our own service only. Do not modify
ElectrumSVP or BSV Desktop, require either peer to adopt a new invoice protocol,
or claim that raw transaction compatibility implies payment-protocol compatibility.
The earlier suggestion to add HD-address support in both peer wallets is withdrawn.

## Existing methods to adapt

| Peer / method | Observed contract | Goshawk work |
| --- | --- | --- |
| ElectrumSVP ordinary address payment | P2PKH, ForkID, `bitcoin:<address>?sv` | Transaction and URI paths implemented; WOC discovery still pending |
| ElectrumSVP payment requests | Existing JSON payment-request/ack workflow in `paymentrequest.py` | Inspect and implement its existing contract without changing the peer |
| ElectrumSVP proof export | `format: BREAD`, version 2, subject `txid`/`hex`, `transactions`, UTXOs and per-transaction merkle/header information | Own BREAD parser and proof normalization; not implemented |
| BSV Desktop direct payment | `@bsv/webrtpay`, authenticated identity, `PaymentToken`, Atomic BEEF, derivation prefix/suffix and output index | Own adapter to the existing protocol; not implemented |
| BSV Desktop message-box payment | `payment_inbox`, same payment token, sender identity | Own message-box adapter, persistent retries and duplicate handling; not implemented |
| BSV Desktop ordinary receive address | P2PKH address and WOC-backed discovery | Compatible transaction output; Goshawk delivery/discovery still pending |

Ledger custody remains the chosen constraint. Outgoing identity payments can in
principle spend Ledger HD inputs into a recipient-derived P2PKH output: a service
identity used for authentication is not a replacement funds key. Incoming BRC29
identity payments are different: their spending keys are derived from the receiving
identity. The current Ledger BCH path signs BIP32 paths and exposes no verified
BRC29 private-key-tweak signing operation. Do not quietly receive those payments
into a software-controlled key, sweep them and call that continuous Ledger custody.
Preserve this as an explicit capability limit pending a compatible signing design.
Ordinary HD address receipt remains the custody-preserving receive method.

## Service responsibilities

The service owns transaction/proof material, outbound retry state, received-payment
validation, durable receipt history, conflicts and a resumable notification cursor.
The Sparrow adapter projects these into history and UTXO views; it must not silently
connect to Bitcoin services or broadcast as a fallback. WOC remains the ordinary
address-discovery adapter, not a replacement for complete wallet recovery history.

A delivery acknowledgment, validation/admission result and block confirmation are
separate facts. BSV Desktop's `received` can mean retained for retry while locked or
unavailable. `accepted` denotes successful internalization, whose submission effects
must be checked against the selected service. No status name alone is an admission
proof. The existing agreement to map pool admission into an unconfirmed history
entry is retained, with the pool scope and receipt evidence recorded. Confirmation
requires proof against the selected BSV chain, and reorgs must revoke stale confirmation.

Do not invent payment context from a raw transaction or an address. Persist recipient,
network, output/amount binding and payment identifier with the signed transaction.
Retries send the same transaction; duplicate notifications do not credit twice.
Unverifiable/incomplete material cannot become a spendable balance or successful payment.

## Read-only evidence

- [ElectrumSVP](https://github.com/linzhm/ElectrumSVP/tree/fff115ea3233ad7045c7a67a9479a9d1e1310c9e): `web.py`, `paymentrequest.py`, `verification_utils.py`.
- [BSV Desktop direct payments](https://github.com/bsv-blockchain/bsv-desktop/blob/f65213f4202de5ef1b48847287bd47111e5ec436/src/lib/pages/Dashboard/Payments/directPayments.ts).
- [BSV Desktop payment creation](https://github.com/bsv-blockchain/bsv-desktop/blob/f65213f4202de5ef1b48847287bd47111e5ec436/src/lib/pages/Dashboard/Payments/messageBoxPayments.ts).

These are inspected contracts, not a completed integration or an interoperability test.
No changes have been written to either reference wallet.
