package com.huetoyou.chatexchange.ui.activity;

import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;
import android.support.v7.widget.AppCompatImageButton;
import android.text.InputType;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;

import com.github.clans.fab.FloatingActionMenu;
import com.huetoyou.chatexchange.auth.Authenticator;
import com.huetoyou.chatexchange.net.RequestFactory;
import com.huetoyou.chatexchange.ui.frags.HomeFragment;
import com.huetoyou.chatexchange.ui.frags.ChatFragment;
import com.huetoyou.chatexchange.R;
import com.huetoyou.chatexchange.auth.AuthenticatorActivity;
import com.huetoyou.chatexchange.ui.misc.Utils;
import com.huetoyou.chatexchange.ui.misc.ImgTextArrayAdapter;
import com.huetoyou.chatexchange.ui.misc.hue.ThemeHue;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.jeremyfeinstein.slidingmenu.lib.app.SlidingActivity;

import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends SlidingActivity {

    private SharedPreferences mSharedPrefs;
    private SharedPreferences.Editor mEditor;
    private AccountManager mAccountManager;

    private SlidingMenu mChatroomSlidingMenu;
    private ListView chatroomsList;
    private ImgTextArrayAdapter chatroomArrayAdapter;
    private SlidingMenu mCurrentUsers_SlidingMenu;
    private FragmentManager mFragmentManager;

    private BroadcastReceiver mAddChatReceiver;

    private Intent mIntent;

    private boolean oncreatejustcalled = false;

    private Handler mHandler;

    private ThemeHue themeHue = null;
    private String mCurrentFragment;

    private RequestFactory mRequestFactory;

    private SparseArray<Fragment> mSOChats = new SparseArray<>();
    private SparseArray<Fragment> mSEChats = new SparseArray<>();

    private SparseIntArray mSOChatColors = new SparseIntArray();
    private SparseIntArray mSEChatColors = new SparseIntArray();

    private SparseArray<String> mSOChatNames = new SparseArray<>();
    private SparseArray<String> mSEChatNames = new SparseArray<>();

    private SparseArray<String> mSOChatUrls = new SparseArray<>();
    private SparseArray<String> mSEChatUrls = new SparseArray<>();

    private SparseArray<Drawable> mSOChatIcons = new SparseArray<>();
    private SparseArray<Drawable> mSEChatIcons = new SparseArray<>();

    private Set<String> mSOChatIDs = new HashSet<>(0);
    private Set<String> mSEChatIDs = new HashSet<>(0);

    private String mCookieString = null;

    @SuppressLint("StaticFieldLeak")
    private static MainActivity mainActivity;
    private AddList mAddList;

    private AdapterView.OnItemClickListener mItemClickListener = new AdapterView.OnItemClickListener()
    {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, final int position, long id)
        {
            chatroomsList.requestFocusFromTouch();
            chatroomsList.setSelection(position);
            chatroomsList.requestFocus();

            mCurrentFragment = chatroomArrayAdapter.getUrls()[position];
            doCloseAnimationForDrawerToggle(mDrawerButton);

            mHandler.postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    setFragmentByTag(chatroomArrayAdapter.getUrls()[position]);
                }
            }, 400);


            getmChatroomSlidingMenu().toggle();
        }
    };
    private VectorDrawableCompat drawable;
    private ViewGroup mActionBar;
    private AppCompatImageButton mDrawerButton;

    /*
     * Activity Lifecycle
     */

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        mainActivity = this;
        themeHue = new ThemeHue();
        themeHue.setTheme(MainActivity.this);
        super.onCreate(savedInstanceState);
        //Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_main);
        preSetup();
        createUsersSlidingMenu();
        setup();

