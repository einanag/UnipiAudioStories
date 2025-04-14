package com.unipi.eianagn.unipiaudiostories.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.unipi.eianagn.unipiaudiostories.R;
import com.unipi.eianagn.unipiaudiostories.models.Story;
import com.unipi.eianagn.unipiaudiostories.utils.FirebaseManager;
import com.unipi.eianagn.unipiaudiostories.utils.LocaleHelper;
import com.unipi.eianagn.unipiaudiostories.utils.TextToSpeechManager;

public class StoryDetailActivity extends AppCompatActivity {

    private static final String TAG = "StoryDetailActivity";
    private static final int CHECK_TTS_DATA = 1001;
    private static final int INSTALL_TTS_DATA = 1002;

    private FirebaseManager firebaseManager;
    private TextToSpeechManager textToSpeechManager;
    private final Handler handler = new Handler();
    private Button btnPlay, btnPause, btnStop;
    private TextView tvStoryTitle, tvStoryContent;
    private ImageView ivStoryImage;
    private Story story;
    private String currentLanguageCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LocaleHelper.refreshLocale(this);
        currentLanguageCode = LocaleHelper.getLanguage(this);

        setContentView(R.layout.activity_story_detail);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Ορίστε το χρώμα του κειμένου του τίτλου σε μαύρο
        toolbar.setTitleTextColor(getResources().getColor(R.color.toolbarText));

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initViews();

        story = (Story) getIntent().getSerializableExtra("story");
        if (story != null) {
            displayStoryDetails();
        }

        firebaseManager = new FirebaseManager();
        textToSpeechManager = new TextToSpeechManager(this);

        handler.postDelayed(() -> {
            if (!textToSpeechManager.isInitialized()) {
                Log.d(TAG, "Προσπάθεια επαναρχικοποίησης TTS");
                textToSpeechManager.reinitialize();
            }
        }, 1500);

        setupButtons();
    }

    private void initViews() {
        tvStoryTitle = findViewById(R.id.tvStoryTitle);
        tvStoryContent = findViewById(R.id.tvStoryContent);
        ivStoryImage = findViewById(R.id.ivStoryImage);
        btnPlay = findViewById(R.id.btnPlay);
        btnPause = findViewById(R.id.btnPause);
        btnStop = findViewById(R.id.btnStop);
    }

    private void displayStoryDetails() {
        if (story != null) {
            tvStoryTitle.setText(story.getTitle());
            tvStoryContent.setText(story.getContent());

            int resourceId = getResources().getIdentifier(story.getImageName(), "drawable", getPackageName());
            if (resourceId != 0) {
                Glide.with(this)
                        .load(resourceId)
                        .centerCrop()
                        .into(ivStoryImage);
            } else {
                ivStoryImage.setImageResource(R.drawable.app_logo);
            }
        }
    }

    private void setupButtons() {
        // Ορίστε τα backgrounds για τα κουμπιά
        btnPlay.setBackgroundResource(R.drawable.pixel_button_background);
        btnPause.setBackgroundResource(R.drawable.pixel_pause_button);
        btnStop.setBackgroundResource(R.drawable.pixel_stop_button);

        btnPlay.setTextColor(getResources().getColor(R.color.buttonTextEnabled));
        btnPause.setTextColor(getResources().getColor(R.color.buttonTextDisabled));
        btnStop.setTextColor(getResources().getColor(R.color.buttonTextDisabled));

        btnPlay.setOnClickListener(v -> {
            if (textToSpeechManager.isInitialized()) {
                if (story != null) {
                    textToSpeechManager.speak(story.getContent());
                    updateButtonStates(true, false);

                    // Προσθέστε αυτή τη γραμμή για να αυξήσετε το playCount μόνο όταν γίνεται αναπαραγωγή
                    firebaseManager.updateStoryStatistic(story);
                }
            } else {
                Toast.makeText(this, "Το σύστημα ομιλίας δεν είναι έτοιμο", Toast.LENGTH_SHORT).show();
                textToSpeechManager.reinitialize();
            }
        });

        btnPause.setOnClickListener(v -> {
            if (textToSpeechManager.isSpeaking()) {
                textToSpeechManager.pause();
                btnPause.setText(R.string.resume);
                updateButtonStates(false, true);
            } else if (textToSpeechManager.isPaused()) {
                textToSpeechManager.resume();
                btnPause.setText(R.string.pause);
                updateButtonStates(true, false);
            }
        });

        btnStop.setOnClickListener(v -> {
            textToSpeechManager.stop();
            updateButtonStates(false, false);
            btnPause.setText(R.string.pause);
        });

        updateButtonStates(false, false);
    }

    private void updateButtonStates(boolean isSpeaking, boolean isPaused) {
        btnPlay.setEnabled(!isSpeaking);
        btnPause.setEnabled(isSpeaking || isPaused);
        btnStop.setEnabled(isSpeaking || isPaused);

        // Ενημερώστε τα χρώματα κειμένου ανάλογα με την κατάσταση
        btnPlay.setTextColor(getResources().getColor(!isSpeaking ? R.color.buttonTextEnabled : R.color.buttonTextDisabled));
        btnPause.setTextColor(getResources().getColor((isSpeaking || isPaused) ? R.color.buttonTextEnabled : R.color.buttonTextDisabled));
        btnStop.setTextColor(getResources().getColor((isSpeaking || isPaused) ? R.color.buttonTextEnabled : R.color.buttonTextDisabled));
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CHECK_TTS_DATA) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                Log.d(TAG, "Τα δεδομένα TTS είναι εγκατεστημένα");
                if (textToSpeechManager != null) {
                    textToSpeechManager.reinitialize();
                }
            } else {
                Log.d(TAG, "Τα δεδομένα TTS δεν είναι εγκατεστημένα, προτροπή για εγκατάσταση");

                Intent installIntent = new Intent();
                installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivityForResult(installIntent, INSTALL_TTS_DATA);
            }
        } else if (requestCode == INSTALL_TTS_DATA) {
            Intent checkIntent = new Intent();
            checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
            startActivityForResult(checkIntent, CHECK_TTS_DATA);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        String newLanguageCode = LocaleHelper.getLanguage(this);
        if (!newLanguageCode.equals(currentLanguageCode)) {
            currentLanguageCode = newLanguageCode;
            LocaleHelper.refreshLocale(this);
            recreate();
            return;
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

