// Modified for Goshawk BSV integration, 2026-11-16.
package com.sparrowwallet.sparrow.io;

import com.sparrowwallet.drongo.ExtendedKey;
import com.sparrowwallet.drongo.IOUtils;
import com.sparrowwallet.drongo.OsType;
import com.sparrowwallet.drongo.OutputDescriptor;
import com.sparrowwallet.drongo.protocol.ScriptType;
import com.sparrowwallet.drongo.psbt.PSBT;
import com.sparrowwallet.drongo.silentpayments.SilentPaymentScanAddress;
import com.sparrowwallet.drongo.wallet.StandardAccount;
import com.sparrowwallet.drongo.wallet.WalletModel;
import com.sparrowwallet.lark.DeviceException;
import com.sparrowwallet.lark.Lark;
import com.sparrowwallet.lark.bsv.*;
import com.sparrowwallet.lark.bitbox02.BitBoxFileNoiseConfig;
import com.sparrowwallet.lark.trezor.TrezorFileNoiseConfig;
import com.sparrowwallet.sparrow.AppServices;
import com.sparrowwallet.sparrow.SparrowWallet;
import com.sparrowwallet.sparrow.control.BitBoxPairingDialog;
import com.sparrowwallet.sparrow.control.TextfieldDialog;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.HBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.smartcardio.CardException;
import javax.smartcardio.CardNotPresentException;
import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

public class Hwi {
    private static final Logger log = LoggerFactory.getLogger(Hwi.class);
    private static final String HWI_HOME_DIR = "hwi";
    private static final String LARK_HOME_DIR = "lark";
    private static final String BITBOX_FILENAME = "bitbox02.json";
    private static final String TREZOR_FILENAME = "trezor.json";

    //Serialises all USB device access, including the periodic enumeration which would otherwise interfere with an operation in progress on the same device
    private static final ReentrantLock deviceLock = new ReentrantLock();

    //Indicates a pin prompt has been shown on a device and not yet answered. Enumerating in this state clears the pin screen
    private static volatile boolean isPromptActive = false;

    private final Set<byte[]> newDeviceRegistrations = new HashSet<>();

    static {
        deleteHwiDir();
    }

    public List<Device> enumerate(String passphrase) throws ImportException {
        deviceLock.lock();
        try {
            List<Device> devices = new ArrayList<>();
            for(BsvDeviceInfo info : getLark().enumerateBsvLedgers()) {
                WalletModel model = WalletModel.valueOf(info.model());
                if(model != WalletModel.LEDGER_NANO_S && model != WalletModel.LEDGER_NANO_S_PLUS && model != WalletModel.LEDGER_NANO_X) continue;
                Device device = new Device();
                device.setType("ledger"); device.setModel(model); device.setPath(info.path());
                device.setNeedsPinSent(false); device.setNeedsPassphraseSent(false);
                device.setSupportedScriptTypes(List.of(ScriptType.P2PKH));
                if(!info.state().equals("ready") && !info.state().equals("wrong_app")) device.setError(info.detail());
                devices.add(device);
            }
            return devices;
        } catch(DeviceException e) { throw new ImportException(e.getMessage(), e); }
        finally { deviceLock.unlock(); }
    }

    public boolean promptPin(Device device) throws ImportException {
        throw new ImportException("Unlock the Ledger on the device, then rescan");
    }

    public boolean sendPin(Device device, String pin) throws ImportException {
        throw new ImportException("Enter the PIN on the Ledger itself");
    }

    public boolean togglePassphrase(Device device) throws ImportException {
        throw new ImportException("Select the passphrase wallet on the Ledger itself");
    }

    public Map<WalletType, ExtendedKey> getXpubs(Device device, String passphrase, Map<WalletType, String> accountDerivationPaths, Map<WalletType, ExtendedKey> accountXpubs) throws ImportException {
        deviceLock.lock();
        try {
            for(Map.Entry<WalletType, String> entry : accountDerivationPaths.entrySet()) {
                accountXpubs.put(entry.getKey(), getXpub(device, passphrase, entry.getValue()));
            }
        } finally {
            deviceLock.unlock();
        }

        return accountXpubs;
    }

