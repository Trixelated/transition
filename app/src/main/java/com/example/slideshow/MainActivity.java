package com.example.slideshow;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.example.slideshow.databinding.ActivityMainBinding;
import com.example.slideshow.newslideshow.SlideShowActivity;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.imagePickers.setOnClickListener(view -> {
            pickMultipleMedia.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });

    }

    ActivityResultLauncher<PickVisualMediaRequest> pickMultipleMedia =
            registerForActivityResult(new ActivityResultContracts.PickMultipleVisualMedia(5), uris -> {
                if (!uris.isEmpty() && uris.size() >= 3) {
                    Log.d("PhotoPicker", "Number of items selected: " + uris.size());
                    startActivity(new Intent(this, SlideShowActivity.class)
                            .putStringArrayListExtra(SlideShowActivity.KEY_IMG_LIST,new ArrayList<>(uris.stream().map(Uri::toString).collect(Collectors.toList())))
                    );
                } else {
                    Toast.makeText(this, "Select at least 3 images", Toast.LENGTH_SHORT).show();
                    Log.d("PhotoPicker", "No media selected");
                }
            });
}