package com.unipi.eianagn.unipiaudiostories.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.unipi.eianagn.unipiaudiostories.R;
import com.unipi.eianagn.unipiaudiostories.models.Story;
import com.unipi.eianagn.unipiaudiostories.models.StoryStatistic;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatisticsAdapter extends RecyclerView.Adapter<StatisticsAdapter.StatisticViewHolder> {
    private final Context context;
    private List<StoryStatistic> statistics;
    private Map<String, Story> storyMap;
    private final SimpleDateFormat dateFormat;

    public StatisticsAdapter(Context context, List<StoryStatistic> statistics, Map<String, Story> storyMap) {
        this.context = context;
        this.statistics = statistics;
        this.storyMap = storyMap;
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    }

    @NonNull
    @Override
    public StatisticViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_statistic, parent, false);
        return new StatisticViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StatisticViewHolder holder, int position) {
        StoryStatistic statistic = statistics.get(position);
        Story story = storyMap.get(statistic.getStoryId());

        if (story != null) {
            holder.tvStoryTitle.setText(story.getTitle());


            int resourceId = context.getResources().getIdentifier(story.getImageName(), "drawable", context.getPackageName());
            if (resourceId != 0) {
                Glide.with(context)
                        .load(resourceId)
                        .centerCrop()
                        .into(holder.ivStoryImage);
            } else {

                holder.ivStoryImage.setImageResource(R.drawable.app_logo);
            }

            if (statistic.isFavorite()) {
                holder.ivFavorite.setImageResource(R.drawable.ic_favorite);
                holder.ivFavorite.setVisibility(View.VISIBLE);
            } else {
                holder.ivFavorite.setVisibility(View.GONE);
            }
        } else {
            holder.tvStoryTitle.setText(R.string.unknown_story);
            holder.ivStoryImage.setImageResource(R.drawable.app_logo);
            holder.ivFavorite.setVisibility(View.GONE);
        }


        holder.tvPlayCount.setText(context.getString(R.string.play_count) + ": " + statistic.getPlayCount());

        Date lastPlayedDate = new Date(statistic.getLastPlayedTimestamp());
        holder.tvLastPlayed.setText(context.getString(R.string.last_played) + ": " + dateFormat.format(lastPlayedDate));
    }

    @Override
    public int getItemCount() {
        return statistics.size();
    }

    public void updateData(List<StoryStatistic> newStatistics, Map<String, Story> newStoryMap) {
        this.statistics = newStatistics;
        this.storyMap = newStoryMap;
        notifyDataSetChanged();
    }

    public static class StatisticViewHolder extends RecyclerView.ViewHolder {
        ImageView ivStoryImage, ivFavorite;
        TextView tvStoryTitle, tvPlayCount, tvLastPlayed;

        public StatisticViewHolder(@NonNull View itemView) {
            super(itemView);
            ivStoryImage = itemView.findViewById(R.id.ivStoryImage);
            ivFavorite = itemView.findViewById(R.id.ivFavorite);
            tvStoryTitle = itemView.findViewById(R.id.tvStoryTitle);
            tvPlayCount = itemView.findViewById(R.id.tvPlayCount);
            tvLastPlayed = itemView.findViewById(R.id.tvLastPlayed);
        }
    }
}