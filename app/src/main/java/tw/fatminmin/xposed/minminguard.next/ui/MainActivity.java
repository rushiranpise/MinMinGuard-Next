package tw.fatminmin.xposed.minminguard.next.ui;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.View;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.core.view.MenuItemCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import tw.fatminmin.xposed.minminguard.next.Common;
import tw.fatminmin.xposed.minminguard.next.PrefFileManager;
import tw.fatminmin.xposed.minminguard.next.R;
import tw.fatminmin.xposed.minminguard.next.ui.adapter.ModeFragmentAdapter;
import tw.fatminmin.xposed.minminguard.next.ui.dialog.SettingsDialogFragment;
import tw.fatminmin.xposed.minminguard.next.ui.dialog.StatsDialogFragment;
import tw.fatminmin.xposed.minminguard.next.ui.fragments.MainFragment;
import tw.fatminmin.xposed.minminguard.next.ui.models.AppDetails;

public class MainActivity extends AppCompatActivity
{

    public volatile ArrayList<AppDetails> masterAppList;

    private ArrayList<AppDetails> filteredAppList;
    private SharedPreferences uiPref;
    public SharedPreferences modPref;
    private ViewPager mViewPager;
    private ModeFragmentAdapter mAdapter;
    private DrawerLayout mDrawerLayout;
    private final NavigationView.OnNavigationItemSelectedListener mNavListener = new NavigationView.OnNavigationItemSelectedListener()
    {
        @Override
        public boolean onNavigationItemSelected(MenuItem menuItem)
        {
            mDrawerLayout.closeDrawers();
            switch (menuItem.getItemId())
            {
                case R.id.action_settings:
                    SettingsDialogFragment.newInstance().show(getSupportFragmentManager(), "dialog");
                    break;
                case R.id.action_statistics:
                    StatsDialogFragment.newInstance().show(getSupportFragmentManager(), "stats");
                    break;
                case R.id.action_tutorial:
                    showIntro();
                    break;
                case R.id.action_donate:
                    Intent it = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=47GXTVHUB4LYS"));
                    startActivity(it);
                    break;
                case R.id.action_about:
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://fatminmin.com/pages/minminguard.html")));
                    break;
                default:
                    break;
            }
            return true;
        }
    };
    private ActionBarDrawerToggle mToggle;
    private ProgressDialog progressDialog;
    private SwipeRefreshLayout swipeRefreshLayout;
    private PrefFileManager prefFileManager;

    @Override
    public void attachBaseContext(Context newBase)
    {
        super.attachBaseContext(newBase);
        prefFileManager = PrefFileManager.getInstance(this);
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        prefFileManager.onResume();
    }

    @Override
    protected void onStop()
    {
        prefFileManager.onPause();
        super.onStop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        /* Deterministic theme selection. The saved pref maps 1:1 to one of two
           EXPLICIT Material 3 themes (AppTheme = light palette, AppThemeDark =
           dark palette) via setTheme(). There is no AppCompat night-mode
           machinery and no night-qualified resource variants, so nothing can go
           stale or half-apply: every recreate - including right after a theme
           switch - inflates with the exact theme chosen here. "System" is
           resolved against the real device state by Common.isDarkTheme(). */
        String theme = getSharedPreferences(Common.UI_PREFS, Common.getPrefMode())
                .getString(Common.KEY_THEME, Common.VALUE_THEME_SYSTEM);
        boolean darkTheme = Common.isDarkTheme(theme);
        setTheme(darkTheme ? R.style.AppThemeDark : R.style.AppTheme);

        /* Android 15+ (and mandatory for targetSdk 35+/36) edge-to-edge.
           The status/nav bar icon color is chosen from the SAME resolved theme
           (dark theme -> light icons, light theme -> dark icons), so it can
           never disagree with the applied palette - including right after a
           theme switch. SystemBarStyle.auto is NOT used because it evaluates
           the device configuration, which may differ from the forced theme. */
        if (darkTheme)
        {
            EdgeToEdge.enable(this, SystemBarStyle.dark(Color.TRANSPARENT));
        }
        else
        {
            EdgeToEdge.enable(this, SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT));
        }

        super.onCreate(savedInstanceState);
        prefFileManager.fixFolderPermissionsAsync();

        Context ctx = ContextCompat.createDeviceProtectedStorageContext(this);
        if (ctx == null) ctx = this;

        uiPref = getSharedPreferences(Common.UI_PREFS, Common.getPrefMode());
        modPref = ctx.getSharedPreferences(Common.MOD_PREFS, Common.getPrefMode());

        progressDialog = new ProgressDialog(this, R.style.MainSpinnerDialogStyle);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage(getString(R.string.msg_wait));
        progressDialog.setCancelable(false);

        if (uiPref.getBoolean(Common.KEY_FIRST_TIME, true))
        {
            uiPref.edit().putBoolean(Common.KEY_FIRST_TIME, false).apply();
            showIntro();
        }

        setContentView(R.layout.activity_main);

