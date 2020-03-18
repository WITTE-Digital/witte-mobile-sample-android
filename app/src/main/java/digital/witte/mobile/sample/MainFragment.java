package digital.witte.mobile.sample;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.tapkey.mobile.TapkeyServiceFactory;
import com.tapkey.mobile.ble.BleLockCommunicator;
import com.tapkey.mobile.ble.BleLockScanner;
import com.tapkey.mobile.concurrent.CancellationToken;
import com.tapkey.mobile.concurrent.CancellationTokens;
import com.tapkey.mobile.manager.CommandExecutionFacade;
import com.tapkey.mobile.manager.KeyManager;
import com.tapkey.mobile.manager.NotificationManager;
import com.tapkey.mobile.manager.UserManager;
import com.tapkey.mobile.model.CommandResult;
import com.tapkey.mobile.model.KeyDetails;
import com.tapkey.mobile.utils.Func1;
import com.tapkey.mobile.utils.ObserverRegistration;

import net.tpky.mc.model.Grant;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import digital.witte.wittemobilelibrary.Configuration;
import digital.witte.wittemobilelibrary.box.BoxFeedback;
import digital.witte.wittemobilelibrary.box.BoxIdConverter;
import digital.witte.wittemobilelibrary.box.BoxState;

public class MainFragment extends Fragment {

    private static final String TAG = MainFragment.class.getCanonicalName();

    private static final int PERMISSIONS_REQUEST__ACCESS_COARSE_LOCATION = 0;

    private TextView _tvCustomerId;
    private TextView _tvSubscriptionKey;
    private TextView _tvSdkKey;
    private TextView _tvUserId;
    private TextView _tvLocalKeys;
    private EditText _tvBoxId;
    private Button _btnTriggerLock;
    private Button _btnLogin;
    private Button _btnLogout;
    private Button _btnQueryLocalKeys;

