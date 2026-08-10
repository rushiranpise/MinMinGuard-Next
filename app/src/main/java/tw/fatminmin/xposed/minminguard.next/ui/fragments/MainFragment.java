package tw.fatminmin.xposed.minminguard.next.ui.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

import tw.fatminmin.xposed.minminguard.next.Common;
import tw.fatminmin.xposed.minminguard.next.FrameworkInfo;
import tw.fatminmin.xposed.minminguard.next.R;
import tw.fatminmin.xposed.minminguard.next.blocker.Util;
import tw.fatminmin.xposed.minminguard.next.ui.MainActivity;
import tw.fatminmin.xposed.minminguard.next.ui.adapter.AppsAdapter;
import tw.fatminmin.xposed.minminguard.next.ui.models.AppDetails;

import java.util.ArrayList;

public class MainFragment extends Fragment
{

    public boolean isAlive = false;
    private MainActivity mainActivity;
    private FragmentMode mMode;
    private Button mBtnMode;
    private AppsAdapter adapter;
    private SharedPreferences mModPref;
    private View mBatchBar;
    private TextView mTxtBatchCount;
    private Button mBtnBatchSelectAll;
    private final View.OnClickListener btnModeClick = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            mModPref.edit().putString(Common.KEY_MODE, Common.getModeString(mMode)).apply();
            adapter.notifyDataSetChanged();
            String[] titles = getResources().getStringArray(R.array.mode_array);
            String modeName = mMode.ordinal() < titles.length ? titles[mMode.ordinal()] : "";
            Snackbar.make(v, getString(R.string.msg_mode_applied, modeName), Snackbar.LENGTH_SHORT).show();
        }
    };

    public static MainFragment newInstance(FragmentMode mode)
    {
        MainFragment fragment = new MainFragment();
        Bundle args = new Bundle();
        args.putInt("mode", mode.ordinal());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        /* Resolve the mode as early as possible so background refresh()
           (which calls getMode()) is safe even before the view exists. */
        mMode = FragmentMode.values()[getArguments().getInt("mode")];
    }

    public void refreshUI()
    {
        /* The Apply button is always visible now - clicking it re-applies this
           tab's mode with a confirmation. Nothing to toggle here. */
        if (mBtnMode != null)
        {
            mBtnMode.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {

        mainActivity = (MainActivity) getActivity();
        mModPref = mainActivity.modPref;

        View view = inflater.inflate(R.layout.fragment_main, container, false);

        TextView mTxtXposedEnabled = view.findViewById(R.id.txt_xposed_enable);
        TextView mTxtRuntime = view.findViewById(R.id.txt_runtime);
        TextView mTxtModeInfo = view.findViewById(R.id.txt_mode_info);
        mBtnMode = view.findViewById(R.id.btn_mode);

        /* Short explanation of how the current mode behaves. */
        int modeInfoRes;
        switch (mMode)
        {
            case AUTO:
                modeInfoRes = R.string.mode_info_auto;
                break;
            case WHITELIST:
                modeInfoRes = R.string.mode_info_whitelist;
                break;
            case BLACKLIST:
            default:
                modeInfoRes = R.string.mode_info_blacklist;
                break;
        }
        mTxtModeInfo.setText(modeInfoRes);

        RecyclerView mRecyclerView = view.findViewById(R.id.recycler_view);

        if (!Util.xposedEnabled())
        {
            mTxtXposedEnabled.setVisibility(View.VISIBLE);
        }

        mTxtRuntime.setText(FrameworkInfo.describe(getActivity()));

        /* setup apply button (always visible; clicking re-applies this tab's mode) */
        mBtnMode.setOnClickListener(btnModeClick);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(mainActivity));
        adapter = new AppsAdapter(getActivity(), mainActivity.masterAppList, mMode);
        mRecyclerView.setAdapter(adapter);

        /* batch selection bar (shown on long-press; hidden in AUTO mode) */
        mBatchBar = view.findViewById(R.id.batch_bar);
        mTxtBatchCount = view.findViewById(R.id.txt_batch_count);
        mBtnBatchSelectAll = view.findViewById(R.id.btn_batch_select_all);
        mBtnBatchSelectAll.setOnClickListener(v -> adapter.toggleSelectAll());
        view.findViewById(R.id.btn_batch_enable).setOnClickListener(v -> applyBatch(true));
        view.findViewById(R.id.btn_batch_disable).setOnClickListener(v -> applyBatch(false));
        view.findViewById(R.id.btn_batch_cancel).setOnClickListener(v -> exitSelectionMode());
        adapter.setOnSelectionChangedListener(count ->
        {
            if (mTxtBatchCount != null)
            {
                mTxtBatchCount.setText(getString(R.string.batch_selected, count));
            }
            updateSelectAllLabel(count);
        });
        adapter.setOnSelectionModeListener(this::enterSelectionMode);
        if (mMode == FragmentMode.AUTO)
        {
            /* selection does not apply in AUTO mode */
            mBatchBar.setVisibility(View.GONE);
        }
        else
        {
            /* visible by default so batch actions are one tap away */
            mTxtBatchCount.setText(getString(R.string.batch_selected, 0));
            updateSelectAllLabel(0);
        }

        isAlive = true;
        return view;
    }

    private void applyBatch(boolean enable)
    {
        java.util.List<String> selected = adapter.getSelectedPackages();
        if (selected.isEmpty())
        {
            exitSelectionMode();
            return;
        }
        SharedPreferences.Editor editor = mModPref.edit();
        for (String pkg : selected)
        {
            if (mMode == FragmentMode.BLACKLIST)
            {
                editor.putBoolean(pkg, enable);
            }
            else if (mMode == FragmentMode.WHITELIST)
            {
                editor.putBoolean(Common.getWhiteListKey(pkg), enable);
            }
        }
        editor.apply();
        exitSelectionMode();
        mainActivity.refresh(false);
    }

    /** Clears the selection but keeps the bar visible (it is the default UI now). */
    private void exitSelectionMode()
    {
        if (adapter != null)
        {
            adapter.setSelectionMode(false);
        }
        if (mTxtBatchCount != null)
        {
            mTxtBatchCount.setText(getString(R.string.batch_selected, 0));
        }
        updateSelectAllLabel(0);
    }

    private void enterSelectionMode()
    {
        if (mMode == FragmentMode.AUTO || adapter == null || mBatchBar == null)
        {
            return;
        }
        adapter.setSelectionMode(true);
        mBatchBar.setVisibility(View.VISIBLE);
        mTxtBatchCount.setText(getString(R.string.batch_selected, 0));
        updateSelectAllLabel(0);
    }

    /** "Select all" flips to "Deselect all" once every app is selected. */
    private void updateSelectAllLabel(int count)
    {
        if (mBtnBatchSelectAll == null || adapter == null)
        {
            return;
        }
        boolean allSelected = count > 0 && count >= adapter.getItemCount();
        mBtnBatchSelectAll.setText(allSelected ? R.string.batch_deselect_all : R.string.batch_select_all);
    }

    /** Called from the adapter long-press to reveal the batch bar. */
    public void onAdapterSelectionRequested()
    {
        enterSelectionMode();
    }

    public FragmentMode getMode()
    {
        return mMode;
    }

    public void updateAndNotify(ArrayList<AppDetails> list)
    {
        /* Guard against a refresh() finishing after the view was destroyed
           (e.g. activity recreate on theme change / rotation). */
        if (adapter != null)
        {
            adapter.setAppList(list);
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
        isAlive = false;
        adapter = null;
    }

    public enum FragmentMode
    {
        AUTO, BLACKLIST, WHITELIST
    }
}