        // Material 3 top app bar
        Toolbar toolbar = findViewById(R.id.top_toolbar);
        if (toolbar instanceof com.google.android.material.appbar.MaterialToolbar)
        {
            /* M3 toolbar content insets: keeps the nav button and the action
               icons clear of the screen edges / rounded corners. The theme
               has no Toolbar style so the defaults are not applied. */
            com.google.android.material.appbar.MaterialToolbar mt =
                (com.google.android.material.appbar.MaterialToolbar) toolbar;
            int inset = (int) (16 * getResources().getDisplayMetrics().density + 0.5f);
            mt.setContentInsetStartWithNavigation(inset);
            mt.setContentInsetEndWithActions(inset);
        }
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
        {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        // drawer view and navigation view setup
        NavigationView mNavigationView = findViewById(R.id.navigation_view);
        mNavigationView.setNavigationItemSelectedListener(mNavListener);

        mDrawerLayout = findViewById(R.id.drawer_layout);
        mToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.app_name, R.string.app_name);
        mDrawerLayout.addDrawerListener(mToggle);
        mToggle.syncState();

        // setup tab view
        mAdapter = new ModeFragmentAdapter(getSupportFragmentManager(), MainActivity.this);
        mViewPager = findViewById(R.id.viewpager);
        mViewPager.setAdapter(mAdapter);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener()
        {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels)
            {
            }

            @Override
            public void onPageSelected(int position)
            {
                MainFragment fragment = getFragmentAt(position);
                if (fragment != null && fragment.isAlive)
                {
                    fragment.refreshUI();
                }
                refresh(true);
            }

