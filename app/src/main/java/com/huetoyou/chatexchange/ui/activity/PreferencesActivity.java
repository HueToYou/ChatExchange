package com.huetoyou.chatexchange.ui.activity;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.widget.Toast;

import com.huetoyou.chatexchange.R;
import com.huetoyou.chatexchange.auth.AuthenticatorActivity;
import com.huetoyou.chatexchange.backend.BackendService;
import com.huetoyou.chatexchange.ui.misc.AppCompatPreferenceActivity;
import com.huetoyou.chatexchange.ui.misc.hue.ActionBarHue;
import com.huetoyou.chatexchange.ui.misc.hue.ThemeHue;
import com.jrummyapps.android.colorpicker.ColorPreference;

import java.util.ArrayList;

public class PreferencesActivity extends AppCompatPreferenceActivity
{
    private static SharedPreferences mSharedPrefs;
    private final ArrayList<CharSequence> mAccountNames = new ArrayList<>();
    private static ActionBarHue actionBarHue = null;
    private static ThemeHue themeHue = null;
    private static boolean darkThemePrevState;
    private static ColorPreference fabColorPreference;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        themeHue = new ThemeHue();
        actionBarHue = new ActionBarHue();
        ThemeHue.setTheme(PreferencesActivity.this);

        super.onCreate(null);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new MyPreferenceFragment()).commit();

        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        ActionBarHue.setActionBarColorToSharedPrefsValue(this);

        AccountManager mAccountManager = AccountManager.get(this);
        if (mAccountManager.getAccounts().length > 0)
        {
            Account[] mAccounts = mAccountManager.getAccounts();

            for (Account a : mAccounts)
            {
                mAccountNames.add(a.name);
            }
        }

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class MyPreferenceFragment extends PreferenceFragment
    {

        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
            mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

            /*
             * Default color preference
             */
            ColorPreference colorPreference = (ColorPreference) findPreference("default_color");
            colorPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
            {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue)
                {
                    ActionBarHue.setActionBarColorToSharedPrefsValue(((PreferencesActivity) getActivity()));
                    return true;
                }
            });

            /*
             * Dynamically change color based on chat room theme preference
             */
            CheckBoxPreference checkBoxPreference = (CheckBoxPreference) findPreference("dynamic_bar_color");
            setAppBarColorChange(checkBoxPreference);

            /*
             * Make the FABs use action bar color pref
             */
            CheckBoxPreference sameFabColorCheckbox = (CheckBoxPreference) findPreference("same_fab_color");


            /*
             * FAB color preference
             */
            fabColorPreference = (ColorPreference) findPreference("fab_color");

            if (sameFabColorCheckbox.isChecked())
            {
                fabColorPreference.setEnabled(false);
            }

            setSameFabColor(sameFabColorCheckbox, fabColorPreference);

            /*
             * Dark theme preference
             */
            CheckBoxPreference darkThemePref = (CheckBoxPreference) findPreference("dark_theme");
            darkThemePrevState = darkThemePref.isChecked();
            setDarkTheme(darkThemePref);

            /*
             * Dynamic message background color preference
             */
            CheckBoxPreference dynamic_msg_bgcolor = (CheckBoxPreference) findPreference("dynamic_msg_bgcolor");

            /*
             * Dynamic message background [dark theme] color preference
             */
            ColorPreference msg_bgcolor_darkTheme = (ColorPreference) findPreference("msg_bgcolor_darkTheme");

            /*
             * Custom message background color preference
             */
            ColorPreference msg_bgcolor = (ColorPreference) findPreference("msg_bgcolor");
            setDynamicMsgBgColor(dynamic_msg_bgcolor, msg_bgcolor, msg_bgcolor_darkTheme);

            if (darkThemePref.isChecked())
            {
                msg_bgcolor_darkTheme.setEnabled(!dynamic_msg_bgcolor.isChecked());
                msg_bgcolor.setEnabled(false);
            }
            else
            {
                msg_bgcolor.setEnabled(!dynamic_msg_bgcolor.isChecked());
                msg_bgcolor_darkTheme.setEnabled(false);
            }

            /*
             * Backend preference
             */
            ListPreference backend = (ListPreference) findPreference("backend_type");
            setBackendMethod(backend);

            /*
             * Account preference
             */
            ListPreference account = (ListPreference) findPreference("account_select");
            setAccount(account);

            Preference addAcc = findPreference("add_account");
            addAcc.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
            {
                @Override
                public boolean onPreferenceClick(Preference preference)
                {
                    startActivity(new Intent(getActivity(), AuthenticatorActivity.class));
                    return false;
                }
            });

        }

        private void setAppBarColorChange(CheckBoxPreference checkBoxPreference)
        {
            checkBoxPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
            {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue)
                {
                    boolean pref = Boolean.parseBoolean(newValue.toString());

                    mSharedPrefs.edit().putBoolean("dynamicallyColorBar", pref).apply();

                    return true;
                }
            });
        }

        private void setBackendMethod(final ListPreference listPreference)
        {
            listPreference.setPersistent(true);
            listPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
            {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue)
                {
                    int index = listPreference.findIndexOfValue(newValue.toString());
                    listPreference.setValueIndex(index != -1 ? index : 0);
                    return false;
                }
            });
        }

        private void setAccount(final ListPreference listPreference)
        {
            listPreference.setPersistent(true);
            ArrayList<CharSequence> accnames = ((PreferencesActivity) getActivity()).mAccountNames;

            CharSequence[] names = new CharSequence[accnames.size()];
            names = accnames.toArray(names);

            listPreference.setEntries(names);
            listPreference.setEntryValues(names);

//            String currentSelected = mSharedPrefs.getString("account_selected", "None");
//            int index = listPreference.findIndexOfValue(currentSelected);
//            listPreference.setValueIndex(index != -1 ? index : 0);

            listPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
            {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue)
                {
//                    mSharedPrefs.edit().putString("account_selected", newValue.toString()).apply();
                    int index = listPreference.findIndexOfValue(newValue.toString());
                    listPreference.setValueIndex(index != -1 ? index : 0);
                    return false;
                }
            });
        }

        private void setDarkTheme(CheckBoxPreference pref)
        {
            pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
            {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue)
                {
                    boolean pref = (Boolean) newValue;

                    mSharedPrefs.edit().putBoolean("darkTheme", pref).apply();

                    if (pref != darkThemePrevState)
                    {
                        mSharedPrefs.edit().putBoolean("FLAG_restartMain", true).apply();
                    }
                    else
                    {
                        mSharedPrefs.edit().putBoolean("FLAG_restartMain", false).apply();
                    }

                    getActivity().recreate();

                    return true;
                }
            });
        }

        private void setDynamicMsgBgColor(final CheckBoxPreference checkBoxPreference, final ColorPreference hue, final ColorPreference hueDark)
        {
            hue.setEnabled(!checkBoxPreference.isChecked());
            hueDark.setEnabled(!checkBoxPreference.isChecked());
            final CheckBoxPreference darkThemePref = (CheckBoxPreference) findPreference("dark_theme");

            checkBoxPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
            {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o)
                {
                    if (darkThemePref.isChecked())
                    {
                        hueDark.setEnabled(!(boolean) o);
                        hue.setEnabled(false);
                    }
                    else
                    {
                        hue.setEnabled(!(boolean) o);
                        hueDark.setEnabled(false);
                    }

                    return true;
                }
            });
        }

        private void setSameFabColor(final CheckBoxPreference sameFabColor, final ColorPreference hue)
        {
            sameFabColor.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
            {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o)
                {
                    if ((boolean) o)
                    {
                        hue.setEnabled(false);
                    }
                    else
                    {
                        hue.setEnabled(true);
                    }
                    return true;
                }
            });
        }
    }

    @Override
    protected void onDestroy()
    {
        Intent intent = new Intent();
        setResult(RESULT_OK, intent);
        super.onDestroy();
    }

}