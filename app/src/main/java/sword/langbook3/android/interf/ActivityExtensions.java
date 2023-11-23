package sword.langbook3.android.interf;

import android.view.MenuInflater;

import androidx.annotation.NonNull;

public interface ActivityExtensions extends ActivityInterface, ContextExtensions {
    @NonNull
    MenuInflater newMenuInflater();
}
