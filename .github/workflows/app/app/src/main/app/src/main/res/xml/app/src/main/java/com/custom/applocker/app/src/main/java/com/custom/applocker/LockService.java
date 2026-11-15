package com.custom.applocker;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.accessibility.AccessibilityEvent;
import java.util.HashSet;
import java.util.Set;

public class LockService extends AccessibilityService {
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (event.getPackageName() == null) return;
            String pkg = event.getPackageName().toString();
            if (pkg.equals(getPackageName())) return;

            SharedPreferences prefs = getSharedPreferences("AppLockPrefs", MODE_PRIVATE);
            Set<String> locked = prefs.getStringSet("locked_apps", new HashSet<>());
            String tempUnlock = prefs.getString("temp_unlock", "");

            if (!pkg.equals(tempUnlock)) {
                prefs.edit().putString("temp_unlock", "").apply();
            }

            if (locked.contains(pkg) && !pkg.equals(tempUnlock)) {
                Intent i = new Intent(this, MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                i.putExtra("LOCKED_PKG", pkg);
                startActivity(i);
            }
        }
    }

    @Override
    public void onInterrupt() {}
}
