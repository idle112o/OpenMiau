package myau;

public final class ClientInfo {
    public static final String NAME = "OpenMiau";
    public static final String VERSION = "${version}";
    public static final String MC_VERSION = "${mcversion}";
    public static final String GIT_COMMIT = "${gitCommit}";
    public static final boolean GITHUB_BUILD = Boolean.parseBoolean("${githubBuild}");

    private ClientInfo() {
    }

    public static String getBuildChannel() {
        return GITHUB_BUILD ? "dev" : "main";
    }

    public static String getDisplayVersion() {
        String suffix = GITHUB_BUILD && GIT_COMMIT != null && !GIT_COMMIT.isEmpty() && !"unknown".equalsIgnoreCase(GIT_COMMIT)
                ? " +" + GIT_COMMIT
                : "";
        return NAME + " (" + getBuildChannel() + suffix + ") " + VERSION + " | MC " + MC_VERSION;
    }
}
