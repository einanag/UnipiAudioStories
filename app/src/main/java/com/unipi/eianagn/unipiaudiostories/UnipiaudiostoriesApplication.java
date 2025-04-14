package com.unipi.eianagn.unipiaudiostories;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.unipi.eianagn.unipiaudiostories.utils.LocaleHelper;

import java.util.Locale;

public class UnipiaudiostoriesApplication extends Application {
    private static final String TAG = "UnipiaudiostoriesApp";
    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;

        // Firebase
        FirebaseApp.initializeApp(this);

        // language
        String language = LocaleHelper.getLanguage(this);
        LocaleHelper.forceUpdateAllContexts(this, language);

        Log.d(TAG, "Application initialized with language: " + language);
    }

    public static Context getAppContext() {
        return context;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // keep selected language during configuration changes
        String language = LocaleHelper.getLanguage(this);
        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Configuration configuration = new Configuration(newConfig);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLocale(locale);
        } else {
            configuration.locale = locale;
        }

        getResources().updateConfiguration(configuration, getResources().getDisplayMetrics());

        Log.d(TAG, "Maintaining language after configuration change: " + language);
    }
}

