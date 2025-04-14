package com.unipi.eianagn.unipiaudiostories.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.unipi.eianagn.unipiaudiostories.R;
import com.unipi.eianagn.unipiaudiostories.utils.FirebaseManager;
import com.unipi.eianagn.unipiaudiostories.utils.LocaleHelper;
import com.unipi.eianagn.unipiaudiostories.utils.TextToSpeechManager;

import java.util.ArrayList;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";
    private static final int TEST_VOICE_RECOGNITION = 1234;

    private Spinner spinnerLanguage;
    private SeekBar seekBarSpeechRate, seekBarSpeechPitch;
    private Button btnResetStatistics;
    private TextView tvVersion;
    private SharedPreferences sharedPreferences;
    private TextToSpeechManager textToSpeechManager;
    private FirebaseManager firebaseManager;
    private String currentLanguageCode;
    private final boolean isLanguageChanging = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LocaleHelper.refreshLocale(this);
        currentLanguageCode = LocaleHelper.getLanguage(this);
        Log.d(TAG, "Τρέχουσα γλώσσα: " + currentLanguageCode);

        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.settings);
        }

        sharedPreferences = getSharedPreferences("app_preferences", MODE_PRIVATE);
        spinnerLanguage = findViewById(R.id.spinnerLanguage);
        seekBarSpeechRate = findViewById(R.id.seekBarSpeechRate);
        seekBarSpeechPitch = findViewById(R.id.seekBarSpeechPitch);
        btnResetStatistics = findViewById(R.id.btnResetStatistics);
        tvVersion = findViewById(R.id.tvVersion);
        setVersionText();
        firebaseManager = new FirebaseManager();
        textToSpeechManager = new TextToSpeechManager(this);
        setupLanguageSpinner();
        setupSpeechControls();

        btnResetStatistics.setOnClickListener(v -> {
            showResetStatisticsConfirmation();
        });
    }

    private void setVersionText() {
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;
            tvVersion.setText(getString(R.string.version) + " " + version);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Error getting package info", e);
            tvVersion.setText(getString(R.string.version) + " unknown");
        }
    }

    private void setupLanguageSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.language_options,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLanguage.setAdapter(adapter);
        String currentLang = LocaleHelper.getLanguage(this);
        int position = 0;
        if (currentLang.equals("el")) {
            position = 0; // Ελληνικά
        } else if (currentLang.equals("en")) {
            position = 1; // English
        } else if (currentLang.equals("de")) {
            position = 2; // Deutsch
        }
        spinnerLanguage.setSelection(position);

        spinnerLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String languageCode;
                if (position == 0) languageCode = "el";
                else if (position == 1) languageCode = "en";
                else languageCode = "de";

                applyLanguageChange(languageCode);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupSpeechControls() {

        float savedSpeechRate = sharedPreferences.getFloat("speech_rate", 1.0f);
        int speechRateProgress = (int) ((savedSpeechRate - 0.5f) * 100);
        seekBarSpeechRate.setProgress(speechRateProgress);
        seekBarSpeechRate.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float rate = 0.5f + (progress / 100.0f);
                Log.d(TAG, "Ταχύτητα ομιλίας: " + rate);

                if (textToSpeechManager != null) {
                    textToSpeechManager.setSpeechRate(rate);
                }

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putFloat("speech_rate", rate);
                editor.apply();

                if (fromUser && textToSpeechManager != null && textToSpeechManager.isInitialized()) {
                    textToSpeechManager.speak(getString(R.string.speech_rate_test));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Do nothing
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Do nothing
            }
        });


        float savedSpeechPitch = sharedPreferences.getFloat("speech_pitch", 1.0f);
        int speechPitchProgress = (int) ((savedSpeechPitch - 0.5f) * 100);
        seekBarSpeechPitch.setProgress(speechPitchProgress);

        seekBarSpeechPitch.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float pitch = 0.5f + (progress / 100.0f);
                Log.d(TAG, "Τόνος ομιλίας: " + pitch);

                if (textToSpeechManager != null) {
                    textToSpeechManager.setSpeechPitch(pitch);
                }

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putFloat("speech_pitch", pitch);
                editor.apply();

                if (fromUser && textToSpeechManager != null && textToSpeechManager.isInitialized()) {
                    textToSpeechManager.speak(getString(R.string.speech_pitch_test));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Do nothing
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Do nothing
            }
        });
    }

    private void showResetStatisticsConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.reset_statistics)
                .setMessage(R.string.reset_statistics_confirmation)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    // Reset statistics in Firebase
                    firebaseManager.resetAllStatistics(() -> {
                        Toast.makeText(this, R.string.statistics_reset_success, Toast.LENGTH_SHORT).show();
                    });
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void applyLanguageChange(String languageCode) {
        if (!languageCode.equals(LocaleHelper.getLanguage(SettingsActivity.this))) {

            LocaleHelper.setLocale(SettingsActivity.this, languageCode);

            //  toast msg
            Toast.makeText(SettingsActivity.this, R.string.language_changed, Toast.LENGTH_SHORT).show();

            if (textToSpeechManager != null) {
                textToSpeechManager.reinitialize();
            }

            // force restart all activities to apply  new language
            Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("LANGUAGE_CHANGED", true);
            startActivity(intent);
            finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // controls for lang.chng
        if (!isLanguageChanging) {
            String newLanguageCode = LocaleHelper.getLanguage(this);
            if (!newLanguageCode.equals(currentLanguageCode)) {
                currentLanguageCode = newLanguageCode;
                LocaleHelper.refreshLocale(this);
                recreate();
                return;
            }
        }

        if (textToSpeechManager != null) {
            textToSpeechManager.checkLanguageChange();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (textToSpeechManager != null) {
            textToSpeechManager.stop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (textToSpeechManager != null) {
            textToSpeechManager.shutdown();
        }
    }
}

