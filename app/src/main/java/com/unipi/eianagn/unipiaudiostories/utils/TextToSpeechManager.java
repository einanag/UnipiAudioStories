package com.unipi.eianagn.unipiaudiostories.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.widget.Toast;

import com.unipi.eianagn.unipiaudiostories.R;

import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;

public class TextToSpeechManager {
    private static final String TAG = "TextToSpeechManager";
    private static final int MAX_INIT_ATTEMPTS = 5;
    private static final int CHECK_TTS_DATA = 1001;
    private static final int INSTALL_TTS_DATA = 1002;

    private TextToSpeech textToSpeech;
    private final Context context;
    private float speechRate = 1.0f;
    private float speechPitch = 1.0f;
    private boolean isInitialized = false;
    private boolean isSpeaking = false;
    private boolean isPaused = false;
    private String lastSpokenText = "";
    private int lastUtterancePosition = 0;
    private final Handler handler;
    private int initAttempts = 0;
    private String currentLanguageCode = "";

    public TextToSpeechManager(Context context) {
        this.context = context;
        this.handler = new Handler(Looper.getMainLooper());
        this.currentLanguageCode = LocaleHelper.getLanguage(context);
        checkTtsData();
        handler.postDelayed(this::initTextToSpeech, 1000);
    }

    private void initTextToSpeech() {
        if (initAttempts >= MAX_INIT_ATTEMPTS) {
            Log.e(TAG, "Υπέρβαση μέγιστου αριθμού προσπαθειών αρχικοποίησης TTS");
            return;
        }

        initAttempts++;

        try {
            if (textToSpeech != null) {
                textToSpeech.shutdown();
            }

            Context appContext = context.getApplicationContext();
            textToSpeech = new TextToSpeech(appContext, status -> {
                if (status == TextToSpeech.SUCCESS) {

                    String languageCode = LocaleHelper.getLanguage(context);
                    currentLanguageCode = languageCode;
                    Locale locale;

                    switch (languageCode) {
                        case "el":
                            locale = new Locale("el", "GR");
                            break;
                        case "de":
                            locale = Locale.GERMAN;
                            break;
                        case "en":
                        default:
                            locale = Locale.US;
                            break;
                    }

                    int result = textToSpeech.setLanguage(locale);

                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e(TAG, "Η γλώσσα δεν υποστηρίζεται: " + locale + ", δοκιμή στα Αγγλικά");
                        result = textToSpeech.setLanguage(Locale.US);

                        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                            Log.e(TAG, "Ούτε τα Αγγλικά υποστηρίζονται, χρήση προεπιλογής");
                            textToSpeech.setLanguage(Locale.getDefault());
                            checkTtsData();
                        }
                    }

                    textToSpeech.setSpeechRate(speechRate);
                    textToSpeech.setPitch(speechPitch);
                    textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                        @Override
                        public void onStart(String utteranceId) {
                            isSpeaking = true;
                            isPaused = false;
                            Log.d(TAG, "Έναρξη ομιλίας: " + utteranceId);
                        }

                        @Override
                        public void onDone(String utteranceId) {
                            isSpeaking = false;
                            isPaused = false;
                            lastUtterancePosition = 0;
                            Log.d(TAG, "Ολοκλήρωση ομιλίας: " + utteranceId);
                        }

                        @Override
                        public void onError(String utteranceId) {
                            isSpeaking = false;
                            isPaused = false;
                            lastUtterancePosition = 0;
                            Log.e(TAG, "Σφάλμα ομιλίας: " + utteranceId);
                        }

                        @Override
                        public void onStop(String utteranceId, boolean interrupted) {
                            super.onStop(utteranceId, interrupted);
                            isSpeaking = false;
                            if (interrupted) {
                                isPaused = true;
                            } else {
                                isPaused = false;
                                lastUtterancePosition = 0;
                            }
                            Log.d(TAG, "Διακοπή ομιλίας: " + utteranceId + ", διακόπηκε: " + interrupted);
                        }
                    });

                    isInitialized = true;
                    initAttempts = 0;
                    Log.d(TAG, "TextToSpeech αρχικοποιήθηκε με επιτυχία με γλώσσα: " + locale);
                } else {
                    Log.e(TAG, "Αποτυχία αρχικοποίησης TextToSpeech: " + status);
                    isInitialized = false;
                    handler.postDelayed(() -> {
                        initTextToSpeech();
                    }, 1500);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Εξαίρεση κατά την αρχικοποίηση του TextToSpeech: " + e.getMessage());
            isInitialized = false;

            handler.postDelayed(() -> {
                initTextToSpeech();
            }, 1500);
        }
    }

    public void checkLanguageChange() {
        String newLanguageCode = LocaleHelper.getLanguage(context);
        if (!newLanguageCode.equals(currentLanguageCode)) {
            Log.d(TAG, "Η γλώσσα άλλαξε από " + currentLanguageCode + " σε " + newLanguageCode);
            currentLanguageCode = newLanguageCode;
            initAttempts = 0;
            initTextToSpeech();
        }
    }

    public void speak(String text) {
        checkLanguageChange();

        if (!isInitialized) {
            Log.e(TAG, "TextToSpeech δεν έχει αρχικοποιηθεί ακόμα");
            initAttempts = 0;
            initTextToSpeech();
            handler.postDelayed(() -> {
                if (isInitialized) {
                    speakInternal(text);
                } else {
                    Log.e(TAG, "Αδυναμία ομιλίας, το TextToSpeech δεν αρχικοποιήθηκε");
                    Toast.makeText(context, "Το σύστημα ομιλίας δεν είναι έτοιμο", Toast.LENGTH_SHORT).show();
                    checkTtsData();
                }
            }, 2000);
            return;
        }

        speakInternal(text);
    }

    private void speakInternal(String text) {
        try {
            if (textToSpeech == null) {
                Log.e(TAG, "TextToSpeech είναι null");
                return;
            }

            lastSpokenText = text;
            lastUtterancePosition = 0;
            textToSpeech.stop();


            String utteranceId = UUID.randomUUID().toString();
            HashMap<String, String> params = new HashMap<>();
            params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
            } else {
                textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, params);
            }

            isSpeaking = true;
            isPaused = false;
            Log.d(TAG, "Ομιλία κειμένου: " + (text.length() > 20 ? text.substring(0, 20) + "..." : text));
        } catch (Exception e) {
            Log.e(TAG, "Σφάλμα κατά την ομιλία: " + e.getMessage());
            isInitialized = false;
            initTextToSpeech();
        }
    }

    public void pause() {
        if (isInitialized && isSpeaking) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                    lastUtterancePosition = textToSpeech.stop();
                    isPaused = true;
                    isSpeaking = false;
                    Log.d(TAG, "Παύση ομιλίας στη θέση: " + lastUtterancePosition);
                } else {

                    textToSpeech.stop();
                    isPaused = true;
                    isSpeaking = false;
                    Log.d(TAG, "Παύση ομιλίας (παλαιά έκδοση Android)");
                }
            } catch (Exception e) {
                Log.e(TAG, "Σφάλμα κατά την παύση: " + e.getMessage());
            }
        }
    }

    public void resume() {
        if (isInitialized && isPaused && !lastSpokenText.isEmpty()) {
            try {

                String utteranceId = UUID.randomUUID().toString();

                HashMap<String, String> params = new HashMap<>();
                params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                    if (lastUtterancePosition > 0) {

                        String remainingText = lastSpokenText.substring(lastUtterancePosition);
                        textToSpeech.speak(remainingText, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
                        Log.d(TAG, "Συνέχιση ομιλίας από τη θέση: " + lastUtterancePosition);
                    } else {

                        textToSpeech.speak(lastSpokenText, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
                        Log.d(TAG, "Συνέχιση ομιλίας από την αρχή");
                    }
                } else {

                    textToSpeech.speak(lastSpokenText, TextToSpeech.QUEUE_FLUSH, params);
                    Log.d(TAG, "Συνέχιση ομιλίας από την αρχή (παλαιά έκδοση Android)");
                }

                isSpeaking = true;
                isPaused = false;
            } catch (Exception e) {
                Log.e(TAG, "Σφάλμα κατά τη συνέχιση: " + e.getMessage());
            }
        }
    }

    public void stop() {
        if (isInitialized) {
            try {
                textToSpeech.stop();
                isSpeaking = false;
                isPaused = false;
                lastUtterancePosition = 0;
                Log.d(TAG, "Διακοπή ομιλίας");
            } catch (Exception e) {
                Log.e(TAG, "Σφάλμα κατά τη διακοπή: " + e.getMessage());
            }
        }
    }

    public void shutdown() {
        if (textToSpeech != null) {
            try {
                textToSpeech.stop();
                textToSpeech.shutdown();
                isInitialized = false;
                isSpeaking = false;
                isPaused = false;
                lastUtterancePosition = 0;
                Log.d(TAG, "Τερματισμός TextToSpeech");
            } catch (Exception e) {
                Log.e(TAG, "Σφάλμα κατά τον τερματισμό: " + e.getMessage());
            }
        }
    }

    public void setSpeechRate(float rate) {
        this.speechRate = rate;
        if (isInitialized && textToSpeech != null) {
            try {
                textToSpeech.setSpeechRate(rate);
                Log.d(TAG, "Ρύθμιση ταχύτητας ομιλίας: " + rate);
            } catch (Exception e) {
                Log.e(TAG, "Σφάλμα κατά τη ρύθμιση ταχύτητας: " + e.getMessage());
            }
        }
    }

    public void setSpeechPitch(float pitch) {
        this.speechPitch = pitch;
        if (isInitialized && textToSpeech != null) {
            try {
                textToSpeech.setPitch(pitch);
                Log.d(TAG, "Ρύθμιση τόνου ομιλίας: " + pitch);
            } catch (Exception e) {
                Log.e(TAG, "Σφάλμα κατά τη ρύθμιση τόνου: " + e.getMessage());
            }
        }
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public boolean isSpeaking() {
        return isSpeaking;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public void reinitialize() {
        initAttempts = 0;
        initTextToSpeech();
    }

    public void checkTtsData() {
        try {
            Intent checkIntent = new Intent();
            checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);

            if (context instanceof Activity) {
                ((Activity) context).startActivityForResult(checkIntent, CHECK_TTS_DATA);
            } else {
                Log.e(TAG, "Το context δεν είναι Activity, δεν μπορεί να γίνει έλεγχος TTS data");

            }
        } catch (Exception e) {
            Log.e(TAG, "Σφάλμα κατά τον έλεγχο TTS data: " + e.getMessage());
        }
    }

    public void handleTtsDataCheckResult(int resultCode) {
        if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
            Log.d(TAG, "Τα δεδομένα TTS είναι εγκατεστημένα");
        } else {
            Log.d(TAG, "Τα δεδομένα TTS δεν είναι εγκατεστημένα, προτροπή για εγκατάσταση");

            try {
                Intent installIntent = new Intent();
                installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                if (context instanceof Activity) {
                    context.startActivity(installIntent);
                }
            } catch (Exception e) {
                Log.e(TAG, "Σφάλμα κατά την εγκατάσταση TTS data: " + e.getMessage());
            }
        }
    }
}

