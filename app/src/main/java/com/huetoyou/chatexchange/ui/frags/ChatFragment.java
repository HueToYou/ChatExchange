package com.huetoyou.chatexchange.ui.frags;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.ColorInt;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import com.huetoyou.chatexchange.R;
import com.huetoyou.chatexchange.net.RequestFactory;
import com.huetoyou.chatexchange.ui.activity.MainActivity;
import com.huetoyou.chatexchange.ui.activity.WebViewActivity;
import com.huetoyou.chatexchange.ui.misc.hue.ActionBarHue;
import com.huetoyou.chatexchange.ui.misc.hue.ChatFragFabsHue;
import com.huetoyou.chatexchange.ui.misc.hue.OtherFabsHue;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatFragment extends Fragment
{

    private SharedPreferences mSharedPreferences;
    private View view;
    private boolean oncreateHasBeenCalled = false;

    private @ColorInt int mAppBarColor;
    private SlidingMenu mSlidingMenu;

    private EditText messageToSend;
    private HorizontalScrollView pingSuggestionsScrollView;

    private ActionBarHue actionBarHue = null;
    private OtherFabsHue otherFabsHue = null;
    private ChatFragFabsHue chatFragFabsHue = null;

    private Spanned mChatDesc;
    private ArrayList<String> mChatTags = new ArrayList<>();
    private Spanned mChatTagsSpanned;
    private String mChatUrl;
    private ArrayList<Bundle> mUserInfo = new ArrayList<>();
    private ArrayList<UserTileFragment> mUserTiles = new ArrayList<>();
    //    private Spanned mStarsSpanned;

    public static final String USER_NAME_KEY = "userName";
    private static final String USER_AVATAR_URL_KEY = "userAvatarUrl";
    private static final String USER_URL_KEY = "chatUrl";
    private static final String USER_ID_KEY = "id";
    private static final String USER_LAST_POST_KEY = "lastPost";
    private static final String USER_REP_KEY = "rep";
    private static final String USER_IS_MOD_KEY = "isMod";
    private static final String USER_IS_OWNER_KEY = "isOwner";
    private static final String CHAT_HOST_DOMAIN = "hostDomain";

    private FragmentManager mFragmentManager;
    private EditText mMessage;

    private RequestFactory mRequestFactory;
    private String mChatTitle;
    private String mChatDomain;
    private Integer mChatId;

    public ChatFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_chat, container, false);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mRequestFactory = new RequestFactory();

        messageToSend = view.findViewById(R.id.messageToSend);
        pingSuggestionsScrollView = view.findViewById(R.id.pingSuggestionsScrollView);

        messageToSend.setOnFocusChangeListener(new View.OnFocusChangeListener()
        {
            @Override
            public void onFocusChange(View v, boolean hasFocus)
            {
                if(hasFocus)
                {
                    /*Toast.makeText(getActivity(), "Got focus",
                    Toast.LENGTH_LONG).show();*/
                    pingSuggestionsScrollView.setVisibility(View.VISIBLE);
                }
            }
        });

        mFragmentManager = getFragmentManager();

        actionBarHue = new ActionBarHue();
        otherFabsHue = new OtherFabsHue();
        chatFragFabsHue = new ChatFragFabsHue();

        mSlidingMenu = ((MainActivity)getActivity()).getCurrentUsers_SlidingMenu();

        Bundle args = getArguments();
        mChatUrl = args.getString("chatUrl", "ERROR");
        mChatTitle = args.getString("chatTitle", "ERROR");
        mChatId = args.getInt("chatId", -1);

        mAppBarColor = args.getInt("chatColor", -1);

        addChatButtons(mChatUrl);

        mRequestFactory.get(mChatUrl, true, new RequestFactory.Listener() {
            @Override
            public void onSucceeded(URL url, String data) {
                GetDesc getDesc = GetDesc.newInstance(new DescGotten() {
                    @Override
                    public void onSuccess(String desc) {
                        mChatDesc = Html.fromHtml("<b>Desc: </b>" + desc);
                    }

                    @Override
                    public void onFail(String message) {

                    }
                });

                getDesc.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, data);

                GetTags getTags = GetTags.newInstance(new TagsGotten() {
                    @Override
                    public void onSuccess(ArrayList<String> tabList) {
                        mChatTags = tabList;

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                String tags = "";
                                if (mChatTags != null) {
                                    tags = mChatTags.toString();
                                    tags = tags.replace("[", "").replace("]", "");
                                }

                                mChatTagsSpanned = Html.fromHtml("<b>Tags: </b>" + tags);
                            }
                        }).start();
                    }

                    @Override
                    public void onFail(String message) {

                    }
                });
                getTags.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, data);

                ParseUsers parseUsers = ParseUsers.newInstance(new UserParsed() {
                    @Override
                    public void onSuccess(String usersJson) {
                        try {
//                            Log.e("TTTT", usersJson.substring(2058));
                            JSONObject object = new JSONObject(usersJson);
                            JSONArray jArray = object.getJSONArray("users");

                            for (int i = 0; i < jArray.length(); i++)
                            {
                                JSONObject jsonObject = jArray.getJSONObject(i);

                                int id = jsonObject.getInt("id");
                                int lastPost = jsonObject.getInt("last_post");
                                int rep = jsonObject.getInt("reputation");

                                boolean isMod = jsonObject.has("is_moderator") && jsonObject.getBoolean("is_moderator");
                                boolean isOwner = jsonObject.has("is_owner") && jsonObject.getBoolean("is_owner");

                                String name = jsonObject.getString("name");
                                String icon = jsonObject.getString("email_hash");

                                if (!(icon.contains("http://") || icon.contains("https://"))) icon = "https://www.gravatar.com/avatar/".concat(icon).concat("?d=identicon");

                                addUser(name, icon, id, lastPost, rep, isMod, isOwner, mChatUrl);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFail(String message) {

                    }
                });
                parseUsers.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, data);
            }

            @Override
            public void onFailed(String message) {

            }
        });

        getActivity().setTitle(mChatTitle);

        setupMessagePingList();
        setupMessages();

        mChatDomain = mSharedPreferences.getString(CHAT_HOST_DOMAIN.concat(mChatUrl), null);

        if (mChatDomain == null || mChatDomain.isEmpty()) {
            if (mChatUrl.contains("stackoverflow")) mChatDomain = "stackoverflow.com";
            else {
                mRequestFactory.get(mChatUrl, true, new RequestFactory.Listener() {
                    @Override
                    public void onSucceeded(URL url, String data) {
                        new GetHostDomainFromHtml(new DomainFoundListener() {
                            @Override
                            public void onSuccess(String text) {
                                mSharedPreferences.edit().putString(CHAT_HOST_DOMAIN.concat(mChatUrl), text).apply();
                                mChatDomain = text;
                            }

                            @Override
                            public void onFail(String text) {
                                mChatDomain = "Unknown";
                            }
                        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, data);
                    }

                    @Override
                    public void onFailed(String message) {
                        Log.e("WHOOPS", message);
                    }
                });
            }
        }

        oncreateHasBeenCalled = true;

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    private void setupMessages() {
        mRequestFactory.get(mChatUrl, true, new RequestFactory.Listener() {
            @Override
            public void onSucceeded(final URL url, final String data) {
                processMessageViews(url, data);

            }

            @Override
            public void onFailed(String message) {

            }
        });
    }

    private void hueAllTheThings()
    {
        if (mSharedPreferences.getBoolean("dynamicallyColorBar", false)) {
            actionBarHue.setActionBarColor((AppCompatActivity) getActivity(), mAppBarColor);
            chatFragFabsHue.setChatFragmentFabColor((AppCompatActivity) getActivity(), mAppBarColor);
            otherFabsHue.setAddChatFabColor((AppCompatActivity) getActivity(), mAppBarColor);
        }

        else
        {
            actionBarHue.setActionBarColorToSharedPrefsValue((AppCompatActivity) getActivity());
            chatFragFabsHue.setChatFragmentFabColorToSharedPrefsValue((AppCompatActivity) getActivity());
            otherFabsHue.setAddChatFabColorToSharedPrefsValue((AppCompatActivity) getActivity());
        }

        getActivity().setTitle(mChatTitle);
    }

    public void hueTest()
    {
        System.out.println("Hue");

        Thread thread = new Thread()
        {
            @Override
            public void run()
            {
                //noinspection StatementWithEmptyBody
                while(!oncreateHasBeenCalled);

                getActivity().runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        hueAllTheThings();
                    }
                });
            }
        };
        thread.start();
    }

    private void processMessageViews(URL url, String html) {
        Document document = Jsoup.parse(html);
        Elements elements = document.select("user-container");

        for (Element e : elements) {
            Elements link = e.select("a");
            Element signature = new Element("");

            for (Element e1: link) {
                if (e1.hasAttr("class") && e1.attr("class").equals("signature")) {
                    signature = e1;
                    break;
                }
            }

        }
    }

    private void setupMessagePingList() {
        mMessage = view.findViewById(R.id.messageToSend);

        mMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ArrayList<UsernameTilePingFragment> fragments = new ArrayList<>();

                if (s.toString().contains("@")) {

                    for (UserTileFragment tile : mUserTiles) {
                        Bundle args = tile.getArguments();
                        UsernameTilePingFragment pingFragment = UsernameTilePingFragment.newInstance(tile, new SetTabCompleteName() {
                            @Override
                            public void setName(UsernameTilePingFragment usernameTilePingFragment) {
                                setTabCompleteName(usernameTilePingFragment);
                            }
                        });
                        pingFragment.setArguments(args);
                        String name = args.getString(USER_NAME_KEY);
                        String currentName = s.toString();

                        Pattern p = Pattern.compile("\\B@(.+?)\\b");
                        Matcher m = p.matcher(currentName);

                        try {
                            while (!m.hitEnd()) {
                                if (m.find()) {
                                    currentName = m.group().replace("@", "");
//                                    Log.e("NAME", currentName);
                                }
                            }
                        } catch (IllegalStateException e) {
//                            e.printStackTrace()
                        }

                        assert name != null;
                        if (name.replace(" ", "").toLowerCase().startsWith(currentName.toLowerCase())) {
                            fragments.add(pingFragment);
                        }
                    }
                }
                mFragmentManager.beginTransaction().replace(R.id.pingSuggestions, new Fragment()).commit();

                for (UsernameTilePingFragment f : fragments) {
                    mFragmentManager.beginTransaction().add(R.id.pingSuggestions, f, "pingFrag").commit();
                }
            }
        });
    }

    private void setTabCompleteName(UsernameTilePingFragment usernameTilePingFragment) {
//        Toast.makeText(getActivity(), usernameTilePingFragment.getmUsername(), Toast.LENGTH_SHORT).show();
        String name = usernameTilePingFragment.getmUsername();
        name = name.replace(" ", "");
        String currentText = mMessage.getText().toString();

        Pattern p = Pattern.compile("\\B@(.+?)\\b");
        Matcher m = p.matcher(currentText);

        while (!m.hitEnd()) {
            if (m.find() && name.toLowerCase().contains(m.group().replace("@", "").toLowerCase())) {
                String before = currentText.substring(0, currentText.toLowerCase().lastIndexOf(m.group().toLowerCase()));
                String after = currentText.substring(currentText.toLowerCase().lastIndexOf(m.group().toLowerCase()) + m.group().length());
                String middle = "@" + name;

                mMessage.setText(before.concat(middle).concat(after));
                mMessage.setSelection(mMessage.getText().toString().length());
            }
        }
    }

    public interface SetTabCompleteName {
        void setName(UsernameTilePingFragment usernameTilePingFragment);
    }

    private static class ParseUsers extends AsyncTask<String, Void, String> {
        private UserParsed mUserParsed;

        static ParseUsers newInstance(UserParsed userParsed) {
            return new ParseUsers(userParsed);
        }

        ParseUsers(UserParsed userParsed) {
            mUserParsed = userParsed;
        }

        @Override
        protected String doInBackground(String... params) {
            Document html = new Document("");

            String users;

            try {
                html = Jsoup.parse(params[0]);
            } catch (Exception e) {
                e.printStackTrace();
            }

            Elements el = html.select("script");
            if (el.hasAttr("type")) el = html.select("script");

            users = el.html();
            String users2 = "";

            Pattern p = Pattern.compile("\\{id:(.*?)\\}");
            Matcher m = p.matcher(users);

            while (!m.hitEnd()) {
                if (m.find()) {
                    try {
                        users2 = users2.concat(m.group());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            users2 = "{\"users\": ["
                    .concat(users2)
                    .concat("]}")
                    .replace("(", "")
                    .replace(")", "")
                    .replace("id:", "\"id\":")
                    .replace(" name", " \"name\"")
                    .replace("email_hash", "\"email_hash\"")
                    .replace("reputation", "\"reputation\"")
                    .replace("last_post", "\"last_post\"")
                    .replace("is_moderator", "\"is_moderator\"")
                    .replace("is_owner", "\"is_owner\"")
//                        .replace("true", "\"true\"")
                    .replace("}{", "},{")
                    .replace("!", "")
                    .replace("\"\"", "\\\"")
                    .replace("=", "\\=")
                    .replace("&", "\\&");

            return users2;
        }

        @Override
        protected void onPostExecute(String s) {
            mUserParsed.onSuccess(s);
            super.onPostExecute(s);
        }
    }

    private interface UserParsed {
        void onSuccess(String usersJson);
        void onFail(String message);
    }

    private void addUser(final String name, final String imgUrl, final int id, final int lastPost, final int rep, final boolean isMod, final boolean isOwner, final String chatUrl) {
        Bundle args = new Bundle();
        args.putString(USER_NAME_KEY, name);
        args.putString(USER_AVATAR_URL_KEY, imgUrl);
        args.putString(USER_URL_KEY, chatUrl);

        args.putInt(USER_ID_KEY, id);
        args.putInt(USER_LAST_POST_KEY, lastPost);
        args.putInt(USER_REP_KEY, rep);

        args.putBoolean(USER_IS_MOD_KEY, isMod);
        args.putBoolean(USER_IS_OWNER_KEY, isOwner);

        UserTileFragment userTileFragment = new UserTileFragment();
        userTileFragment.setArguments(args);

        mUserInfo.add(args);
        mUserTiles.add(userTileFragment);
        mFragmentManager.beginTransaction().add(R.id.users_scroll_slide, userTileFragment).commit();
    }

    private void addChatButtons(final String url) {

        FloatingActionButton openInBrowser = view.findViewById(R.id.open_in_browser_fab);
        FloatingActionButton roomInfo = view.findViewById(R.id.room_info_fab);
        FloatingActionButton stars = view.findViewById(R.id.star_fab);
        FloatingActionButton users = view.findViewById(R.id.show_users_fab);

        openInBrowser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(browserIntent);
            }
        });

        roomInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog d = new AlertDialog.Builder(getActivity())
                        .setTitle("Info")
                        .setView(R.layout.room_desc)
                        .setPositiveButton(getResources().getText(R.string.generic_ok), null)
                        .create();
                d.show();

                TextView desc = d.findViewById(R.id.desc_text);
                assert desc != null;
                desc.setText(mChatDesc);
                desc.setMovementMethod(LinkMovementMethod.getInstance());

                TextView tag = d.findViewById(R.id.tag_text);
                assert tag != null;
                tag.setText(mChatTagsSpanned);
                tag.setMovementMethod(LinkMovementMethod.getInstance());

                TextView url = d.findViewById(R.id.url_text);
                assert url != null;
                url.setText(Html.fromHtml("<b>URL: </b><a href=\"".concat(mChatUrl).concat("\">").concat(mChatUrl).concat("</a>")));
                url.setMovementMethod(LinkMovementMethod.getInstance());

                TextView host = d.findViewById(R.id.domain_text);
                assert host != null;
                host.setText(Html.fromHtml("<b>Domain: </b><a href=\"".concat("https://").concat(mChatDomain).concat("\">").concat("https://").concat(mChatDomain).concat("</a>")));
                host.setMovementMethod(LinkMovementMethod.getInstance());
            }
        });

        stars.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View web = View.inflate(getActivity(), R.layout.fragment_star_webview, null);

                final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity())
                        .setTitle(getResources().getText(R.string.stars))
                        .setView(web);
