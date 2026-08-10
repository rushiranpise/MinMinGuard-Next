package tw.fatminmin.xposed.minminguard.next.blocker;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;

public class ViewBlocking
{
    public static void removeAdView(String packageName, View view)
    {
        Util.notifyRemoveAdView(view.getContext(), packageName, 1);
        removeAdView(packageName, view, true, 51);
    }

    private static void removeAdView(final String packageName, final View view, final boolean first, final float heightLimit)
    {
        float adHeight = convertPixelsToDp(view.getContext(), view.getHeight());

        if (first || (adHeight > 0 && adHeight <= heightLimit))
        {
            ViewGroup.LayoutParams params = view.getLayoutParams();

            if (params == null)
                params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0);
            else
                params.height = 0;

            view.setLayoutParams(params);
        }

        // preventing view not ready situation
        view.post(new Runnable()
        {
            @Override
            public void run()
            {
                float adHeight = convertPixelsToDp(view.getContext(), view.getHeight());

                if (first || (adHeight > 0 && adHeight <= heightLimit))
                {
                    ViewGroup.LayoutParams params = view.getLayoutParams();

                    if (params == null)
                        params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0);
                    else
                        params.height = 0;

                    view.setLayoutParams(params);
                }
            }
        });

        if (view.getParent() != null && view.getParent() instanceof ViewGroup)
        {
            ViewGroup parent = (ViewGroup) view.getParent();

            removeAdView(packageName, parent, false, heightLimit);
        }
    }

    private static float convertPixelsToDp(Context context, float px)
    {
        if (context == null)
            return 0;

        DisplayMetrics metrics = context.getResources().getDisplayMetrics();

        return px / metrics.density;
    }
}
