package tw.fatminmin.xposed.minminguard.next.blocker.adnetwork;

import tw.fatminmin.xposed.minminguard.next.blocker.Blocker;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Mintegral (com.mintegral.msdk).
 *
 * Name-based blocking on the SDK root package; the public banner entry point
 * (com.mintegral.msdk.out.BannerView) is matched exactly as well.
 */
public class Mintegral extends Blocker
{
    public static final String BANNER = "com.mintegral.msdk.out.BannerView";
    public static final String BANNER_PREFIX = "com.mintegral.msdk";

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
