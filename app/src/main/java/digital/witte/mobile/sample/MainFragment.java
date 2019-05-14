package digital.witte.mobile.sample;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
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

import net.tpky.mc.AndroidTapkeyServiceFactory;
import net.tpky.mc.concurrent.CancellationTokens;
import net.tpky.mc.manager.BleLockManager;
import net.tpky.mc.manager.CommandExecutionFacade;
import net.tpky.mc.manager.KeyManager;
import net.tpky.mc.manager.NotificationManager;
import net.tpky.mc.manager.UserManager;
import net.tpky.mc.model.CommandResult;
import net.tpky.mc.model.Identity;
import net.tpky.mc.model.User;
import net.tpky.mc.model.webview.CachedKeyInformation;
import net.tpky.mc.utils.Func1;
import net.tpky.mc.utils.ObserverRegistration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import digital.witte.wittemobilelibrary.Configuration;
import digital.witte.wittemobilelibrary.box.BoxFeedback;
import digital.witte.wittemobilelibrary.box.BoxIdConverter;
import digital.witte.wittemobilelibrary.box.BoxState;
import digital.witte.wittemobilelibrary.net.IdTokenRequest;

public class MainFragment extends Fragment {

    private static final String TAG = MainFragment.class.getCanonicalName();

    private static final int PERMISSIONS_REQUEST__ACCESS_COARSE_LOCATION = 0;

    private TextView _tvCustomerId;
    private TextView _tvSubscriptionKey;
    private TextView _tvSdkKey;
    private TextView _tvUserId;
    private EditText _tvBoxId;
    private Button _btnTriggerLock;

    private BleLockManager _bleLockManager;
    private KeyManager _keyManager;
    private CommandExecutionFacade _commandExecutionFacade;
    private UserManager _userManager;
    private NotificationManager _notificationManager;
    private ObserverRegistration _keyUpdateObserverRegistration;
    private ArrayList<CachedKeyInformation> _keys = new ArrayList<>();

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

        Button _btnAuthenticate = view.findViewById(R.id.main_frag_btn_authenticate);
        _btnAuthenticate.setOnClickListener(button -> authenticate());

        _tvBoxId = view.findViewById(R.id.main_frag_et_box_id);
        _tvBoxId.setHint("e.g. C1-1F-8E-7C");
        _btnTriggerLock = view.findViewById(R.id.main_frag_btn_trigger);
        _btnTriggerLock.setOnClickListener(button -> triggerLock());

        _tvBoxId.setEnabled(false);
        _btnTriggerLock.setEnabled(false);

