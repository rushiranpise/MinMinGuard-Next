package tw.fatminmin.xposed.minminguard.next.blocker.adnetwork;

import tw.fatminmin.xposed.minminguard.next.blocker.Blocker;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Pangle (ByteDance / TikTok Ads, com.bytedance.sdk.openadsdk).
 *
 * Name-based blocking only: every view whose class lives under the SDK root
 * package is treated as an ad. The SDK obfuscates many of its internal view
 * classes, so the root-prefix match is the reliable part; the exact banner
 * view class names change between SDK versions and are intentionally left out.
 */
public class Pangle extends Blocker
{
    public static final String BANNER_PREFIX = "com.bytedance.sdk.openadsdk";

    @Override
    public String getBannerPrefix()
    {
        return BANNER_PREFIX;
    }

    @Override
    public String getBanner()
    {
        return null;
    }

    @Override
    public boolean handleLoadPackage(String packageName, XC_LoadPackage.LoadPackageParam lpparam)
    {
        return false;
    }
}
