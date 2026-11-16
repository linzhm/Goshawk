/* Copyright 2026 Goshawk contributors. Licensed under the Apache License, Version 2.0. */
package com.sparrowwallet.sparrow.io;

import com.fasterxml.jackson.databind.*;
import com.sparrowwallet.drongo.*;
import com.sparrowwallet.drongo.crypto.*;
import com.sparrowwallet.drongo.protocol.*;
import com.sparrowwallet.drongo.psbt.*;
import com.sparrowwallet.drongo.wallet.*;
import com.sparrowwallet.drongo.policy.*;
import com.sparrowwallet.drongo.address.*;
import com.sparrowwallet.drongo.uri.*;
import com.sparrowwallet.lark.bsv.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class BsvWalletSigningTest {
    static final JsonNode DATA;
    static { try { DATA = new ObjectMapper().readTree(BsvWalletSigningTest.class.getResourceAsStream("bsv-vectors.json")); } catch(Exception e) { throw new ExceptionInInitializerError(e); } }
    static JsonNode fixture() { return DATA.get("fixtures").get(0); }
    static BsvAccount account() { return new BsvAccount("m/44'/0'/0'", DATA.get("account").asText()); }
    static ECKey privateKey(int index) {
        DeterministicKey key = HDKeyDerivation.createMasterPrivateKey(Utils.hexToBytes(DATA.get("public_test_seed").asText()));
        for(var child : KeyDerivation.parsePath("m/44'/0'/0'/0/" + index)) key = HDKeyDerivation.deriveChildKey(key, child);
        return key;
    }
    static PSBT container() {
        PSBT psbt = new PSBT(new Transaction(Utils.hexToBytes(fixture().get("transaction").asText())));
        for(int i = 0; i < 2; i++) {
            var input = psbt.getPsbtInputs().get(i);
            input.setNonWitnessUtxo(new Transaction(Utils.hexToBytes(fixture().get("inputs").get(i).get("previousTransaction").asText())));
            input.setSigHash(SigHash.ALL_FORKID);
            input.getDerivedPublicKeys().put(ECKey.fromPublicOnly(privateKey(i)), new KeyDerivation("3442193e", "m/44'/0'/0'/0/" + i));
        }
        return psbt;
    }
    @Test void addressDisplayUsesExactlyAccountBranchAndIndex() {
        var account = account(); var xpub = ExtendedKey.fromDescriptor(account.xpub());
        var descriptor = new OutputDescriptor(ScriptType.P2PKH, 1, Map.of(xpub, new KeyDerivation("3442193e", account.path())), Map.of(xpub, "0/1"));
        assertEquals("m/44'/0'/0'/0/1", BsvWalletSigning.addressPath(account, descriptor));
        var wildcard = new OutputDescriptor(ScriptType.P2PKH, xpub, new KeyDerivation("3442193e", account.path()));
        assertThrows(IllegalArgumentException.class, () -> BsvWalletSigning.addressPath(account, wildcard));
    }
    @Test void brandChangesPreserveBip32SeedDerivation() {
        var key = HDKeyDerivation.createMasterPrivateKey(Utils.hexToBytes(DATA.get("public_test_seed").asText()));
        assertEquals("3442193e", Utils.bytesToHex(key.getFingerprint()));
        assertEquals(fixture().get("targets").get(0).get("slot").get("publicKey").asText(), Utils.bytesToHex(privateKey(0).getPubKey()));
    }
    @Test void walletSignsFinalizesAndRecognizesItsOwnBsvSpend() throws Exception {
        Wallet wallet = new Wallet("fixture");
        wallet.setPolicyType(PolicyType.SINGLE_HD); wallet.setScriptType(ScriptType.P2PKH);
        var master = HDKeyDerivation.createMasterPrivateKey(Utils.hexToBytes(DATA.get("public_test_seed").asText()));
        var keystore = Keystore.fromMasterPrivateExtendedKey(new MasterPrivateExtendedKey(master), PolicyType.SINGLE_HD, KeyDerivation.parsePath("m/44'/0'/0'"));
        wallet.getKeystores().add(keystore);
        wallet.setDefaultPolicy(Policy.getPolicy(PolicyType.SINGLE_HD, ScriptType.P2PKH, wallet.getKeystores(), 1));
        var psbt = container();
        wallet.sign(Map.of(psbt.getPsbtInputs().get(0), new WalletNode(wallet, KeyPurpose.RECEIVE, 0), psbt.getPsbtInputs().get(1), new WalletNode(wallet, KeyPurpose.RECEIVE, 1)));
        psbt.verifySignatures(); wallet.finalise(psbt);
        Transaction tx = psbt.extractTransaction();
        assertFalse(tx.isSegwit()); assertTrue(tx.hasScriptSigs());
        Map<Sha256Hash, Transaction> parents = new HashMap<>();
        for(var input : fixture().get("inputs")) { var parent = new Transaction(Utils.hexToBytes(input.get("previousTransaction").asText())); parents.put(parent.getTxId(), parent); }
        assertTrue(wallet.getUnsignedInputs(tx, parents::get).isEmpty());
    }

    @Test void softwareSignerUsesForkIdAndMatchesIndependentDigestsForAllSixModes() throws Exception {
        for(var mode : SigHash.LEGACY_SIGNING_TYPES) {
            PSBT psbt = container();
            for(int i = 0; i < 2; i++) {
                var input = psbt.getPsbtInputs().get(i); input.setSigHash(mode);
                assertTrue(input.sign(privateKey(i)));
                var signature = input.getPartialSignatures().values().iterator().next();
                assertEquals(mode.value, signature.sighashFlags);
                String hash = fixture().get("hashes").get(i + ":" + String.format("%02x", mode.intValue())).asText();
                assertTrue(signature.verify(Utils.hexToBytes(hash), privateKey(i)));
            }
            psbt.verifySignatures();
            new PSBT(psbt.serialize()).verifySignatures();
        }
    }
    @Test void editorHardwareAdapterPreservesOriginalAndChecksAccountAndSignatures() throws Exception {
        PSBT psbt = container(); byte[] before = psbt.serialize();
        var request = BsvWalletSigning.prepare(psbt, account());
        var signatures = new ArrayList<BsvSigningRequest.Signature>();
        for(int i = 0; i < 2; i++) signatures.add(new BsvSigningRequest.Signature(request.targets().get(i).slot(), fixture().get("signatures").get(i + ":41").asText()));
        var signed = BsvWalletSigning.apply(psbt, request, new BsvSigningRequest.Result(request.id(), signatures));
        signed.verifySignatures();
        assertArrayEquals(before, psbt.serialize());
        assertTrue(signed.getPsbtInputs().stream().allMatch(PSBTInput::isSigned));
        var complete = request.apply(new BsvSigningRequest.Result(request.id(), signatures));
        assertEquals(fixture().get("finalTransaction").asText(), Utils.bytesToHex(complete.finalizeTransaction().bitcoinSerialize(false)));
        assertThrows(IllegalArgumentException.class, () -> BsvWalletSigning.apply(psbt, request, new BsvSigningRequest.Result("wrong", signatures)));
        var other = new BsvAccount("m/44'/145'/0'", account().xpub());
        assertThrows(IllegalArgumentException.class, () -> BsvWalletSigning.prepare(psbt, other));
    }
    @Test void rejectsBitcoinSighashAndWitnessMetadataBeforeSigning() {
        var psbt = container(); var input = psbt.getPsbtInputs().getFirst(); input.setSigHash(SigHash.ALL);
        assertThrows(IllegalArgumentException.class, () -> input.sign(privateKey(0)));
        input.setSigHash(SigHash.ALL_FORKID); input.setWitnessUtxo(input.getUtxo());
        assertThrows(IllegalArgumentException.class, () -> input.sign(privateKey(0)));
        assertThrows(IllegalArgumentException.class, () -> BsvWalletSigning.prepare(psbt, account()));
    }
    @Test void rejectsMismatchedParentsAndTamperedSighashSuffix() throws Exception {
        var psbt = container(); var input = psbt.getPsbtInputs().getFirst();
        input.setNonWitnessUtxo(psbt.getPsbtInputs().get(1).getNonWitnessUtxo());
        assertThrows(IllegalArgumentException.class, () -> input.sign(privateKey(0)));
        var signed = container(); signed.getPsbtInputs().getFirst().sign(privateKey(0));
        var sig = signed.getPsbtInputs().getFirst().getPartialSignatures().values().iterator().next().encodeToBitcoin(); sig[sig.length-1] = 0x42;
        signed.getPsbtInputs().getFirst().getPartialSignatures().put(ECKey.fromPublicOnly(privateKey(0)), TransactionSignature.decodeFromBitcoin(sig, true));
        assertThrows(PSBTSignatureException.class, signed::verifySignatures);
    }
    @Test void rejectsP2shAndAllWitnessOutputTemplates() {
        for(ScriptType type : List.of(ScriptType.P2SH, ScriptType.P2WPKH, ScriptType.P2WSH, ScriptType.P2TR)) {
            byte[] hash = new byte[type == ScriptType.P2WPKH || type == ScriptType.P2SH ? 20 : 32];
            Script script = type.getOutputScript(hash);
            assertThrows(IllegalArgumentException.class, () -> BsvTransaction.requireOutput(script));
            assertThrows(InvalidAddressException.class, () -> Address.fromString(type.getAddress(hash).toString()));
        }
    }
    @Test void emitsAndReadsElectrumSvpNetworkMarkedUrisAndUsesBsvUnit() throws Exception {
        var address = ScriptType.P2PKH.getAddress(privateKey(0).getPubKeyHash());
        var uri = BitcoinURI.convertToBitcoinURI(address, 123L, "test", "payment");
        for(String label : List.of("", "label")) for(String message : List.of("", "message")) {
            assertEquals(address, new BitcoinURI(BitcoinURI.convertToBitcoinURI(address, null, label, message)).getAddress());
        }
        assertTrue(uri.contains("sv")); assertEquals(123L, new BitcoinURI(uri).getAmount().longValue());
        assertEquals(address, new BitcoinURI("bitcoin:" + address + "?sv").getAddress());
        assertThrows(BitcoinURIParseException.class, () -> new BitcoinURI("bitcoin:" + address + "?amount=1"));
        assertEquals("BSV", BitcoinUnit.BSV.getLabel()); assertEquals(100000000L, BitcoinUnit.BSV.getSatsValue(1));
    }
}