    public ExtendedKey getXpub(Device device, String passphrase, String derivationPath) throws ImportException {
        deviceLock.lock();
        try {
            prepareBsvDevice(device, passphrase);
            BsvAccount account = getLark().getBsvAccount(device.getPath(), derivationPath);
            // Never substitute an account fingerprint for a root fingerprint.
            device.setFingerprint(getLark().getBsvMasterFingerprint(device.getPath(), account));
            return ExtendedKey.fromDescriptor(account.xpub(), true);
        } catch(DeviceException | IllegalArgumentException e) { throw new ImportException(e.getMessage(), e); }
        finally { deviceLock.unlock(); }
    }

    public SilentPaymentScanAddress getSpscan(Device device, String passphrase, String derivationPath) throws ImportException {
        throw new ImportException("Silent payments are not supported by BitcoinSV");
    }

    public String displayAddress(Device device, String passphrase, ScriptType scriptType, OutputDescriptor addressDescriptor,
                                 OutputDescriptor walletDescriptor, String walletName, byte[] walletRegistration) throws DisplayAddressException {
        deviceLock.lock();
        try {
            BsvAccount account = BsvWalletSigning.account(walletDescriptor);
            String path = BsvWalletSigning.addressPath(account, addressDescriptor);
            prepareBsvDevice(device, passphrase);
            return getLark().displayBsvAddress(device.getPath(), account, path);
        } catch(DeviceException | IllegalArgumentException e) { throw new DisplayAddressException(e.getMessage(), e); }
        finally { deviceLock.unlock(); }
    }

    public String signMessage(Device device, String passphrase, String message, String derivationPath, OutputDescriptor descriptor) throws SignMessageException {
        deviceLock.lock();
        try {
            BsvAccount account = BsvWalletSigning.account(descriptor);
            prepareBsvDevice(device, passphrase);
            return getLark().signBsvMessage(device.getPath(), account, derivationPath, message);
        } catch(DeviceException | IllegalArgumentException e) { throw new SignMessageException(e.getMessage(), e); }
        finally { deviceLock.unlock(); }
    }

    public PSBT signPSBT(Device device, String passphrase, PSBT psbt,
                         OutputDescriptor walletDescriptor, String walletName, byte[] walletRegistration) throws SignTransactionException {
        deviceLock.lock();
        try {
            BsvAccount account = BsvWalletSigning.account(walletDescriptor);
            BsvSigningRequest request = BsvWalletSigning.prepare(psbt, account);
            prepareBsvDevice(device, passphrase);
            var result = getLark().signBsvTransaction(device.getPath(), account, request, BsvSigningRequest.VerificationPolicy.REQUIRE_TRUSTED);
            return BsvWalletSigning.apply(psbt, request, result);
        } catch(Exception e) { throw new SignTransactionException(e.getMessage(), e); }
        finally { deviceLock.unlock(); }
    }

    private void prepareBsvDevice(Device device, String passphrase) throws DeviceException {
        if(!"ledger".equals(device.getType())) throw new DeviceException("No BitcoinSV signing route is implemented for this device");
        if(passphrase != null && !passphrase.isEmpty()) throw new DeviceException("Select the passphrase wallet on the Ledger itself");
        var selected = getLark().enumerateBsvLedgers().stream().filter(info -> info.path().equals(device.getPath())).findFirst()
                .orElseThrow(() -> new DeviceException("The selected Ledger disconnected; rescan and select it again"));
        device.setPath(getLark().openBsvLedgerApp(selected).path());
    }

    public List<BsvDeviceInfo> enumerateBsvLedgers() throws DeviceException {
        deviceLock.lock();
        try { return getLark().enumerateBsvLedgers(); }
        finally { deviceLock.unlock(); }
    }

    public BsvDeviceInfo openBsvLedgerApp(BsvDeviceInfo selected) throws DeviceException {
        deviceLock.lock();
        try { return getLark().openBsvLedgerApp(selected); }
        finally { deviceLock.unlock(); }
    }

