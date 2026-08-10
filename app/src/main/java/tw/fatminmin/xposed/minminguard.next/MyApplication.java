package tw.fatminmin.xposed.minminguard.next;

import android.app.Application;
import android.content.SharedPreferences;

import com.google.android.material.color.DynamicColors;

import java.io.PrintWriter;
import java.io.StringWriter;

import de.robv.android.xposed.XposedBridge;

/**
 * Created by fatminmin on 2015/10/3.
 */
public class MyApplication extends Application
{
    @Override
    public void onCreate()
    {
        super.onCreate();
        /* Material You: on Android 12+ the app follows the wallpaper palette;
           on older devices it falls back to the red-tonal palette in styles.xml. */
        DynamicColors.applyToActivitiesIfAvailable(this);

        SharedPreferences uiPref = getSharedPreferences(Common.UI_PREFS, Common.getPrefMode());
        if (uiPref.getBoolean(Common.KEY_ENABLE_CRASH_REPORT, false))
        {
            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler()
            {
                private final Thread.UncaughtExceptionHandler mDefault =
                        Thread.getDefaultUncaughtExceptionHandler();

                @Override
                public void uncaughtException(Thread thread, Throwable throwable)
                {
                    try
                    {
                        StringWriter sw = new StringWriter();
                        throwable.printStackTrace(new PrintWriter(sw));
                        XposedBridge.log("MinMinGuard Next crash in " + thread.getName()
                                + ":\n" + sw.toString());
                    }
                    catch (Throwable ignored)
                    {
                    }
                    if (mDefault != null)
                    {
                        mDefault.uncaughtException(thread, throwable);
                    }
                }
            });
        }
    }
}
