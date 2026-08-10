package tw.fatminmin.xposed.minminguard.next.blocker.adnetwork;

import tw.fatminmin.xposed.minminguard.next.blocker.Blocker;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Tencent GDT / 腾讯优量汇 (com.qq.e).
 *
 * Name-based blocking on the SDK root package; the unified banner view entry
 * point (com.qq.e.ads.banner2.UnifiedBannerView) is matched exactly as well.
 */
public class TencentGDT extends Blocker
{
    public static final String BANNER = "com.qq.e.ads.banner2.UnifiedBannerView";
    public static final String BANNER_PREFIX = "com.qq.e";

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