    public BsvAccount getBsvAccount(String devicePath, String accountPath) throws DeviceException {
        deviceLock.lock();
        try { return getLark().getBsvAccount(devicePath, accountPath); }
        finally { deviceLock.unlock(); }
    }

    public String displayBsvAddress(String devicePath, BsvAccount account, String path) throws DeviceException {
        deviceLock.lock();
        try { return getLark().displayBsvAddress(devicePath, account, path); }
        finally { deviceLock.unlock(); }
    }

    public String signBsvMessage(String devicePath, BsvAccount account, String path, String message) throws DeviceException {
        deviceLock.lock();
        try { return getLark().signBsvMessage(devicePath, account, path, message); }
        finally { deviceLock.unlock(); }
    }

    public BsvSigningRequest.VerificationPlan planBsvSigning(String devicePath, BsvSigningRequest request) throws DeviceException {
        deviceLock.lock();
        try { return getLark().planBsvSigning(devicePath, request); }
        finally { deviceLock.unlock(); }
    }

    public BsvSigningRequest signBsvTransaction(String devicePath, BsvAccount account, BsvSigningRequest request,
                                              BsvSigningRequest.VerificationPolicy policy) throws DeviceException {
        deviceLock.lock();
        try { return request.apply(getLark().signBsvTransaction(devicePath, account, request, policy)); }
        finally { deviceLock.unlock(); }
    }

    private Lark getLark() {
        return getLark(null);
    }

    private Lark getLark(String passphrase) {
        return getLark(passphrase, null, null, null);
    }

    private Lark getLark(String passphrase, OutputDescriptor walletDescriptor, String walletName, byte[] walletRegistration) {
        Lark lark = new Lark(AppServices.getHttpClientService());
        lark.setBitBoxNoiseConfig(new BitBoxFxNoiseConfig());
        lark.setTrezorNoiseConfig(new TrezorFxNoiseConfig());
        if(passphrase != null) {
            lark.setPassphrase(passphrase);
        }

        if(walletDescriptor != null && walletName != null) {
            if(walletRegistration != null) {
                lark.addWalletRegistration(walletDescriptor, walletName, walletRegistration);
            } else {
                lark.addWalletName(walletDescriptor, walletName);
            }
        }

        return lark;
    }

    private static void deleteHwiDir() {
        try {
            if(OsType.getCurrent() == OsType.MACOS || OsType.getCurrent() == OsType.WINDOWS) {
                File hwiHomeDir = new File(Storage.getCacheDir(), HWI_HOME_DIR);
                if(hwiHomeDir.exists()) {
                    IOUtils.deleteDirectory(hwiHomeDir);
                }
            }
        } catch(Exception e) {
            log.error("Error deleting hwi directory", e);
        }
    }

    public static class EnumerateService extends Service<List<Device>> {
        private final String passphrase;

        public EnumerateService(String passphrase) {
            this.passphrase = passphrase;
        }

        @Override
        protected Task<List<Device>> createTask() {
            return new Task<>() {
                protected List<Device> call() throws ImportException {
                    Hwi hwi = new Hwi();
                    return hwi.enumerate(passphrase);
                }
            };
        }
    }

    public static class ScheduledEnumerateService extends ScheduledService<List<Device>> {
        private final String passphrase;

        public ScheduledEnumerateService(String passphrase) {
            this.passphrase = passphrase;
        }

        @Override
        protected Task<List<Device>> createTask() {
            return new Task<>() {
                protected List<Device> call() throws ImportException {
                    if(!isPromptActive && deviceLock.tryLock()) {
                        try {
                            Hwi hwi = new Hwi();
                            return hwi.enumerate(passphrase);
                        } finally {
                            deviceLock.unlock();
                        }
                    }

                    return null;
                }
            };
        }
    }

    public static class PromptPinService extends Service<Boolean> {
        private final Device device;

        public PromptPinService(Device device) {
            this.device = device;
        }

