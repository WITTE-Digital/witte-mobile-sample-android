package digital.witte.mobile.sample;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleObserver;

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
import com.tapkey.mobile.model.UserGrant;
import com.tapkey.mobile.tlcp.commands.DefaultTriggerLockCommandBuilder;
import com.tapkey.mobile.utils.ObserverRegistration;

import net.tpky.mc.tlcp.model.TriggerLockCommand;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import digital.witte.mobile.sample.backend.DemoBackendAccessor;
import digital.witte.wittemobilelibrary.box.BoxCommandBuilder;
import digital.witte.wittemobilelibrary.box.BoxFeedback;
import digital.witte.wittemobilelibrary.box.BoxFeedbackV3;
import digital.witte.wittemobilelibrary.box.BoxFeedbackV3Parser;
import digital.witte.wittemobilelibrary.box.BoxIdConverter;
import digital.witte.wittemobilelibrary.box.BoxState;

/**
 * The MainFragment class represents the main fragment of the application.
 * It extends the Fragment class and implements the LifecycleObserver interface.
 * This fragment is responsible for handling user interactions and displaying UI elements.
 */
public class MainFragment extends Fragment implements LifecycleObserver {

    private static final String TAG = MainFragment.class.getCanonicalName();
    
    /**
     * Activity result launcher for requesting permission.
     * This launcher is used to request a specific permission and handle the result.
     */
    private final ActivityResultLauncher<String> _requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
        if (isGranted) {
            Log.d(TAG, "Permission granted");
        }
        else {
            Log.d(TAG, "Permission denied");
        }
    });

    /**
     * Activity result launcher for requesting multiple permissions.
     * It registers for activity result using ActivityResultContracts.RequestMultiplePermissions
     * and handles the result by checking if all permissions are granted or if any permission is denied.
     */
    private final ActivityResultLauncher<String[]> _requestMultiplePermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), isGrantedMap -> {
        if (!isGrantedMap.containsValue(false)) {
            Log.d(TAG, "All permissions granted");
        }
        else {
            Log.d(TAG, "One or more permissions denied");
        }
    });

    // An ArrayList to store KeyDetails objects
    private final ArrayList<KeyDetails> _keys = new ArrayList<>();

    // A TextView to display local keys
    private TextView mTvLocalKeys;

    // An EditText to input box id
    private EditText mTvBoxId;

    // A Button to trigger lock
    private Button mBtnTriggerLock;

    // A Button to unlock
    private Button mBtnUnlock;

    // A Button to lock
    private Button mBtnLock;

    // A Button to login
    private Button mBtnLogin;

    // A Button to logout
    private Button mBtnLogout;

    // A Button to query local keys
    private Button mBtnQueryLocalKeys;

    // A TokenProvider to manage tokens
    private TokenProvider mTokenProvider;

    // A BleLockCommunicator to manage BLE lock communication
    private BleLockCommunicator mBleBleLockCommunicator;

    // A BleLockScanner to scan for BLE locks
    private BleLockScanner mBleLockScanner;

    // A KeyManager to manage keys
    private KeyManager mKeyManager;

    // A CommandExecutionFacade to execute commands
    private CommandExecutionFacade mCommandExecutionFacade;

    // A UserManager to manage users
    private UserManager mUserManager;

    // A NotificationManager to manage notifications
    private NotificationManager mNotificationManager;

    // An ObserverRegistration for key update observer
    private ObserverRegistration mKeyUpdateObserverRegistration;

    // An ObserverRegistration for foreground scan observer
    private ObserverRegistration mForegroundScanRegistration;

    // A ProgressDialog to display progress
    private ProgressDialog mProgressDialog;

    /**
     * Called when the fragment is attached to a context.
     * Initializes various dependencies required by the fragment.
     *
     * @param context The context to which the fragment is attached.
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        Activity activity = getActivity();
        if (null != activity) {
            App app = ((App) activity.getApplication());
            if (null != app) {
                mTokenProvider = app.getTokenProvider();
                TapkeyServiceFactory serviceFactory = app.getTapkeyServiceFactory();
                mBleLockScanner = serviceFactory.getBleLockScanner();
                mBleBleLockCommunicator = serviceFactory.getBleLockCommunicator();
                mKeyManager = serviceFactory.getKeyManager();
                mCommandExecutionFacade = serviceFactory.getCommandExecutionFacade();
                mUserManager = serviceFactory.getUserManager();
                mNotificationManager = serviceFactory.getNotificationManager();
            }
        }
    }

    /**
     * Creates and returns the View for the fragment's UI.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container          The parent view that the fragment's UI should be attached to.
     * @param savedInstanceState The saved instance state of the fragment.
     * @return The View for the fragment's UI.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.main_fragment, container, false);
        TextView mTvCustomerId = view.findViewById(R.id.main_frag_tv_customer_id);
        TextView _tvApiKey = view.findViewById(R.id.main_frag_tv_subscription_key);
        TextView _tvSdkKey = view.findViewById(R.id.main_frag_tv_sdk_key);
        TextView _tvUserId = view.findViewById(R.id.main_frag_tv_user_id);
        mTvLocalKeys = view.findViewById(R.id.main_frag_tv_local_keys);

        mBtnLogin = view.findViewById(R.id.main_frag_btn_authenticate);
        mBtnLogin.setOnClickListener(button -> login());

        mBtnLogout = view.findViewById(R.id.main_frag_btn_logout);
        mBtnLogout.setOnClickListener(button -> logout());

        mTvBoxId = view.findViewById(R.id.main_frag_et_box_id);
        mTvBoxId.setHint("e.g. C1-1F-8E-7C");        

        mBtnTriggerLock = view.findViewById(R.id.main_frag_btn_trigger);
        mBtnTriggerLock.setOnClickListener(button -> triggerLock());

        mBtnUnlock = view.findViewById(R.id.main_frag_btn_unlock);
        mBtnUnlock.setOnClickListener(button -> unlock());

        mBtnLock = view.findViewById(R.id.main_frag_btn_lock);
        mBtnLock.setOnClickListener(button -> lock());

        mBtnQueryLocalKeys = view.findViewById(R.id.main_frag_btn_query_local_keys);
        mBtnQueryLocalKeys.setOnClickListener(button -> queryLocalKeys());

        mTvCustomerId.setText(String.format(Locale.US, "%d", DemoBackendAccessor.FlinkeyCustomerId));
        _tvApiKey.setText(DemoBackendAccessor.FlinkeyApiKey);
        _tvSdkKey.setText(DemoBackendAccessor.FlinkeySdkKey);
        _tvUserId.setText(String.format(Locale.US, "%d", DemoBackendAccessor.FlinkeyUserId));

        return view;
    }

    /**
     * Called when the fragment is resumed. This method is called after the fragment has been
     * visible to the user. It checks for required permissions, requests them if necessary,
     * starts scanning for flinkey boxes, and registers for digital key updates.
     * It also updates the UI.
     */
    @Override
    public void onResume() {
        super.onResume();

        // check required permission
        Context context = getContext();
        if (null != context) {
            // Check and request permissions
            boolean scanningPermissionGranted = false;
            if (Build.VERSION_CODES.S <= Build.VERSION.SDK_INT) {
                scanningPermissionGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
                        && ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
                if (!scanningPermissionGranted) {
                    if (!shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_SCAN) && !shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_CONNECT)) {
                        _requestMultiplePermissionLauncher.launch(new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT});
                    }
                    else {
                        Log.e(TAG, "TODO: show permission rationale");
                    }
                }
            }
            else {
                scanningPermissionGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
                if (!scanningPermissionGranted) {
                    if (!shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                        _requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                    }
                    else {
                        Log.e(TAG, "TODO: show permission rationale");
                    }
                }
            }

            // Start scanning
            if (scanningPermissionGranted && null == mForegroundScanRegistration) {
                // Start scanning for flinkey boxes
                mForegroundScanRegistration = mBleLockScanner.startForegroundScan();
            }

            if (null == mKeyUpdateObserverRegistration) {
                // Register for digital key updates
                mKeyUpdateObserverRegistration = mKeyManager.getKeyUpdateObservable().addObserver(aVoid -> queryLocalKeys());
            }

            updateUI();
        }
    }

    /**
     * Called when the fragment is no longer in the foreground and is being paused.
     * This method is responsible for stopping the scanning for flinkey boxes and closing any observer registrations.
     */
    @Override
    public void onPause() {
        super.onPause();

        if (null != mForegroundScanRegistration) {
            // Stop scanning for flinkey boxes
            mForegroundScanRegistration.close();
            mForegroundScanRegistration = null;
        }

        if (null != mKeyUpdateObserverRegistration) {
            mKeyUpdateObserverRegistration.close();
            mKeyUpdateObserverRegistration = null;
        }
    }

    /**
     * Queries the local keys for the logged in user and displays the result in the UI.
     */
    private void queryLocalKeys() {
        if (isUserLoggedIn()) {
            // query for this user's keys
            String userId = mUserManager.getUsers().get(0);
            List<KeyDetails> keys = mKeyManager.getLocalKeys(userId);
            _keys.clear();
            _keys.addAll(keys);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            StringBuilder sb = new StringBuilder();

            for (KeyDetails key : keys) {
                UserGrant grant = key.getGrant();
                if (null != grant) {
                    String physicalLockId = grant.getBoundLock().getPhysicalLockId();
                    String boxId = BoxIdConverter.toBoxId(physicalLockId);
                    Date grantValidFrom = grant.getValidFrom();
                    Date grantValidBefore = grant.getValidBefore();
                    Date keyValidBefore = key.getValidBefore();

                    sb.append(String.format("• %s%s", boxId, System.lineSeparator()));
                    if (null != grantValidFrom) {
                        sb.append(String.format("\tgrant starts: %s%s", sdf.format(grantValidFrom), System.lineSeparator()));
                    }
                    else {
                        sb.append(String.format("\tgrant starts: undefined %s", System.lineSeparator()));
                    }

                    if (null != grantValidBefore) {
                        sb.append(String.format("\tgrant ends: %s%s", sdf.format(keyValidBefore), System.lineSeparator()));
                    }
                    else {
                        sb.append(String.format("\tgrant ends: unlimited%s", System.lineSeparator()));
                    }

                    sb.append(String.format("\tvalid before: %s%s", sdf.format(keyValidBefore), System.lineSeparator()));
                }
            }

            mTvLocalKeys.setText(sb.toString());
        }
    }

    /**
     * Checks if a user is logged in to the Tapkey Mobile Library.
     *
     * @return true is a user is logged in
     */
    private boolean isUserLoggedIn() {
        boolean isLoggedIn = false;

        if (null != mUserManager) {
            List<String> userIds = mUserManager.getUsers();
            if (1 == userIds.size()) {
                isLoggedIn = true;
            }
        }

        return isLoggedIn;
    }

    /**
     * Updates UI controls according to the state of the users login.
     */
    private void updateUI() {
        if (isUserLoggedIn()) {
            mBtnLogout.setEnabled(true);
            mBtnLogin.setEnabled(false);
            mBtnTriggerLock.setEnabled(true);
            mBtnUnlock.setEnabled(true);
            mBtnLock.setEnabled(true);
            mBtnQueryLocalKeys.setEnabled(true);
            mTvBoxId.setEnabled(true);
        }
        else {
            mBtnLogout.setEnabled(false);
            mBtnLogin.setEnabled(true);
            mBtnTriggerLock.setEnabled(false);
            mBtnUnlock.setEnabled(false);
            mBtnLock.setEnabled(false);
            mBtnQueryLocalKeys.setEnabled(false);
            mTvBoxId.setEnabled(false);
            mTvLocalKeys.setText("");
        }
    }

    /**
     * Authenticates a user with the Tapkey Mobile Library.
     */
    private void login() {
        if (isUserLoggedIn()) {
            return;
        }

        mProgressDialog = ProgressDialog.show(getContext(), "", "Log in...", true);

        // retrieve an access token
        mTokenProvider.AccessToken().continueOnUi(accessToken -> {
            if (null != accessToken && !"".equals(accessToken)) {
                // login with access token
                mUserManager.logInAsync(accessToken, CancellationTokens.None).continueOnUi(userId -> {
                    // synchronize digital keys
                    mNotificationManager.pollForNotificationsAsync(CancellationTokens.None)
                            .catchOnUi(e -> {
                                Log.e(TAG, "Failed to poll for notifications.", e);
                                return null;
                            }).conclude();

                    updateUI();
                    return null;
                }).catchOnUi(e -> {
                    e.printStackTrace();
                    return null;
                }).finallyOnUi(() -> {
                    if (null != mProgressDialog) {
                        mProgressDialog.dismiss();
                    }
                    mProgressDialog = null;
                });
            }
            return null;
        }).catchOnUi(e -> {
            e.printStackTrace();
            return null;
        }).finallyOnUi(() -> {
            if (null != mProgressDialog) {
                mProgressDialog.dismiss();
            }
            mProgressDialog = null;
        });
    }

    /**
     * Logs the user out from the Tapkey Mobile Library
     */
    private void logout() {
        if (!isUserLoggedIn()) {
            return;
        }
        String userId = mUserManager.getUsers().get(0);
        mUserManager.logOutAsync(userId, CancellationTokens.None).finallyOnUi(this::updateUI);
    }

    /**
     * Unlocks the box by building an unlock command and executing it.
     */
    private void unlock() {
        byte[] bytes = BoxCommandBuilder.buildUnlockCarUnlockBox();
        String boxCommandData = Base64.getEncoder().encodeToString(bytes);
        executeBoxCommand(boxCommandData);
    }

    
    /**
     * Locks the box by building a lock box command and executing it.
     */
    private void lock() {
        byte[] bytes = BoxCommandBuilder.buildLockCarLockBox();
        String boxCommandData = Base64.getEncoder().encodeToString(bytes);
        executeBoxCommand(boxCommandData);
    }

    /**
     * Opens of closes (triggers) a flinkey box.
     */
    @SuppressLint("MissingPermission")
    private void triggerLock() {
        executeBoxCommand(null);
    }

    /**
     * Executes a box command.
     * 
     * @param boxCommandData The data for the box command.
     */
    private void executeBoxCommand(String boxCommandData) {
        String boxId = mTvBoxId.getText().toString();
        if ("".equals(boxId)) {
            Toast.makeText(getContext(), "Please enter your box ID.", Toast.LENGTH_SHORT).show();
            return;
        }

        String physicalLockId;
        try {
            physicalLockId = BoxIdConverter.toPhysicalLockId(boxId);
            if (!mBleLockScanner.isLockNearby(physicalLockId)) {
                Toast.makeText(getContext(), "The box is not in reach.", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        catch (Exception exception) {
            Toast.makeText(getContext(), exception.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        if (ActivityCompat.checkSelfPermission(this.requireActivity(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        // 60s timeout
        final int timeoutInMs = 60 * 1000;
        CancellationToken timeout = CancellationTokens.fromTimeout(timeoutInMs);
        String bluetoothAddress = mBleLockScanner.getLock(physicalLockId).getBluetoothAddress();

        mProgressDialog = ProgressDialog.show(this.requireActivity(),"", "");

        mBleBleLockCommunicator.executeCommandAsync(
                        bluetoothAddress,
                        physicalLockId,
                        tlcpConnection ->
                        {
                            TriggerLockCommand triggerLockCommand = null;
                            if (null != boxCommandData) {
                                triggerLockCommand = new DefaultTriggerLockCommandBuilder()
                                        .setCustomCommandData(Base64.getDecoder().decode(boxCommandData))
                                        .build();
                            }
                            else {
                                triggerLockCommand = new DefaultTriggerLockCommandBuilder().build();
                            }

                            return mCommandExecutionFacade.executeStandardCommandAsync(tlcpConnection, triggerLockCommand, timeout);
                        },
                        timeout)
                .continueOnUi(commandResult -> {
                    boolean success = false;

                    String erroMessage = "";
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
                                    // the legacy flinkey box v2.4 returns a 10 byte response
                                    // The same is true for the recent flinkey box 3.3 (aka flinkey BLE) when used without box commands
                                    if (10 == responseData.length) {
                                        BoxFeedback boxFeedback = BoxFeedback.create(responseData);
                                        int boxState = boxFeedback.getBoxState();
                                        if (BoxState.UNLOCKED == boxState) {
                                            Log.d(TAG, "Box has been opened");
                                        }
                                        else if (BoxState.LOCKED == boxState) {
                                            Log.d(TAG, "Box has been closed");
                                        }
                                        else if (BoxState.DRAWER_OPEN == boxState) {
                                            Log.d(TAG, "The drawer of the Box is open.");
                                        }
                                    }
                                    else {
                                        // When used with box commands the flinkey box 3.3 returns a response that is less or more
                                        // than 10 bytes but never exactly 10 bytes.
                                        BoxFeedbackV3 boxFeedbackV3 = BoxFeedbackV3Parser.parse(responseData);
                                        if(boxFeedbackV3.isDrawerState()) {
                                            Log.d(TAG, "The drawer of the Box is open.");
                                        }
                                        else if(boxFeedbackV3.isDrawerAccessibility()) {
                                            Log.d(TAG, "Box has been unlocked");
                                        }
                                        else {
                                            Log.d(TAG, "Box has been locked");
                                        }
                                    }
                                }
                                catch (IllegalArgumentException iaEx) {
                                    Log.e(TAG, iaEx.getMessage());
                                }
                            }
                            break;
                        }
                        case LockCommunicationError: {
                            Log.e(TAG, "A transport-level error occurred when communicating with the locking device");
                            erroMessage = "A transport-level error occurred when communicating with the locking device";
                            break;
                        }
                        case LockDateTimeInvalid: {
                            Log.e(TAG, "Lock date/time are invalid.");
                            erroMessage = "A transport-level error occurred when communicating with the locking device";
                            break;
                        }
                        case ServerCommunicationError: {
                            Log.e(TAG, "An error occurred while trying to communicate with the Tapkey Trust Service (e.g. due to bad internet connection).");
                            erroMessage = "An error occurred while trying to communicate with the Tapkey Trust Service (e.g. due to bad internet connection).";
                            break;
                        }
                        case TechnicalError: {
                            Log.e(TAG, "Some unspecific technical error has occurred.");
                            erroMessage = "Some unspecific technical error has occurred.";
                            break;
                        }
                        case Unauthorized: {
                            Log.e(TAG, "Communication with the security backend succeeded but the user is not authorized for the given command on this locking device.");
                            erroMessage = "Communication with the security backend succeeded but the user is not authorized for the given command on this locking device.";
                            break;
                        }
                        case UserSpecificError: {
                            // If there is a UserSpecificError we need to have look at the list
                            // of UserCommandResults in order to determine what exactly caused the error
                            // https://developers.tapkey.io/mobile/android/reference/Tapkey.MobileLib/latest/com/tapkey/mobile/model/CommandResult.UserCommandResult.html
                            erroMessage = "triggerLockAsync failed with UserSpecificError";
                            List<CommandResult.UserCommandResult> userCommandResults = commandResult.getUserCommandResults();
                            for (CommandResult.UserCommandResult ucr : userCommandResults) {
                                Log.e(TAG, "triggerLockAsync failed with UserSpecificError and UserCommandResultCode " + ucr.getUserCommandResultCode());
                                erroMessage = "triggerLockAsync failed with UserSpecificError and UserCommandResultCode " + ucr.getUserCommandResultCode();
                            }
                            break;
                        }
                        default: {
                            break;
                        }
                    }

                    if (success) {
                        Toast.makeText(getContext(), "executeBoxCommand successful", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Toast.makeText(getContext(), "executeBoxCommand error:" + erroMessage, Toast.LENGTH_LONG).show();
                    }

                    return success;
                })
                .finallyOnUi(() -> {
                    if (null != mProgressDialog) {
                        mProgressDialog.dismiss();
                    }
                    mProgressDialog = null;
                })
                .catchOnUi(e -> {
                    Toast.makeText(getContext(), "executeBoxCommand exception", Toast.LENGTH_LONG).show();
                    return false;
                });
    }
}
