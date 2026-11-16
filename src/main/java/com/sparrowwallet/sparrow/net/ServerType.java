// Modified for Goshawk BitcoinSV, 2026-11-16.
package com.sparrowwallet.sparrow.net;

public enum ServerType {
    BSV_SPV("BitcoinSV SPV service"),
    BITCOIN_CORE("Bitcoin Core"), ELECTRUM_SERVER("Private Electrum"), PUBLIC_ELECTRUM_SERVER("Public Electrum");

    private final String name;

    ServerType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
