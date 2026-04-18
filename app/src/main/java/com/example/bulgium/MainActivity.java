package com.example.bulgium;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.SharedPreferences;
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

import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private BottomNavigationView bottomNavigation;
    private NavigationView navigationView;
    private ImageView btnOpenDrawer;
    private MaterialCardView bottomNavCard;
    private ViewTreeObserver.OnGlobalLayoutListener keyboardLayoutListener;

    // Onboarding State
    private View onboardingRoot;
    private TextView tvOnboardingTitle, tvOnboardingDesc;
    private MaterialButton btnOnboardingNext, btnOnboardingSkip;
    private int onboardingStep = 0;

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
        setContentView(R.layout.activity_main);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            final WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
        }

        drawerLayout = findViewById(R.id.drawer_layout);
        bottomNavigation = findViewById(R.id.bottom_navigation);
        navigationView = findViewById(R.id.navigation_view);
        btnOpenDrawer = findViewById(R.id.btn_open_drawer);
        bottomNavCard = findViewById(R.id.bottom_nav_card);

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

        if (!isFirstRun) return;

        new TapTargetSequence(this)
                .targets(
                        TapTarget.forView(findViewById(R.id.btn_open_drawer), "Main Menu", "Access all features like Cash Flow, Calculator, and Settings here.")
                                .outerCircleColor(R.color.primary)
                                .targetCircleColor(android.R.color.white)
                                .titleTextSize(20)
                                .descriptionTextSize(14)
                                .descriptionTextColor(android.R.color.white)
                                .drawShadow(true)
                                .cancelable(false)
                                .tintTarget(true)
                                .transparentTarget(true),
                        TapTarget.forView(findViewById(R.id.bottom_navigation), "Quick Navigation", "Switch between your Home dashboard and deep Financial Analytics.")
                                .outerCircleColor(R.color.primary)
                                .targetCircleColor(android.R.color.white)
                                .titleTextSize(20)
                                .descriptionTextSize(14)
                                .descriptionTextColor(android.R.color.white)
                                .drawShadow(true)
                                .cancelable(false)
                                .tintTarget(true)
                                .transparentTarget(true)
                )
                .listener(new TapTargetSequence.Listener() {
                    @Override
                    public void onSequenceFinish() {
                        prefs.edit().putBoolean("is_first_run", false).apply();
                    }

                    @Override
                    public void onSequenceStep(TapTarget lastTarget, boolean targetClicked) {}

                    @Override
                    public void onSequenceCanceled(TapTarget lastTarget) {}
                })
                .start();
    }

    private void updateOnboardingUI() {
        switch (onboardingStep) {
            case 1:
                tvOnboardingTitle.setText("Track with Ease");
                tvOnboardingDesc.setText("Swipe left to delete or right to edit your transactions instantly.");
                break;
            case 2:
                tvOnboardingTitle.setText("Financial Pulse");
                tvOnboardingDesc.setText("Monitor your Daily Allowance and Savings Health at a single glance.");
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
        onboardingRoot.setVisibility(View.GONE);
        getSharedPreferences("bulgium_prefs", MODE_PRIVATE)
                .edit()
                .putBoolean("is_first_run", false)
                .apply();
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
