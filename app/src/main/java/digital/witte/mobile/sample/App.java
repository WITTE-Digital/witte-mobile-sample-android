package digital.witte.mobile.sample;

import android.app.Application;

import net.tpky.mc.AndroidTapkeyServiceFactory;
import net.tpky.mc.TapkeyAppContext;
import net.tpky.mc.TapkeyServiceFactoryBuilder;
import net.tpky.mc.broadcast.PushNotificationReceiver;
import net.tpky.mc.time.ServerClock;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.CookieStore;

import digital.witte.wittemobilelibrary.Configuration;

public class App extends Application implements TapkeyAppContext {

    /**
     * The TapkeyServiceFactory holds all needed services
     */
    private AndroidTapkeyServiceFactory _tapkeyServiceFactory;

    /**
     * Customer specific configuration
     */
    private final static int CustomerId = Integer.parseInt(BuildConfig.WITTE_CUSTOMER_ID);
    private final static String SdkKey = BuildConfig.WITTE_SDK_KEY;
    private final static String SubscriptionKey = BuildConfig.WITTE_SUBSCRIPTION_KEY;
    final static Configuration WitteConfiguration = new Configuration(
            CustomerId,
            SdkKey,
            SubscriptionKey);

    /**
     * User Id of one specific WITTE user (this needs to be retrieved at runtime in production apps)
     */
    public final static int WitteUserId = 30;

    @Override
    public void onCreate() {
        super.onCreate();

        WitteIdentityProvider _witteWitteIdentityProvider = new WitteIdentityProvider(WitteConfiguration, WitteUserId);

        _tapkeyServiceFactory = new TapkeyServiceFactoryBuilder()
                .setBleServiceUuid(Configuration.BleServiceUuid)
                .setTenantId(Configuration.TenantId)
                .build(this);

        _tapkeyServiceFactory
                .getIdentityProviderRegistration()
                .registerIdentityProvider(Configuration.IpId, _witteWitteIdentityProvider);

        /*
         * The Tapkey libs require cookies to be persisted in order to survive app/device restarts
         * because server authentication involves authentication cookies.
         * Persistence is done using the cookie store provided by the TapkeyServiceFactory instance.
         */
        CookieStore cookieStore = _tapkeyServiceFactory.getCookieStore();
        CookieHandler.setDefault(new CookieManager(cookieStore, CookiePolicy.ACCEPT_ORIGINAL_SERVER));

        /*
         * Register PushNotificationReceiver
         *
         * The PushNotificationReceiver polls for notifications from the Tapkey backend.
         * The JobId must a unique id across the whole app
         * The default interval is 8 hours and can be changed fitting requirements of the provided service
         *
         */
        ServerClock serverClock = _tapkeyServiceFactory.getServerClock();
        PushNotificationReceiver.register(this, serverClock, 1, PushNotificationReceiver.DEFAULT_INTERVAL);
    }

    @Override
    public AndroidTapkeyServiceFactory getTapkeyServiceFactory() {

        return _tapkeyServiceFactory;
    }
}
