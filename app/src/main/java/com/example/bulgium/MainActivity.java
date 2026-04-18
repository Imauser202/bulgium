package com.example.bulgium;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.animation.AnticipateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetSequence;
import com.getkeepsafe.taptargetview.TapTargetView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.navigation.NavigationView;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private BottomNavigationView bottomNavigation;
    private NavigationView navigationView;
    private ImageView btnOpenDrawer;
    private MaterialCardView bottomNavCard;
    private View toolbarLayout;
    private ViewTreeObserver.OnGlobalLayoutListener keyboardLayoutListener;

    // Onboarding UI Elements
    private View onboardingRoot;
    private TextView tvOnboardingTitle, tvOnboardingDesc;
    private MaterialButton btnOnboardingNext, btnOnboardingSkip;

    // Onboarding State
    private int onboardingStep = 0;
    public static boolean isTutorialRunning = false;

    // Calculator State
    private double firstValue = Double.NaN;
    private char currentOperation = ' ';
    private boolean isNewOp = true;
    private DecimalFormat decimalFormat = new DecimalFormat("#.##########");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences settingsPrefs = getSharedPreferences("bulgium_settings", Context.MODE_PRIVATE);
        boolean isDark = settingsPrefs.getBoolean("dark_mode", false);
        AppCompatDelegate.setDefaultNightMode(isDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

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
        
        // Modern Edge-to-Edge: Handle notches properly on all phones (Infinix, etc.)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        
        setContentView(R.layout.activity_main);

        drawerLayout = findViewById(R.id.drawer_layout);
        bottomNavigation = findViewById(R.id.bottom_navigation);
        navigationView = findViewById(R.id.navigation_view);
        btnOpenDrawer = findViewById(R.id.btn_open_drawer);
        bottomNavCard = findViewById(R.id.bottom_nav_card);
        toolbarLayout = findViewById(R.id.toolbar_layout);
        View notchSpacer = findViewById(R.id.notch_spacer);

        // Dynamic Notch Support: Adjust the spacer height instead of padding
        ViewCompat.setOnApplyWindowInsetsListener(toolbarLayout, (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            if (insets.getDisplayCutout() != null) {
                topInset = Math.max(topInset, insets.getDisplayCutout().getSafeInsetTop());
            }
            
            // Set the spacer's height to match the notch
            if (notchSpacer != null) {
                android.view.ViewGroup.LayoutParams params = notchSpacer.getLayoutParams();
                params.height = topInset;
                notchSpacer.setLayoutParams(params);
            }
            
            // Handle bottom insets for the navigation island (Gesture bar support)
            int bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            if (bottomNavCard != null) {
                android.view.ViewGroup.MarginLayoutParams marginParams = (android.view.ViewGroup.MarginLayoutParams) bottomNavCard.getLayoutParams();
                marginParams.bottomMargin = (int) (24 * getResources().getDisplayMetrics().density) + bottomInset;
                bottomNavCard.setLayoutParams(marginParams);
            }

            return insets;
        });

        // Initialize Onboarding Views
        onboardingRoot = findViewById(R.id.onboarding_root);
        tvOnboardingTitle = findViewById(R.id.tv_onboarding_title);
        tvOnboardingDesc = findViewById(R.id.tv_onboarding_desc);
        btnOnboardingNext = findViewById(R.id.btn_onboarding_next);
        btnOnboardingSkip = findViewById(R.id.btn_onboarding_skip);

        if (btnOnboardingNext != null) {
            btnOnboardingNext.setOnClickListener(v -> {
                onboardingStep++;
                updateOnboardingUI();
            });
        }
        if (btnOnboardingSkip != null) {
            btnOnboardingSkip.setOnClickListener(v -> finishOnboarding());
        }

        setupOnboarding();
        setupKeyboardListener();

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
            bottomNavigation.setSelectedItemId(R.id.navigation_home);
        }

        bottomNavigation.setOnItemSelectedListener(item -> {
            triggerSoftFeedback(bottomNavigation);
            Fragment selected = null;
            int id = item.getItemId();
            if (id == R.id.navigation_home) selected = new HomeFragment();
            else if (id == R.id.navigation_dashboard) selected = new DashboardFragment();
            
            if (selected != null) loadFragment(selected);
            return true;
        });

        btnOpenDrawer.setOnClickListener(v -> {
            triggerSoftFeedback(v);
            drawerLayout.openDrawer(GravityCompat.START);
        });

        navigationView.setNavigationItemSelectedListener(item -> {
            triggerSoftFeedback(navigationView);
            Fragment selected = null;
            int id = item.getItemId();
            
            if (id == R.id.nav_add_record) selected = new AddTransactionFragment();
            else if (id == R.id.nav_calculator) {
                showCalculatorDialog();
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }
            else if (id == R.id.nav_cash_flow) selected = new CashFlowFragment();
            else if (id == R.id.nav_calendar) selected = new CalendarFragment();
            else if (id == R.id.nav_reminders) selected = new ReminderFragment();
            else if (id == R.id.nav_savings) selected = new SavingsFragment();
            else if (id == R.id.nav_reports) selected = new ReportsFragment();
            else if (id == R.id.nav_settings) selected = new SettingsFragment();

            if (selected != null) {
                loadFragmentWithBackStack(selected);
                drawerLayout.closeDrawer(GravityCompat.START);
            }
            return true;
        });
    }

    private void setupKeyboardListener() {
        final View contentView = findViewById(android.R.id.content);
        keyboardLayoutListener = () -> {
            android.graphics.Rect r = new android.graphics.Rect();
            contentView.getWindowVisibleDisplayFrame(r);
            int screenHeight = contentView.getRootView().getHeight();
            int keypadHeight = screenHeight - r.bottom;

            if (keypadHeight > screenHeight * 0.15) { // Keyboard is visible
                if (bottomNavCard != null) bottomNavCard.setVisibility(View.GONE);
            } else { // Keyboard is hidden
                if (bottomNavCard != null) bottomNavCard.setVisibility(View.VISIBLE);
            }
        };
        contentView.getViewTreeObserver().addOnGlobalLayoutListener(keyboardLayoutListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (keyboardLayoutListener != null) {
            findViewById(android.R.id.content).getViewTreeObserver().removeOnGlobalLayoutListener(keyboardLayoutListener);
        }
    }

    private void triggerSoftFeedback(View v) {
        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
    }

    private void setupOnboarding() {
        SharedPreferences prefs = getSharedPreferences("bulgium_prefs", MODE_PRIVATE);
        boolean isFirstRun = prefs.getBoolean("is_first_run", true);

        if (isFirstRun) {
            onboardingStep = 0;
            updateOnboardingUI();
            onboardingRoot.setVisibility(View.VISIBLE);
        }
    }

    private void startTapTargetTutorial() {
        new TapTargetSequence(this)
                .targets(
                        TapTarget.forView(findViewById(R.id.btn_open_drawer), "Main Menu", "Access all features like Cash Flow, Calculator, and Settings here.")
                                .id(1)
                                .outerCircleColor(R.color.primary)
                                .targetCircleColor(android.R.color.white)
                                .titleTextSize(28)
                                .descriptionTextSize(18)
                                .titleTextColor(android.R.color.white)
                                .descriptionTextColor(android.R.color.white)
                                .textTypeface(Typeface.SANS_SERIF)
                                .drawShadow(true)
                                .cancelable(false)
                                .tintTarget(true)
                                .transparentTarget(true)
                                .targetRadius(40),
                        TapTarget.forView(findViewById(R.id.bottom_navigation), "Quick Navigation", "Switch between your Home dashboard and deep Financial Analytics.")
                                .id(2)
                                .outerCircleColor(R.color.primary)
                                .targetCircleColor(android.R.color.white)
                                .titleTextSize(28)
                                .descriptionTextSize(18)
                                .titleTextColor(android.R.color.white)
                                .descriptionTextColor(android.R.color.white)
                                .textTypeface(Typeface.SANS_SERIF)
                                .drawShadow(true)
                                .cancelable(false)
                                .tintTarget(true)
                                .transparentTarget(true)
                                .targetRadius(60)
                )
                .listener(new TapTargetSequence.Listener() {
                    @Override
                    public void onSequenceFinish() {
                        // Optimizing for all phones: Use a listener instead of a timer
                        drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
                            @Override
                            public void onDrawerOpened(View drawerView) {
                                startMenuTutorial();
                                drawerLayout.removeDrawerListener(this);
                            }
                        });
                        drawerLayout.openDrawer(GravityCompat.START);
                    }

                    @Override
                    public void onSequenceStep(TapTarget lastTarget, boolean targetClicked) {}

                    @Override
                    public void onSequenceCanceled(TapTarget lastTarget) {
                        finishTutorial();
                    }
                })
                .start();
    }

    private void startMenuTutorial() {
        // Wait for drawer to be fully open before searching for views
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            navigationView.post(() -> {
                View addRecord = navigationView.findViewById(R.id.nav_add_record);
                View calculator = navigationView.findViewById(R.id.nav_calculator);
                View cashFlow = navigationView.findViewById(R.id.nav_cash_flow);
                View calendar = navigationView.findViewById(R.id.nav_calendar);
                View reminders = navigationView.findViewById(R.id.nav_reminders);
                View savings = navigationView.findViewById(R.id.nav_savings);

                if (addRecord == null) {
                    finishTutorial();
                    return;
                }

                new TapTargetSequence(this)
                        .targets(
                                TapTarget.forView(addRecord, "New Record", "Quickly add your expenses or income here.")
                                        .outerCircleColor(R.color.primary)
                                        .targetCircleColor(android.R.color.white)
                                        .titleTextSize(28)
                                        .descriptionTextSize(18)
                                        .titleTextColor(android.R.color.white)
                                        .descriptionTextColor(android.R.color.white)
                                        .textTypeface(Typeface.SANS_SERIF)
                                        .drawShadow(true)
                                        .cancelable(false)
                                        .transparentTarget(true)
                                        .targetRadius(35),
                                TapTarget.forView(calculator, "Smart Calculator", "Perform quick financial calculations without leaving the app.")
                                        .outerCircleColor(R.color.primary)
                                        .targetCircleColor(android.R.color.white)
                                        .titleTextSize(28)
                                        .descriptionTextSize(18)
                                        .titleTextColor(android.R.color.white)
                                        .descriptionTextColor(android.R.color.white)
                                        .textTypeface(Typeface.SANS_SERIF)
                                        .drawShadow(true)
                                        .cancelable(false)
                                        .transparentTarget(true)
                                        .targetRadius(35),
                                TapTarget.forView(cashFlow, "Cash Flow Analytics", "Analyze your spending habits with detailed charts.")
                                        .outerCircleColor(R.color.primary)
                                        .targetCircleColor(android.R.color.white)
                                        .titleTextSize(28)
                                        .descriptionTextSize(18)
                                        .titleTextColor(android.R.color.white)
                                        .descriptionTextColor(android.R.color.white)
                                        .textTypeface(Typeface.SANS_SERIF)
                                        .drawShadow(true)
                                        .cancelable(false)
                                        .transparentTarget(true)
                                        .targetRadius(35),
                                TapTarget.forView(calendar, "Financial Calendar", "Keep track of your daily spending over time.")
                                        .outerCircleColor(R.color.primary)
                                        .targetCircleColor(android.R.color.white)
                                        .titleTextSize(28)
                                        .descriptionTextSize(18)
                                        .titleTextColor(android.R.color.white)
                                        .descriptionTextColor(android.R.color.white)
                                        .textTypeface(Typeface.SANS_SERIF)
                                        .drawShadow(true)
                                        .cancelable(false)
                                        .transparentTarget(true)
                                        .targetRadius(35),
                                TapTarget.forView(reminders, "Reminders", "Never miss a bill payment or saving goal deadline again.")
                                        .outerCircleColor(R.color.primary)
                                        .targetCircleColor(android.R.color.white)
                                        .titleTextSize(28)
                                        .descriptionTextSize(18)
                                        .titleTextColor(android.R.color.white)
                                        .descriptionTextColor(android.R.color.white)
                                        .textTypeface(Typeface.SANS_SERIF)
                                        .drawShadow(true)
                                        .cancelable(false)
                                        .transparentTarget(true)
                                        .targetRadius(35),
                                TapTarget.forView(savings, "Savings Goals", "Set targets and track your progress towards financial freedom.")
                                        .outerCircleColor(R.color.primary)
                                        .targetCircleColor(android.R.color.white)
                                        .titleTextSize(28)
                                        .descriptionTextSize(18)
                                        .titleTextColor(android.R.color.white)
                                        .descriptionTextColor(android.R.color.white)
                                        .textTypeface(Typeface.SANS_SERIF)
                                        .drawShadow(true)
                                        .cancelable(false)
                                        .transparentTarget(true)
                                        .targetRadius(35)
                        )
                        .listener(new TapTargetSequence.Listener() {
                            @Override
                            public void onSequenceFinish() {
                                finishTutorial();
                            }

                            @Override
                            public void onSequenceStep(TapTarget lastTarget, boolean targetClicked) {}

                            @Override
                            public void onSequenceCanceled(TapTarget lastTarget) {
                                finishTutorial();
                            }
                        })
                        .start();
            });
        } else {
            // Re-open if closed or retry
            drawerLayout.openDrawer(GravityCompat.START);
            findViewById(android.R.id.content).postDelayed(() -> startMenuTutorial(), 500);
        }
    }

    private void finishTutorial() {
        isTutorialRunning = false;
        drawerLayout.closeDrawer(GravityCompat.START);
        getSharedPreferences("bulgium_prefs", MODE_PRIVATE)
                .edit()
                .putBoolean("is_first_run", false)
                .apply();

        // After the main tutorial and menu tutorial are done, trigger the Home tour
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (currentFragment instanceof HomeFragment) {
            ((HomeFragment) currentFragment).startLocalTour();
        }
    }

    private void updateOnboardingUI() {
        switch (onboardingStep) {
            case 0:
                tvOnboardingTitle.setText("Welcome to Bulgium!");
                tvOnboardingDesc.setText("Your personal financial co-pilot is ready to help you grow your wealth.");
                btnOnboardingNext.setText("Next");
                break;
            case 1:
                tvOnboardingTitle.setText("Track with Ease");
                tvOnboardingDesc.setText("Swipe left to delete or right to edit your transactions instantly.");
                btnOnboardingNext.setText("Next");
                break;
            case 2:
                tvOnboardingTitle.setText("Financial Pulse");
                tvOnboardingDesc.setText("Monitor your Daily Allowance and Savings Health at a single glance.");
                btnOnboardingNext.setText("Next");
                break;
            case 3:
                tvOnboardingTitle.setText("Cash Flow Trends");
                tvOnboardingDesc.setText("Check the side menu for detailed graphs of your income vs. expenses.");
                btnOnboardingNext.setText("Get Started");
                break;
            default:
                finishOnboarding();
                break;
        }
    }

    private void finishOnboarding() {
        if (isTutorialRunning) return;
        isTutorialRunning = true;

        // Force remove the onboarding view from the hierarchy to prevent ANY overlap
        if (onboardingRoot != null && onboardingRoot.getParent() != null) {
            ((android.view.ViewGroup) onboardingRoot.getParent()).removeView(onboardingRoot);
        }
        
        // Slight delay to allow the layout to re-calculate without the onboarding view
        findViewById(android.R.id.content).postDelayed(() -> startTapTargetTutorial(), 100);
    }

    private void showCalculatorDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_calculator, null);
        TextView tvCalcDisplay = dialogView.findViewById(R.id.tv_calc_display);
        
        firstValue = Double.NaN;
        currentOperation = ' ';
        isNewOp = true;

        View.OnClickListener numberListener = v -> {
            triggerSoftFeedback(v);
            MaterialButton button = (MaterialButton) v;
            String currentText = tvCalcDisplay.getText().toString();
            if (currentText.equals("0") || isNewOp) {
                tvCalcDisplay.setText(button.getText());
                isNewOp = false;
            } else {
                tvCalcDisplay.setText(currentText + button.getText());
            }
        };

        dialogView.findViewById(R.id.btn_calc_0).setOnClickListener(numberListener);
        dialogView.findViewById(R.id.btn_calc_1).setOnClickListener(numberListener);
        dialogView.findViewById(R.id.btn_calc_2).setOnClickListener(numberListener);
        dialogView.findViewById(R.id.btn_calc_3).setOnClickListener(numberListener);
        dialogView.findViewById(R.id.btn_calc_4).setOnClickListener(numberListener);
        dialogView.findViewById(R.id.btn_calc_5).setOnClickListener(numberListener);
        dialogView.findViewById(R.id.btn_calc_6).setOnClickListener(numberListener);
        dialogView.findViewById(R.id.btn_calc_7).setOnClickListener(numberListener);
        dialogView.findViewById(R.id.btn_calc_8).setOnClickListener(numberListener);
        dialogView.findViewById(R.id.btn_calc_9).setOnClickListener(numberListener);
        dialogView.findViewById(R.id.btn_calc_dot).setOnClickListener(v -> {
            triggerSoftFeedback(v);
            if (isNewOp) {
                tvCalcDisplay.setText("0.");
                isNewOp = false;
            } else if (!tvCalcDisplay.getText().toString().contains(".")) {
                tvCalcDisplay.setText(tvCalcDisplay.getText().toString() + ".");
            }
        });

        dialogView.findViewById(R.id.btn_calc_ac).setOnClickListener(v -> {
            triggerSoftFeedback(v);
            tvCalcDisplay.setText("0");
            firstValue = Double.NaN;
            currentOperation = ' ';
            isNewOp = true;
        });

        View.OnClickListener opListener = v -> {
            triggerSoftFeedback(v);
            MaterialButton button = (MaterialButton) v;
            String opStr = button.getText().toString();
            char nextOp = ' ';
            if (opStr.equals("+")) nextOp = '+';
            else if (opStr.equals("−")) nextOp = '-';
            else if (opStr.equals("×")) nextOp = '*';
            else if (opStr.equals("÷")) nextOp = '/';

            try {
                double val = Double.parseDouble(tvCalcDisplay.getText().toString());
                if (!Double.isNaN(firstValue)) {
                    calculateResult(val);
                    tvCalcDisplay.setText(decimalFormat.format(firstValue));
                } else {
                    firstValue = val;
                }
                currentOperation = nextOp;
                isNewOp = true;
            } catch (Exception e) {}
        };

        dialogView.findViewById(R.id.btn_calc_plus).setOnClickListener(opListener);
        dialogView.findViewById(R.id.btn_calc_minus).setOnClickListener(opListener);
        dialogView.findViewById(R.id.btn_calc_multiply).setOnClickListener(opListener);
        dialogView.findViewById(R.id.btn_calc_divide).setOnClickListener(opListener);

        dialogView.findViewById(R.id.btn_calc_equal).setOnClickListener(v -> {
            triggerSoftFeedback(v);
            if (!Double.isNaN(firstValue)) {
                try {
                    double secondValue = Double.parseDouble(tvCalcDisplay.getText().toString());
                    
                    // Easter Egg: 67 + 67
                    if (firstValue == 67 && secondValue == 67 && currentOperation == '+') {
                        showEasterEggVideo();
                    }

                    calculateResult(secondValue);
                    tvCalcDisplay.setText(decimalFormat.format(firstValue));
                    firstValue = Double.NaN;
                    currentOperation = ' ';
                    isNewOp = true;
                } catch (Exception e) {}
            }
        });

        dialogView.findViewById(R.id.btn_calc_plus_minus).setOnClickListener(v -> {
            triggerSoftFeedback(v);
            try {
                double val = Double.parseDouble(tvCalcDisplay.getText().toString());
                tvCalcDisplay.setText(decimalFormat.format(val * -1));
            } catch (Exception e) {}
        });

        dialogView.findViewById(R.id.btn_calc_percent).setOnClickListener(v -> {
            triggerSoftFeedback(v);
            try {
                double val = Double.parseDouble(tvCalcDisplay.getText().toString());
                tvCalcDisplay.setText(decimalFormat.format(val / 100));
            } catch (Exception e) {}
        });
        
        new AlertDialog.Builder(this)
                .setTitle(R.string.calc_title)
                .setView(dialogView)
                .setPositiveButton(R.string.calc_close, null)
                .show();
    }

    private void calculateResult(double secondValue) {
        switch (currentOperation) {
            case '+': firstValue += secondValue; break;
            case '-': firstValue -= secondValue; break;
            case '*': firstValue *= secondValue; break;
            case '/': firstValue = (secondValue == 0) ? 0 : firstValue / secondValue; break;
        }
    }

    private void showEasterEggVideo() {
        int resId = getResources().getIdentifier("easter_egg", "raw", getPackageName());
        if (resId == 0) return; // Video file not found

        View videoLayout = LayoutInflater.from(this).inflate(R.layout.dialog_easter_egg, null);
        VideoView videoView = videoLayout.findViewById(R.id.videoView);
        
        String videoPath = "android.resource://" + getPackageName() + "/" + resId;
        videoView.setVideoURI(Uri.parse(videoPath));

        AlertDialog videoDialog = new AlertDialog.Builder(this)
                .setView(videoLayout)
                .create();

        videoView.setOnCompletionListener(mp -> videoDialog.dismiss());
        videoView.setOnErrorListener((mp, what, extra) -> {
            videoDialog.dismiss();
            return true;
        });
        videoView.start();
        videoDialog.show();
    }

    private void loadFragment(Fragment f) {
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragment_container, f)
                .commit();
    }

    private void loadFragmentWithBackStack (Fragment f) {
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragment_container, f)
                .addToBackStack(null)
                .commit();
    }
}
