package com.unipi.eianagn.unipiaudiostories.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.unipi.eianagn.unipiaudiostories.R;
import com.unipi.eianagn.unipiaudiostories.models.Story;

import java.util.List;

public class StoryAdapter extends RecyclerView.Adapter<StoryAdapter.StoryViewHolder> {

    private final Context context;
    private final List<Story> stories;
    private final OnStoryClickListener listener;

    public interface OnStoryClickListener {
        void onStoryClick(Story story);
        void onFavoriteClick(Story story, int position);
    }

    public StoryAdapter(Context context, List<Story> stories, OnStoryClickListener listener) {
        this.context = context;
        this.stories = stories;
        this.listener = listener;
    }

    @NonNull
    @Override
    public StoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_story, parent, false);
        return new StoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StoryViewHolder holder, int position) {
        Story story = stories.get(position);
        holder.bind(story, position);
    }

    @Override
    public int getItemCount() {
        return stories.size();
    }

    class StoryViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivStoryImage;
        private final TextView tvStoryTitle;
        private final ImageButton btnFavorite;
        private final Button btnListen;

        StoryViewHolder(@NonNull View itemView) {
            super(itemView);
            ivStoryImage = itemView.findViewById(R.id.ivStoryImage);
            tvStoryTitle = itemView.findViewById(R.id.tvStoryTitle);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
            btnListen = itemView.findViewById(R.id.btnListen);
        }

        void bind(final Story story, final int position) {
            tvStoryTitle.setText(story.getTitle());

            int resourceId = context.getResources().getIdentifier(story.getImageName(), "drawable", context.getPackageName());
            if (resourceId != 0) {
                Glide.with(context)
                        .load(resourceId)
                        .centerCrop()
                        .into(ivStoryImage);
            } else {
                ivStoryImage.setImageResource(R.drawable.app_logo);
            }


            btnFavorite.setImageResource(story.isFavorite() ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);

            btnListen.setBackgroundResource(R.drawable.pixel_button_background);
            btnListen.setTextColor(context.getResources().getColor(R.color.buttonTextEnabled));

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onStoryClick(story);
                }
            });

            btnListen.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onStoryClick(story);
                }
            });

            btnFavorite.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onFavoriteClick(story, position);
                }
            });
        }
    }
}

