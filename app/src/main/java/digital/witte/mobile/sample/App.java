package digital.witte.mobile.sample;

import android.app.Application;
import android.util.Log;

import com.tapkey.mobile.TapkeyAppContext;
import com.tapkey.mobile.TapkeyEnvironmentConfig;
import com.tapkey.mobile.TapkeyEnvironmentConfigBuilder;
import com.tapkey.mobile.TapkeyServiceFactory;
import com.tapkey.mobile.TapkeyServiceFactoryBuilder;
import com.tapkey.mobile.auth.TokenRefreshHandler;
import com.tapkey.mobile.ble.TapkeyBleAdvertisingFormat;
import com.tapkey.mobile.ble.TapkeyBleAdvertisingFormatBuilder;
import com.tapkey.mobile.broadcast.PollingScheduler;
import com.tapkey.mobile.concurrent.CancellationToken;
import com.tapkey.mobile.concurrent.Promise;

import digital.witte.mobile.sample.backend.DemoBackendAccessor;
import digital.witte.wittemobilelibrary.Configuration;
import digital.witte.wittemobilelibrary.box.BoxIdConverter;

public class App extends Application implements TapkeyAppContext {
    /**
     * The TapkeyServiceFactory holds all needed services of the Tapkey Mobile Library
     */
    private TapkeyServiceFactory _tapkeyServiceFactory;

    /**
     * The access token provider for login to the Tapkey Mobile SDK and token refresh
     */
    private TokenProvider _tokenProvider;

    @Override
    public void onCreate() {
        super.onCreate();

        TapkeyEnvironmentConfig tapkeyEnvironmentConfig = new TapkeyEnvironmentConfigBuilder(this)
                .setTenantId(Configuration.TenantId)
                .build();

        TapkeyBleAdvertisingFormat bleAdvertisingFormat = new TapkeyBleAdvertisingFormatBuilder()
                .addV1Format(Configuration.BleAdvertisingFormatV1)
                .addV2Format(Configuration.BleAdvertisingFormatV2)
                .build();

        _tokenProvider = new TokenProvider(this, new DemoBackendAccessor());

        _tapkeyServiceFactory = new TapkeyServiceFactoryBuilder(this)
                .setConfig(tapkeyEnvironmentConfig)
                .setBluetoothAdvertisingFormat(bleAdvertisingFormat)
                .setTokenRefreshHandler(new TokenRefreshHandler() {
                    @Override
                    public Promise<String> refreshAuthenticationAsync(String s, CancellationToken cancellationToken) {
                        return _tokenProvider.AccessToken();
                    }

                    @Override
                    public void onRefreshFailed(String tapkeyUserId) {
                        //"https://developers.tapkey.io/mobile/android/reference/Tapkey.MobileLib/latest/com/tapkey/mobile/auth/TokenRefreshHandler.html#onRefreshFailed-java.lang.String-"
                        Log.e("", "Reauthentication of an access token has failed in a non-recoverable way.");
                    }
                })
                .build();

        // The polling scheduler will take care of regular synchronization and refresh of digital keys
        // https://developers.tapkey.io/mobile/android/getting_started/#polling-for-data
        int uniqueJobId = 321;
        PollingScheduler.register(this, uniqueJobId, PollingScheduler.DEFAULT_INTERVAL);
    }

    @Override
    public TapkeyServiceFactory getTapkeyServiceFactory() {
        return _tapkeyServiceFactory;
    }

    public TokenProvider getTokenProvider() {
        return _tokenProvider;
    }
}
