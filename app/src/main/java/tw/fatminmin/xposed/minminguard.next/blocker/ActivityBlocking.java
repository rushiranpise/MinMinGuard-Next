package tw.fatminmin.xposed.minminguard.next.blocker;

import android.app.Activity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import tw.fatminmin.xposed.minminguard.next.Common;

/**
 * Kills full-screen interstitial / rewarded / app-open ad activities the moment
 * they are created, so the user never sees them.
 *
 * Ad SDKs register dedicated Activity classes (declared in their manifest) for
 * full-screen formats. This hooks {@code Activity.onCreate} and finishes the
 * activity when its class name matches a known list or one of the user's custom
 * class prefixes.
 *
 * The built-in list intentionally contains only well-documented, stable class
 * names; everything else can be added by the user via Settings -> Custom ad
 * class prefixes.
 */
public final class ActivityBlocking
{
    /* Well-known full-screen ad activities. Prefix-matching: an entry is also
       a match if the activity class starts with it (e.g. the rewarded variants). */
    private static final List<String> KNOWN_ACTIVITY_PREFIXES = new ArrayList<>(Arrays.asList(
            /* AdMob / Google Mobile Ads - interstitial, rewarded, app open */
            "com.google.android.gms.ads.AdActivity",
            /* Pangle / ByteDance */
            "com.bytedance.sdk.openadsdk.activity.TTInterstitialActivity",
            "com.bytedance.sdk.openadsdk.activity.TTFullScreenVideoActivity",
            "com.bytedance.sdk.openadsdk.activity.TTRewardVideoActivity",
            "com.bytedance.sdk.openadsdk.activity.TTAppOpenAdActivity",
            /* Mintegral */
            "com.mintegral.msdk.activity.MTGInterstitialActivity",
            "com.mintegral.msdk.activity.MTGRewardActivity",
            /* Tapjoy */
            "com.tapjoy.TJAdUnitActivity",
            "com.tapjoy.TJContentActivity",
            /* Tencent GDT - full screen video / rewarded */
            "com.qq.e.ads.ADActivity",
            "com.qq.e.ads.rewardvideo.RewardVideoADActivity",
            /* Unity Ads */
            "com.unity3d.services.ads.adunit.AdUnitActivity"
    ));

    private ActivityBlocking() throws InstantiationException
    {
        throw new InstantiationException("This class is not for instantiation");
    }

    public static void blockInterstitialActivities(final String packageName,
                                                   final XC_LoadPackage.LoadPackageParam lpparam)
    {
        try
        {
            XposedHelpers.findAndHookMethod("android.app.Activity", lpparam.classLoader, "onCreate",
                    android.os.Bundle.class, new XC_MethodHook()
                    {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param)
                        {
                            Activity activity = (Activity) param.thisObject;
                            String clazz = activity.getClass().getName();

                            if (matches(clazz, packageName, lpparam))
                            {
                                Util.log(packageName, "Finishing ad activity: " + clazz);
                                Util.notifyRemoveAdView(null, packageName, 1);
                                activity.finish();
                            }
                        }
                    });
        }
        catch (Throwable e)
        {
            Util.log(packageName, "ActivityBlocking: " + e);
        }
    }

    private static boolean matches(String clazz, String packageName, XC_LoadPackage.LoadPackageParam lpparam)
    {
        for (String prefix : KNOWN_ACTIVITY_PREFIXES)
        {
            if (clazz.startsWith(prefix))
            {
                return true;
            }
        }

        /* User-defined prefixes from Settings -> Custom ad class prefixes. */
        try
        {
            de.robv.android.xposed.XSharedPreferences pref =
                    new de.robv.android.xposed.XSharedPreferences(
                            tw.fatminmin.xposed.minminguard.next.Main.class.getPackage().getName(),
                            Common.MOD_PREFS);
            if (pref.getFile().canRead())
            {
                String user = pref.getString(Common.KEY_USER_BLOCK_PREFIXES, "");
                if (user != null)
                {
                    for (String line : user.split("\n"))
                    {
                        String prefix = line.trim();
                        if (prefix.length() > 0 && clazz.startsWith(prefix))
                        {
                            return true;
                        }
                    }
                }
            }
        }
        catch (Throwable ignored)
        {
        }

        return false;
    }
}