        @Override
        protected Task<Boolean> createTask() {
            return new Task<>() {
                protected Boolean call() throws ImportException {
                    Hwi hwi = new Hwi();
                    return hwi.promptPin(device);
                }
            };
        }
    }

    public static class SendPinService extends Service<Boolean> {
        private final Device device;
        private final String pin;

        public SendPinService(Device device, String pin) {
            this.device = device;
            this.pin = pin;
        }

        @Override
        protected Task<Boolean> createTask() {
            return new Task<>() {
                protected Boolean call() throws ImportException {
                    Hwi hwi = new Hwi();
                    return hwi.sendPin(device, pin);
                }
            };
        }
    }

    public static class TogglePassphraseService extends Service<Boolean> {
        private final Device device;

        public TogglePassphraseService(Device device) {
            this.device = device;
        }

        @Override
        protected Task<Boolean> createTask() {
            return new Task<>() {
                protected Boolean call() throws ImportException {
                    Hwi hwi = new Hwi();
                    return hwi.togglePassphrase(device);
                }
            };
        }
    }

    public static class DisplayAddressService extends Service<String> {
        private final Device device;
        private final String passphrase;
        private final ScriptType scriptType;
        private final OutputDescriptor addressDescriptor;
        private final OutputDescriptor walletDescriptor;
        private final String walletName;
        private final byte[] walletRegistration;
        private final Set<byte[]> newDeviceRegistrations = new HashSet<>();

        public DisplayAddressService(Device device, String passphrase, ScriptType scriptType, OutputDescriptor addressDescriptor, OutputDescriptor walletDescriptor, String walletName, byte[] walletRegistration) {
            this.device = device;
            this.passphrase = passphrase;
            this.scriptType = scriptType;
            this.addressDescriptor = addressDescriptor;
            this.walletDescriptor = walletDescriptor;
            this.walletName = walletName;
            this.walletRegistration = walletRegistration;
        }

        public Set<byte[]> getNewDeviceRegistrations() {
            return newDeviceRegistrations;
        }

        @Override
        protected Task<String> createTask() {
            return new Task<>() {
                protected String call() throws DisplayAddressException {
                    Hwi hwi = new Hwi();
                    String address = hwi.displayAddress(device, passphrase, scriptType, addressDescriptor, walletDescriptor, walletName, walletRegistration);
                    newDeviceRegistrations.addAll(hwi.newDeviceRegistrations);
                    return address;
                }
            };
        }
    }

    public static class SignMessageService extends Service<String> {
        private final Device device;
        private final OutputDescriptor descriptor;
        private final String passphrase;
        private final String message;
        private final String derivationPath;

        public SignMessageService(Device device, String passphrase, String message, String derivationPath, OutputDescriptor descriptor) {
            this.device = device;
            this.descriptor = descriptor;
            this.passphrase = passphrase;
            this.message = message;
            this.derivationPath = derivationPath;
        }

        @Override
        protected Task<String> createTask() {
            return new Task<>() {
                protected String call() throws SignMessageException {
                    Hwi hwi = new Hwi();
                    return hwi.signMessage(device, passphrase, message, derivationPath, descriptor);
                }
            };
        }
    }

    public static class GetXpubService extends Service<ExtendedKey> {
        private final Device device;
        private final String passphrase;
        private final String derivationPath;

        public GetXpubService(Device device, String passphrase, String derivationPath) {
            this.device = device;
            this.passphrase = passphrase;
            this.derivationPath = derivationPath;
        }

        @Override
        protected Task<ExtendedKey> createTask() {
            return new Task<>() {
                protected ExtendedKey call() throws ImportException {
                    Hwi hwi = new Hwi();
                    return hwi.getXpub(device, passphrase, derivationPath);
                }
            };
        }
    }

    public static class GetSpscanService extends Service<SilentPaymentScanAddress> {
        private final Device device;
        private final String passphrase;
        private final String derivationPath;

        public GetSpscanService(Device device, String passphrase, String derivationPath) {
            this.device = device;
            this.passphrase = passphrase;
            this.derivationPath = derivationPath;
        }