    private BleLockCommunicator _bleBleLockCommunicator;
    private BleLockScanner _bleLockScanner;
    private KeyManager _keyManager;
    private CommandExecutionFacade _commandExecutionFacade;
    private UserManager _userManager;
    private NotificationManager _notificationManager;
    private ObserverRegistration _keyUpdateObserverRegistration;
    private ObserverRegistration _foregroundScanRegistration;
    private ArrayList<KeyDetails> _keys = new ArrayList<>();
    private ProgressDialog _progressDialog;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.main_fragment, container, false);
        _tvCustomerId = view.findViewById(R.id.main_frag_tv_customer_id);
        _tvSubscriptionKey = view.findViewById(R.id.main_frag_tv_subscription_key);
        _tvSdkKey = view.findViewById(R.id.main_frag_tv_sdk_key);
        _tvUserId = view.findViewById(R.id.main_frag_tv_user_id);
        _tvLocalKeys = view.findViewById(R.id.main_frag_tv_local_keys);

        _btnLogin = view.findViewById(R.id.main_frag_btn_authenticate);
        _btnLogin.setOnClickListener(button -> login());

        _btnLogout = view.findViewById(R.id.main_frag_btn_logout);
        _btnLogout.setOnClickListener(button -> logout());

        _tvBoxId = view.findViewById(R.id.main_frag_et_box_id);
        _tvBoxId.setHint("e.g. C1-1F-8E-7C");

        _btnTriggerLock = view.findViewById(R.id.main_frag_btn_trigger);
        _btnTriggerLock.setOnClickListener(button -> triggerLock());

        _btnQueryLocalKeys = view.findViewById(R.id.main_frag_btn_query_local_keys);
        _btnQueryLocalKeys.setOnClickListener(button -> queryLocalKeys());

        return view;
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        TapkeyServiceFactory serviceFactory = ((App) getActivity().getApplication()).getTapkeyServiceFactory();
        _bleLockScanner = serviceFactory.getBleLockScanner();
        _bleBleLockCommunicator = serviceFactory.getBleLockCommunicator();
        _keyManager = serviceFactory.getKeyManager();
        _commandExecutionFacade = serviceFactory.getCommandExecutionFacade();
        _userManager = serviceFactory.getUserManager();
        _notificationManager = serviceFactory.getNotificationManager();

        Configuration witteConfiguration = App.WitteConfiguration;
        _tvCustomerId.setText(String.format("%d", witteConfiguration.getWitteCustomerId()));
        _tvSubscriptionKey.setText(witteConfiguration.getWitteSubscriptionKey());
        _tvSdkKey.setText(witteConfiguration.getWitteSdkKey());
        _tvUserId.setText(String.format("%d", App.WitteUserId));
    }

    @Override
    public void onResume() {
        super.onResume();

        // check permissions
        if (ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_COARSE_LOCATION)) {
                // TODO: show permission rationale
            } else {
                requestPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSIONS_REQUEST__ACCESS_COARSE_LOCATION);
            }
        } else {
            if (null == _foregroundScanRegistration) {
                _foregroundScanRegistration = _bleLockScanner.startForegroundScan();
            }
        }

        // Listen for changes in the available keys. Changes might happen, e.g. due to push
        // notifications received from the Tapkey backend.
        if (_keyUpdateObserverRegistration == null) {
            _keyUpdateObserverRegistration = _keyManager
                    .getKeyUpdateObservable()
                    .addObserver(aVoid -> onKeyUpdate());
        }

        onKeyUpdate();

        updateUI();
    }

    @Override
    public void onPause() {
        super.onPause();

        if (null != _foregroundScanRegistration) {
            _foregroundScanRegistration.close();
            _foregroundScanRegistration = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void queryLocalKeys() {
        if (isUserLoggedIn()) {
            // query for this user's keys
            String userId = _userManager.getUsers().get(0);
            _keyManager.queryLocalKeysAsync(userId, CancellationTokens.None)
                    .continueOnUi((Func1<List<KeyDetails>, Void, Exception>) keyDetails -> {
                        _keys.clear();
                        _keys.addAll(keyDetails);

                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US);
                        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                        StringBuilder sb = new StringBuilder();

                        for (KeyDetails key : keyDetails) {
                            Grant grant = key.getGrant();
                            if (null != grant) {
                                String physicalLockId = grant.getBoundLock().getPhysicalLockId();
                                String boxId = BoxIdConverter.toBoxId(physicalLockId);
                                Date grantValidFrom = grant.getValidFrom();
                                Date grantValidBefore = grant.getValidBefore();
                                Date keyValidBefore = key.getValidBefore();

                                sb.append(String.format("• %s%s", boxId, System.lineSeparator()));
                                sb.append(String.format("\tgrant starts: %s%s", sdf.format(grantValidFrom), System.lineSeparator()));
                                if (null != grantValidBefore) {
                                    sb.append(String.format("\tgrant ends: %s%s", sdf.format(keyValidBefore), System.lineSeparator()));
                                } else {
                                    sb.append(String.format("\tgrant ends: unlimited%s", System.lineSeparator()));
                                }

                                sb.append(String.format("\tvalid before: %s%s", sdf.format(keyValidBefore), System.lineSeparator()));
                            }
                        }

                        _tvLocalKeys.setText(sb.toString());

                        return null;
                    })
                    .catchOnUi(e -> {
                        Log.e(TAG, "queryLocalKeys failed ", e);
                        return null;
                    })
                    // make sure, we don't miss any exceptions.
                    .conclude();
        }
    }

    private void onKeyUpdate() {
        queryLocalKeys();
    }

    private boolean isUserLoggedIn() {
        boolean isLoggedIn = false;

        if (null != _userManager) {
            List<String> userIds = _userManager.getUsers();
            if (1 == userIds.size()) {
                isLoggedIn = true;
            }
        }

        return isLoggedIn;
    }

    private void updateUI() {
        if (isUserLoggedIn()) {
            _btnLogout.setEnabled(true);
            _btnLogin.setEnabled(false);
            _btnTriggerLock.setEnabled(true);
            _btnQueryLocalKeys.setEnabled(true);
            _tvBoxId.setEnabled(true);
        } else {
            _btnLogout.setEnabled(false);
            _btnLogin.setEnabled(true);
            _btnTriggerLock.setEnabled(false);
            _btnQueryLocalKeys.setEnabled(false);
            _tvBoxId.setEnabled(false);
            _tvLocalKeys.setText("");
        }
    }

    private void login() {
        if (!isUserLoggedIn()) {

            try {
                _progressDialog = ProgressDialog.show(
                        getContext(),
                        "",
                        "Logging in...",
                        true);
            } catch (Exception e) {
                e.printStackTrace();
            }

            WitteTokenProvider witteTokenProvider = new WitteTokenProvider(
                    this.getContext(),
                    App.WitteConfiguration,
                    App.WitteUserId);

            witteTokenProvider.AccessToken()
                    .continueOnUi(accessToken -> {
                        if (null != accessToken && accessToken != "") {
                            // login with access token
                            _userManager.logInAsync(accessToken, CancellationTokens.None)
                                    .continueOnUi(userId -> {
                                        // query keys
                                        _notificationManager.pollForNotificationsAsync(CancellationTokens.None)
                                                .catchOnUi(e -> {
                                                    Log.e(TAG, "Failed to poll for notifications.", e);
                                                    return null;
                                                }).conclude();

                                        updateUI();

                                        return null;
                                    })
                                    .catchOnUi(e -> {
                                        e.printStackTrace();
                                        return null;
                                    })
                                    .finallyOnUi(() -> {
                                        if (null != _progressDialog) {
                                            _progressDialog.dismiss();
                                        }
                                        _progressDialog = null;
                                    });
                        }
                        return null;
                    })
                    .catchOnUi(e -> {
                        e.printStackTrace();
                        return null;
                    })
                    .finallyOnUi(() -> {
                        if (null != _progressDialog) {
                            _progressDialog.dismiss();
                        }
                        _progressDialog = null;
                    });
        }
    }

    private void logout() {
        if (isUserLoggedIn()) {
            String userId = _userManager.getUsers().get(0);
            _userManager.logOutAsync(userId, CancellationTokens.None)
                    .finallyOnUi(() -> {
                        updateUI();
                    });
        }
    }

    private void triggerLock() {
        String boxId = _tvBoxId.getText().toString();
        if ("".equals(boxId)) {
            Toast.makeText(getContext(), "Please enter your box ID.", Toast.LENGTH_SHORT).show();
            return;
        }

        String physicalLockId;
        try {
            physicalLockId = BoxIdConverter.toPhysicalLockId(boxId);
            if (!_bleLockScanner.isLockNearby(physicalLockId)) {
                Toast.makeText(getContext(), "The box is not in reach.", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (Exception exception) {
            Toast.makeText(getContext(), exception.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        // 60s timeout
        final int timeoutInMs = 60 * 1000;
        CancellationToken timeout = CancellationTokens.fromTimeout(timeoutInMs);

        String bluetoothAddress = _bleLockScanner.getLock(physicalLockId).getBluetoothAddress();
        _bleBleLockCommunicator.executeCommandAsync(bluetoothAddress, physicalLockId, tlcpConnection -> _commandExecutionFacade.triggerLockAsync(tlcpConnection, timeout), timeout)
                .continueOnUi(commandResult -> {
                    boolean success = false;

                    // The CommandResultCode indicates if triggerLockAsync completed successfully
                    // or if an error occurred during the execution of the command.
                    // https://developers.tapkey.io/mobile/android/reference/Tapkey.MobileLib/latest/com/tapkey/mobile/model/CommandResult.CommandResultCode.html
                    CommandResult.CommandResultCode commandResultCode = commandResult.getCommandResultCode();
                    switch (commandResultCode) {
                        case Ok: {
                            success = true;

                            // get the 10 byte box feedback from the command result
                            Object object = commandResult.getResponseData();
                            if (object instanceof byte[]) {

                                byte[] responseData = (byte[]) object;
                                try {
                                    BoxFeedback boxFeedback = BoxFeedback.create(responseData);
                                    int boxState = boxFeedback.getBoxState();
                                    if (BoxState.UNLOCKED == boxState) {

                                        Log.d(TAG, "Box has been opened");
                                    } else if (BoxState.LOCKED == boxState) {

                                        Log.d(TAG, "Box has been closed");
                                    } else if (BoxState.DRAWER_OPEN == boxState) {

                                        Log.d(TAG, "The drawer of the Box is open.");
                                    }
                                } catch (IllegalArgumentException iaEx) {
                                    Log.e(TAG, iaEx.getMessage());
                                }
                            }
                            break;
                        }
                        case LockCommunicationError:
                        {
                            Log.e(TAG, "A transport-level error occurred when communicating with the locking device");
                            break;
                        }
                        case LockDateTimeInvalid:
                        {
                            Log.e(TAG, "Lock date/time are invalid.");
                            break;
                        }
                        case ServerCommunicationError:
                        {
                            Log.e(TAG, "An error occurred while trying to communicate with the Tapkey Trust Service (e.g. due to bad internet connection).");
                            break;
                        }
                        case TechnicalError:
                        {
                            Log.e(TAG, "Some unspecific technical error has occurred.");
                            break;
                        }
                        case Unauthorized:
                        {
                            Log.e(TAG, "Communication with the security backend succeeded but the user is not authorized for the given command on this locking device.");
                            break;
                        }
                        case UserSpecificError: {
                            // If there is a UserSpecificError we need to have look at the list
                            // of UserCommandResults in order to determine what exactly caused the error
                            // https://developers.tapkey.io/mobile/android/reference/Tapkey.MobileLib/latest/com/tapkey/mobile/model/CommandResult.UserCommandResult.html
                            List<CommandResult.UserCommandResult> userCommandResults = commandResult.getUserCommandResults();
                            for (CommandResult.UserCommandResult ucr : userCommandResults) {
                                Log.e(TAG, "triggerLockAsync failed with UserSpecificError and UserCommandResultCode " + ucr.getUserCommandResultCode());
                            }
                            break;
                        }
                        default: {
                            break;
                        }
                    }

                    if(success) {
                        Toast.makeText(getContext(), "triggerLock successful", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Toast.makeText(getContext(), "triggerLock error", Toast.LENGTH_SHORT).show();
                    }

                    return success;
                })
                .catchOnUi(e -> {
                    Toast.makeText(getContext(), "triggerLock exception", Toast.LENGTH_SHORT).show();
                    return false;
                });
    }
}
