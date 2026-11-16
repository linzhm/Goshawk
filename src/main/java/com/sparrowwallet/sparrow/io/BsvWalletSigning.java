/* Copyright 2026 Goshawk contributors. Licensed under the Apache License, Version 2.0. */
package com.sparrowwallet.sparrow.io;

import com.sparrowwallet.drongo.*;
import com.sparrowwallet.drongo.crypto.ECKey;
import com.sparrowwallet.drongo.protocol.*;
import com.sparrowwallet.drongo.psbt.*;
import com.sparrowwallet.lark.bsv.*;
import java.util.*;

/** Adapts Sparrow's transaction editor to the BSV signer; no Bitcoin hardware fallback. */
public final class BsvWalletSigning {
    private BsvWalletSigning() {}

    public static BsvAccount account(OutputDescriptor descriptor) {
        if(descriptor == null || descriptor.getExtendedPublicKeys().size() != 1 || descriptor.getScriptType() != ScriptType.P2PKH) {
            throw new IllegalArgumentException("A single BitcoinSV P2PKH account descriptor is required");
        }
        var xpub = descriptor.getExtendedPublicKeys().iterator().next();
        var origin = descriptor.getKeyDerivation(xpub);
        if(origin == null) throw new IllegalArgumentException("The account derivation path is missing");
        return new BsvAccount(origin.getDerivationPath(), xpub.getExtendedKey(ExtendedKey.Header.xpub));
    }

    public static String addressPath(BsvAccount account, OutputDescriptor descriptor) {
        if(!account.equals(account(descriptor))) throw new IllegalArgumentException("Address descriptor does not belong to the selected account");
        var xpub = descriptor.getExtendedPublicKeys().iterator().next();
        if(descriptor.describesMultipleAddresses(xpub)) throw new IllegalArgumentException("Select one concrete address");
        var children = descriptor.getChildDerivation(xpub);
        String path = account.path() + KeyDerivation.writePath(children.subList(1, children.size())).substring(1);
        account.derive(path); // Checks branch, depth and hardened children before opening the device.
        return path;
    }

    public static BsvSigningRequest prepare(PSBT psbt, BsvAccount account) {
        BsvTransaction.requireContainer(psbt);
        List<BsvSigningRequest.Input> inputs = new ArrayList<>();
        List<BsvSigningRequest.Target> targets = new ArrayList<>();
        List<BsvSigningRequest.Signature> signatures = new ArrayList<>();
        for(int i = 0; i < psbt.getPsbtInputs().size(); i++) {
            PSBTInput input = psbt.getPsbtInputs().get(i);
            if(input.isFinalized()) throw new IllegalArgumentException("Import partial signatures before finalizing the transaction");
            if(input.getNonWitnessUtxo() == null || input.getUtxo() == null) throw new IllegalArgumentException("Every input needs its complete previous transaction");
            Script script = input.getUtxo().getScript();
            List<ECKey> keys;
            if(ScriptType.MULTISIG.isScriptType(script)) keys = Arrays.asList(ScriptType.MULTISIG.getPublicKeysFromScript(script));
            else {
                Set<ECKey> candidates = new LinkedHashSet<>(input.getDerivedPublicKeys().keySet());
                candidates.addAll(input.getPartialSignatures().keySet());
                keys = candidates.stream().filter(k -> Arrays.equals(k.getPubKeyHash(), script.getPubKeyHash())).toList();
                if(keys.size() != 1) throw new IllegalArgumentException("A P2PKH input needs its public key");
            }
            String scriptHex = Utils.bytesToHex(script.getProgram());
            inputs.add(new BsvSigningRequest.Input(input.getUtxo().getValue(), scriptHex, scriptHex,
                    keys.stream().map(k -> Utils.bytesToHex(k.getPubKey())).toList(), Utils.bytesToHex(input.getNonWitnessUtxo().bitcoinSerialize(false))));
            for(ECKey key : keys) {
                var slot = new BsvSigningRequest.Slot(i, Utils.bytesToHex(key.getPubKey()));
                if(input.getPartialSignatures().containsKey(key)) signatures.add(new BsvSigningRequest.Signature(slot, Utils.bytesToHex(input.getPartialSignatures().get(key).encodeToBitcoin())));
                var origin = input.getDerivedPublicKeys().get(key);
                if(origin != null) {
                    String path = origin.getDerivationPath();
                    // Match the full account and child public key, never an absent or coincident fingerprint.
                    boolean owned;
                    try { owned = Arrays.equals(account.derive(path).getPubKey(), key.getPubKey()); }
                    catch(IllegalArgumentException ignored) { owned = false; }
                    if(owned) targets.add(new BsvSigningRequest.Target(slot, path, input.getSigHash() == null ? 0x41 : input.getSigHash().intValue()));
                }
            }
        }
        if(targets.isEmpty()) throw new IllegalArgumentException("No transaction input belongs to the selected account");
        return new BsvSigningRequest(psbt.getTransaction(), inputs, targets, signatures);
    }

    public static PSBT apply(PSBT retained, BsvSigningRequest request, BsvSigningRequest.Result result) throws SignatureDecodeException {
        BsvSigningRequest signed = request.apply(result);
        if(!Arrays.equals(retained.getTransaction().bitcoinSerialize(false), request.transaction().bitcoinSerialize(false))) throw new IllegalArgumentException("Transaction changed while signing");
        PSBT copy = retained.copy();
        for(var entry : signed.signatures().entrySet()) {
            copy.getPsbtInputs().get(entry.getKey().inputIndex()).getPartialSignatures().put(BsvSigningRequest.key(entry.getKey().publicKey()),
                    TransactionSignature.decodeFromBitcoin(TransactionSignature.Type.ECDSA, Utils.hexToBytes(entry.getValue()), true));
        }
        return copy;
    }
}