//                        .setPositiveButton(getResources().getText(R.string.ok), null);

                final AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();

                final WebView webView = alertDialog.findViewById(R.id.stars_view);
                Button openInWV = alertDialog.findViewById(R.id.open_in_webview);
                Button back = alertDialog.findViewById(R.id.go_back);
                Button forward = alertDialog.findViewById(R.id.go_forward);

                assert webView != null;
                webView.loadUrl(mChatUrl.replace("rooms/", "rooms/info/").replace("#", "").concat("/?tab=stars"));
//                webView.getSettings().setDefaultZoom(WebSettings.ZoomDensity.FAR);
//                webView.setInitialScale();
                webView.getSettings().setLoadWithOverviewMode(true);
                webView.getSettings().setUseWideViewPort(true);
                webView.getSettings().setBuiltInZoomControls(true);
                webView.setWebViewClient(new WebViewClient(){

                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url){
                        view.loadUrl(url);
                        return true;
                    }
                });

                assert openInWV != null;
                openInWV.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(getActivity(), WebViewActivity.class);
                        intent.putExtra("url", mChatUrl.replace("rooms/", "rooms/info/").replace("#", "").concat("/?tab=stars"));
                        intent.setAction(Intent.ACTION_VIEW);
                        startActivity(intent);
                    }
                });

                assert back != null;
                back.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (webView.canGoBack()) webView.goBack();
                    }
                });

                assert forward != null;
                forward.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (webView.canGoForward()) webView.goForward();
                    }
                });
            }
        });

