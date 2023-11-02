package sword.langbook3.android;

import static sword.langbook3.android.LangbookApplicationUtils.initializeComponents;

import android.app.Application;

public final class LangbookApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        initializeComponents(this);
    }
}
