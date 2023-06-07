package com.faras.callingapp;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.faras.callingapp.databinding.ActivityMainBinding;
import com.permissionx.guolindev.PermissionX;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.enterBtn.setOnClickListener(view ->
                PermissionX.init(this).permissions(
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.CAMERA
                ).request((allGranted, grantedList, deniedList) -> {
                    if (allGranted) {
                        startActivity(
                                new Intent(this, CallActivity.class)
                                        .putExtra("username", binding.username.getText().toString()));
                    } else {
                        Toast.makeText(this, "Please Grant All Permissions", Toast.LENGTH_LONG);
                    }
                }));
    }
}