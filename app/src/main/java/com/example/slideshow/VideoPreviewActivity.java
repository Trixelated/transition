package com.example.slideshow;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.example.slideshow.databinding.ActivityVideoPreviewBinding;

public class VideoPreviewActivity extends AppCompatActivity {

    public static final String KEY_PATH = "VideoPreviewActivity_PATH";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityVideoPreviewBinding binding = ActivityVideoPreviewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.videoView.setVideoPath(getIntent().getStringExtra(KEY_PATH));
        binding.videoView.start();

        binding.videoView.setOnClickListener(view -> {
            try {
                if (binding.videoView.isPlaying()) {
                    binding.videoView.pause();
                } else {
                    binding.videoView.start();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        });


    }
}