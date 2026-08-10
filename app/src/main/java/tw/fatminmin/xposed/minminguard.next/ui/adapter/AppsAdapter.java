package tw.fatminmin.xposed.minminguard.next.ui.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.database.sqlite.SQLiteDatabase;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.widget.SwitchCompat;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tw.fatminmin.xposed.minminguard.next.Common;
import tw.fatminmin.xposed.minminguard.next.R;
import tw.fatminmin.xposed.minminguard.next.orm.AppData;
import tw.fatminmin.xposed.minminguard.next.orm.AppDataDao;
import tw.fatminmin.xposed.minminguard.next.orm.DaoMaster;
import tw.fatminmin.xposed.minminguard.next.orm.DaoSession;
import tw.fatminmin.xposed.minminguard.next.ui.UIUtils;
import tw.fatminmin.xposed.minminguard.next.ui.dialog.AppDetailDialogFragment;
import tw.fatminmin.xposed.minminguard.next.ui.fragments.MainFragment;
import tw.fatminmin.xposed.minminguard.next.ui.models.AppDetails;

/**
 * Created by fatminmin on 2015/10/1.
 */
public class AppsAdapter extends RecyclerView.Adapter<AppsAdapter.ViewHolder>
{

    private final Context mContext;
    private final SharedPreferences mPref;
    private final DaoMaster.DevOpenHelper helper;
    private final SQLiteDatabase db;
    private final DaoMaster daoMaster;
    private final DaoSession daoSession;
    private final AppDataDao appDataDao;
    private ArrayList<AppDetails> appList;
    private Map<String, AppData> mAppDataMap; // retrieved AppData from sqlite
    private MainFragment.FragmentMode mMode;
    private final java.util.Set<String> mSelected = new java.util.HashSet<>();
    private boolean mSelectionMode;
    private OnSelectionChangedListener mSelectionListener;

    public interface OnSelectionChangedListener
    {
        void onSelectionChanged(int count);
    }

    public interface OnSelectionModeListener
    {
        void onSelectionModeRequested();
    }

    private OnSelectionModeListener mModeListener;

    public void setOnSelectionModeListener(OnSelectionModeListener listener)
    {
        mModeListener = listener;
    }

    public void setOnSelectionChangedListener(OnSelectionChangedListener listener)
    {
        mSelectionListener = listener;
    }

    public boolean isSelectionMode()
    {
        return mSelectionMode;
    }

    public void setSelectionMode(boolean enabled)
    {
        mSelectionMode = enabled;
        if (!enabled)
        {
            mSelected.clear();
        }
        notifyDataSetChanged();
    }

    public void toggleSelection(String pkgName)
    {
        if (!mSelectionMode)
        {
            return;
        }
        if (!mSelected.remove(pkgName))
        {
            mSelected.add(pkgName);
        }
        notifyDataSetChanged();
        if (mSelectionListener != null)
        {
            mSelectionListener.onSelectionChanged(mSelected.size());
        }
    }

    /** Selects everything, or clears the selection when all are already selected.
        Selection mode is entered implicitly (the batch bar is always visible now),
        so the cards get highlighted and taps toggle selection without needing a
        prior long-press. */
    public void toggleSelectAll()
    {
        if (appList.isEmpty())
        {
            return;
        }
        if (!mSelectionMode)
        {
            setSelectionMode(true);
        }
        if (mSelected.size() == appList.size())
        {
            mSelected.clear();
        }
        else
        {
            mSelected.clear();
            for (AppDetails info : appList)
            {
                mSelected.add(info.getPackageName());
            }
        }
        notifyDataSetChanged();
        if (mSelectionListener != null)
        {
            mSelectionListener.onSelectionChanged(mSelected.size());
        }
    }

    public java.util.List<String> getSelectedPackages()
    {
        return new ArrayList<>(mSelected);
    }

    public int getSelectedCount()
    {
        return mSelected.size();
    }

    public AppsAdapter(Context context, ArrayList<AppDetails> list, MainFragment.FragmentMode mode)
    {
        super();
        mContext = context;
        appList = list;

        Context ctx = ContextCompat.createDeviceProtectedStorageContext(context);
        if (ctx == null) ctx = context;

        mPref = ctx.getSharedPreferences(Common.MOD_PREFS, Common.getPrefMode());

        helper = new DaoMaster.DevOpenHelper(context, "mmg", null);
        db = helper.getWritableDatabase();
        daoMaster = new DaoMaster(db);
        daoSession = daoMaster.newSession();
        appDataDao = daoSession.getAppDataDao();

        mMode = mode;

        mAppDataMap = new HashMap<>();
    }

