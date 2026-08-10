package tw.fatminmin.xposed.minminguard.next.ui.dialog;

import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tw.fatminmin.xposed.minminguard.next.R;
import tw.fatminmin.xposed.minminguard.next.orm.AppData;
import tw.fatminmin.xposed.minminguard.next.orm.AppDataDao;
import tw.fatminmin.xposed.minminguard.next.orm.DaoMaster;
import tw.fatminmin.xposed.minminguard.next.orm.DaoSession;

/**
 * Aggregate blocking statistics from the provider database: total ads blocked,
 * top apps by block count and most-detected ad networks.
 */
public class StatsDialogFragment extends DialogFragment
{
    public static StatsDialogFragment newInstance()
    {
        return new StatsDialogFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        getDialog().setTitle(R.string.action_statistics);

        View v = inflater.inflate(R.layout.dialog_stats, container, false);
        TextView txtTotal = v.findViewById(R.id.stats_total);
        TextView txtApps = v.findViewById(R.id.stats_apps);
        TextView txtNetworks = v.findViewById(R.id.stats_networks);

        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(requireContext(), "mmg", null);
        SQLiteDatabase db = helper.getWritableDatabase();
        DaoSession session = new DaoMaster(db).newSession();
        AppDataDao dao = session.getAppDataDao();

        List<AppData> all = dao.loadAll();
        helper.close();

        if (all.isEmpty())
        {
            txtTotal.setText("0");
            txtApps.setText(R.string.stats_empty);
            txtNetworks.setText("");
            return v;
        }

        long total = 0;
        Map<String, Integer> networkCount = new HashMap<>();
        for (AppData data : all)
        {
            if (data.getBlockNum() != null)
            {
                total += data.getBlockNum();
            }
            if (data.getAdNetworks() != null)
            {
                for (String net : data.getAdNetworks().split(" "))
                {
                    if (net.trim().length() > 0)
                    {
                        Integer count = networkCount.get(net);
                        networkCount.put(net, count == null ? 1 : count + 1);
                    }
                }
            }
        }

        txtTotal.setText(String.valueOf(total));

        /* Top apps by block count. */
        List<AppData> sorted = new ArrayList<>(all);
        Collections.sort(sorted, new Comparator<AppData>()
        {
            @Override
            public int compare(AppData l, AppData r)
            {
                int lc = l.getBlockNum() == null ? 0 : l.getBlockNum();
                int rc = r.getBlockNum() == null ? 0 : r.getBlockNum();
                return Integer.compare(rc, lc);
            }
        });

        StringBuilder appsSb = new StringBuilder();
        int shown = 0;
        for (AppData data : sorted)
        {
            if (data.getBlockNum() == null || data.getBlockNum() == 0) continue;
            appsSb.append(data.getPkgName()).append(" - ").append(data.getBlockNum())
                    .append(getString(R.string.msg_ads_blocked).toLowerCase().replace(":", "")).append("\n");
            if (++shown >= 10) break;
        }
        if (shown == 0)
        {
            appsSb.append(getString(R.string.stats_empty));
        }
        txtApps.setText(appsSb.toString().trim());

        /* Top ad networks by number of apps using them. */
        List<Map.Entry<String, Integer>> nets = new ArrayList<>(networkCount.entrySet());
        Collections.sort(nets, new Comparator<Map.Entry<String, Integer>>()
        {
            @Override
            public int compare(Map.Entry<String, Integer> l, Map.Entry<String, Integer> r)
            {
                return Integer.compare(r.getValue(), l.getValue());
            }
        });

        StringBuilder netsSb = new StringBuilder();
        shown = 0;
        for (Map.Entry<String, Integer> e : nets)
        {
            netsSb.append(e.getKey()).append(" - ")
                    .append(getString(R.string.stats_apps_count, e.getValue())).append("\n");
            if (++shown >= 10) break;
        }
        txtNetworks.setText(netsSb.toString().trim());

        return v;
    }
}