        @Override
        protected Task<SilentPaymentScanAddress> createTask() {
            return new Task<>() {
                protected SilentPaymentScanAddress call() throws ImportException {
                    Hwi hwi = new Hwi();
                    return hwi.getSpscan(device, passphrase, derivationPath);
                }
            };
        }
    }

    public static class GetXpubsService extends Service<Map<WalletType, ExtendedKey>> {
        private final Device device;
        private final String passphrase;
        private final Map<WalletType, String> accountDerivationPaths;

        public GetXpubsService(Device device, String passphrase, Map<WalletType, String> accountDerivationPaths) {
            this.device = device;
            this.passphrase = passphrase;
            this.accountDerivationPaths = accountDerivationPaths;
        }

        @Override
        protected Task<Map<WalletType, ExtendedKey>> createTask() {
            return new Task<>() {
                protected Map<WalletType, ExtendedKey> call() throws ImportException {
                    Hwi hwi = new Hwi();
                    updateProgress(0, accountDerivationPaths.size());
                    ObservableMap<WalletType, ExtendedKey> accountXpubs = FXCollections.observableMap(new LinkedHashMap<>());
                    accountXpubs.addListener((MapChangeListener<? super WalletType, ? super ExtendedKey>) _ -> updateProgress(accountXpubs.size(), accountDerivationPaths.size()));
                    return hwi.getXpubs(device, passphrase, accountDerivationPaths, accountXpubs);
                }
            };
        }
    }

    public static class SignPSBTService extends Service<PSBT> {
        private final Device device;
        private final String passphrase;
        private final PSBT psbt;
        private final OutputDescriptor walletDescriptor;
        private final String walletName;
        private final byte[] walletRegistration;
        private final Set<byte[]> newDeviceRegistrations = new HashSet<>();

        public SignPSBTService(Device device, String passphrase, PSBT psbt, OutputDescriptor walletDescriptor, String walletName, byte[] walletRegistration) {
            this.device = device;
            this.passphrase = passphrase;
            this.psbt = psbt;
            this.walletDescriptor = walletDescriptor;
            this.walletName = walletName;
            this.walletRegistration = walletRegistration;
        }

        public Set<byte[]> getNewDeviceRegistrations() {
            return newDeviceRegistrations;
        }

        @Override
        protected Task<PSBT> createTask() {
            return new Task<>() {
                protected PSBT call() throws SignTransactionException {
                    Hwi hwi = new Hwi();
                    PSBT signed = hwi.signPSBT(device, passphrase, psbt, walletDescriptor, walletName, walletRegistration);
                    newDeviceRegistrations.addAll(hwi.newDeviceRegistrations);
                    return signed;
                }
            };
        }
    }

    private static final class BitBoxFxNoiseConfig extends BitBoxFileNoiseConfig {
        private static final AtomicBoolean attestationWarningShown = new AtomicBoolean(false);

        private BitBoxPairingDialog pairingDialog;

        public BitBoxFxNoiseConfig() {
            super(Path.of(Storage.getDataHome().getAbsolutePath(), LARK_HOME_DIR, BITBOX_FILENAME).toFile());
        }

        @Override
        public void attestationCheck(boolean result) {
            if(!result) {
                log.warn("BitBox02 attestation check failed, device may not be genuine");
                //Devices are opened repeatedly while enumerating, so warn only once per session
                if(attestationWarningShown.compareAndSet(false, true)) {
                    Platform.runLater(() -> AppServices.showWarningDialog("BitBox02 Attestation Failed",
                            "This BitBox02 did not pass the attestation check, which means it may not be a genuine device.\n\n" +
                                    "Do not use it to store funds until you have verified it externally."));
                }
            }
        }

