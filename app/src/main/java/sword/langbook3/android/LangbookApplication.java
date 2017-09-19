package sword.langbook3.android;

import android.app.Application;

public class LangbookApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        DbManager.createInstance(this);
    }
}
