package tw.fatminmin.xposed.minminguard;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.zip.ZipFile;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import tw.fatminmin.xposed.minminguard.blocker.ApiBlocking;
import tw.fatminmin.xposed.minminguard.blocker.Blocker;
import tw.fatminmin.xposed.minminguard.blocker.NameBlocking;
import tw.fatminmin.xposed.minminguard.blocker.UrlFiltering;
import tw.fatminmin.xposed.minminguard.blocker.Util;
import tw.fatminmin.xposed.minminguard.blocker.adnetwork.Ad2iction;
import tw.fatminmin.xposed.minminguard.blocker.adnetwork.AdMarvel;
import tw.fatminmin.xposed.minminguard.blocker.adnetwork.Adbert;
import tw.fatminmin.xposed.minminguard.blocker.adnetwork.Adcolony;
import tw.fatminmin.xposed.minminguard.blocker.adnetwork.Adfurikun;
import tw.fatminmin.xposed.minminguard.blocker.adnetwork.Adtech;
import tw.fatminmin.xposed.minminguard.blocker.adnetwork.Amazon;
import tw.fatminmin.xposed.minminguard.blocker.adnetwork.Amobee;
import tw.fatminmin.xposed.minminguard.blocker.adnetwork.Aotter;
import tw.fatminmin.xposed.minminguard.blocker.adnetwork.AppBrain;
import tw.fatminmin.xposed.minminguard.blocker.adnetwork.Applovin;
import tw.fatminmin.xposed.minminguard.blocker.adnetwork.Appnext;
import tw.fatminmin.xposed.minminguard.blocker.adnetwork.AppodealMRAID;
import tw.fatminmin.xposed.minminguard.blocker.adnetwork.Avocarrot;
import tw.fatminmin.xposed.minminguard.blocker.adnetwork.Bonzai;
import tw.fatminmin.xposed.minminguard.blocker.adnetwork.Chartboost;
import tw.fatminmin.xposed.minminguard.blocker.adnetwork.Clickforce;
import tw.fatminmin.xposed.minminguard.blocker.adnetwork.Domob;
import tw.fatminmin.xposed.minminguard.blocker.adnetwork.Facebook;
import tw.fatminmin.xposed.minminguard.blocker.adnetwork.Flurry;
import tw.fatminmin.xposed.minminguard.blocker.adnetwork.Freewheel;
import tw.fatminmin.xposed.minminguard.blocker.adnetwork.GoogleAdmob;
import tw.fatminmin.xposed.minminguard.blocker.adnetwork.GoogleGms;
import tw.fatminmin.xposed.minminguard.blocker.adnetwork.GoogleGmsDoubleClick;
import tw.fatminmin.xposed.minminguard.blocker.adnetwork.Hodo;
import tw.fatminmin.xposed.minminguard.blocker.adnetwork.Inmobi;
import tw.fatminmin.xposed.minminguard.blocker.adnetwork.Intowow;
import tw.fatminmin.xposed.minminguard.blocker.adnetwork.Ironsource;
import tw.fatminmin.xposed.minminguard.blocker.adnetwork.KuAd;
import tw.fatminmin.xposed.minminguard.blocker.adnetwork.Madvertise;
import tw.fatminmin.xposed.minminguard.blocker.adnetwork.MasAd;
import tw.fatminmin.xposed.minminguard.blocker.adnetwork.MdotM;
import tw.fatminmin.xposed.minminguard.blocker.adnetwork.Millennial;
import tw.fatminmin.xposed.minminguard.blocker.adnetwork.MoPub;
import tw.fatminmin.xposed.minminguard.blocker.adnetwork.MobFox;
import tw.fatminmin.xposed.minminguard.blocker.adnetwork.Mobclix;
import tw.fatminmin.xposed.minminguard.blocker.adnetwork.Nend;
import tw.fatminmin.xposed.minminguard.blocker.adnetwork.Og;
import tw.fatminmin.xposed.minminguard.blocker.adnetwork.Onelouder;
import tw.fatminmin.xposed.minminguard.blocker.adnetwork.OpenX;
import tw.fatminmin.xposed.minminguard.blocker.adnetwork.SmartAdserver;
import tw.fatminmin.xposed.minminguard.blocker.adnetwork.SourcekitMRAID;
import tw.fatminmin.xposed.minminguard.blocker.adnetwork.Startapp;
import tw.fatminmin.xposed.minminguard.blocker.adnetwork.TWMads;
import tw.fatminmin.xposed.minminguard.blocker.adnetwork.Tapfortap;
import tw.fatminmin.xposed.minminguard.blocker.adnetwork.UnityAds;
import tw.fatminmin.xposed.minminguard.blocker.adnetwork.Vpadn;
import tw.fatminmin.xposed.minminguard.blocker.adnetwork.Vpon;
import tw.fatminmin.xposed.minminguard.blocker.adnetwork.Vungle;
import tw.fatminmin.xposed.minminguard.blocker.adnetwork.Waystorm;
import tw.fatminmin.xposed.minminguard.blocker.adnetwork.Yandex;
import tw.fatminmin.xposed.minminguard.blocker.adnetwork.mAdserve;
import tw.fatminmin.xposed.minminguard.blocker.custom_mod.NextMedia;
import tw.fatminmin.xposed.minminguard.blocker.custom_mod.OneWeather;
import tw.fatminmin.xposed.minminguard.blocker.custom_mod.Viafree;
import tw.fatminmin.xposed.minminguard.blocker.custom_mod._2chMate;

