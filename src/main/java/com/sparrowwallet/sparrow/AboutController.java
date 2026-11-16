// Modified for Goshawk on 2026-11-16; see MODIFICATIONS.md. Original notices retained.
package com.sparrowwallet.sparrow;

import com.sparrowwallet.sparrow.control.DialogImage;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class AboutController {
    private Stage stage;

    @FXML
    private Label title;

    @FXML
    private DialogImage dialogImage;

    public void initializeView() {
        title.setText(SparrowWallet.APP_NAME + " " + SparrowWallet.APP_VERSION + SparrowWallet.APP_VERSION_SUFFIX);
    }

    public void refreshTheme() {
        String darkCss = AppServices.class.getResource("darktheme.css").toExternalForm();
        if(AppServices.isDarkTheme()) {
            if(!stage.getScene().getStylesheets().contains(darkCss)) {
                stage.getScene().getStylesheets().add(darkCss);
            }
        } else {
            stage.getScene().getStylesheets().remove(darkCss);
        }

        dialogImage.refresh();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void close(ActionEvent event) {
        stage.close();
    }

    public void openUpstream(ActionEvent event) {
        AppServices.get().getApplication().getHostServices().showDocument("https://sparrowwallet.com");
    }

    public void openLicenses(ActionEvent event) {
        javafx.scene.control.TextArea text = new javafx.scene.control.TextArea();
        text.setEditable(false);
        text.setWrapText(true);
        text.setPrefSize(700, 480);
        try(var notice = getClass().getResourceAsStream("/legal/NOTICE");
            var license = getClass().getResourceAsStream("/legal/LICENSE")) {
            if(notice == null || license == null) {
                throw new java.io.IOException("Bundled license resources are missing");
            }
            text.setText(new String(notice.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)
                    + "\n\n" + new String(license.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
        } catch(java.io.IOException e) {
            text.setText("Unable to load bundled notices. See LICENSE and NOTICE in the " + com.sparrowwallet.sparrow.SparrowWallet.APP_NAME + " source distribution.");
        }
        javafx.scene.control.Dialog<Void> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle(com.sparrowwallet.sparrow.SparrowWallet.APP_NAME + " — Licenses & acknowledgements");
        dialog.initOwner(stage);
        dialog.setResizable(true);
        dialog.getDialogPane().setContent(text);
        dialog.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CLOSE);
        dialog.showAndWait();
    }

    public void openDonate(ActionEvent event) {
        AppServices.get().getApplication().getHostServices().showDocument("https://sparrowwallet.com/donate");
    }
}
