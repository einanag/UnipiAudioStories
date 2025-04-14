package com.unipi.eianagn.unipiaudiostories.utils;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.unipi.eianagn.unipiaudiostories.models.Story;
import com.unipi.eianagn.unipiaudiostories.models.StoryStatistic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseManager {
    private static final String TAG = "FirebaseManager";

    private final FirebaseAuth auth;
    private final FirebaseDatabase database;
    private final DatabaseReference storiesRef;
    private final DatabaseReference statisticsRef;
    private final DatabaseReference userFavoritesRef;

    public interface OnOperationCompleteListener {
        void onSuccess();
        void onError(String errorMessage);
    }

    public interface OnStoriesLoadedListener {
        void onStoriesLoaded(List<Story> stories);
        void onError(Exception e);
    }

    public interface OnStatisticsLoadedListener {
        void onStatisticsLoaded(List<StoryStatistic> statistics);
        void onError(Exception e);
    }

    public interface OnFavoriteCheckListener {
        void onResult(boolean isFavorite);
        void onError(String errorMessage);
    }

    public FirebaseManager() {
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        storiesRef = database.getReference("stories");
        statisticsRef = database.getReference("statistics");
        userFavoritesRef = database.getReference("userFavorites");
    }

    public boolean isUserLoggedIn() {
        return auth.getCurrentUser() != null;
    }

    public String getCurrentUserId() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    public void registerUser(String name, String email, String password, OnOperationCompleteListener listener) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {

                        FirebaseUser user = auth.getCurrentUser();
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setDisplayName(name)
                                .build();

                        user.updateProfile(profileUpdates)
                                .addOnCompleteListener(profileTask -> {
                                    if (profileTask.isSuccessful()) {
                                        listener.onSuccess();
                                    } else {
                                        listener.onError(profileTask.getException().getMessage());
                                    }
                                });
                    } else {
                        listener.onError(task.getException().getMessage());
                    }
                });
    }

    public void loginUser(String email, String password, OnOperationCompleteListener listener) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        listener.onSuccess();
                    } else {
                        listener.onError(task.getException().getMessage());
                    }
                });
    }

    public void logoutUser() {
        auth.signOut();
    }

    public void getAllStories(OnStoriesLoadedListener listener) {
        String userId = getCurrentUserId();

        storiesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Story> stories = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Story story = snapshot.getValue(Story.class);
                    if (story != null) {
                        stories.add(story);
                    }
                }

                if (userId != null) {
                    userFavoritesRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot userDataSnapshot) {
                            for (Story story : stories) {
                                DataSnapshot storySnapshot = userDataSnapshot.child(story.getId());
                                if (storySnapshot.exists() && storySnapshot.getValue(Boolean.class) != null) {
                                    story.setFavorite(storySnapshot.getValue(Boolean.class));
                                }
                            }
                            listener.onStoriesLoaded(stories);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            listener.onError(new Exception(databaseError.getMessage()));
                        }
                    });
                } else {
                    listener.onStoriesLoaded(stories);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                listener.onError(new Exception(databaseError.getMessage()));
            }
        });
    }

    public void updateStory(Story story, OnOperationCompleteListener listener) {
        String userId = getCurrentUserId();
        if (userId == null) {
            listener.onError("Δεν έχετε συνδεθεί");
            return;
        }


        userFavoritesRef.child(userId).child(story.getId()).setValue(story.isFavorite())
                .addOnSuccessListener(aVoid -> {

                    updateFavoriteStatistics(story.getId(), story.isFavorite(), listener);
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }


    public void isStoryFavorite(String storyId, OnFavoriteCheckListener listener) {
        String userId = getCurrentUserId();
        if (userId == null) {
            listener.onResult(false);
            return;
        }

        userFavoritesRef.child(userId).child(storyId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                listener.onResult(dataSnapshot.exists() && dataSnapshot.getValue(Boolean.class) == Boolean.TRUE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                listener.onError(databaseError.getMessage());
            }
        });
    }

    private void updateFavoriteStatistics(String storyId, boolean isFavorite, OnOperationCompleteListener listener) {
        String userId = getCurrentUserId();
        if (userId == null) {
            listener.onError("Δεν έχετε συνδεθεί");
            return;
        }

        String statisticId = createStatisticId(storyId);
        statisticsRef.child(statisticId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                StoryStatistic statistic;
                if (dataSnapshot.exists()) {
                    statistic = dataSnapshot.getValue(StoryStatistic.class);
                } else {
                    statistic = new StoryStatistic(storyId);
                    statistic.setId(statisticId);

                }

                if (statistic != null) {
                    statistic.setFavorite(isFavorite);

                    statisticsRef.child(statisticId).setValue(statistic)
                            .addOnSuccessListener(aVoid -> listener.onSuccess())
                            .addOnFailureListener(e -> listener.onError(e.getMessage()));
                } else {
                    listener.onError("Σφάλμα κατά την ενημέρωση των στατιστικών");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                listener.onError(databaseError.getMessage());
            }
        });
    }


    private String createStatisticId(String storyId) {
        String userId = getCurrentUserId();
        if (userId == null) {
            return storyId;
        }
        return userId + "_" + storyId;
    }

    public void getAllStatistics(OnStatisticsLoadedListener listener) {
        String userId = getCurrentUserId();
        if (userId == null) {
            listener.onError(new Exception("Δεν έχετε συνδεθεί"));
            return;
        }

        statisticsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<StoryStatistic> statistics = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    StoryStatistic statistic = snapshot.getValue(StoryStatistic.class);
                    if (statistic != null && statistic.getId().startsWith(userId + "_")) {
                        statistics.add(statistic);
                    }
                }
                listener.onStatisticsLoaded(statistics);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                listener.onError(new Exception(databaseError.getMessage()));
            }
        });
    }

    public void updateStatistic(StoryStatistic statistic, OnOperationCompleteListener listener) {
        String userId = getCurrentUserId();
        if (userId == null) {
            listener.onError("Δεν έχετε συνδεθεί");
            return;
        }

        String statisticId = createStatisticId(statistic.getStoryId());
        statistic.setId(statisticId);

        statisticsRef.child(statisticId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    StoryStatistic existingStatistic = dataSnapshot.getValue(StoryStatistic.class);
                    if (existingStatistic != null) {
                        existingStatistic.incrementPlayCount();
                        statisticsRef.child(statisticId).setValue(existingStatistic)
                                .addOnSuccessListener(aVoid -> listener.onSuccess())
                                .addOnFailureListener(e -> listener.onError(e.getMessage()));
                    } else {

                        statisticsRef.child(statisticId).setValue(statistic)
                                .addOnSuccessListener(aVoid -> listener.onSuccess())
                                .addOnFailureListener(e -> listener.onError(e.getMessage()));
                    }
                } else {

                    statisticsRef.child(statisticId).setValue(statistic)
                            .addOnSuccessListener(aVoid -> listener.onSuccess())
                            .addOnFailureListener(e -> listener.onError(e.getMessage()));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                listener.onError(databaseError.getMessage());
            }
        });
    }

    public void updateStoryStatistic(Story story) {
        if (story == null) return;

        String userId = getCurrentUserId();
        if (userId == null) return;

        String statisticId = createStatisticId(story.getId());

        statisticsRef.child(statisticId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                StoryStatistic statistic;
                if (dataSnapshot.exists()) {
                    statistic = dataSnapshot.getValue(StoryStatistic.class);
                    if (statistic != null) {
                        statistic.incrementPlayCount();
                    } else {
                        statistic = new StoryStatistic(story.getId());
                        statistic.setId(statisticId);
                        statistic.incrementPlayCount();
                    }
                } else {
                    statistic = new StoryStatistic(story.getId());
                    statistic.setId(statisticId);
                    statistic.incrementPlayCount();
                }

                statisticsRef.child(statisticId).setValue(statistic);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Σφάλμα κατά την ενημέρωση στατιστικών: " + databaseError.getMessage());
            }
        });
    }

    public void updateStoryFavorite(Story story) {
        if (story == null) return;

        String userId = getCurrentUserId();
        if (userId == null) return;

        userFavoritesRef.child(userId).child(story.getId()).setValue(story.isFavorite())
                .addOnSuccessListener(aVoid -> {

                    updateFavoriteStatistics(story.getId(), story.isFavorite(), new OnOperationCompleteListener() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "Επιτυχής ενημέρωση αγαπημένων");
                        }

                        @Override
                        public void onError(String errorMessage) {
                            Log.e(TAG, "Σφάλμα κατά την ενημέρωση αγαπημένων: " + errorMessage);
                        }
                    });
                })
                .addOnFailureListener(e -> Log.e(TAG, "Σφάλμα κατά την ενημέρωση αγαπημένων: " + e.getMessage()));
    }


    public void resetAllStatistics(Runnable onComplete) {
        String userId = getCurrentUserId();
        if (userId == null) {
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }

        statisticsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String key = snapshot.getKey();
                    if (key != null && key.startsWith(userId + "_")) {
                        statisticsRef.child(key).removeValue();
                    }
                }

                if (onComplete != null) {
                    onComplete.run();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Σφάλμα κατά την επαναφορά στατιστικών: " + databaseError.getMessage());
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
    }
}