public class Main implements IXposedHookZygoteInit, IXposedHookLoadPackage
{
    private static final String MY_PACKAGE_NAME = Main.class.getPackage().getName();
    private static final String QUERY_ALL_PACKAGES = "android.permission.QUERY_ALL_PACKAGES";

    private static String MODULE_PATH;
    public static Set<String> patterns = new HashSet<>();
    public static Integer xposedVersionCode;
    private static SharedPreferences pref;

    public static Blocker[] blockers = {
            /* Popular adnetwork */
            new Ad2iction(), new Adbert(), new Adcolony(), new Adfurikun(), new AdMarvel(), new AppodealMRAID(), new GoogleAdmob(), new GoogleGms(), new Adtech(), new Amazon(), new Amobee(),
            new Aotter(), new AppBrain(), new Applovin(), new Appnext(), new Avocarrot(), new Bonzai(), new Chartboost(), new Clickforce(), new Domob(), new Facebook(),
            new Freewheel(), new Flurry(), new GoogleGmsDoubleClick(), new Hodo(), new Inmobi(), new Intowow(), new Ironsource(), new KuAd(), new mAdserve(), new Madvertise(),
            new MasAd(), new MdotM(), new Millennial(), new Mobclix(), new MobFox(), new MoPub(), new Nend(), new Og(), new Onelouder(), new OpenX(), new SmartAdserver(), new SourcekitMRAID(),
            new Startapp(), new Tapfortap(), new TWMads(), new UnityAds(), new Vpadn(), new Vpon(), new Vungle(), new Waystorm(), new Yandex(),
            /* Custom Mod*/
    };

    private static boolean isEnabled(SharedPreferences pref, String pkgName)
    {
        String mode = pref.getString(Common.KEY_MODE, Common.VALUE_MODE_BLACKLIST);

        if (mode.equals(Common.VALUE_MODE_AUTO))
        {
            return true;
        }
        else if (mode.equals(Common.VALUE_MODE_BLACKLIST))
        {
            return pref.getBoolean(pkgName, false);
        }
        else
        {
            return !pref.getBoolean(Common.getWhiteListKey(pkgName), false);
        }
    }

