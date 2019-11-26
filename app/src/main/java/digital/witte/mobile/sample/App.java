package digital.witte.mobile.sample;

import android.app.Application;

import com.tapkey.mobile.TapkeyAppContext;
import com.tapkey.mobile.TapkeyEnvironmentConfig;
import com.tapkey.mobile.TapkeyEnvironmentConfigBuilder;
import com.tapkey.mobile.TapkeyServiceFactory;
import com.tapkey.mobile.TapkeyServiceFactoryBuilder;
import com.tapkey.mobile.broadcast.PollingScheduler;

import digital.witte.wittemobilelibrary.Configuration;

public class App extends Application implements TapkeyAppContext {

    /**
     * The TapkeyServiceFactory holds all needed services
     */
    private TapkeyServiceFactory _tapkeyServiceFactory;

    /**
     * Your WITTE Customer Id.
     * TODO: Add your WITTE Customer Id here.
     */
    private final static int CustomerId = -1;

    /**
     * Your WITTE SDK Key.
     * TODO: Add your WITTE SDK Key here.
     */
    private final static String SdkKey = "Todo: Add your WITTE sdk key here";

    /**
     * Your WITTE Subscription Key.
     * TODO: Add your WITTE Subscription Key here.
     */
    private final static String SubscriptionKey = "Todo: Add your WITTE subscription key here";

    /**
     * Customer specific configuration
     */
    final static Configuration WitteConfiguration = new Configuration(
            CustomerId,
            SdkKey,
            SubscriptionKey);

    /**
     * User Id of one specific WITTE user (this needs to be retrieved at runtime in production apps)
     * TODO: Add your WITTE User Id here.
     */
    public final static int WitteUserId = -1;

    @Override
    public void onCreate() {
        super.onCreate();

        TapkeyEnvironmentConfig tapkeyEnvironmentConfig = new TapkeyEnvironmentConfigBuilder(this)
                .setBleServiceUuid(Configuration.BleServiceUuid)
                .setTenantId(Configuration.TenantId)
                .build();

        _tapkeyServiceFactory = new TapkeyServiceFactoryBuilder(this)
                .setConfig(tapkeyEnvironmentConfig)
                .setTokenRefreshHandler(new WitteTokenRefreshHandler(new WitteTokenProvider(this, WitteConfiguration, WitteUserId)))
                .build();

        /*
         * Register PushNotificationReceiver
         *
         * The PushNotificationReceiver polls for notifications from the Tapkey backend.
         * The JobId must a unique id across the whole app
         * The default interval is 8 hours and can be changed fitting requirements of the provided service
         *
         */
        int uniqueJobId = 321;
        PollingScheduler.register(this, uniqueJobId, PollingScheduler.DEFAULT_INTERVAL);
    }

    @Override
    public TapkeyServiceFactory getTapkeyServiceFactory() {
        return _tapkeyServiceFactory;
    }
}
