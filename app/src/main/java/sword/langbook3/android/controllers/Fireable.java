package sword.langbook3.android.controllers;

import androidx.annotation.NonNull;
import sword.langbook3.android.presenters.Presenter;

public interface Fireable {
    void fire(@NonNull Presenter presenter, int requestCode);
}