    private static void appSpecific(String packageName, LoadPackageParam lpparam)
    {
        _2chMate.handleLoadPackage(packageName, lpparam);
        OneWeather.handleLoadPackage(packageName, lpparam);
        NextMedia.handleLoadPackage(packageName, lpparam);
        Viafree.handleLoadPackage(packageName, lpparam);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void initZygote(StartupParam startupParam) throws Throwable
    {
        try
        {
            xposedVersionCode = XposedBridge.getXposedVersion();
        }
        catch (Exception e)
        {
            xposedVersionCode = XposedBridge.XPOSED_BRIDGE_VERSION;
        }

        MODULE_PATH = startupParam.modulePath;

        /* Load the host / url-filtering pattern lists from our own APK.
           The original code used XModuleResources which only works on legacy
           Xposed (< API 90); on LSPosed / Vector the pattern lists were never
           loaded, silently disabling URL filtering. Reading the assets straight
           from the module APK works on every framework. */
        UnpackResources();

        Util.notifyWorker = Executors.newSingleThreadExecutor();

        if (xposedVersionCode < 93) {
            /* This doesn't work in LSPosed API >=93 because prefs are stored in a random directory
               https://github.com/LSPosed/LSPosed/wiki/New-XSharedPreferences#for-the-hooked-app */
            String dataDir = "data/";
            if (Build.VERSION.SDK_INT > 23) dataDir = "user_de/0/";

            File f = new File("/data/" + dataDir + MY_PACKAGE_NAME + "/shared_prefs/" + Common.MOD_PREFS + ".xml");
            pref = new XSharedPreferences(f);
            Util.log(MY_PACKAGE_NAME, "Using LEGACY XSharedPreferences");
        } else {
            pref = new XSharedPreferences(MY_PACKAGE_NAME, Common.MOD_PREFS);
            Util.log(MY_PACKAGE_NAME, "Using NEW XSharedPreferences");
        }
    }

    private void UnpackResources()
    {
        try
        {
            addPatternsFromAsset("host/output_file");
            addPatternsFromAsset("host/mmg_pattern");
        }
        catch (Exception e)
        {
            Util.log(MY_PACKAGE_NAME, "Failed to load pattern lists: " + e);
        }
    }

    private void addPatternsFromAsset(String assetPath)
    {
        try (ZipFile zip = new ZipFile(new File(MODULE_PATH)))
        {
            InputStream in = zip.getInputStream(zip.getEntry("assets/" + assetPath));
            if (in != null)
            {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                int n;
                while ((n = in.read(buf)) != -1)
                {
                    bos.write(buf, 0, n);
                }
                in.close();

                String decoded = new String(bos.toByteArray(), "UTF-8");
                String[] sUrls = decoded.split("\n");
                Collections.addAll(patterns, sUrls);
                Util.log(MY_PACKAGE_NAME, "Loaded " + sUrls.length + " patterns from " + assetPath);
            }
        }
        catch (Exception e)
        {
            Util.log(MY_PACKAGE_NAME, "Failed to read " + assetPath + ": " + e);
        }
    }

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam)
    {
        if (lpparam.packageName.equals(MY_PACKAGE_NAME)) {
            XposedHelpers.findAndHookMethod("tw.fatminmin.xposed.minminguard.blocker.Util", lpparam.classLoader, "xposedEnabled", XC_MethodReplacement.returnConstant(true));
            if (xposedVersionCode >= 93)
                XposedHelpers.findAndHookMethod("tw.fatminmin.xposed.minminguard.Common", lpparam.classLoader, "getPrefMode", XC_MethodReplacement.returnConstant(Context.MODE_WORLD_READABLE));
        }

        /**
         * https://developer.android.com/about/versions/11/privacy/package-visibility
         * API30 adds new restrictions limiting apps from seeing our custom content provider at
         * content://tw.fatminmin.xposed.minminguard/ used to report back ad networks and removed ads.
         *
         * To get around this the hooked app needs QUERY_ALL_PACKAGES. This hack dynamically adds that
         * permission to every app MinMinGuard is enabled for, right before the permission state is
         * (re-)restored by PackageManager, so it gets granted like a manifest-declared permission.
         *
         * Note that this requires hooking the system framework ("android" scope) and a reboot / app
         * reinstall after enabling an app in MinMinGuard.
         *
         * The hook must survive framework changes:
         *   - Android 11-14: com.android.server.pm.permission.PermissionManagerService
         *                    restorePermissionState(SettingBase setting, boolean replace, ...)
         *   - Android 15/16/17: implementation split into PermissionManagerServiceImpl, first
         *                    argument is now the AndroidPackage interface (getPackageName() /
         *                    getRequestedPermissions()) instead of a PackageSetting object.
         * We hook every overload of "restorePermissionState" on both class names and read the
         * package data defensively (interface method first, then plain field).
         */
        if (Build.VERSION.SDK_INT > 29) {
            if (lpparam.packageName.equals("android")) {
                hookRestorePermissionState(lpparam, "com.android.server.pm.permission.PermissionManagerServiceImpl");
                hookRestorePermissionState(lpparam, "com.android.server.pm.permission.PermissionManagerService");
            }
        }

        /**
         * https://developer.android.com/reference/android/app/Application.html#onCreate()
         * wait for the app started to get remote preferences
         */
        XposedHelpers.findAndHookMethod("android.app.Application", lpparam.classLoader, "onCreate", new XC_MethodHook()
        {
            @Override
            protected void afterHookedMethod(MethodHookParam param)
            {
                final String packageName = lpparam.packageName;
                Context context = Util.getCurrentApplication();

                if (null == context)
                {
                    Util.log(packageName, "failed to get context");
                    return;
                }

                if (isEnabled(pref, packageName))
                {
                    Util.log(packageName, "is enabled for MinMinGuard");

                    // Api based blocking
                    ApiBlocking.handle(packageName, lpparam, param);
                    appSpecific(packageName, lpparam);

                    // Name based blocking
                    NameBlocking.nameBasedBlocking(packageName, lpparam);

                    // url filtering
                    if (pref.getBoolean(packageName + "_url", false))
                    {
                        UrlFiltering.removeWebViewAds(packageName, lpparam);
                    }
                }
            }
        });
    }

