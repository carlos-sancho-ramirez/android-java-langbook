package sword.langbook3.android.presenters;

import android.app.Activity;

import androidx.annotation.NonNull;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.ImmutableCorrelationArray;
import sword.langbook3.android.models.Conversion;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;
import static sword.langbook3.android.util.PreconditionUtils.ensureNull;

public final class AddSentenceSpanIntentionFirstPresenter extends AbstractPresenter {

    public AcceptationId immediateResult;

    public AddSentenceSpanIntentionFirstPresenter(@NonNull Activity activity) {
        super(activity);
    }

    @Override
    public void finish() {
        throw new UnsupportedOperationException("Unexpected");
    }

    @Override
    public void finish(@NonNull AcceptationId acceptation) {
        ensureNonNull(acceptation);
        ensureNull(immediateResult);
        immediateResult = acceptation;
    }

    @Override
    public void finish(@NonNull Conversion<AlphabetId> conversion) {
        throw new UnsupportedOperationException("Unexpected");
    }

    @Override
    public void finish(@NonNull ImmutableCorrelationArray<AlphabetId> correlationArray) {
        throw new UnsupportedOperationException("Unexpected");
    }

    @Override
    public void setTitle(String title) {
        throw new UnsupportedOperationException("Unexpected");
    }

    @Override
    public void setTitle(int title, String param1, String param2) {
        throw new UnsupportedOperationException("Unexpected");
    }
}