        return view;
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        AndroidTapkeyServiceFactory serviceFactory = ((App) getActivity().getApplication()).getTapkeyServiceFactory();
        _bleLockManager = serviceFactory.getBleLockManager();
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
            }
            else {
                requestPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSIONS_REQUEST__ACCESS_COARSE_LOCATION);
            }
        }
        else {
            if (null != _bleLockManager) {
                _bleLockManager.startForegroundScan();
            }
        }

        // Listen for changes in the available keys. Changes might happen, e.g. due to push
        // notifications received from the Tapkey backend.
        if (_keyUpdateObserverRegistration == null) {
            _keyUpdateObserverRegistration = _keyManager
                    .getKeyUpdateObservable()
                    .addObserver(aVoid -> onKeyUpdate(false));
        }

        onKeyUpdate(true);
    }

    @Override
    public void onPause() {
        super.onPause();

        if (null != _bleLockManager) {
            _bleLockManager.stopForegroundScan();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void onKeyUpdate(boolean forceUpdate) {

        // We only support a single user today, so the first user is the only user.
        User firstUser = _userManager.getFirstUser();
        if (null == firstUser) {
            return;
        }

        // query for this user's keys asynchronously
        _keyManager.queryLocalKeysAsync(firstUser, forceUpdate, CancellationTokens.None)
                // when completed with success, continue on the UI thread
                .continueOnUi((Func1<List<CachedKeyInformation>, Void, Exception>) cachedKeyInformations -> {
                    _keys.clear();
                    _keys.addAll(cachedKeyInformations);

                    return null;
                })
                // handle async exceptions
                .catchOnUi(e -> {
                    //Log.e(TAG, "query local keys failed ", e);
                    // Handle error
                    return null;
                })
                // make sure, we don't miss any exceptions.
                .conclude();
    }

    private void authenticate() {
        User user = _userManager.getFirstUser();
        if(null != user){
            _userManager.logOff(user, CancellationTokens.None);
        }

        Configuration witteConfiguration = App.WitteConfiguration;
        AuthenticateAsyncTask _authenticationTask = new AuthenticateAsyncTask(
                getActivity(),
                witteConfiguration,
                _userManager,
                _notificationManager);

        _authenticationTask.execute(App.WitteUserId);
    }

    private void triggerLock() {
        String boxId = _tvBoxId.getText().toString();
        if ("".equals(boxId)) {
            Toast.makeText(getContext(), "Please enter your box ID.", Toast.LENGTH_SHORT).show();
            return;
        }

        String physicalLockId;
        try{
             physicalLockId = BoxIdConverter.toPhysicalLockId(boxId);
            if (!_bleLockManager.isLockNearby(physicalLockId)) {
                Toast.makeText(getContext(), "The box is not in reach.", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        catch (Exception exception){
            Toast.makeText(getContext(), exception.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        _bleLockManager
                .executeCommandAsync(
                        new String[0],
                        physicalLockId,
                        tlcpConnection -> _commandExecutionFacade.triggerLockAsync(tlcpConnection, CancellationTokens.None),
                        CancellationTokens.None)
                .continueOnUi(commandResult -> {
                    if (commandResult.getCommandResultCode() == CommandResult.CommandResultCode.Ok) {
                        Toast.makeText(getContext(), "triggerLock successful", Toast.LENGTH_SHORT).show();

                        // get the 10 byte box feedback from the command result
                        Object object = commandResult.getResponseData();
                        if (object instanceof byte[]) {

                            byte[] responseData = (byte[]) object;
                            try {
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
                            catch (IllegalArgumentException iaEx) {

                                Log.e(TAG, iaEx.getMessage());
                            }
                        }
                        return true;
                    }
                    else {
                        Toast.makeText(getContext(), "triggerLock error", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                })
                .catchOnUi(exception -> {
                    Toast.makeText(getContext(), "triggerLock exception", Toast.LENGTH_SHORT).show();
                    return false;
                });
    }

    @SuppressLint("StaticFieldLeak")
    public class AuthenticateAsyncTask extends AsyncTask<Integer, Void, String> {
        private final String TAG = AuthenticateAsyncTask.class.getCanonicalName();
        private Context _context;
        private Configuration _witteConfiguration;
        private UserManager _userManager;
        private ProgressDialog _progressDialog;

        AuthenticateAsyncTask(
                Context context,
                Configuration configuration,
                UserManager userManager,
                NotificationManager notificationManager) {

            _context = context;
            _witteConfiguration = configuration;
            _userManager = userManager;
            _notificationManager = notificationManager;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            try {
                _progressDialog = ProgressDialog.show(
                        _context,
                        "",
                        "Authenticating...",
                        true);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        protected String doInBackground(Integer... integers) {
            String idToken = null;

            try {
                int witteUserId = integers[0];
                IdTokenRequest request = new IdTokenRequest();
                idToken = request.execute(_witteConfiguration, witteUserId);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            return idToken;
        }

        @Override
        protected void onPostExecute(String idToken) {
            if (null != idToken && !"".equals(idToken)) {
                Identity identity = new Identity(Configuration.IpId, idToken);
                _userManager.authenticateAsync(identity, CancellationTokens.None)
                        .continueOnUi(user -> {

                             // successfully authenticated user with Tapkey backend
                             // actively poll for notifications, so we don't have to wait for push
                             // notifications being delivered.
                            _notificationManager.pollForNotificationsAsync()
                                    .catchOnUi(e -> {
                                        Log.e(TAG, "Failed to poll for notifications.", e);
                                        return null;
                                    }).conclude();

                            _bleLockManager.startForegroundScan();
                            _tvBoxId.setEnabled(true);
                            _btnTriggerLock.setEnabled(true);

                            return null;
                        })
                        .catchOnUi(e -> {
                            // authentication failed
                            return null;
                        })
                        .finallyOnUi(() -> {
                            _progressDialog.dismiss();
                        })
                        .conclude();
            }
            else {
                Log.e(TAG, "Failed to retrieve wma token from WITTE backend");
                if (null != _progressDialog) {
                    _progressDialog.dismiss();
                    _progressDialog = null;
                }
            }
        }
    }
}
