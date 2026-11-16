// Modified for Goshawk on 2026-11-16; see MODIFICATIONS.md. Original notices retained.
package com.sparrowwallet.sparrow;

import javafx.application.Preloader;
import javafx.stage.Stage;

public class SparrowWalletPreloader extends Preloader {
    @Override
    public void start(Stage stage) {
        com.sun.glass.ui.Application.GetApplication().setName(com.sparrowwallet.sparrow.SparrowWallet.APP_NAME);
    }
}
