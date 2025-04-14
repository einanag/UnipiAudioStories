package com.unipi.eianagn.unipiaudiostories.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.unipi.eianagn.unipiaudiostories.R;
import com.unipi.eianagn.unipiaudiostories.adapters.StatisticsAdapter;
import com.unipi.eianagn.unipiaudiostories.models.Story;
import com.unipi.eianagn.unipiaudiostories.models.StoryStatistic;
import com.unipi.eianagn.unipiaudiostories.utils.FirebaseManager;
import com.unipi.eianagn.unipiaudiostories.utils.LocaleHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatisticsActivity extends AppCompatActivity {

    private static final String TAG = "StatisticsActivity";
    private RecyclerView recyclerView;
    private StatisticsAdapter adapter;
    private final List<StoryStatistic> statistics = new ArrayList<>();
    private final Map<String, Story> storyMap = new HashMap<>();
    private FirebaseManager firebaseManager;

    private TextView tvNoStatistics;
    private ProgressBar progressBar;
    private String currentLanguageCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LocaleHelper.refreshLocale(this);
        currentLanguageCode = LocaleHelper.getLanguage(this);
        setContentView(R.layout.activity_statistics);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.statistics);
        }

        recyclerView = findViewById(R.id.rvStatistics);
        tvNoStatistics = findViewById(R.id.tvNoStatistics);
        progressBar = findViewById(R.id.progressBar);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new StatisticsAdapter(this, statistics, storyMap);
        recyclerView.setAdapter(adapter);
        firebaseManager = new FirebaseManager();

        loadStatistics();
    }

    private void loadStatistics() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        tvNoStatistics.setVisibility(View.GONE);

        firebaseManager.getAllStories(new FirebaseManager.OnStoriesLoadedListener() {
            @Override
            public void onStoriesLoaded(List<Story> stories) {
                storyMap.clear();
                for (Story story : stories) {
                    storyMap.put(story.getId(), story);
                }

                firebaseManager.getAllStatistics(new FirebaseManager.OnStatisticsLoadedListener() {
                    @Override
                    public void onStatisticsLoaded(List<StoryStatistic> loadedStatistics) {
                        progressBar.setVisibility(View.GONE);

                        statistics.clear();
                        statistics.addAll(loadedStatistics);
                        adapter.updateData(statistics, storyMap);

                        if (statistics.isEmpty()) {
                            tvNoStatistics.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                        } else {
                            tvNoStatistics.setVisibility(View.GONE);
                            recyclerView.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        progressBar.setVisibility(View.GONE);

                        Log.e(TAG, "Σφάλμα κατά τη φόρτωση των στατιστικών: " + e.getMessage());
                        tvNoStatistics.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                progressBar.setVisibility(View.GONE);

                Log.e(TAG, "Σφάλμα κατά τη φόρτωση των ιστοριών: " + e.getMessage());
                tvNoStatistics.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            }
        });
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
    protected void onResume() {
        super.onResume();

        String newLanguageCode = LocaleHelper.getLanguage(this);
        if (!newLanguageCode.equals(currentLanguageCode)) {
            currentLanguageCode = newLanguageCode;
            LocaleHelper.refreshLocale(this);
            recreate();
            return;
        }

        loadStatistics();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
}