package sword.langbook3.android;

import static sword.langbook3.android.LangbookApplicationUtils.initializeComponents;

import androidx.multidex.MultiDexApplication;

public final class LangbookApplication extends MultiDexApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        initializeComponents(this);
    }
}
