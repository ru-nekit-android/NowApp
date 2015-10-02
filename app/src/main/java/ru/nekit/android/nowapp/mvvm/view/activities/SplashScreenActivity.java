package ru.nekit.android.nowapp.mvvm.view.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import ru.nekit.android.nowapp.R;
import ru.nekit.android.nowapp.mvvm.view.fragments.SplashScreenFragment;

public class SplashScreenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        getSupportFragmentManager().beginTransaction().add(R.id.content_holder, new SplashScreenFragment()).commit();
    }

}
