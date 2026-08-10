package tw.fatminmin.xposed.minminguard;

import android.app.Application;

import com.google.android.material.color.DynamicColors;

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
    }
}
