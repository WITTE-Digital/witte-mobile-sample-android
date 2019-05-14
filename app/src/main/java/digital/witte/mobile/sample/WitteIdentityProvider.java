package digital.witte.mobile.sample;

import net.tpky.mc.concurrent.Async;
import net.tpky.mc.concurrent.CancellationToken;
import net.tpky.mc.concurrent.Promise;
import net.tpky.mc.manager.idenitity.IdentityProvider;
import net.tpky.mc.model.Identity;
import net.tpky.mc.model.User;

import java.io.IOException;

import digital.witte.wittemobilelibrary.Configuration;
import digital.witte.wittemobilelibrary.net.IdTokenRequest;

public class WitteIdentityProvider implements IdentityProvider {

    private final Configuration _witteConfiguration;
    private int _userId;

    WitteIdentityProvider(Configuration configuration, int userId) {

        _witteConfiguration = configuration;
        _userId = userId;
    }

    @Override
    public Promise<Void> logOutAsync(User user, CancellationToken cancellationToken) {
        return null;
    }

    @Override
    public Promise<Identity> refreshToken(User user, CancellationToken cancellationToken) {
        return Async.executeAsync(() -> {

            Identity result = null;

            try {

                IdTokenRequest request = new IdTokenRequest();
                String wmaToken = request.execute(_witteConfiguration, _userId);
                result = new Identity(Configuration.IpId, wmaToken);
            }
            catch (IOException e) {

                e.printStackTrace();
            }
            catch (Exception e) {

                e.printStackTrace();
            }

            return result;
        });
    }
}
