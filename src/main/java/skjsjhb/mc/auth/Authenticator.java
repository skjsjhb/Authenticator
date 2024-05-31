package skjsjhb.mc.auth;

import me.friwi.jcefmaven.CefAppBuilder;
import me.friwi.jcefmaven.CefInitializationException;
import me.friwi.jcefmaven.MavenCefAppHandlerAdapter;
import me.friwi.jcefmaven.UnsupportedPlatformException;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.CefSettings;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.handler.CefLoadHandlerAdapter;
import org.cef.network.CefRequest;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class Authenticator {
    // Load the login page
    private static final String LOGIN_URL =
            "https://login.live.com/oauth20_authorize.srf" +
                    "?client_id=00000000402b5328&response_type=code" +
                    "&scope=service%3A%3Auser.auth.xboxlive.com%3A%3AMBI_SSL" +
                    "&redirect_uri=https%3A%2F%2Flogin.live.com%2Foauth20_desktop.srf";

    private static final String MAVEN_MIRROR_CN = "https://maven.aliyun.com/repository/central/me/friwi/jcef-natives-{platform}/{tag}/jcef-natives-{platform}-{tag}.jar";

    // After login, the browser redirects to a URL prefixed by the following
    private static final String LOGIN_REDIRECT_URL = "https://login.live.com/oauth20_desktop.srf";

    public static void main(String[] args) throws UnsupportedPlatformException, CefInitializationException, IOException, InterruptedException, InvocationTargetException {
        // A cache ID can be specified to distinguish between different Microsoft accounts
        String cacheId = "default";

        // This is required for some districts, passed by the launcher
        boolean useMavenCNMirror = false;

        if (args.length >= 1) {
            cacheId = args[0];
        }

        if (args.length >= 2) {
            useMavenCNMirror = args[1].equals("mirror");
        }

        CefAppBuilder builder = new CefAppBuilder();
        builder.setInstallDir(getCefBundlePath().toFile());

        CefSettings settings = builder.getCefSettings();
        settings.cache_path = getCefCachePath(cacheId).toString();
        settings.windowless_rendering_enabled = false; // Required or it complains
        settings.locale = Locale.getDefault().toLanguageTag();

        builder.setAppHandler(new MavenCefAppHandlerAdapter() {
            @Override
            public void stateHasChanged(CefApp.CefAppState state) {
                // Exit the app when the window is closed by the user
                if (state == CefApp.CefAppState.TERMINATED) {
                    System.exit(0);
                }
            }
        });

        if (useMavenCNMirror) {
            // ...Why this is still happening?
            builder.setMirrors(List.of(MAVEN_MIRROR_CN));
        }


        CefApp app = builder.build();

        CefClient client = app.createClient();
        CefMessageRouter router = CefMessageRouter.create();
        client.addMessageRouter(router);

        CefBrowser browser = client.createBrowser(LOGIN_URL, false, false);

        AtomicReference<JFrame> mainFrame = new AtomicReference<>(null);

        // A latch to "race" between window close event and browser redirection
        // The main thread exists when any gets fulfilled
        CountDownLatch latch = new CountDownLatch(1);

        client.addLoadHandler(new CefLoadHandlerAdapter() {
            @Override
            public void onLoadStart(CefBrowser browser, CefFrame frame, CefRequest.TransitionType transitionType) {
                // Extract and print the code
                if (browser.getURL().toLowerCase(Locale.ROOT).startsWith(LOGIN_REDIRECT_URL)) {
                    System.out.println("==== DO NOT SHARE THE CODE BELOW ====");
                    System.out.println("Code=" + extractCode(browser.getURL()));
                    System.out.println("==== DO NOT SHARE THE CODE ABOVE ====");
                    latch.countDown();
                }
            }
        });

        SwingUtilities.invokeAndWait(() -> {
            // Create main frame
            JFrame f = new JFrame("Login");
            f.add(browser.getUIComponent());
            f.pack();

            // Sets an appropriate size
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            f.setSize((int) (screenSize.width * 0.6), (int) (screenSize.height * 0.6));

            f.setLocationRelativeTo(null);

            f.setVisible(true);
            f.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

            // Listen for window closing
            f.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    latch.countDown();
                }
            });
            mainFrame.set(f);
        });

        // Blocks until either the code is retrieved or the window is closed manually
        latch.await();

        // Cleanup
        SwingUtilities.invokeAndWait(() -> {
            mainFrame.get().setVisible(false); // Required or the display will freeze
            mainFrame.get().dispose();
        });

        app.dispose();
    }

    private static String extractCode(String url) {
        try {
            String[] queries = new URI(url).toURL().getQuery().split("&");
            return Arrays.stream(queries)
                    .map(it -> it.split("="))
                    .filter(it -> it.length == 2)
                    .filter(it -> it[0].toLowerCase(Locale.ROOT).equals("code"))
                    .findFirst().orElseThrow()[1];
        } catch (URISyntaxException | MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private static Path getCefBundlePath() {
        return getStoragePath().resolve("bundle");
    }

    private static Path getCefCachePath(String cacheId) {
        return getStoragePath().resolve("session-" + cacheId);
    }

    private static Path getStoragePath() {
        String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (osName.contains("darwin") || osName.contains("mac")) {
            return Path.of(System.getProperty("os.home"), "Library", "Application Support", "Alicorn-R-Authenticator");
        }
        if (osName.contains("windows")) {
            return Path.of(System.getenv("LOCALAPPDATA"), "Alicorn-R-Authenticator");
        }
        return Path.of(System.getProperty("os.home"), ".alicorn-r-authenticator");
    }
}
