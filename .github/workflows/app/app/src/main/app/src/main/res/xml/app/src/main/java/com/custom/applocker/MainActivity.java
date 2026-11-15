package com.custom.applocker;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    SharedPreferences prefs;
    LinearLayout mainLayout, listContainer;
    boolean showHidden = false;
    int selectedLength = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("AppLockPrefs", MODE_PRIVATE);
        selectedLength = prefs.getInt("pass_len", 4);

        mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(Color.parseColor("#121212"));
        mainLayout.setPadding(30, 50, 30, 30);
        setContentView(mainLayout);

        String savedPass = prefs.getString("password", "");
        String targetPkg = getIntent().getStringExtra("LOCKED_PKG");

        if (savedPass.isEmpty()) {
            showSetupPasswordScreen();
        } else {
            showVerifyPasswordScreen(targetPkg);
        }
    }

    void showSetupPasswordScreen() {
        mainLayout.removeAllViews();
        TextView title = new TextView(this);
        title.setText("Set New Password (4, 6, or 15 Digits)");
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);
        mainLayout.addView(title);

        RadioGroup rg = new RadioGroup(this);
        rg.setOrientation(LinearLayout.HORIZONTAL);
        int[] lengths = {4, 6, 15};
        for (int len : lengths) {
            RadioButton rb = new RadioButton(this);
            rb.setText(len + " Digit");
            rb.setTextColor(Color.WHITE);
            rb.setId(len);
            if (len == selectedLength) rb.setChecked(true);
            rg.addView(rb);
        }
        mainLayout.addView(rg);

        EditText passInput = new EditText(this);
        passInput.setHint("Enter " + selectedLength + " digit password");
        passInput.setHintTextColor(Color.GRAY);
        passInput.setTextColor(Color.WHITE);
        passInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        passInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(selectedLength)});
        mainLayout.addView(passInput);

        rg.setOnCheckedChangeListener((group, checkedId) -> {
            selectedLength = checkedId;
            passInput.setText("");
            passInput.setHint("Enter " + selectedLength + " digit password");
            passInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(selectedLength)});
        });

        Button saveBtn = new Button(this);
        saveBtn.setText("Save Password");
        saveBtn.setOnClickListener(v -> {
            String p = passInput.getText().toString();
            if (p.length() != selectedLength) {
                Toast.makeText(this, "Please enter exact " + selectedLength + " digits!", Toast.LENGTH_SHORT).show();
                return;
            }
            prefs.edit().putString("password", p).putInt("pass_len", selectedLength).apply();
            Toast.makeText(this, "Password Saved!", Toast.LENGTH_SHORT).show();
            showDashboard();
        });
        mainLayout.addView(saveBtn);
    }

    void showVerifyPasswordScreen(String lockedPkg) {
        mainLayout.removeAllViews();
        int len = prefs.getInt("pass_len", 4);

        TextView title = new TextView(this);
        title.setText(lockedPkg != null ? "App Locked! Enter " + len + "-Digit PIN" : "Enter Master PIN");
        title.setTextColor(Color.WHITE);
        title.setTextSize(20);
        title.setGravity(Gravity.CENTER);
        mainLayout.addView(title);

        EditText passInput = new EditText(this);
        passInput.setTextColor(Color.WHITE);
        passInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        passInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(len)});
        mainLayout.addView(passInput);

        Button unlockBtn = new Button(this);
        unlockBtn.setText("Unlock");
        unlockBtn.setOnClickListener(v -> {
            String saved = prefs.getString("password", "");
            if (passInput.getText().toString().equals(saved)) {
                if (lockedPkg != null) {
                    prefs.edit().putString("temp_unlock", lockedPkg).apply();
                    finish();
                } else {
                    showDashboard();
                }
            } else {
                Toast.makeText(this, "Wrong Password!", Toast.LENGTH_SHORT).show();
                passInput.setText("");
            }
        });
        mainLayout.addView(unlockBtn);
    }

    void showDashboard() {
        mainLayout.removeAllViews();

        LinearLayout topBtns = new LinearLayout(this);
        topBtns.setOrientation(LinearLayout.HORIZONTAL);

        Button permBtn = new Button(this);
        permBtn.setText("Enable Lock Service");
        permBtn.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        topBtns.addView(permBtn);

        Button changePassBtn = new Button(this);
        changePassBtn.setText("Change PIN");
        changePassBtn.setOnClickListener(v -> showSetupPasswordScreen());
        topBtns.addView(changePassBtn);

        Button toggleHideBtn = new Button(this);
        toggleHideBtn.setText(showHidden ? "Hide Vault" : "Show Hidden");
        toggleHideBtn.setOnClickListener(v -> {
            showHidden = !showHidden;
            showDashboard();
        });
        topBtns.addView(toggleHideBtn);

        mainLayout.addView(topBtns);

        ScrollView scroll = new ScrollView(this);
        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(listContainer);
        mainLayout.addView(scroll);

        loadApps();
    }

    void loadApps() {
        listContainer.removeAllViews();
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        Set<String> lockedApps = new HashSet<>(prefs.getStringSet("locked_apps", new HashSet<>()));
        Set<String> hiddenApps = new HashSet<>(prefs.getStringSet("hidden_apps", new HashSet<>()));

        // Always keep Android Settings lockable at top
        Collections.sort(apps, (a, b) -> pm.getApplicationLabel(a).toString().compareToIgnoreCase(pm.getApplicationLabel(b).toString()));

        for (ApplicationInfo app : apps) {
            if (app.packageName.equals(getPackageName())) continue;
            if (pm.getLaunchIntentForPackage(app.packageName) == null && !app.packageName.equals("com.android.settings")) continue;

            boolean isHidden = hiddenApps.contains(app.packageName);
            if (isHidden && !showHidden) continue;

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(10, 20, 10, 20);

            TextView name = new TextView(this);
            name.setText(pm.getApplicationLabel(app).toString() + (app.packageName.equals("com.android.settings") ? " (Phone Settings)" : ""));
            name.setTextColor(Color.WHITE);
            name.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            name.setOnClickListener(v -> {
                Intent launch = pm.getLaunchIntentForPackage(app.packageName);
                if (launch != null) startActivity(launch);
            });
            row.addView(name);

            CheckBox lockBox = new CheckBox(this);
            lockBox.setText("Lock");
            lockBox.setTextColor(Color.CYAN);
            lockBox.setChecked(lockedApps.contains(app.packageName));
            lockBox.setOnCheckedChangeListener((b, checked) -> {
                if (checked) lockedApps.add(app.packageName);
                else lockedApps.remove(app.packageName);
                prefs.edit().putStringSet("locked_apps", lockedApps).apply();
            });
            row.addView(lockBox);

            CheckBox hideBox = new CheckBox(this);
            hideBox.setText("Hide");
            hideBox.setTextColor(Color.YELLOW);
            hideBox.setChecked(isHidden);
            hideBox.setOnCheckedChangeListener((b, checked) -> {
                if (checked) hiddenApps.add(app.packageName);
                else hiddenApps.remove(app.packageName);
                prefs.edit().putStringSet("hidden_apps", hiddenApps).apply();
                loadApps();
            });
            row.addView(hideBox);

            listContainer.addView(row);
        }
    }

    @Override
    public void onBackPressed() {
        if (getIntent().getStringExtra("LOCKED_PKG") != null) {
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_HOME);
            home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(home);
        } else {
            super.onBackPressed();
        }
    }
}