        @Override
        public boolean showPairing(String code, DeviceResponse response) throws DeviceException {
            CountDownLatch latch = new CountDownLatch(1);
            AtomicBoolean confirmedDevice = new AtomicBoolean(false);
            AtomicReference<DeviceException> deviceException = new AtomicReference<>();

            Thread showPairingDeviceThread = new Thread(() -> {
                try {
                    confirmedDevice.set(response.call());
                } catch(DeviceException e) {
                    deviceException.set(e);
                } finally {
                    latch.countDown();
                }
            });
            showPairingDeviceThread.start();

            Platform.runLater(() -> {
                pairingDialog = new BitBoxPairingDialog(code);
                pairingDialog.initOwner(AppServices.getActiveWindow());
                pairingDialog.show();
            });

            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            Platform.runLater(() -> {
                if(pairingDialog != null && pairingDialog.isShowing()) {
                    pairingDialog.setResult(ButtonType.APPLY);
                }
                if(deviceException.get() == null && !confirmedDevice.get()) {
                    AppServices.showWarningDialog("Pairing Refused", "Pairing was refused on the device.");
                }
            });

            if(deviceException.get() != null) {
                throw deviceException.get();
            }

            return confirmedDevice.get();
        }
    }

    private static final class TrezorFxNoiseConfig extends TrezorFileNoiseConfig {
        private String deviceInfo;

        public TrezorFxNoiseConfig() {
            super(Path.of(Storage.getDataHome().getAbsolutePath(), LARK_HOME_DIR, TREZOR_FILENAME).toFile());
        }

        @Override
        public String promptForPairingCode() {
            CompletableFuture<String> future = new CompletableFuture<>();
            Platform.runLater(() -> {
                TextfieldDialog textfieldDialog = new TextfieldDialog();
                textfieldDialog.initOwner(AppServices.getActiveWindow());
                textfieldDialog.setTitle("Enter Pairing Code");
                textfieldDialog.setHeaderText("Enter the code shown on the " + deviceInfo + ":");
                textfieldDialog.getDialogPane().setPrefWidth(300);
                textfieldDialog.getEditor().setOnAction(_ -> textfieldDialog.setResult(textfieldDialog.getEditor().getText()));
                textfieldDialog.getEditor().setTextFormatter(new TextFormatter<>(change -> {
                    String newText = change.getControlNewText();
                    if(newText.matches("\\d*")) {
                        return change;
                    }
                    return null;
                }));
                textfieldDialog.getEditor().setStyle("-fx-font-size: 30px;");
                HBox.setMargin(textfieldDialog.getEditor(), new Insets(0, 65, 0, 65));
                textfieldDialog.getEditor().requestFocus();
                textfieldDialog.showAndWait().ifPresentOrElse(future::complete, () -> future.complete(null));
            });

            try {
                return future.get(); // Block until dialog is closed
            } catch (InterruptedException | ExecutionException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }

        @Override
        public boolean confirmPairing(String deviceInfo) {
            this.deviceInfo = deviceInfo;
            CompletableFuture<ButtonType> future = new CompletableFuture<>();
            Platform.runLater(() -> {
                AppServices.showAlertDialog("Pairing Required", "Pair the " + deviceInfo + " with " + SparrowWallet.APP_NAME + "?",
                        Alert.AlertType.CONFIRMATION, ButtonType.YES, ButtonType.NO).ifPresentOrElse(future::complete, () -> future.complete(null));
            });

            try {
                return future.get() == ButtonType.YES; // Block until dialog is closed
            } catch (InterruptedException | ExecutionException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        @Override
        public void displayPairingCode(String code) {
            super.displayPairingCode(code);
        }

        @Override
        public String getAppName() {
            return SparrowWallet.APP_NAME;
        }

        @Override
        public void pairingFailed(String reason) {
            Platform.runLater(() -> AppServices.showErrorDialog("Pairing Failed", "Pairing failed: " + reason));
        }

        @Override
        public void pairingSuccessful(String deviceInfo) {
            this.deviceInfo = deviceInfo;
            Platform.runLater(() -> AppServices.showSuccessDialog("Pairing Successful", "The " + deviceInfo + " has been successfully paired."));
        }
    }

    public record WalletType(ScriptType scriptType, StandardAccount standardAccount) {}
}
