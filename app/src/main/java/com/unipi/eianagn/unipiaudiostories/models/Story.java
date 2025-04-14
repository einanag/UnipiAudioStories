package com.unipi.eianagn.unipiaudiostories.models;

import java.io.Serializable;

public class Story implements Serializable {
    private String id;
    private String title;
    private String content;
    private String imageName;
    private boolean favorite;


    public Story() {
    }

    public Story(String id, String title, String content, String imageName) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.imageName = imageName;
        this.favorite = false;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    @Override
    public String toString() {
        return "Story{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", imageName='" + imageName + '\'' +
                ", favorite=" + favorite +
                '}';
    }
}