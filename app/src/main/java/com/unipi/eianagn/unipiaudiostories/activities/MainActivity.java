package com.unipi.eianagn.unipiaudiostories.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.unipi.eianagn.unipiaudiostories.R;
import com.unipi.eianagn.unipiaudiostories.adapters.StoryAdapter;
import com.unipi.eianagn.unipiaudiostories.models.Story;
import com.unipi.eianagn.unipiaudiostories.utils.FirebaseManager;
import com.unipi.eianagn.unipiaudiostories.utils.LocaleHelper;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements StoryAdapter.OnStoryClickListener {

    private static final String TAG = "MainActivity";
    private RecyclerView recyclerView;
    private StoryAdapter adapter;
    private final List<Story> stories = new ArrayList<>();
    private FirebaseManager firebaseManager;
    private TextView tvNoStories;
    private ProgressBar progressBar;
    private String currentLanguageCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LocaleHelper.forceUpdateAllContexts(this, LocaleHelper.getLanguage(this));
        currentLanguageCode = LocaleHelper.getLanguage(this);

        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        recyclerView = findViewById(R.id.rvStories);
        tvNoStories = findViewById(R.id.tvNoStories);
        progressBar = findViewById(R.id.progressBar);

        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new StoryAdapter(this, stories, this);
        recyclerView.setAdapter(adapter);

        firebaseManager = new FirebaseManager();

        // to load stories
        loadStories();

        if (getIntent().getBooleanExtra("LANGUAGE_CHANGED", false)) {
            Toast.makeText(this, R.string.language_changed, Toast.LENGTH_SHORT).show();
        }
    }

    private void loadStories() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        tvNoStories.setVisibility(View.GONE);

        firebaseManager.getAllStories(new FirebaseManager.OnStoriesLoadedListener() {
            @Override
            public void onStoriesLoaded(List<Story> loadedStories) {
                progressBar.setVisibility(View.GONE);

                stories.clear();
                stories.addAll(loadedStories);
                adapter.notifyDataSetChanged();

                if (stories.isEmpty()) {
                    tvNoStories.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    tvNoStories.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(Exception e) {
                progressBar.setVisibility(View.GONE);
                Log.e(TAG, "Σφάλμα κατά τη φόρτωση των ιστοριών: " + e.getMessage());
                tvNoStories.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onStoryClick(Story story) {
        Intent intent = new Intent(this, StoryDetailActivity.class);
        intent.putExtra("story", story);
        startActivity(intent);
        firebaseManager.updateStoryStatistic(story);
    }

    @Override
    public void onFavoriteClick(Story story, int position) {
        story.setFavorite(!story.isFavorite());
        adapter.notifyItemChanged(position);
        firebaseManager.updateStoryFavorite(story);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_statistics) {
            Intent intent = new Intent(this, StatisticsActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_logout) {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
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
            LocaleHelper.forceUpdateAllContexts(this, newLanguageCode);
            recreate(); // Recreate the activity to apply changes
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            // User is not logged in, go to login screen
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        loadStories();
    }
}