//        android.support.v7.widget.Toolbar toolbar = new android.support.v7.widget.Toolbar(this);
//        toolbar.setId(1001);
//        TypedValue typedValue = new TypedValue();
//        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, TypedValue.complexToDimensionPixelSize(typedValue.data, getResources().getDisplayMetrics()));
//        toolbar.setLayoutParams(layoutParams);
//        toolbar.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
//
//        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        drawable = VectorDrawableCompat.create(getResources(), R.drawable.ic_menu_black_24dp, null);
        drawable.setTintList(ColorStateList.valueOf(Color.rgb(255, 255, 255)));
        getSupportActionBar().setHomeAsUpIndicator(drawable);

        mActionBar = getActionBar(getWindow().getDecorView());
        mDrawerButton = (AppCompatImageButton) mActionBar.getChildAt(1);
        mDrawerButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                Log.e("CLICKED", "CLICKED");
                if (mChatroomSlidingMenu.isMenuShowing())
                {
                    doCloseAnimationForDrawerToggle(view);
                } else
                {
                    doOpenAnimationForDrawerToggle(view);
                }
                onSupportNavigateUp();
            }
        });

        oncreatejustcalled = true;
    }

    public ViewGroup getActionBar(View view) {
        try {
            if (view instanceof ViewGroup) {
                ViewGroup viewGroup = (ViewGroup) view;

                if (viewGroup instanceof android.support.v7.widget.Toolbar) {
                    return viewGroup;
                }

                for (int i = 0; i < viewGroup.getChildCount(); i++) {
                    ViewGroup actionBar = getActionBar(viewGroup.getChildAt(i));

                    if (actionBar != null) {
                        return actionBar;
                    }
                }
            }
        } catch (Exception e) {
        }

        return null;
    }

    @Override
    protected void onResume()
    {
        themeHue.setThemeOnResume(MainActivity.this, oncreatejustcalled);

        if(oncreatejustcalled)
        {
            oncreatejustcalled = false;
        }

        doFragmentStuff();
        super.onResume();

        System.out.println("Hellu!");

        if (mFragmentManager.findFragmentByTag("home").isDetached())
        {
            //noinspection ConstantConditions
//            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//            getSupportActionBar().setHomeAsUpIndicator(VectorDrawableCompat.create(getResources(), R.drawable.ic_home_white_24dp, null));
        }

        else
        {
            //noinspection ConstantConditions
//            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mIntent = getIntent();
        respondToNotificationClick();

        /*if (!mFragmentManager.findFragmentByTag("home").isDetached())
        {
            actionBarHue.setActionBarColorToSharedPrefsValue(this);
        }

        else if (!mSharedPrefs.getBoolean("dynamicallyColorBar", false))
        {
            actionBarHue.setActionBarColorToSharedPrefsValue(this);
        }*/
    }

    @Override
    public void onBackPressed() {
        if (mFragmentManager.findFragmentByTag("home").isDetached()) {
            setFragmentByTag("home");
            for (Fragment fragment : mFragmentManager.getFragments()) {
                if (fragment != null && !fragment.isDetached() && fragment instanceof ChatFragment) if (((ChatFragment) fragment).getmSlidingMenu().isMenuShowing()) ((ChatFragment) fragment).getmSlidingMenu().showContent(true);
            }
            if (mChatroomSlidingMenu.isMenuShowing()) mChatroomSlidingMenu.showContent(true);
        } else if (mChatroomSlidingMenu.isMenuShowing()) {
            mChatroomSlidingMenu.showContent(true);
        } else {
            super.onBackPressed();
        }
    }

    /*
     * Setup procedure
     */

    private void preSetup()
    {
        setBehindContentView(R.layout.chatroom_slideout);
        mChatroomSlidingMenu = getSlidingMenu();

        mChatroomSlidingMenu.setTouchModeAbove(SlidingMenu.TOUCHMODE_MARGIN);
        mChatroomSlidingMenu.setShadowWidthRes(R.dimen.shadow_width);
        mChatroomSlidingMenu.setShadowDrawable(new ColorDrawable(getResources().getColor(R.color.transparentGrey)));
        mChatroomSlidingMenu.setBehindWidthRes(R.dimen.sliding_menu_chats_width);
        mChatroomSlidingMenu.setFadeDegree(0.35f);

        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        mEditor = mSharedPrefs.edit();
        mEditor.apply();


        mHandler = new Handler();

//        Log.e("URLS", mChatUrls.toString());

        //mEditor.putInt("tabIndex", 0).apply();
        mFragmentManager = getSupportFragmentManager();

        mIntent = getIntent();
    }

    private void setup()
    {
        final FloatingActionMenu fam = findViewById(R.id.chat_slide_menu);

        com.github.clans.fab.FloatingActionButton floatingActionButton = findViewById(R.id.add_chat_fab);
        floatingActionButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                showAddTabDialog();
                fam.close(true);
            }
        });

        com.github.clans.fab.FloatingActionButton fab = findViewById(R.id.home_fab);
        fab.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                doCloseAnimationForDrawerToggle(mDrawerButton);
                mChatroomSlidingMenu.toggle();

                mHandler.postDelayed(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        setFragmentByTag("home");

                    }
                }, 400);
                fam.close(false);
            }
        });
        mRequestFactory = new RequestFactory();

        mAccountManager = AccountManager.get(this);

        AccountManagerCallback<Bundle> accountManagerCallback = new AccountManagerCallback<Bundle>() {
            @Override
            public void run(AccountManagerFuture<Bundle> accountManagerFuture) {
                Log.e("AUtH", "AAA");
                String authToken = "";
                try {
                    authToken = accountManagerFuture.getResult().getString(AccountManager.KEY_AUTHTOKEN);
                    Log.e("Auth", authToken);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("RI", "P");
                }
                mRequestFactory = new RequestFactory(authToken);
                mCookieString = authToken;
                doFragmentStuff();
            }
        };

        mSOChatIDs = mSharedPrefs.getStringSet("SOChatIDs", new HashSet<String>());
        mSEChatIDs = mSharedPrefs.getStringSet("SEChatIDs", new HashSet<String>());

        if(mSharedPrefs.getBoolean("isFirstRun", true))
        {
            Intent intent = new Intent(this, IntroActivity.class);
            startActivity(intent);

            finish();
        }

        else if (mAccountManager.getAccounts().length < 1)
        {
            startActivity(new Intent(this, AuthenticatorActivity.class));
            finish();
        }

        else
        {
            if (mFragmentManager.findFragmentByTag("home") == null) mFragmentManager.beginTransaction().add(R.id.content_main, new HomeFragment(), "home").commit();
            mAccountManager.getAuthToken(mAccountManager.getAccounts()[0], Authenticator.ACCOUNT_TYPE, null, true, accountManagerCallback, null);
        }

        respondToNotificationClick();
        setupACBR();
    }

    private void doCloseAnimationForDrawerToggle(View view) {
        view.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rotate_180_around_center));
    }

    private void doOpenAnimationForDrawerToggle(View view) {
        view.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rotate_neg180_around_center));
    }

    /**
     * BroadcastReceiver listening for click on chat URL from WebViewActivity
     * @see WebViewActivity#client(WebView)
     */

    private void setupACBR() {
        mAddChatReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final Bundle extras = intent.getExtras();
                if (extras != null) {
                    if (extras.containsKey("idSE")) {
                        mSEChatIDs.add(extras.getString("idSE"));
                        mEditor.putStringSet("SEChatIDs", mSEChatIDs).apply();
                        doFragmentStuff();
//                        new AsyncTask<Void, Void, Void>() {
//                            @Override
//                            protected Void doInBackground(Void... voids) {
//                                while (mSEChatUrls.get(Integer.decode(extras.getString("idSE"))) == null);
//                                while (mFragmentManager.findFragmentByTag(mSEChatUrls.get(Integer.decode(extras.getString("idSE")))) == null);
//                                return null;
//                            }
//
//                            @Override
//                            protected void onPostExecute(Void aVoid) {
//                                 try { setFragmentByChatId(extras.getString("idSE"), "exchange"); }
//                                 catch (Exception e) { e.printStackTrace(); }
//                                 super.onPostExecute(aVoid);
//                            }
//                        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

                        ReceiveACB.newInstance(new ACBInterface() {
                            @Override
                            public boolean urlFound() {
                                return mSEChatUrls.get(Integer.decode(extras.getString("idSE"))) != null;
                            }

                            @Override
                            public boolean fragmentFound() {
                                return mFragmentManager.findFragmentByTag(mSEChatUrls.get(Integer.decode(extras.getString("idSE")))) != null;
                            }

                            @Override
                            public void onFinish() {
                                try { setFragmentByChatId(extras.getString("idSE"), "exchange"); }
                                catch (Exception e) { e.printStackTrace(); }
                                if (mCurrentUsers_SlidingMenu.isMenuShowing()) mCurrentUsers_SlidingMenu.toggle();
                            }
                        }, "idSE").executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

                    } else if (extras.containsKey("idSO")) {
                        mSOChatIDs.add(extras.getString("idSO"));
                        mEditor.putStringSet("SOChatIDs", mSOChatIDs).apply();
                        doFragmentStuff();
//                        new AsyncTask<Void, Void, Void>() {
//                            @Override
//                            protected Void doInBackground(Void... voids) {
//                                while (mSOChatUrls.get(Integer.decode(extras.getString("idSO"))) == null);
//                                while (mFragmentManager.findFragmentByTag(mSOChatUrls.get(Integer.decode(extras.getString("idSO")))) == null);
//                                return null;
//                            }
//
//                            @Override
//                            protected void onPostExecute(Void aVoid) {
//                                 try { setFragmentByChatId(extras.getString("idSO"), "overflow"); }
//                                 catch (Exception e) { e.printStackTrace(); }
//                                 super.onPostExecute(aVoid);
//                            }
//                        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                        ReceiveACB.newInstance(new ACBInterface() {
                            @Override
                            public boolean urlFound() {
                                return mSOChatUrls.get(Integer.decode(extras.getString("idSO"))) != null;
                            }

                            @Override
                            public boolean fragmentFound() {
                                return mFragmentManager.findFragmentByTag(mSOChatUrls.get(Integer.decode(extras.getString("idSO")))) != null;
                            }

                            @Override
                            public void onFinish() {
                                try { setFragmentByChatId(extras.getString("idSO"), "overflow"); }
                                catch (Exception e) { e.printStackTrace(); }
                                if (mCurrentUsers_SlidingMenu.isMenuShowing()) mCurrentUsers_SlidingMenu.toggle();
                            }
                        }, "idSO").executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    }
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("idAdd");

        LocalBroadcastManager.getInstance(this).registerReceiver(mAddChatReceiver, intentFilter);
    }

    static private class ReceiveACB extends AsyncTask<Void, Void, Void> {
        ACBInterface mInterface;
        String mKey;

        static ReceiveACB newInstance(ACBInterface acbInterface, String key) {
            return new ReceiveACB(acbInterface, key);
        }

        ReceiveACB(ACBInterface acbInterface, String key) {
            mInterface = acbInterface;
            mKey = key;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            while(!mInterface.urlFound());
            while(!mInterface.fragmentFound());
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            mInterface.onFinish();
            super.onPostExecute(aVoid);
        }
    }

    private interface ACBInterface {
        boolean urlFound();
        boolean fragmentFound();
        void onFinish();
    }

    /**
     * If Firebase notification comes with data, and that data is room info, open the room if added
     */

    private void respondToNotificationClick() {
        if (getIntent().getExtras() != null) {
            Log.e("NOTIF", "NOTIF");
            final String chatId = mIntent.getExtras().getString("chatId");
            final String chatDomain = mIntent.getExtras().getString("chatDomain");

            if (chatId != null && chatDomain != null) {
                NotificationHandler.newInstance(new NHInterface() {
                    @Override
                    public boolean seContainsId() {
                        return mSEChatUrls.get(Integer.decode(chatId)) != null;
                    }

                    @Override
                    public boolean soContainsId() {
                        return mSOChatUrls.get(Integer.decode(chatId)) != null;
                    }

                    @Override
                    public void onFinish() {
                        setFragmentByChatId(chatId, chatDomain);
                    }
                }, chatDomain).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        }
    }

    private static class NotificationHandler extends AsyncTask<Void, Void, Void> {
        NHInterface mInterface;
        String mKey;

        static NotificationHandler newInstance(NHInterface nhInterface, String key) {
            return new NotificationHandler(nhInterface, key);
        }

        NotificationHandler(NHInterface nhInterface, String key) {
            mInterface = nhInterface;
            mKey = key;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            if (mKey.contains("overflow")) while (!mInterface.soContainsId());
            else if (mKey.contains("exchange")) while (!mInterface.seContainsId());
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            mInterface.onFinish();
            super.onPostExecute(aVoid);
        }
    }

    private interface NHInterface {
        boolean seContainsId();
        boolean soContainsId();
        void onFinish();
    }

    /**
     * Needed to convert some SparseArrays to ArrayLists
     * @param sparseArray the SparseArray to be converted
     * @param <C> dummy class for compatibility or something
     * @return returns the resulting ArrayList
     */

    private static <C> List<C> asList(SparseArray<C> sparseArray) {
        if (sparseArray == null) return null;
        List<C> arrayList = new ArrayList<>(sparseArray.size());
        for (int i = 0; i < sparseArray.size(); i++)
            arrayList.add(sparseArray.valueAt(i));
        return arrayList;
    }

    /**
     * Same as {@link MainActivity#asList(SparseArray)} but for SparseIntArray
     * @param sparseIntArray Array to be converted
     * @return returns resulting ArrayList
     */

    private static List<Integer> sparseIntArrayAsList(SparseIntArray sparseIntArray) {
        if (sparseIntArray == null) return null;
        List<Integer> arrayList = new ArrayList<>(sparseIntArray.size());
        for (int i = 0; i < sparseIntArray.size(); i++)
            arrayList.add(sparseIntArray.valueAt(i));
        return arrayList;
    }

    /*
     * Setup fragments
     */

    /**
     * Instantiate fragments and add them to {@link MainActivity#mChatroomSlidingMenu}
     */

    private void doFragmentStuff() {
        resetArrays(false);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.loading_progress).setVisibility(View.VISIBLE);
            }
        });
