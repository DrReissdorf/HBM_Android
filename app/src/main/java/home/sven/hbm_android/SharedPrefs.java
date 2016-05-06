package home.sven.hbm_android;

public class SharedPrefs {
    public static final String SHARED_PREFS_KEY = "HBM_ANDROID";
    public static final String LUX_ACTIVATION_LIMIT_STRING = "lux_activation_limit";
    public static final String LUX_DEACTIVATION_LIMIT_STRING = "lux_deactivation_limit";
    public static final String AUTOMATIC_HBM_STRING = "auto_hbm";
    public static final String SERVICE_AUTO_BOOT = "service_auto_boot";
    public static final String SCREEN_ACTIVATED = "screen_activated";

    /* DEFAULTS */
    public static final int DEFAULT_ACTIVATION_LIMIT = 2000; //standard value if no sharedpref is found
    public static final int DEFAULT_DEACTIVATION_LIMIT = 1200; //standard value if no sharedpref is found
}
