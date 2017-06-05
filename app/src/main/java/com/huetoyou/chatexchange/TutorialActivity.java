package com.huetoyou.chatexchange;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.SimpleShowcaseEventListener;
import com.github.amlcurran.showcaseview.targets.ViewTarget;

public class TutorialActivity extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);

        displayShowcases();
    }

    private void displayShowcases()
    {
        new ShowcaseView.Builder(this)
                .setTarget(new ViewTarget(findViewById(R.id.show_chats_fab_tutorial)))
                .withNewStyleShowcase()
                .setStyle(R.style.CustomShowcaseTheme)
                .setContentTitle("Show chatrooms")
                .setContentText("Click this button to reveal the chatrooms sliding panel\n\nSwiping inwards from the left edge of the screen has the same effect")
                .blockAllTouches()
                .setShowcaseEventListener(new SimpleShowcaseEventListener()
                {
                    @Override
                    public void onShowcaseViewDidHide(ShowcaseView showcaseView)
                    {
                        showcaseRemoveChat();
                    }

                })
                .build();
    }

    private void showcaseRemoveChat()
    {
        new ShowcaseView.Builder(this)
                .setTarget(new ViewTarget(findViewById(R.id.close_chat_frag_tutorial)))
                .withNewStyleShowcase()
                .setStyle(R.style.CustomShowcaseTheme)
                .setContentTitle("Remove chatroom")
                .setContentText("Click this button to remove the current chatroom from the sliding panel")
                .blockAllTouches()
                .setShowcaseEventListener(new SimpleShowcaseEventListener()
                {
                    @Override
                    public void onShowcaseViewDidHide(ShowcaseView showcaseView)
                    {
                        showcaseStar();
                    }

                })
                .build();
    }

    private void showcaseStar()
    {
        new ShowcaseView.Builder(this)
                .setTarget(new ViewTarget(findViewById(R.id.star_fab_tutorial)))
                .withNewStyleShowcase()
                .setStyle(R.style.CustomShowcaseTheme)
                .setContentTitle("Show starred messages")
                .setContentText("This button opens a browser window showing the messages currently on the starwall")
                .blockAllTouches()
                .setShowcaseEventListener(new SimpleShowcaseEventListener()
                {
                    @Override
                    public void onShowcaseViewDidHide(ShowcaseView showcaseView)
                    {
                        showcaseInfo();
                    }

                })
                .build();
    }

    private void showcaseInfo()
    {
        new ShowcaseView.Builder(this)
                .setTarget(new ViewTarget(findViewById(R.id.room_info_fab_tutorial)))
                .withNewStyleShowcase()
                .setStyle(R.style.CustomShowcaseTheme)
                .setContentTitle("Room info")
                .setContentText("This button display's the current chatrooms's description, and other data")
                .blockAllTouches()
                .setShowcaseEventListener(new SimpleShowcaseEventListener()
                {
                    @Override
                    public void onShowcaseViewDidHide(ShowcaseView showcaseView)
                    {
                        showcaseOpenInBrowser();
                    }

                })
                .build();
    }

    private void showcaseOpenInBrowser()
    {
        new ShowcaseView.Builder(this)
                .setTarget(new ViewTarget(findViewById(R.id.open_in_browser_fab_tutorial)))
                .withNewStyleShowcase()
                .setStyle(R.style.CustomShowcaseTheme)
                .setContentTitle("Open in browser")
                .setContentText("Click this button to open the current chatroom in a browser window")
                .blockAllTouches()
                .setShowcaseEventListener(new SimpleShowcaseEventListener()
                {
                    @Override
                    public void onShowcaseViewDidHide(ShowcaseView showcaseView)
                    {
                        showcaseUsersPanel();
                    }

                })
                .build();
    }

    private void showcaseUsersPanel()
    {
        new ShowcaseView.Builder(this)
                .setTarget(new ViewTarget(findViewById(R.id.show_users_fab_tutorial)))
                .withNewStyleShowcase()
                .setStyle(R.style.CustomShowcaseTheme)
                .setContentTitle("Reveal users")
                .setContentText("This button reveals current users sliding panel\n\nSwiping inwards from the right edge of the screen has the same effect")
                .blockAllTouches()
                .setShowcaseEventListener(new SimpleShowcaseEventListener()
                {
                    @Override
                    public void onShowcaseViewDidHide(ShowcaseView showcaseView)
                    {
                        finish();
                    }

                })
                .build();
    }
}