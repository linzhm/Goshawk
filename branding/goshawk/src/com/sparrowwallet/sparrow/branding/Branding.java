package com.sparrowwallet.sparrow.branding;

/** Product identity only; upstream packages and wallet formats remain stable. */
public final class Branding {
    public static final String ID = "goshawk";
    public static final String IMAGE_PREFIX = "/image/" + ID;
    public static final String NAME = "Goshawk";
    public static final String VERSION = "0.1.0";
    public static final String VERSION_SUFFIX = "-dev";
    public static final String UPSTREAM_VERSION = "2.5.7";
    public static final boolean UPDATES_ENABLED = false;
    public static final String REPOSITORY = "https://github.com/linzhm/Goshawk";
    public static final String DOCUMENTATION = REPOSITORY + "#readme";
    public static final String ISSUES = REPOSITORY + "/issues";
    private Branding() {}
}
