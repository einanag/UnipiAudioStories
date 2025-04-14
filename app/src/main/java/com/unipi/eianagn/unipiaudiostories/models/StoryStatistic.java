package com.unipi.eianagn.unipiaudiostories.models;

import java.io.Serializable;
import java.util.Date;

public class StoryStatistic implements Serializable {
    private String id;
    private String storyId;
    private int playCount;
    private long lastPlayedTimestamp;
    private boolean favorite;


    public StoryStatistic() {
    }

    public StoryStatistic(String storyId) {
        this.id = storyId;
        this.storyId = storyId;
        this.playCount = 0;  // Αλλαγή από 1 σε 0
        this.lastPlayedTimestamp = new Date().getTime();
        this.favorite = false;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStoryId() {
        return storyId;
    }

    public void setStoryId(String storyId) {
        this.storyId = storyId;
    }

    public int getPlayCount() {
        return playCount;
    }

    public void setPlayCount(int playCount) {
        this.playCount = playCount;
    }

    public long getLastPlayedTimestamp() {
        return lastPlayedTimestamp;
    }

    public void setLastPlayedTimestamp(long lastPlayedTimestamp) {
        this.lastPlayedTimestamp = lastPlayedTimestamp;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public void incrementPlayCount() {
        this.playCount++;
        this.lastPlayedTimestamp = new Date().getTime();
    }

    @Override
    public String toString() {
        return "StoryStatistic{" +
                "id='" + id + '\'' +
                ", storyId='" + storyId + '\'' +
                ", playCount=" + playCount +
                ", lastPlayedTimestamp=" + lastPlayedTimestamp +
                ", favorite=" + favorite +
                '}';
    }
}

