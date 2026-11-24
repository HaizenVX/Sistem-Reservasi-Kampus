import java.net.URI;

/** Konfigurasi transport; tidak mengubah controller atau model. */
public final class DeploymentConfig {
    static final String ORIGIN = System.getenv().getOrDefault("PUBLIC_ORIGIN", "");
    static final String BIND = System.getenv().getOrDefault("BIND_ADDRESS", "127.0.0.1");
    static {
        if (!ORIGIN.isEmpty()) {
            URI uri = URI.create(ORIGIN);
            if (!"https".equals(uri.getScheme()) || uri.getHost() == null
                    || uri.getRawUserInfo() != null || uri.getRawQuery() != null
                    || uri.getRawFragment() != null || !uri.getRawPath().isEmpty())
                throw new IllegalArgumentException("PUBLIC_ORIGIN harus https://domain tanpa path atau slash akhir");
        } else if (!BIND.equals("127.0.0.1")) {
            throw new IllegalArgumentException("Bind publik membutuhkan PUBLIC_ORIGIN HTTPS");
        }
    }
    static boolean hostAllowed(String host) {
        if (host == null) return false;
        return ORIGIN.isEmpty() ? host.matches("(localhost|127\\.0\\.0\\.1)(:[0-9]+)?")
                : URI.create(ORIGIN).getRawAuthority().equalsIgnoreCase(host);
    }
    static boolean originAllowed(String origin, String host) {
        return origin == null || origin.equals(ORIGIN.isEmpty() ? "http://" + host : ORIGIN);
    }
    static String cookieSecurity() { return ORIGIN.isEmpty() ? "" : "; Secure"; }
}