    public void setAppList(ArrayList<AppDetails> list)
    {
        appList = list;

        /* update appdata map */
        mAppDataMap.clear();
        for (AppDetails info : appList)
        {
            final String pkgName = info.getPackageName();
            AppData appData = null;

            List<AppData> results = appDataDao.queryBuilder().where(AppDataDao.Properties.PkgName.eq(pkgName)).list();

            if (results.size() == 1)
            {
                appData = results.get(0);
                appDataDao.refresh(appData);
            }

            mAppDataMap.put(pkgName, appData);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_app, parent, false);
        return new ViewHolder(v);
    }

    private void setState(String pkgName, boolean checked)
    {
        /* auto mode */
        if (mMode == MainFragment.FragmentMode.AUTO)
        {
            return;
        }

        if (mMode == MainFragment.FragmentMode.BLACKLIST)
        {
            /* blacklist mode */
            mPref.edit().putBoolean(pkgName, checked).apply();
        }
        else
        {
            /* whitelist mode */
            mPref.edit().putBoolean(Common.getWhiteListKey(pkgName), checked).apply();
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position)
    {

        final AppDetails currentAppDetails = appList.get(position);

        final AppData appData = mAppDataMap.get(currentAppDetails.getPackageName());
        if (appData != null && appData.getBlockNum() != 0)
        {
            holder.txtBlockNum.setText(mContext.getString(R.string.msg_block_num, appData.getBlockNum()));
        }
        else
        {
            holder.txtBlockNum.setText("");
        }

        boolean selected = mSelected.contains(currentAppDetails.getPackageName());
        holder.card.setActivated(selected);
        holder.card.setSelected(selected);
        if (mSelectionMode)
        {
            int base = selected
                    ? resolveColor(com.google.android.material.R.attr.colorSecondaryContainer)
                    : resolveColor(com.google.android.material.R.attr.colorSurfaceContainerLow);
            holder.card.setCardBackgroundColor(base);
        }
        else
        {
            holder.card.setCardBackgroundColor(
                    ColorStateList.valueOf(resolveColor(com.google.android.material.R.attr.colorSurfaceContainerLow)));
        }

        currentAppDetails.loadIcon(holder.imgAppIcon);
        holder.txtAppName.setText(currentAppDetails.getName());

        if (mMode == MainFragment.FragmentMode.AUTO)
        {
            holder.switchEnable.setVisibility(View.GONE);
        }
        else
        {
            holder.switchEnable.setVisibility(View.VISIBLE);
        }

        holder.switchEnable.setChecked(currentAppDetails.isEnabled());
        holder.switchEnable.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                SwitchCompat sw = (SwitchCompat) v;
                setState(currentAppDetails.getPackageName(), sw.isChecked());
                /* Keep the in-memory list in sync with the pref, otherwise a
                   later notifyDataSetChanged() (e.g. the Apply button) rebinds
                   the row from the stale cached value and silently reverts the
                   manual toggle. */
                currentAppDetails.setEnabled(sw.isChecked());
            }
        });

        holder.card.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (mSelectionMode)
                {
                    toggleSelection(currentAppDetails.getPackageName());
                    return;
                }
                DialogFragment dialog = AppDetailDialogFragment.newInstance(currentAppDetails.getName(), currentAppDetails.getPackageName(), appData);
                AppCompatActivity activity = (AppCompatActivity) mContext;
                dialog.show(activity.getSupportFragmentManager(), "dialog");
            }
        });

        holder.card.setOnLongClickListener(new View.OnLongClickListener()
        {
            @Override
            public boolean onLongClick(View v)
            {
                if (!mSelectionMode && mMode != MainFragment.FragmentMode.AUTO)
                {
                    if (mModeListener != null)
                    {
                        mModeListener.onSelectionModeRequested();
                    }
                    setSelectionMode(true);
                    toggleSelection(currentAppDetails.getPackageName());
                    if (mSelectionListener != null)
                    {
                        mSelectionListener.onSelectionChanged(1);
                    }
                }
                return true;
            }
        });

        holder.imgAppIcon.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                UIUtils.restartApp(mContext, currentAppDetails.getPackageName());
            }
        });
    }

    private int resolveColor(int attr)
    {
        TypedValue tv = new TypedValue();
        if (mContext.getTheme().resolveAttribute(attr, tv, true))
        {
            return tv.data;
        }
        return 0xFFFFFFFF;
    }

    @Override
    public int getItemCount()
    {
        return appList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder
    {

        com.google.android.material.card.MaterialCardView card;
        ImageView imgAppIcon;
        TextView txtAppName;
        TextView txtBlockNum;
        SwitchCompat switchEnable;

        ViewHolder(View v)
        {
            super(v);

            card = (com.google.android.material.card.MaterialCardView) v;
            imgAppIcon = v.findViewById(R.id.img_app_icon);
            txtAppName = v.findViewById(R.id.txt_app_name);
            txtBlockNum = v.findViewById(R.id.txt_block_num);
            switchEnable = v.findViewById(R.id.switch_enable);
        }
    }
}