//        Looper.prepare();
        Log.e("IDS", mSEChatIDs.toString());

        for (String s : mSEChatIDs) {
            Log.e("ID", s);
            final String chatUrl = "https://chat.stackexchange.com/rooms/".concat(s);
            final String id = s;
            mRequestFactory.get(chatUrl, true, new RequestFactory.Listener() {
                @Override
                public void onSucceeded(final URL url, String data) {
                    mSEChatUrls.put(Integer.decode(id), chatUrl);
                    mAddList = AddList.newInstance(mSharedPrefs, data, id, chatUrl, new AddListListener() {
                        @Override
                        public void onStart() {

                        }

                        @Override
                        public void onProgress(String name, Drawable icon, Integer color) {
                            Fragment fragment = addFragment(chatUrl, name, color, Integer.decode(id));
                            Log.e("RRR", fragment.getArguments().getString("chatUrl", "").concat("HUE"));
                            mSEChats.put(Integer.decode(id), fragment);
                            mSEChatColors.put(Integer.decode(id), color);
                            mSEChatIcons.put(Integer.decode(id), icon);
                            mSEChatNames.put(Integer.decode(id), name);
                        }

                        @Override
                        public void onFinish() {
                            ArrayList<String> names = new ArrayList<>();
                            names.addAll(asList(mSEChatNames));
                            names.addAll(asList(mSOChatNames));

                            ArrayList<String> urls = new ArrayList<>();
                            urls.addAll(asList(mSEChatUrls));
                            urls.addAll(asList(mSOChatUrls));

                            ArrayList<Drawable> icons = new ArrayList<>();
                            icons.addAll(asList(mSEChatIcons));
                            icons.addAll(asList(mSOChatIcons));

                            ArrayList<Integer> colors = new ArrayList<>();
                            colors.addAll(sparseIntArrayAsList(mSEChatColors));
                            colors.addAll(sparseIntArrayAsList(mSOChatColors));

                            ArrayList<Fragment> fragments = new ArrayList<>();
                            fragments.addAll(asList(mSEChats));
                            fragments.addAll(asList(mSOChats));

                            addFragmentsToList(names, urls, icons, colors, fragments);
                            initiateCurrentFragments(fragments);
                        }
                    });

                    mAddList.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }

                @Override
                public void onFailed(String message) {
                    Toast.makeText(MainActivity.this, "Failed to load chat ".concat(id).concat(": ").concat(message), Toast.LENGTH_LONG).show();
                    mSEChatIDs.remove(id);
                    mEditor.putStringSet("SEChatIDs", mSEChatIDs).apply();
                    Log.e("Couldn't load SE chat ".concat(id), message);
                }
            });
        }

        for (String s : mSOChatIDs) {
            final String chatUrl = "https://chat.stackoverflow.com/rooms/".concat(s);
            final String id = s;
            mRequestFactory.get(chatUrl, true, new RequestFactory.Listener() {
                @Override
                public void onSucceeded(final URL url, String data) {
                    mSOChatUrls.put(Integer.decode(id), chatUrl);
                    AddList addList = AddList.newInstance(mSharedPrefs, data, id, chatUrl, new AddListListener() {
                        @Override
                        public void onStart() {
                        }

                        @Override
                        public void onProgress(String name, Drawable icon, Integer color) {
                            Fragment fragment = addFragment(chatUrl, name, color, Integer.decode(id));
                            mSOChats.put(Integer.decode(id), fragment);
                            mSOChatColors.put(Integer.decode(id), color);
                            mSOChatIcons.put(Integer.decode(id), icon);
                            mSOChatNames.put(Integer.decode(id), name);
                        }

                        @Override
                        public void onFinish() {
                            ArrayList<String> names = new ArrayList<>();
                            names.addAll(asList(mSEChatNames));
                            names.addAll(asList(mSOChatNames));

                            ArrayList<String> urls = new ArrayList<>();
                            urls.addAll(asList(mSEChatUrls));
                            urls.addAll(asList(mSOChatUrls));

                            ArrayList<Drawable> icons = new ArrayList<>();
                            icons.addAll(asList(mSEChatIcons));
                            icons.addAll(asList(mSOChatIcons));

                            ArrayList<Integer> colors = new ArrayList<>();
                            colors.addAll(sparseIntArrayAsList(mSEChatColors));
                            colors.addAll(sparseIntArrayAsList(mSOChatColors));

                            ArrayList<Fragment> fragments = new ArrayList<>();
                            fragments.addAll(asList(mSEChats));
                            fragments.addAll(asList(mSOChats));

                            addFragmentsToList(names, urls, icons, colors, fragments);
                            initiateCurrentFragments(fragments);
                        }
                    });

                    addList.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }

                @Override
                public void onFailed(String message) {
                    Toast.makeText(MainActivity.this, "Failed to load chat ".concat(id), Toast.LENGTH_SHORT).show();
                    mSOChatIDs.remove(id);
                    mEditor.putStringSet("SOChatIDs", mSOChatIDs).apply();
                    Log.e("Couldn't load SO chat ".concat(id), message);
                }
            });
        }

        if (mSEChatIDs.size() == 0 && mSOChatIDs.size() == 0) {
            removeAllFragmentsFromList();
            findViewById(R.id.loading_progress).setVisibility(View.GONE);
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(350);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                while (chatroomsList == null);
                while (chatroomsList.getCount() <  mSEChatIDs.size() + mSOChatIDs.size()) {
//                    Log.e("ChildSize", chatroomsList.getCount() + "");
//                    Log.e("ChatIDSize", mSEChatIDs.size() + mSOChatIDs.size() + "");
//                    try {
//                        Thread.sleep(350);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        findViewById(R.id.loading_progress).setVisibility(View.GONE);
                        Log.e("VIS", "GONE");
                    }
                });
            }
        }).start();
