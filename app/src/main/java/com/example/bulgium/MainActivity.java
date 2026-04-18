package com.example.bulgium;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AnticipateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.biometric.BiometricPrompt;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity {

    public static boolean isTutorialRunning = false;
    private DrawerLayout drawerLayout;
    private BottomNavigationView bottomNavigation;
    private NavigationView navigationView;
    private ImageView btnOpenDrawer;
    private FrameLayout bottomNavCard;
    private View toolbarLayout;
    private View biometricOverlay;
    private Runnable keyboardLayoutListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences settingsPrefs = getSharedPreferences("bulgium_settings", Context.MODE_PRIVATE);
        boolean isDark = settingsPrefs.getBoolean("dark_mode", false);
        AppCompatDelegate.setDefaultNightMode(isDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        // Apply Privacy Mode (Screenshot block) on startup
        boolean isPrivacyEnabled = settingsPrefs.getBoolean("privacy_mode", false);
        applyPrivacyMode(isPrivacyEnabled);

        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        splashScreen.setOnExitAnimationListener(splashScreenView -> {
            View view = splashScreenView.getView();
            ObjectAnimator alpha = ObjectAnimator.ofFloat(view, View.ALPHA, 1f, 0f);
            alpha.setDuration(250L);
            alpha.setInterpolator(new AnticipateInterpolator());
            alpha.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    splashScreenView.remove();
                }
            });
            alpha.start();
        });

        super.onCreate(savedInstanceState);
        
        // Modern Edge-to-Edge: Handle notches properly on all phones
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        
        setContentView(R.layout.activity_main);

        drawerLayout = findViewById(R.id.drawer_layout);
        bottomNavigation = findViewById(R.id.bottom_navigation);
        navigationView = findViewById(R.id.navigation_view);
        btnOpenDrawer = findViewById(R.id.btn_open_drawer);
        bottomNavCard = findViewById(R.id.bottom_nav_card);
        toolbarLayout = findViewById(R.id.toolbar_layout);
        biometricOverlay = findViewById(R.id.biometric_overlay);

        // Dynamic Notch Support
        ViewCompat.setOnApplyWindowInsetsListener(toolbarLayout, (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            if (insets.getDisplayCutout() != null) {
                topInset = Math.max(topInset, insets.getDisplayCutout().getSafeInsetTop());
            }
            v.setPadding(0, topInset, 0, 0);
            return insets;
        });

        setupNavigation();
        setupKeyboardListener();

        // Biometric Lock Check - Moved after setContentView and super.onCreate
        if (settingsPrefs.getBoolean("biometric_lock", false)) {
            biometricOverlay.setVisibility(View.VISIBLE);
            showBiometricPrompt();
        }

        // Set default fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }
    }

    public void applyPrivacyMode(boolean enabled) {
        if (enabled) {
            getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE,
                    android.view.WindowManager.LayoutParams.FLAG_SECURE);
        } else {
            getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);
        }
    }

    private void setupNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment selected = null;
            int id = item.getItemId();
            if (id == R.id.nav_home) selected = new HomeFragment();
            else if (id == R.id.nav_dashboard) selected = new DashboardFragment();
            else if (id == R.id.nav_cash_flow) selected = new CashFlowFragment();
            else if (id == R.id.nav_savings) selected = new SavingsFragment();

            if (selected != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selected)
                        .commit();
            }
            return true;
        });

        navigationView.setNavigationItemSelectedListener(item -> {
            Fragment selected = null;
            int id = item.getItemId();
            if (id == R.id.nav_add_record) selected = new AddTransactionFragment();
            else if (id == R.id.nav_calculator) {
                new CalculatorFragment().show(getSupportFragmentManager(), "calculator");
            }
            else if (id == R.id.nav_cash_flow) selected = new CashFlowFragment();
            else if (id == R.id.nav_savings) selected = new SavingsFragment();
            else if (id == R.id.nav_calendar) selected = new CalendarFragment();
            else if (id == R.id.nav_reminders) selected = new ReminderFragment();
            else if (id == R.id.nav_reports) selected = new ReportsFragment();
            else if (id == R.id.nav_settings) selected = new SettingsFragment();

            if (selected != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selected)
                        .commit();
            }
            drawerLayout.closeDrawers();
            return true;
        });

        btnOpenDrawer.setOnClickListener(v -> drawerLayout.openDrawer(androidx.core.view.GravityCompat.START));
    }

    private void showBiometricPrompt() {
        Executor executor = androidx.core.content.ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(MainActivity.this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                finish(); // Exit if auth fails or is cancelled
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                biometricOverlay.setVisibility(View.GONE);
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Bulgium Security")
                .setSubtitle("Unlock to access your financial data")
                .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG | 
                                         androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    private void setupKeyboardListener() {
        final View contentView = findViewById(android.R.id.content);
        keyboardLayoutListener = () -> {
            android.graphics.Rect r = new android.graphics.Rect();
            contentView.getWindowVisibleDisplayFrame(r);
            int screenHeight = contentView.getRootView().getHeight();
            int keypadHeight = screenHeight - r.bottom;

            if (keypadHeight > screenHeight * 0.15) {
                bottomNavCard.setVisibility(View.GONE);
            } else {
                bottomNavCard.setVisibility(View.VISIBLE);
            }
        };
        contentView.getViewTreeObserver().addOnGlobalLayoutListener(() -> keyboardLayoutListener.run());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (keyboardLayoutListener != null) {
            findViewById(android.R.id.content).getViewTreeObserver().removeOnGlobalLayoutListener(() -> keyboardLayoutListener.run());
        }
    }
}
