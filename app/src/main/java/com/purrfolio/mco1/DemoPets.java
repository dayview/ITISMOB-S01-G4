package com.purrfolio.mco1;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import java.util.Arrays;
import java.util.List;

/**
 * Sample pets for development. Only shown in debug builds (what Android Studio's Run button installs),
 * never in a release build, and they are never saved unless the tester taps "Add to my pets".
 */
public final class DemoPets {
    private DemoPets() {}

    public static final class Demo {
        public final Pet pet;
        public final int portraitRes;
        Demo(Pet pet, int portraitRes) { this.pet = pet; this.portraitRes = portraitRes; }
    }

    public static boolean enabled(Activity activity) {
        return (activity.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }

    public static List<Demo> all() {
        return Arrays.asList(
                new Demo(new Pet("demo-luna", "Luna", "Rabbit", "Holland Lop",
                        "2025-09-01", "Female", "1.8", "Sample pet for testing."), R.drawable.pdf_luna),
                new Demo(new Pet("demo-simba", "Simba", "Cat", "Orange Tabby",
                        "2022-06-15", "Male", "5.1", "Sample pet for testing."), R.drawable.pdf_simba));
    }
}