//        showChats.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                MainActivity activity = (MainActivity) getActivity();
//                activity.getmChatroomSlidingMenu().toggle();
//            }
//        });

        users.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSlidingMenu.toggle();
            }
        });
    }

    @Override
    public void onResume()
    {
        super.onResume();

        System.out.println("Chat Frag OnResume");

        hueAllTheThings();

    }

    private static class GetDesc extends AsyncTask<String, Void, String> {
        private DescGotten mDescGotten;

        static GetDesc newInstance(DescGotten descGotten) {
            return new GetDesc(descGotten);
        }

        GetDesc(DescGotten descGotten) {
            mDescGotten = descGotten;
        }

        @Override
        protected String doInBackground(String... params) {
            try {
                Elements divs = Jsoup.parse(params[0]).select("div");

                for (Element e : divs) {
                    if (e.hasAttr("id") && e.attr("id").equals("roomdesc")) return e.html();
                }

                mDescGotten.onFail("NULL");
                return null;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String s) {
            mDescGotten.onSuccess(s);
        }
    }

    private interface DescGotten {
        void onSuccess(String desc);
        void onFail(@SuppressWarnings("SameParameterValue") String message);
    }

    private static class GetTags extends AsyncTask<String, Void, ArrayList<String>> {
        private TagsGotten mTagsGotten;

        public static GetTags newInstance(TagsGotten tagsGotten) {
            return new GetTags(tagsGotten);
        }

        GetTags(TagsGotten tagsGotten) {
            mTagsGotten = tagsGotten;
        }

        @Override
        protected ArrayList<String> doInBackground(String... params) {
            try {
                Elements divs = Jsoup.parse(params[0]).select("div").select("a");
                ArrayList<String> tagList = new ArrayList<>();

                for (Element e : divs) {
                    if (e.hasAttr("class") && e.attr("class").equals("tag")) {
                        tagList.add(e.html());
                    }
                }

                return tagList;
            } catch (Exception e) {
                mTagsGotten.onFail("NULL");
                return null;
            }
        }

        @Override
        protected void onPostExecute(ArrayList<String> strings) {
            mTagsGotten.onSuccess(strings);
        }
    }

    private interface TagsGotten {
        void onSuccess(ArrayList<String> tabList);
        void onFail(@SuppressWarnings("SameParameterValue") String message);
    }

    public SlidingMenu getmSlidingMenu() {
        return mSlidingMenu;
    }

    public ArrayList<Bundle> getmUserInfo() {
        return mUserInfo;
    }

    public int getmAppBarColor() { return mAppBarColor; }

    private static class GetHostDomainFromHtml extends AsyncTask<String, Void, String> {
        DomainFoundListener mDomainFoundListener;

        public static GetHostDomainFromHtml newInstance(DomainFoundListener domainFoundListener) {
            return new GetHostDomainFromHtml(domainFoundListener);
        }

        GetHostDomainFromHtml(DomainFoundListener domainFoundListener) {
            mDomainFoundListener = domainFoundListener;
        }

        @Override
        protected String doInBackground(String... strings) {
            try {
                Document document = Jsoup.parse(strings[0]);
                Log.e("DOC", document.html());
                Elements scripts = document.select("script");
                Log.e("S", scripts.html());

                Pattern p = Pattern.compile("host:(.*?),");
                Matcher m = p.matcher(scripts.html());

                while (!m.hitEnd()) {
                    if (m.find()) return m.group().replace(",", "").replace("host: ", "").replace("'", "");
                }
            } catch (Exception e) {
                mDomainFoundListener.onFail(e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(String domain) {
            mDomainFoundListener.onSuccess(domain);
        }
    }

    private interface DomainFoundListener {
        void onSuccess(String text);
        void onFail(String text);
    }

    public Integer getChatId() {
        return mChatId;
    }

//    private class GetStars extends AsyncTask<String, Void, ArrayList<String >> {
//        @Override
//        protected ArrayList<String> doInBackground(String... params) {
//            String chatUrl = params[0];
//            String starUrl = chatUrl.replace("rooms/", "rooms/info/").replace("#", "").concat("/?tab=stars");
//
//            ArrayList<String> ret = new ArrayList<>();
//
//            try {
//                Elements monologues = Jsoup.connect(starUrl).get().select("div");
//                for (Element e : monologues) {
//                    if (e.hasAttr("class") && e.attr("class").contains("monologue")) ret.add(e.toString());
//                }
//
//                return ret;
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//
//            return null;
//        }
//
//        @Override
//        protected void onPostExecute(ArrayList<String> strings) {
//            String stars = strings.toString().replace("[", "").replace("]", "").replace(">,", ">-----").replace("href=\"//", "href=\"http://");
//            mStarsSpanned = Html.fromHtml(stars);
//        }
//    }
}
