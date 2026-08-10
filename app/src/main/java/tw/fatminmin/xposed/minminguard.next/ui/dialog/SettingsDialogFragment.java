package tw.fatminmin.xposed.minminguard.next.ui.dialog;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import tw.fatminmin.xposed.minminguard.next.Common;
import tw.fatminmin.xposed.minminguard.next.R;
import tw.fatminmin.xposed.minminguard.next.blocker.Util;
import tw.fatminmin.xposed.minminguard.next.ui.LauncherAliasActivity;
import tw.fatminmin.xposed.minminguard.next.ui.MainActivity;

public class SettingsDialogFragment extends DialogFragment
{
    private SharedPreferences mUiPref;
    private SharedPreferences mModPref;
    private MaterialCheckBox mCbShowSystemApps;
    private MaterialCheckBox mCbShowDebugInfo;
    private MaterialCheckBox mCbShowLauncherIcon;
    private MaterialCheckBox mCbEnableCr;

    private final ActivityResultLauncher<String> mExportLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("application/json"), uri ->
            {
                if (uri != null)
                {
                    exportConfig(uri);
                }
            });

    private final ActivityResultLauncher<String[]> mImportLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri ->
            {
                if (uri != null)
                {
                    importConfig(uri);
                }
            });

    public static SettingsDialogFragment newInstance()
    {
        return new SettingsDialogFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        Context ctx = requireContext();
        mUiPref = ctx.getSharedPreferences(Common.UI_PREFS, Common.getPrefMode());
        Context devCtx = androidx.core.content.ContextCompat.createDeviceProtectedStorageContext(ctx);
        if (devCtx == null) devCtx = ctx;
        mModPref = devCtx.getSharedPreferences(Common.MOD_PREFS, Common.getPrefMode());

        getDialog().setTitle(R.string.action_settings);

        View v = inflater.inflate(R.layout.dialog_settings, container, false);

        mCbShowSystemApps = v.findViewById(R.id.cb_show_system_apps);
        mCbShowSystemApps.setChecked(mUiPref.getBoolean(Common.KEY_SHOW_SYSTEM_APPS, false));

        mCbShowDebugInfo = v.findViewById(R.id.cb_show_debug_info);
        mCbShowDebugInfo.setChecked(Util.DEBUG);

        mCbShowLauncherIcon = v.findViewById(R.id.cb_show_launcher_icon);
        mCbShowLauncherIcon.setChecked(mUiPref.getBoolean(Common.KEY_SHOW_LAUNCHER_ICON, true));

        mCbEnableCr = v.findViewById(R.id.cb_enable_cr);
        mCbEnableCr.setChecked(mUiPref.getBoolean(Common.KEY_ENABLE_CRASH_REPORT, false));

        v.findViewById(R.id.btn_theme).setOnClickListener(view -> showThemeDialog());
        v.findViewById(R.id.btn_sort).setOnClickListener(view -> showSortDialog());
        v.findViewById(R.id.btn_user_prefixes).setOnClickListener(view -> showUserPrefixesDialog());
        v.findViewById(R.id.btn_export).setOnClickListener(view ->
                mExportLauncher.launch("MinMinGuardNext-config.json"));
        v.findViewById(R.id.btn_import).setOnClickListener(view ->
                mImportLauncher.launch(new String[]{"application/json", "text/plain", "*/*"}));

        v.findViewById(R.id.btn_ok).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Util.DEBUG = mCbShowDebugInfo.isChecked();

                mUiPref.edit()
                        .putBoolean(Common.KEY_SHOW_SYSTEM_APPS, mCbShowSystemApps.isChecked())
                        .putBoolean(Common.KEY_ENABLE_CRASH_REPORT, mCbEnableCr.isChecked())
                        .apply();

                setLauncherIconEnabled(mCbShowLauncherIcon.isChecked());

                ((MainActivity) getActivity()).refresh(true);
                dismiss();
            }
        });

        return v;
    }

    private void setLauncherIconEnabled(boolean enabled)
    {
        mUiPref.edit().putBoolean(Common.KEY_SHOW_LAUNCHER_ICON, enabled).apply();
        try
        {
            ComponentName alias = new ComponentName(requireContext(), LauncherAliasActivity.class);
            int state = enabled
                    ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                    : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
            requireContext().getPackageManager().setComponentEnabledSetting(alias, state,
                    PackageManager.DONT_KILL_APP);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void showThemeDialog()
    {
        String[] items = {
                getString(R.string.setting_theme_system),
                getString(R.string.setting_theme_light),
                getString(R.string.setting_theme_dark)
        };
        int checked = getThemeIndex();
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.setting_theme)
                .setSingleChoiceItems(items, checked, (dialog, which) ->
                {
                    String value;
                    switch (which)
                    {
                        case 1: value = Common.VALUE_THEME_LIGHT; break;
                        case 2: value = Common.VALUE_THEME_DARK; break;
                        default: value = Common.VALUE_THEME_SYSTEM; break;
                    }
                    mUiPref.edit().putString(Common.KEY_THEME, value).apply();
                    applyTheme(value);
                    dialog.dismiss();
                })
                .show();
    }

    private int getThemeIndex()
    {
        String value = mUiPref.getString(Common.KEY_THEME, Common.VALUE_THEME_SYSTEM);
        if (Common.VALUE_THEME_LIGHT.equals(value)) return 1;
        if (Common.VALUE_THEME_DARK.equals(value)) return 2;
        return 0;
    }

    private void applyTheme(String value)
    {
        /* The activity applies the saved theme itself via setTheme() in
           onCreate (AppTheme / AppThemeDark - two explicit themes, no night
           mode machinery). This just persists the choice and recreates, so the
           recreated activity is always themed correctly on the first try. */
        mUiPref.edit().putString(Common.KEY_THEME, value).apply();
        requireActivity().getWindow().getDecorView().post(() -> requireActivity().recreate());
    }

    private void showSortDialog()
    {
        String[] items = {
                getString(R.string.setting_sort_name),
                getString(R.string.setting_sort_package),
                getString(R.string.setting_sort_blocked)
        };
        int checked = getSortIndex();
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.setting_sort)
                .setSingleChoiceItems(items, checked, (dialog, which) ->
                {
                    String value;
                    switch (which)
                    {
                        case 1: value = Common.VALUE_SORT_PACKAGE; break;
                        case 2: value = Common.VALUE_SORT_BLOCKED; break;
                        default: value = Common.VALUE_SORT_NAME; break;
                    }
                    mUiPref.edit().putString(Common.KEY_SORT, value).apply();
                    dialog.dismiss();
                    ((MainActivity) requireActivity()).refresh(false);
                })
                .show();
    }

    private int getSortIndex()
    {
        String value = mUiPref.getString(Common.KEY_SORT, Common.VALUE_SORT_NAME);
        if (Common.VALUE_SORT_PACKAGE.equals(value)) return 1;
        if (Common.VALUE_SORT_BLOCKED.equals(value)) return 2;
        return 0;
    }

    private void showUserPrefixesDialog()
    {
        final EditText edit = new EditText(requireContext());
        edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        edit.setMinLines(6);
        edit.setText(mModPref.getString(Common.KEY_USER_BLOCK_PREFIXES, ""));
        edit.setHint(R.string.setting_user_prefixes_hint);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.setting_user_prefixes)
                .setView(edit)
                .setPositiveButton(R.string.btn_ok, (dialog, which) ->
                {
                    mModPref.edit().putString(Common.KEY_USER_BLOCK_PREFIXES,
                            edit.getText().toString().trim()).apply();
                    Snackbar.make(requireView(), R.string.msg_config_exported, Snackbar.LENGTH_SHORT)
                            .show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void exportConfig(Uri uri)
    {
        try
        {
            JSONObject root = new JSONObject();

            JSONObject mod = new JSONObject();
            for (Map.Entry<String, ?> e : mModPref.getAll().entrySet())
            {
                putJson(mod, e.getKey(), e.getValue());
            }
            root.put("mod", mod);

            JSONObject ui = new JSONObject();
            ui.put(Common.KEY_SHOW_SYSTEM_APPS,
                    mUiPref.getBoolean(Common.KEY_SHOW_SYSTEM_APPS, false));
            ui.put(Common.KEY_SHOW_LAUNCHER_ICON,
                    mUiPref.getBoolean(Common.KEY_SHOW_LAUNCHER_ICON, true));
            root.put("ui", ui);

            OutputStream os = requireContext().getContentResolver().openOutputStream(uri, "w");
            if (os != null)
            {
                os.write(root.toString(2).getBytes(StandardCharsets.UTF_8));
                os.flush();
                os.close();
                Toast.makeText(requireContext(), R.string.msg_config_exported, Toast.LENGTH_SHORT).show();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Toast.makeText(requireContext(), R.string.msg_config_export_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void importConfig(Uri uri)
    {
        try
        {
            InputStream is = requireContext().getContentResolver().openInputStream(uri);
            if (is == null) return;
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null)
            {
                sb.append(line);
            }
            reader.close();

            JSONObject root = new JSONObject(sb.toString());
            JSONObject mod = root.getJSONObject("mod");
            SharedPreferences.Editor modEditor = mModPref.edit();
            modEditor.clear();
            for (java.util.Iterator<String> it = mod.keys(); it.hasNext(); )
            {
                String key = it.next();
                putFromJson(modEditor, key, mod.get(key));
            }
            modEditor.apply();

            if (root.has("ui"))
            {
                JSONObject ui = root.getJSONObject("ui");
                SharedPreferences.Editor uiEditor = mUiPref.edit();
                if (ui.has(Common.KEY_SHOW_SYSTEM_APPS))
                {
                    uiEditor.putBoolean(Common.KEY_SHOW_SYSTEM_APPS,
                            ui.getBoolean(Common.KEY_SHOW_SYSTEM_APPS));
                }
                if (ui.has(Common.KEY_SHOW_LAUNCHER_ICON))
                {
                    uiEditor.putBoolean(Common.KEY_SHOW_LAUNCHER_ICON,
                            ui.getBoolean(Common.KEY_SHOW_LAUNCHER_ICON));
                }
                uiEditor.apply();
            }

            Toast.makeText(requireContext(), R.string.msg_config_imported, Toast.LENGTH_SHORT).show();
            ((MainActivity) getActivity()).refresh(true);
        }
        catch (IOException | JSONException e)
        {
            e.printStackTrace();
            Toast.makeText(requireContext(), R.string.msg_config_import_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private static void putJson(JSONObject obj, String key, Object value) throws JSONException
    {
        if (value instanceof String) obj.put(key, (String) value);
        else if (value instanceof Integer) obj.put(key, (Integer) value);
        else if (value instanceof Long) obj.put(key, (Long) value);
        else if (value instanceof Boolean) obj.put(key, (Boolean) value);
        else if (value instanceof Float) obj.put(key, (Float) value);
        else if (value instanceof java.util.Set)
        {
            obj.put(key, new JSONArray((java.util.Set<?>) value));
        }
    }

    private static void putFromJson(SharedPreferences.Editor editor, String key, Object value)
    {
        if (value instanceof String) editor.putString(key, (String) value);
        else if (value instanceof Integer) editor.putInt(key, (Integer) value);
        else if (value instanceof Long) editor.putLong(key, (Long) value);
        else if (value instanceof Boolean) editor.putBoolean(key, (Boolean) value);
        else if (value instanceof Float) editor.putFloat(key, (Float) value);
        else if (value instanceof JSONArray)
        {
            try
            {
                java.util.Set<String> set = new java.util.HashSet<>();
                for (int i = 0; i < ((JSONArray) value).length(); i++)
                {
                    set.add(((JSONArray) value).getString(i));
                }
                editor.putStringSet(key, set);
            }
            catch (JSONException ignored)
            {
            }
        }
    }
}