    /**
     * Hooks every overload of {@code restorePermissionState} on the given PermissionManager
     * implementation class and injects QUERY_ALL_PACKAGES into the package's requested permissions.
     */
    private static void hookRestorePermissionState(final LoadPackageParam lpparam, final String className)
    {
        try
        {
            Class<?> clazz = XposedHelpers.findClass(className, lpparam.classLoader);
            XposedBridge.hookAllMethods(clazz, "restorePermissionState", new XC_MethodHook()
            {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable
                {
                    Object pkg = param.args[0];
                    if (pkg == null) return;

                    String pkgName = null;
                    try
                    {
                        pkgName = (String) XposedHelpers.callMethod(pkg, "getPackageName");
                    }
                    catch (Throwable ignored)
                    {
                    }
                    if (pkgName == null)
                    {
                        try
                        {
                            pkgName = (String) XposedHelpers.getObjectField(pkg, "packageName");
                        }
                        catch (Throwable ignored)
                        {
                        }
                    }

                    if (pkgName == null || !isEnabled(pref, pkgName)) return;

                    Object permissionsObj = null;
                    /* Concrete implementation classes usually keep a mutable ArrayList/HashSet field
                       (PackageSetting on A11-14, AndroidPackageImpl on A15+). Fall back to the
                       interface accessor. Note: on A16+ getRequestedPermissions() returns a Set, on
                       A11-15 a List - always handle it as a generic Collection. */
                    try
                    {
                        permissionsObj = XposedHelpers.getObjectField(pkg, "requestedPermissions");
                    }
                    catch (Throwable ignored)
                    {
                    }
                    if (permissionsObj == null)
                    {
                        try
                        {
                            permissionsObj = XposedHelpers.callMethod(pkg, "getRequestedPermissions");
                        }
                        catch (Throwable ignored)
                        {
                        }
                    }

                    if (permissionsObj instanceof Collection)
                    {
                        Collection<?> permissions = (Collection<?>) permissionsObj;
                        if (!permissions.contains(QUERY_ALL_PACKAGES))
                        {
                            try
                            {
                                ((Collection<String>) permissions).add(QUERY_ALL_PACKAGES);
                                Util.log(MY_PACKAGE_NAME, "Added " + QUERY_ALL_PACKAGES + " to " + pkgName);
                            }
                            catch (UnsupportedOperationException | ClassCastException e)
                            {
                                Util.log(MY_PACKAGE_NAME, "Could not add QUERY_ALL_PACKAGES to " + pkgName + ": " + e);
                            }
                        }
                    }
                }
            });
        }
        catch (Throwable e)
        {
            Util.log(MY_PACKAGE_NAME, className + " -> " + e);
        }
    }
}
