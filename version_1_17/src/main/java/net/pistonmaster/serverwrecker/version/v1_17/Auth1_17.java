package net.pistonmaster.serverwrecker.version.v1_17;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.auth.service.AuthenticationService;
import com.github.steveice10.mc.auth.service.MojangAuthenticationService;
import net.pistonmaster.serverwrecker.common.GameVersion;
import net.pistonmaster.serverwrecker.common.IAuth;
import net.pistonmaster.serverwrecker.common.IPacketWrapper;
import net.pistonmaster.serverwrecker.common.ServiceServer;
import net.pistonmaster.serverwrecker.version.v1_17.ProtocolWrapper;

import java.net.Proxy;

public class Auth1_17 implements IAuth {
    public IPacketWrapper authenticate(GameVersion gameVersion, String username, String password, Proxy proxy, ServiceServer serviceServer) throws Exception {
        MojangAuthenticationService authService = new MojangAuthenticationService();

        authService.setBaseUri(serviceServer.getAuth());

        authService.setUsername(username);
        authService.setPassword(password);
        authService.setProxy(proxy);

        authService.login();

        GameProfile profile = authService.getSelectedProfile();
        String accessToken = authService.getAccessToken();

        return new ProtocolWrapper(profile, accessToken);
    }
}
