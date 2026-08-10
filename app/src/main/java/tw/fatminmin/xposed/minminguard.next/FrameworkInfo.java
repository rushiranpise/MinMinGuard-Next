package tw.fatminmin.xposed.minminguard.next;

import android.content.Context;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import de.robv.android.xposed.XposedBridge;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Runtime detection helpers used to show which Xposed framework and which root
 * manager (Magisk / KernelSU / APatch) the module is running under.
 *
 * MinMinGuard is a regular LSPosed module, so no special support code is needed
 * to run on KernelSU / APatch: install a Zygisk provider (ZygiskNext / NeoZygisk)
 * plus LSPosed or Vector, then enable the module. This class is purely informational.
 */
public final class FrameworkInfo
{
    private FrameworkInfo() throws InstantiationException
    {
        throw new InstantiationException("This class is not for instantiation");
    }

    /** Returns the Xposed framework API version, or -1 when not running under a framework. */
    public static int getXposedVersion()
    {
        try
        {
            return XposedBridge.getXposedVersion();
        }
        catch (Throwable t)
        {
            return -1;
        }
    }

    /** Best-effort name of the Xposed framework, e.g. "LSPosed", "Vector", "EdXposed". */
    public static String getFrameworkName(Context context)
    {
        int version = getXposedVersion();
        String name = null;

        if (isPackageInstalled(context, "org.lsposed.manager") ||
                isPackageInstalled(context, "org.lsposed.lspd") ||
                isPackageInstalled(context, "io.github.lsposed.manager"))
        {
            name = "LSPosed / Vector";
        }
        else if (isPackageInstalled(context, "com.elderdrivers.riru.edxp") ||
                isPackageInstalled(context, "org.meowcat.edxposed.manager"))
        {
            name = "EdXposed";
        }
        else if (isPackageInstalled(context, "de.robv.android.xposed.installer"))
        {
            name = "Xposed";
        }

        if (version < 0)
        {
            return "not active";
        }

        return name != null ? name + " (API " + version + ")" : "Xposed-compatible (API " + version + ")";
    }

    /** Best-effort name of the root manager: Magisk, KernelSU, APatch or unknown. */
    public static String getRootManager(Context context)
    {
        List<String> found = new ArrayList<>();

        if (isPackageInstalled(context, "com.topjohnwu.magisk") || new File("/data/adb/magisk").exists())
        {
            found.add("Magisk");
        }
        if (isPackageInstalled(context, "me.weishu.kernelsu") || new File("/data/adb/ksu").exists())
        {
            found.add("KernelSU");
        }
        if (isPackageInstalled(context, "me.bmax.apatch") || new File("/data/adb/ap").exists())
        {
            found.add("APatch");
        }

        return found.isEmpty() ? "unknown" : TextUtils.join(" + ", found);
    }

    /** Compact status line for the main UI. */
    public static String describe(Context context)
    {
        if (context == null)
        {
            return "";
        }
        return context.getString(R.string.msg_runtime, getFrameworkName(context), getRootManager(context));
    }

    private static boolean isPackageInstalled(Context context, String pkgName)
    {
        try
        {
            context.getPackageManager().getPackageInfo(pkgName, 0);
            return true;
        }
        catch (PackageManager.NameNotFoundException e)
        {
            return false;
        }
    }
}
