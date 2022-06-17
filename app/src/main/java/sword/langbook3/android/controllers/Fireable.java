package sword.langbook3.android.controllers;

import android.app.Activity;

import androidx.annotation.NonNull;

public interface Fireable {
    void fire(@NonNull Activity activity, int requestCode);
}
