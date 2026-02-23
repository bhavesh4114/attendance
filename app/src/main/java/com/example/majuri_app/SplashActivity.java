package com.example.majuri_app;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.OvershootInterpolator;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Premium animated splash screen with logo scale/fade, notification dot pulse,
 * app name and subtitle reveal, then exit to LoginActivity after 2.5s.
 */
public class SplashActivity extends AppCompatActivity {

    private static final int LOGO_DURATION_MS = 600;
    private static final int DOT_DELAY_MS = 300;
    private static final int APP_NAME_DELAY_MS = 500;
    private static final int APP_NAME_DURATION_MS = 600;
    private static final int SUBTITLE_DELAY_MS = 700;
    private static final int TAGLINE_DELAY_MS = 900;
    private static final int SECURITY_DELAY_MS = 1000;
    private static final int EXIT_DELAY_MS = 2500;
    private static final int EXIT_DURATION_MS = 400;

    private View splashRoot;
    private View logoContainer;
    private View notificationDot;
    private View appName;
    private View subtitle;
    private View tagline;
    private View securityRow;
    private ValueAnimator pulseAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        splashRoot = findViewById(R.id.splashRoot);
        logoContainer = findViewById(R.id.logoContainer);
        notificationDot = findViewById(R.id.notificationDot);
        appName = findViewById(R.id.appName);
        subtitle = findViewById(R.id.subtitle);
        tagline = findViewById(R.id.tagline);
        securityRow = findViewById(R.id.securityRow);

        startEnterAnimations();
        scheduleExit();
    }

    private void startEnterAnimations() {
        // 1) Logo: scale 0.8 -> 1, fade in, 600ms, OvershootInterpolator
        logoContainer.setScaleX(0.8f);
        logoContainer.setScaleY(0.8f);
        ObjectAnimator logoScaleX = ObjectAnimator.ofFloat(logoContainer, View.SCALE_X, 0.8f, 1f);
        ObjectAnimator logoScaleY = ObjectAnimator.ofFloat(logoContainer, View.SCALE_Y, 0.8f, 1f);
        ObjectAnimator logoAlpha = ObjectAnimator.ofFloat(logoContainer, View.ALPHA, 0f, 1f);
        logoScaleX.setDuration(LOGO_DURATION_MS);
        logoScaleY.setDuration(LOGO_DURATION_MS);
        logoAlpha.setDuration(LOGO_DURATION_MS);
        OvershootInterpolator overshoot = new OvershootInterpolator(1.2f);
        logoScaleX.setInterpolator(overshoot);
        logoScaleY.setInterpolator(overshoot);
        AnimatorSet logoSet = new AnimatorSet();
        logoSet.playTogether(logoScaleX, logoScaleY, logoAlpha);
        logoSet.start();

        // 2) Notification dot: delay 300ms, fade + scale pop, then infinite pulse
        notificationDot.setScaleX(0f);
        notificationDot.setScaleY(0f);
        notificationDot.postDelayed(this::animateDot, DOT_DELAY_MS);

        // 3) App name: delay 500ms, translateY 40dp -> 0, fade in, 600ms
        float translatePx = 40f * getResources().getDisplayMetrics().density;
        appName.setTranslationY(translatePx);
        appName.postDelayed(() -> {
            appName.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(APP_NAME_DURATION_MS)
                    .start();
        }, APP_NAME_DELAY_MS);

        // 4) Subtitle: delay 700ms, fade in
        subtitle.postDelayed(() -> subtitle.animate().alpha(1f).setDuration(400).start(), SUBTITLE_DELAY_MS);

        // 5) Tagline: delay 900ms, fade in
        tagline.postDelayed(() -> tagline.animate().alpha(1f).setDuration(400).start(), TAGLINE_DELAY_MS);

        // 6) Security row: subtle fade after 1000ms
        securityRow.postDelayed(() -> securityRow.animate().alpha(1f).setDuration(500).start(), SECURITY_DELAY_MS);
    }

    private void animateDot() {
        ObjectAnimator dotScaleX = ObjectAnimator.ofFloat(notificationDot, View.SCALE_X, 0f, 1f);
        ObjectAnimator dotScaleY = ObjectAnimator.ofFloat(notificationDot, View.SCALE_Y, 0f, 1f);
        ObjectAnimator dotAlpha = ObjectAnimator.ofFloat(notificationDot, View.ALPHA, 0f, 1f);
        dotScaleX.setDuration(250);
        dotScaleY.setDuration(250);
        dotAlpha.setDuration(250);
        OvershootInterpolator overshoot = new OvershootInterpolator(1.5f);
        dotScaleX.setInterpolator(overshoot);
        dotScaleY.setInterpolator(overshoot);
        AnimatorSet popSet = new AnimatorSet();
        popSet.playTogether(dotScaleX, dotScaleY, dotAlpha);
        popSet.start();

        // Infinite subtle pulse
        startPulseAnimation();
    }

    private void startPulseAnimation() {
        pulseAnimator = ValueAnimator.ofFloat(1f, 1.15f, 1f);
        pulseAnimator.setDuration(1200);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setRepeatMode(ValueAnimator.RESTART);
        pulseAnimator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            notificationDot.setScaleX(value);
            notificationDot.setScaleY(value);
        });
        notificationDot.postDelayed(() -> {
            if (pulseAnimator != null) pulseAnimator.start();
        }, 400);
    }

    private void scheduleExit() {
        new Handler(Looper.getMainLooper()).postDelayed(this::runExitTransition, EXIT_DELAY_MS);
    }

    private void runExitTransition() {
        if (pulseAnimator != null && pulseAnimator.isRunning()) {
            pulseAnimator.cancel();
        }
        // Scale 1.0 -> 1.05, fade out
        splashRoot.animate()
                .scaleX(1.05f)
                .scaleY(1.05f)
                .alpha(0f)
                .setDuration(EXIT_DURATION_MS)
                .withEndAction(() -> {
                    startActivity(new Intent(this, LoginActivity.class));
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    finish();
                })
                .start();
    }

    @Override
    protected void onDestroy() {
        if (pulseAnimator != null && pulseAnimator.isRunning()) {
            pulseAnimator.cancel();
        }
        super.onDestroy();
    }
}
