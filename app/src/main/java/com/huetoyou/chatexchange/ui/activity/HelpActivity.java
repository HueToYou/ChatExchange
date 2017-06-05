package com.huetoyou.chatexchange.ui.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.huetoyou.chatexchange.R;
import com.huetoyou.chatexchange.TutorialActivity;
import com.huetoyou.chatexchange.ui.misc.HueUtils;

public class HelpActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        HueUtils hueUtils = new HueUtils();
        hueUtils.setActionBarColorDefault(this);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public void showTutorial(View v)
    {
        Intent intent = new Intent(this, TutorialActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onSupportNavigateUp()
    {
        finish();
        return true;
    }
}
