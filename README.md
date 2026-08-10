# ![icon](images/ic_launcher.png) MinMinGuard

- [Introduction](#introduction)
- [Compatibility](#compatibility)
- [Installation](#installation)
- [Discussion and Supports](#discussion-and-supports)
- [Developing and Contributing](#developing-and-contributing)
- [Building](#building)
- [Legal and Licensing](#legal-and-licensing)

## Introduction

MinMinGuard is an ad-remover made with the Xposed Framework for Android. MinMinGuard can completely remove both the ads inside apps and the empty space caused by those ads. Conventional ads-removing apps are only able to block the ad content, but the space taken by the ad will still remain unused (black). MinMinGuard successfully removes that black space, which extends the app window and makes your user-experience better!

![Comparison](http://fatminmin.com/mmg/compare.png)

How it works:

- **API-based blocking** – intercepts the ad SDK calls (`loadAd`, `displayAd`, `showInterstitial`, ...) of 50+ ad networks and returns empty results / removes the banner views.
- **Name-based blocking** – watches `ViewGroup.addView()` / `LayoutInflater.inflate()` for known ad view classes and collapses them.
- **URL filtering** – optional per-app WebView filtering using the bundled host/pattern lists.
- **Per-app control** – Auto / Blacklist / Whitelist modes with per-app toggles and blocking statistics.

## Compatibility

- **Android 5.0 – 17 (API 21 – 37)**, built against Android 17 (compileSdk 37, targetSdk 36).
- **Framework**: any Xposed-compatible runtime:
  - **LSPosed** (API 93+) – recommended
  - **Vector** (the actively maintained LSPosed successor by JingMatrix, API 93+)
  - Legacy Xposed (API 82+) and EdXposed
- **Root managers**: Magisk, **KernelSU** and **APatch**.

### KernelSU / APatch notes

MinMinGuard itself needs no special support code to run on KernelSU or APatch – it is a regular
LSPosed module. KernelSU/APatch do not include a Zygisk environment, so install a Zygisk provider
first and then an LSPosed-compatible framework:

1. Install a Zygisk provider module: **ZygiskNext** or **NeoZygisk** (open-source alternative).
2. Install **LSPosed** or **Vector** (the framework runs on top of the Zygisk provider).
3. Install the MinMinGuard APK and enable it in the framework's manager app, with the **"Android"
   system framework checked in the module scope** (required for the Android 11+ package-visibility
   workaround).
4. Reboot and enable MinMinGuard for the apps you want.

The main screen shows the detected framework (LSPosed / Vector / EdXposed, API version) and root
manager (Magisk / KernelSU / APatch) in the status bar.

## Installation

1. Download the latest APK from the [Releases](https://github.com/rushiranpise/MinMinGuard/releases) page.
2. Install it as a normal app.
3. Enable the module in your Xposed framework's manager (LSPosed / Vector / Xposed Installer) and
   include the **Android system framework** in the module's scope, then reboot.
4. Open MinMinGuard and pick a mode:
   - **Auto**: block ads in every app.
   - **Blacklist**: only block in apps you toggle on.
   - **Whitelist**: block everywhere except apps you toggle on.

## Discussion and Supports

If you have any idea or need some support, you are welcome to post on the
[Xda Developers forum](http://forum.xda-developers.com/xposed/modules/xposed-minminguard-v1-7-0-cancelled-t2597332).

## Developing and Contributing

### Adding a new ad network into support

You may find some apps' ads are not blocked and removed by MinMinGuard. This may happen because the
apps use non-supported ad networks (some local ads providers, ex: Vpon in Taiwan). In this case you
can help by reverse engineering the app and adding the non-supported ad network into support.

```java
public abstract class Blocker {
    /**
     * @param packageName
     * @param lpparam
     * @return True if current app uses this ad network. False otherwise.
     */
    abstract public boolean handleLoadPackage(final String packageName, XC_LoadPackage.LoadPackageParam lpparam);
    abstract public String getBanner();
    abstract public String getBannerPrefix();
    public String getName() {
        return getClass().getSimpleName();
    }
}
```

You can implement an ad network blocker by extending the `Blocker` class. The `handleLoadPackage`
method is used for **API-based blocking** to intercept api calls for loading ads from the ad view.
You can leverage `ApiBlocking.removeBanner` and `ApiBlocking.blockAdFunction` to remove banner views
and block ad functions. The `getBanner` and `getBannerPrefix` methods are used for **Name-based
blocking**. You can return `null` and name-based blocking will not be triggered.

```java
public class Flurry extends Blocker {

  public static final String BANNER = "com.flurry.android.FlurryAds";
  public static final String BANNER_PREFIX = "com.flurry.android";

  public static final String NATIVE_AD = "com.flurry.android.ads.FlurryAdNative";

  @Override
  public String getBannerPrefix() {
    return BANNER_PREFIX;
  }

  @Override
  public String getBanner() {
    return BANNER;
  }

  public boolean handleLoadPackage(final String packageName, LoadPackageParam lpparam) {
    boolean result = false;
    result |= ApiBlocking.removeBanner(packageName, BANNER, "displayAd", lpparam);
    result |= ApiBlocking.blockAdFunction(packageName, BANNER, "fetchAd", lpparam);
    result |= ApiBlocking.blockAdFunction(packageName, NATIVE_AD, "fetchAd", lpparam);
    return result;
  }
}
```

```java
public static Blocker[] blockers = {
        /* Popular adnetwork */
        new Ad2iction(), new Adbert(), new Adfurikun(), new AdMarvel(), new Admob(), new AdmobGms(), new Amazon(),
        new Amobee(), new AppBrain(), new Bonzai(), new Chartboost(), new Domob(), new Facebook(), new Flurry(),
        new GmsDoubleClick(), new Hodo(), new ImpAct(), new Inmobi(), new Intowow(), new KuAd(), new mAdserve(),
        new Madvertise(), new MasAd(), new MdotM(), new Millennial(), new Mobclix(), new MoPub(), new Nend(),
        new Og(), new Onelouder(), new OpenX(), new SmartAdserver(), new Smarti(), new Startapp(), new Tapfortap(),
        new TWMads(), new UnityAds(), new Vpadn(), new Vpon(), new Waystorm(), new Yahoo()
};
```

Above is a classic example. After you complete the new blocker, remember to add it to the `blockers`
array in the `Main` class to activate it.

## Building

Requirements: JDK 17, Android SDK with platform **android-37** (Android 17).

```bash
./gradlew assembleDebug          # debug APK
./gradlew assembleRelease        # release APK (unsigned, sign with uber-apk-signer)
```

The Xposed API (`de.robv.android.xposed`, API 93) is compiled from the
[XposedBridge](https://github.com/rovo89/XposedBridge) sources and vendored at
`app/libs/api-93.jar` – the `api.lsposed.org` maven is frequently offline, so the jar is checked in
to keep the build hermetic. It is a compile-time stub; the real implementation comes from the
framework at runtime.

## Legal and Licensing

MinMinGuard is licensed under the [GPL-3.0 license](LICENSE).