//        mAddListItemsFromURLList = AddListItemsFromURLList.newInstance(new AddItemsListener() {
//            @Override
//            public void onStart() {
//                mCanAddChat = false;
//            }
//
//            @Override
//            public void onProgressMade(String url, ArrayList<String> names, ArrayList<String> urls, ArrayList<Drawable> icons, ArrayList<Integer> colors, ArrayList<Fragment> fragments) {
//                fragments = addTab(url, names, urls, icons, colors, fragments);
//                if (fragments.size() > 0) {
//                    initiateCurrentFragments(fragments);
//                    addFragmentsToList(names, urls, icons, colors, fragments);
//                } else {
//                    removeAllFragmentsFromList();
//                }
//            }
//
//            @Override
//            public void onFinished() {
//                mCanAddChat = true;
//                findViewById(R.id.loading_progress).setVisibility(View.GONE);
//            }
//        });
//        mAddListItemsFromURLList.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mChatUrls);
    }

    /**
     * Handles the actual data parsing for chats
     * (in the background to avoid ANRs)
     */

    private static class AddList extends AsyncTask<String, Void, Void> {
        private String mHtmlData;
        private String mChatId;
        private String mChatUrl;
        private AddListListener mAddListListener;
        private SharedPreferences mSharedPreferences;
        private String mName;
        private Drawable mIcon;
        private Integer mColor;

        static AddList newInstance(SharedPreferences sharedPreferences, String data, String id, String url, AddListListener addListListener) {
            return new AddList(sharedPreferences, data, id, url, addListListener);
        }

        AddList(SharedPreferences sharedPreferences, String data, String id, String url, AddListListener addListListener) {
            mSharedPreferences = sharedPreferences;
            mHtmlData = data;
            mChatId = id;
            mChatUrl = url;
            mAddListListener = addListListener;
        }

        @Override
        protected Void doInBackground(String... strings) {
            mAddListListener.onStart();
            mName = getName(mHtmlData, mChatUrl);
            mIcon = getIcon(mHtmlData, mChatUrl);
            mColor = new Utils().getColorInt(mainActivity, mChatUrl);

            publishProgress();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            mAddListListener.onFinish();
            super.onPostExecute(aVoid);
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            mAddListListener.onProgress(mName, mIcon, mColor);
            super.onProgressUpdate(values);
        }

        @Nullable
        private String getName(String html, String url) {
            try {
                Elements spans = Jsoup.parse(html).select("span");

                for (Element e : spans) {
                    if (e.hasAttr("id") && e.attr("id").equals("roomname")) {
                        mSharedPreferences.edit().putString(url + "Name", e.ownText()).apply();
                        return e.ownText();
                    }
                }
                String ret = Jsoup.connect(url).get().title().replace("<title>", "").replace("</title>", "").replace(" | chat.stackexchange.com", "").replace(" | chat.stackoverflow.com", "");
                mSharedPreferences.edit().putString(url + "Name", ret).apply();
                return ret;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Nullable
        private Drawable getIcon(String html, String chatUrl) {
            try {
                Document document = Jsoup.parse(html);
                Element head = document.head();
                Element link = head.select("link").first();

                String fav = link.attr("href");
                if (!fav.contains("http")) fav = "https:".concat(fav);
                URL url = new URL(fav);

                Bitmap bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());

                String FILENAME = "FAVICON_" + chatUrl.replace("/", "");
                FileOutputStream fos = mainActivity.openFileOutput(FILENAME, Context.MODE_PRIVATE);
                bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.close();

                Resources r = mainActivity.getResources();
                int px = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, r.getDisplayMetrics());

                return new BitmapDrawable(Resources.getSystem(), Bitmap.createScaledBitmap(bmp, px, px, true));

            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    private interface AddListListener {
        void onStart();
        void onProgress(String name, Drawable icon, Integer color);
        void onFinish();
    }

    /**
     * Setup current users {@link SlidingMenu}
     */

    private void createUsersSlidingMenu()
    {
        // configure the SlidingMenu
        mCurrentUsers_SlidingMenu = new SlidingMenu(MainActivity.this);
        mCurrentUsers_SlidingMenu.setMode(SlidingMenu.RIGHT);
        //mCurrentUsers_SlidingMenu.setTouchModeAbove(SlidingMenu.TOUCHMODE_MARGIN);
        mCurrentUsers_SlidingMenu.setShadowWidthRes(R.dimen.shadow_width);
        mCurrentUsers_SlidingMenu.setShadowDrawable(new ColorDrawable(getResources().getColor(R.color.transparentGrey)));
        mCurrentUsers_SlidingMenu.setBehindWidthRes(R.dimen.sliding_menu_width);
        mCurrentUsers_SlidingMenu.setFadeDegree(0.35f);
        mCurrentUsers_SlidingMenu.attachToActivity(MainActivity.this, SlidingMenu.SLIDING_CONTENT);
        mCurrentUsers_SlidingMenu.setMenu(R.layout.users_slideout);
        mCurrentUsers_SlidingMenu.setSecondaryOnOpenListner(new SlidingMenu.OnOpenListener() {
            @Override
            public void onOpen()
            {
                if (getmChatroomSlidingMenu().isMenuShowing())
                {
                    getmChatroomSlidingMenu().showContent(true);
                }
            }
        });
        mCurrentUsers_SlidingMenu.setTouchModeAbove(SlidingMenu.TOUCHMODE_NONE);
    }

    /**
     * Get the sliding menu from another class
     * @return returns the current users {@link SlidingMenu}
     */

    public SlidingMenu getCurrentUsers_SlidingMenu()
    {
        return mCurrentUsers_SlidingMenu;
    }

    /*
     * Menu
     */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_settings:
                Intent prefIntent = new Intent(this, PreferencesActivity.class);
                int requestCode = 1; // Or some number you choose
                startActivityForResult(prefIntent, requestCode);
                break;
            case R.id.action_about:
                Intent intent = new Intent(getApplicationContext(), AboutActivity.class);
                startActivity(intent);
                break;
            case R.id.action_help:
                Intent startHelpActivity = new Intent(getApplicationContext(), HelpActivity.class);
                startActivity(startHelpActivity);
                break;
            case R.id.action_browser:
                Intent browserIntent;
                if (mCurrentFragment == null || mCurrentFragment.equals("home")) {
                    WebView webView = findViewById(R.id.stars_view);
                    String url = webView.getUrl();
                    browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                } else {
                    browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(mCurrentFragment));
                }
                startActivity(browserIntent);
                break;
            default:
//                setFragmentByTag("home");
//                for (Fragment fragment : mFragmentManager.getFragments()) {
//                    if (fragment != null && !fragment.isDetached() && fragment instanceof ChatFragment) if (((ChatFragment) fragment).getmSlidingMenu().isMenuShowing()) ((ChatFragment) fragment).getmSlidingMenu().showContent(true);
//                }
//                if (mChatroomSlidingMenu.isMenuShowing()) mChatroomSlidingMenu.showContent(true);
//                mChatroomSlidingMenu.toggle();
//                drawable.start();
//                ((AnimatedVectorDrawableCompat)item.getIcon()).start();
//                onSupportNavigateUp();

                //LOOK UNDER onCreate() for Drawer Toggle!
                break;
        }

        return true;
    }

    /*
     * Fragment Stuffs
     */

    /**
     * Add specified fragments to the {@link FragmentManager}
     * @param fragments list of Fragments to be added
     */

    private void initiateCurrentFragments(ArrayList<Fragment> fragments) {
        for (int i = 0; i < fragments.size(); i++) {
            try {
                Fragment fragment = fragments.get(i);
                String tag = fragment.getArguments().getString("chatUrl");
                if (mFragmentManager.findFragmentByTag(tag) == null) {
                    mFragmentManager.beginTransaction().add(R.id.content_main, fragment, tag).detach(fragment).commit();
                }

                if ((mCurrentFragment == null || mCurrentFragment.equals("home")) && mFragmentManager.findFragmentByTag("home") == null) {
                    mFragmentManager.beginTransaction().add(R.id.content_main, new HomeFragment(), "home").commit();
                    //hueUtils.setActionBarColorToSharedPrefsValue(this);
//                    hueUtils.setAddChatFabColorDefault(this);
                }

                mFragmentManager.executePendingTransactions();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Add fragments to the ListView/SlidingMenu
     * @param chatroomNames list of chat names
     * @param chatUrls list of corresponding chat URLs
     * @param chatIcons list of corresponding chat favicons
     * @param chatColors list of corresponding chat accent colors
     * @param chatFragments list of chat fragments (TODO: remove this variable?)
     */

    private void addFragmentsToList(ArrayList<String> chatroomNames,
                                    ArrayList<String> chatUrls,
                                    ArrayList<Drawable> chatIcons,
                                    ArrayList<Integer> chatColors,
                                    ArrayList<Fragment> chatFragments) {

        String[] names = new String[chatroomNames.size()];
        names = chatroomNames.toArray(names);

        String[] urls = new String[chatUrls.size()];
        urls = chatUrls.toArray(urls);

        Drawable[] ico = new Drawable[chatIcons.size()];
        ico = chatIcons.toArray(ico);

        Integer[] colors = new Integer[chatColors.size()];
        colors = chatColors.toArray(colors);

        chatroomArrayAdapter = new ImgTextArrayAdapter(this, names, urls, ico, colors);
        if (names.length < 1) chatroomArrayAdapter.clear();
//        Log.e("LE", names.length + "");

        chatroomsList = findViewById(R.id.chatroomsListView);
        chatroomsList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        // Here, you set the data in your ListView
        chatroomsList.setAdapter(chatroomArrayAdapter);

        chatroomsList.setOnItemClickListener(mItemClickListener);

        chatroomsList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener()
        {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id)
            {
//                chatroomsList.requestFocusFromTouch();
                chatroomsList.setOnItemClickListener(null);
                chatroomsList.setSelection(position);
//                chatroomsList.requestFocus();

                mCurrentFragment = chatroomArrayAdapter.getUrls()[position];

                confirmClose(view);
                return true;
            }
        });
    }

    /**
     * Might be useful for a batch removal later, but right now, it just enables removal of the only chat added
     */

    private void removeAllFragmentsFromList() {
        if (chatroomsList != null) chatroomsList.setAdapter(null);
        resetArrays(true);
    }

    /**
     * Open a chat by the specified ID
     * @param id the ID of the desired chat
     * @param domain the domain of the desired chat ("exchange" or "overflow")
     */

    private void setFragmentByChatId(String id, String domain) {
//        for (String url : mChatUrls) {
//            if (url.contains(domain) && url.contains(id)) {
//                setFragmentByTag(url);
//                break;
//            }
//        }

        Log.e("SETID", id.concat(domain));

        if (domain.contains("exchange")) {
            if (mSEChatUrls.get(Integer.decode(id)) != null) setFragmentByTag(mSEChatUrls.get(Integer.decode(id)));
            else Toast.makeText(this, "Chat not added", Toast.LENGTH_SHORT).show();
        } else if (domain.contains("overflow")) {
            if (mSOChatUrls.get(Integer.decode(id)) != null) setFragmentByTag(mSOChatUrls.get(Integer.decode(id)));
            else Toast.makeText(this, "Chat not added", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Open a chat using its tag
     * @param tag the chat's fragment tag (should be its URL)
     */

    private void setFragmentByTag(String tag)
    {
        Log.e("TAG", tag);
        if (mFragmentManager.getFragments() != null)
        {
            for (Fragment fragment : mFragmentManager.getFragments())
            {
                if (fragment != null && !fragment.isDetached())
                {
                    mFragmentManager.beginTransaction().detach(fragment).commit();
                }
            }
            Fragment fragToAttach = mFragmentManager.findFragmentByTag(tag);

            if (fragToAttach != null)
            {

                if (tag.equals("home"))
                {
                    mFragmentManager.beginTransaction().setCustomAnimations(R.anim.fade_in, R.anim.fade_out).attach(fragToAttach).commit();
                    //noinspection ConstantConditions
//                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//                hueUtils.showAddChatFab(this, true);
                    //hueUtils.setAddChatFabColorToSharedPrefsValue(this);
//                hueUtils.setActionBarColorDefault(this);
                    mCurrentUsers_SlidingMenu.setTouchModeAbove(SlidingMenu.TOUCHMODE_NONE);
                    ((HomeFragment) fragToAttach).hueTest();
                } else
                {
                    if (mFragmentManager.findFragmentByTag("home").isDetached())
                    {
                        mFragmentManager.beginTransaction().attach(fragToAttach).commit();
                    } else
                    {
                        mFragmentManager.beginTransaction().setCustomAnimations(R.anim.fade_in, R.anim.fade_out).attach(fragToAttach).commit();
                    }
                    mCurrentUsers_SlidingMenu.setTouchModeAbove(SlidingMenu.TOUCHMODE_MARGIN);
                    //noinspection ConstantConditions
//                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//                VectorDrawableCompat drawable = VectorDrawableCompat.create(getResources(), R.drawable.ic_menu_black_24dp, null);
//                drawable.setTintList(ColorStateList.valueOf(Color.rgb(255, 255, 255)));
//                getSupportActionBar().setHomeAsUpIndicator(drawable);
//                hueUtils.showAddChatFab(this, falzse);
                    ((ChatFragment) fragToAttach).hueTest();
                }
            } else
            {
                Log.e("TAG", tag);
            }

        }
    }

    /*
     * Other Stuffs
     */

    /**
     * Handle adding chats
     * TODO: remove add-by-URL option
     */

    private void showAddTabDialog() {
//        if (/*mCanAddChat*/true) { //TODO: Experiment with adding a chat while chats are loading, then fix this
//
//
//        } else {
//            Toast.makeText(this, getResources().getText(R.string.cant_add_chat), Toast.LENGTH_LONG).show();
//        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getText(R.string.activity_main_add_chat));

        View view = View.inflate(this, R.layout.add_chat_dialog, null);
        final EditText input = view.findViewById(R.id.url_edittext);

        final Spinner domains = view.findViewById(R.id.domain_spinner);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.domain_spinner, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        domains.setAdapter(adapter);

        domains.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                input.setHint(getResources().getText(R.string.activity_main_chat_url_hint));
                input.setInputType(InputType.TYPE_CLASS_NUMBER);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        builder.setView(view);
        builder.setPositiveButton(getResources().getText(R.string.generic_ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                String inputText = input.getText().toString();
                if (!inputText.isEmpty()) {
//                    String url;
                    if (mAddList != null && !mAddList.getStatus().equals(AsyncTask.Status.FINISHED)) {
                        mAddList.cancel(true);
                    }

                    if (domains.getSelectedItem().toString().equals(getResources().getText(R.string.stackoverflow).toString())) {
//                        url = getResources().getText(R.string.stackoverflow).toString().concat("rooms/").concat(inputText);
                        mSOChatIDs.add(inputText);
                    } else //noinspection StatementWithEmptyBody
                        if (domains.getSelectedItem().toString().equals(getResources().getText(R.string.stackexchange).toString())) {
//                        url = getResources().getText(R.string.stackexchange).toString().concat("rooms/").concat(inputText);
                            mSEChatIDs.add(inputText);
                        } else {
//                        url = inputText;
                        }

                    mEditor.putStringSet("SOChatIDs", mSOChatIDs).apply();
                    mEditor.putStringSet("SEChatIDs", mSEChatIDs).apply();

//                    mChatUrls.add(url);
//                    mEditor.putStringSet(CHAT_URLS_KEY, mChatUrls);
//                    mEditor.apply();
//                    Log.e("URLSA", mChatUrls.toString());
                    doFragmentStuff();
                } else {
                    Toast.makeText(getBaseContext(), "Please enter an ID", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton(getResources().getText(R.string.generic_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        AlertDialog al = builder.create();
        al.show();
    }

    /**
     * Handle removing a chat
     * @param v the view that called this method
     */

    public void confirmClose(View v) {
        if (chatroomsList.getSelectedItemPosition() != 0) {
            Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            // Vibrate for 500 milliseconds
            vib.vibrate(100);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle(getResources().getText(R.string.activity_main_delete_chat_title))
                            .setMessage(getResources().getText(R.string.activity_main_delete_chat_message))
                            .setPositiveButton(getResources().getText(R.string.generic_yes), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String domain = "";
                                    String id = "";

                                    Pattern domP = Pattern.compile("//(.+?)\\.com");
                                    Matcher domM = domP.matcher(mCurrentFragment);

                                    while (!domM.hitEnd()) {
                                        if (domM.find()) {
                                            domain = domM.group();
                                        }
                                    }

                                    Pattern idP = Pattern.compile("rooms/(.+?)\\b");
                                    Matcher idM = idP.matcher(mCurrentFragment);

                                    while (!idM.hitEnd()) {
                                        if (idM.find()) {
                                            id = idM.group().replace("rooms/", "");
                                        }
                                    }

                                    Log.e("IDDDDD", id);
                                    Log.e("DOMAIN", domain);

                                    if (!domain.isEmpty() && !id.isEmpty()) {
                                        if (domain.contains("overflow")) {
                                            mSOChatIDs.remove(id);
                                        } else if (domain.contains("exchange")) {
                                            mSEChatIDs.remove(id);
                                        }

                                        mFragmentManager.getFragments().remove(mFragmentManager.findFragmentByTag(mCurrentFragment));

                                        mEditor.putStringSet("SOChatIDs", mSOChatIDs).apply();
                                        mEditor.putStringSet("SEChatIDs", mSEChatIDs).apply();

                                        setFragmentByTag("home");
                                        doFragmentStuff();
                                    }

//                                    mChatUrls.remove(remFrag.getTag());
//                                    Log.e("TAG", remFrag.getTag());
//                                    mEditor.putStringSet(CHAT_URLS_KEY, mChatUrls);
//                                    mEditor.apply();
//                                    Log.e("URLSR", mChatUrls.toString());
                                }
                            })
                            .setNegativeButton(getResources().getText(R.string.generic_no), null)
                            .setOnDismissListener(new DialogInterface.OnDismissListener()
                            {
                                @Override
                                public void onDismiss(DialogInterface dialogInterface)
                                {
                                    chatroomsList.setOnItemClickListener(mItemClickListener);
                                }
                            })
                            .show();
                }
            });
        }
    }

    /**
     * Get the chatroom list {@link SlidingMenu} instance from other classes
     * @return returns the chatroom SlidingMenu
     */

    private SlidingMenu getmChatroomSlidingMenu() {
        return mChatroomSlidingMenu;
    }

    /**
     * Instantiate/create the appropriate chat fragment, if necessary
     * @param url URL of chat
     * @param name Name of chat
     * @param color Accent color of chat
     * @param id ID of chat
     * @return the created Fragment
     */

    private Fragment addFragment(String url, String name, Integer color, Integer id) {
        Fragment fragment;
        if (mFragmentManager.findFragmentByTag(url) != null) {
            fragment = mFragmentManager.findFragmentByTag(url);
        } else {
            fragment = new ChatFragment();
            Bundle args = new Bundle();
            args.putString("chatTitle", name);
            args.putString("chatUrl", url);
            args.putInt("chatColor", color);
            args.putInt("chatId", id);

            fragment.setArguments(args);
        }

        return fragment;
    }

    /**
     * Handle user press of Home button in ActionBar
     * @return true
     */

    @Override
    public boolean onSupportNavigateUp()
    {
//        mHandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                setFragmentByTag("home");
//            }
//        }, 400);
        mChatroomSlidingMenu.toggle();
        return true;
    }

    /**
     * Open or close chatroom list
     * @param v The View calling the method
     */

    public void toggleChatsSlide(View v) {
        mChatroomSlidingMenu.toggle();
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mAddChatReceiver);
        super.onDestroy();
    }

    /**
     * Empty all specified arrays related to chats
     * @param shouldEmptyIDs should the ID Set be emptied?
     */

    private void resetArrays(boolean shouldEmptyIDs) {
        if (shouldEmptyIDs) {
            mSEChatIDs = new HashSet<>(0);
            mSOChatIDs = new HashSet<>(0);
            mEditor.putStringSet("SEChatIDs", mSEChatIDs).apply();
            mEditor.putStringSet("SOChatIDs", mSOChatIDs).apply();
        }

        mSEChatUrls = new SparseArray<>();
        mSOChatUrls = new SparseArray<>();
        mSEChats = new SparseArray<>();
        mSOChats = new SparseArray<>();
        mSEChatNames = new SparseArray<>();
        mSOChatNames = new SparseArray<>();
        mSEChatIcons = new SparseArray<>();
        mSOChatIcons = new SparseArray<>();
        mSEChatColors = new SparseIntArray();
        mSOChatColors = new SparseIntArray();
    }

    /**
     * Removes all chats on confirmation
     * @param v the view calling this function
     */

    public void removeAllChats(View v)
    {
        final FloatingActionMenu fam = findViewById(R.id.chat_slide_menu);
        fam.close(true);

        new AlertDialog.Builder(this)
                .setTitle("Are you sure?")
                .setMessage("Are you sure you want to remove all chats?")
                .setPositiveButton(getResources().getText(R.string.generic_yes), new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)
                    {
                        removeAllFragmentsFromList();
                    }
                })
                .setNegativeButton(getResources().getText(R.string.generic_no), null)
                .show();
    }

    /**
     * Get cookies from other classes
     * @return the authToken/Cookie string of the current account
     */

    public String getCookieString() {
        return mCookieString;
    }
}
