package tw.fatminmin.xposed.minminguard.next.blocker.adnetwork;

import tw.fatminmin.xposed.minminguard.next.blocker.Blocker;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Tapjoy (com.tapjoy).
 *
 * Name-based blocking: the public display/banner view (TJAdView) is matched
 * exactly, and the whole SDK root is caught by the prefix match.
 */
public class Tapjoy extends Blocker
{
    public static final String BANNER = "com.tapjoy.TJAdView";
    public static final String BANNER_PREFIX = "com.tapjoy";

    @Override
    public String getBannerPrefix()
    {
        return BANNER_PREFIX;
    }

    @Override
    public String getBanner()
    {
        return BANNER;
    }

    @Override
    public boolean handleLoadPackage(String packageName, XC_LoadPackage.LoadPackageParam lpparam)
    {
        return false;
    }
}