            @Override
            public void onPageScrollStateChanged(int state)
            {
                if (state == 1)
                {
                    swipeRefreshLayout.setEnabled(false);
                }
                else if (state == 0)
                {
                    swipeRefreshLayout.setEnabled(true);
                }
            }
        });

        TabLayout mTabLayout = findViewById(R.id.sliding_tabs);
        mTabLayout.setupWithViewPager(mViewPager);

        switch (modPref.getString(Common.KEY_MODE, Common.VALUE_MODE_BLACKLIST))
        {
            case Common.VALUE_MODE_AUTO:
                mViewPager.setCurrentItem(0);
                break;
            case Common.VALUE_MODE_BLACKLIST:
                mViewPager.setCurrentItem(1);
                break;
            case Common.VALUE_MODE_WHITELIST:
                mViewPager.setCurrentItem(2);
                break;
            default:
                break;
        }

        /* Keep the content above the system bars (edge-to-edge).
           The DrawerLayout has fitsSystemWindows="true" so the NavigationView
           drawer draws behind the status bar; the top app bar gets the status
           bar inset and the list gets the navigation bar inset. */
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawer_layout), (v, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            );
            /* The AppBarLayout handles the status bar inset for the toolbar;
               we only keep the list above the navigation bar, and the toolbar
               icons clear of display cutouts / rounded corners. */
            View toolbar2 = findViewById(R.id.top_toolbar);
            if (toolbar2 != null) {
                toolbar2.setPadding(bars.left, 0, bars.right, 0);
            }
            View content = findViewById(R.id.swipeRefresh);
            if (content != null) {
                content.setPadding(0, 0, 0, bars.bottom);
            }
            return insets;
        });

        masterAppList = new ArrayList<>();
        filteredAppList = new ArrayList<>();

        swipeRefreshLayout = findViewById(R.id.swipeRefresh);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener()
        {
            @Override
            public void onRefresh()
            {
                refresh(false);
            }
        });

        refresh(true);
    }

    @Override
    public void onResume()
    {
        refresh(false);
        super.onResume();
    }

    @Override
    public void onBackPressed()
    {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START))
        {
            mDrawerLayout.closeDrawer(GravityCompat.START);
            return;
        }
        else if (mDrawerLayout.isDrawerOpen(GravityCompat.END))
        {
            mDrawerLayout.closeDrawer(GravityCompat.END);
            return;
        }

        moveTaskToBack(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.action_search));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener()
        {
            @Override
            public boolean onQueryTextSubmit(String query)
            {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String query)
            {
                filterAppList(query);
                return true;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        return mToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    private MainFragment getCurrentFragment()
    {
        /* After an activity recreate (theme change / rotation) the pager
           adapter's pre-created instances are NOT the fragments actually
           attached to the FragmentManager - the manager restores its own
           instances with the standard ViewPager tag. Resolve the attached
           fragment, otherwise refresh() would hit a phantom fragment with
           no view/adapter and the app list would never load. */
        androidx.fragment.app.Fragment f = getSupportFragmentManager().findFragmentByTag(
                "android:switcher:" + R.id.viewpager + ":" + mViewPager.getCurrentItem());
        if (f instanceof MainFragment)
        {
            return (MainFragment) f;
        }
        return (MainFragment) mAdapter.getItem(mViewPager.getCurrentItem());
    }

    private MainFragment getFragmentAt(int position)
    {
        androidx.fragment.app.Fragment f = getSupportFragmentManager().findFragmentByTag(
                "android:switcher:" + R.id.viewpager + ":" + position);
        if (f instanceof MainFragment)
        {
            return (MainFragment) f;
        }
        return (MainFragment) mAdapter.getItem(position);
    }

    private void showIntro()
    {
        Intent intent = new Intent(this, MinMinGuardIntro.class);
        startActivity(intent);
    }

    public void refresh(final boolean displayProgressDialog)
    {
        new AsyncTask<Void, Void, Void>()
        {

            @Override
            protected void onPreExecute()
            {
                if (displayProgressDialog)
                {
                    progressDialog.show();
                }
            }

            @Override
            protected Void doInBackground(Void... voids)
            {
                updateAppList(getPackageManager());
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid)
            {
                getCurrentFragment().updateAndNotify(masterAppList);
                progressDialog.dismiss();
                swipeRefreshLayout.setRefreshing(false);
            }
        }.execute();
    }

    private void updateAppList(final PackageManager pm)
    {

        boolean showSystemApps = uiPref.getBoolean(Common.KEY_SHOW_SYSTEM_APPS, false);
        String sortKey = uiPref.getString(Common.KEY_SORT, Common.VALUE_SORT_NAME);
        ArrayList<AppDetails> temp = new ArrayList<>();

        for (PackageInfo info : pm.getInstalledPackages(0))
        {
            if (showSystemApps || (info.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0)
            {
                String name = (String) info.applicationInfo.loadLabel(pm);
                String packageName = info.packageName;
                boolean isEnabled = isPackageEnabled(packageName);

                temp.add(new AppDetails(name, packageName, info.applicationInfo, isEnabled));
            }

            // setting initial value for system apps
            if ((info.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 1 && !modPref.contains(info.packageName))
            {
                modPref.edit().putBoolean(info.packageName, false).putBoolean(Common.getWhiteListKey(info.packageName), true).apply();
            }
        }

        if (Common.VALUE_SORT_PACKAGE.equals(sortKey))
        {
            Collections.sort(temp, new Comparator<AppDetails>()
            {
                @Override
                public int compare(AppDetails l, AppDetails r)
                {
                    return l.getPackageName().compareToIgnoreCase(r.getPackageName());
                }
            });
        }
        else if (Common.VALUE_SORT_BLOCKED.equals(sortKey))
        {
            /* Read the block counts from the provider DB (same data the
               adapter shows) so the list can be sorted by most-blocked. */
            final java.util.Map<String, Integer> blockCounts = new java.util.HashMap<>();
            try
            {
                tw.fatminmin.xposed.minminguard.next.orm.DaoMaster.DevOpenHelper helper =
                        new tw.fatminmin.xposed.minminguard.next.orm.DaoMaster.DevOpenHelper(this, "mmg", null);
                tw.fatminmin.xposed.minminguard.next.orm.DaoSession session =
                        new tw.fatminmin.xposed.minminguard.next.orm.DaoMaster(helper.getWritableDatabase()).newSession();
                for (tw.fatminmin.xposed.minminguard.next.orm.AppData data : session.getAppDataDao().loadAll())
                {
                    blockCounts.put(data.getPkgName(), data.getBlockNum());
                }
                helper.close();
            }
            catch (Exception ignored)
            {
            }
            Collections.sort(temp, new Comparator<AppDetails>()
            {
                @Override
                public int compare(AppDetails l, AppDetails r)
                {
                    int lc = blockCounts.containsKey(l.getPackageName()) ? blockCounts.get(l.getPackageName()) : 0;
                    int rc = blockCounts.containsKey(r.getPackageName()) ? blockCounts.get(r.getPackageName()) : 0;
                    int byBlocked = Integer.compare(rc, lc);
                    return byBlocked != 0 ? byBlocked : l.getName().compareToIgnoreCase(r.getName());
                }
            });
        }
        else
        {
            Collections.sort(temp, new Comparator<AppDetails>()
            {
                @Override
                public int compare(AppDetails l, AppDetails r)
                {
                    return l.getName().compareToIgnoreCase(r.getName());
                }
            });
        }

        masterAppList = temp;
    }

    private boolean isPackageEnabled(String pkgName)
    {
        if (getCurrentFragment().getMode() == MainFragment.FragmentMode.AUTO)
        {
            return true;
        }

        if (getCurrentFragment().getMode() == MainFragment.FragmentMode.BLACKLIST)
        {
            return modPref.getBoolean(pkgName, false);
        }

        return modPref.getBoolean(Common.getWhiteListKey(pkgName), false);
    }

    private void filterAppList(String keyword)
    {
        filteredAppList.clear();

        String kw = keyword.toLowerCase();
        for (AppDetails info : masterAppList)
        {
            if (info.getName().toLowerCase().contains(kw)
                    || info.getPackageName().toLowerCase().contains(kw))
            {
                filteredAppList.add(info);
            }
        }

        getCurrentFragment().updateAndNotify(filteredAppList);
    }
}
