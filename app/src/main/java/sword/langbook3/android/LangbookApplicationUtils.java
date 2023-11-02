package sword.langbook3.android;

import android.content.Context;

import androidx.annotation.NonNull;

final class LangbookApplicationUtils {

    static void initializeComponents(@NonNull Context context) {
        DbManager.createInstance(context);
        LangbookPreferences.createInstance(context);
    }

    private LangbookApplicationUtils() {
    }
}
