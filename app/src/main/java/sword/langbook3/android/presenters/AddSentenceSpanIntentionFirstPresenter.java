package sword.langbook3.android.presenters;

import android.app.Activity;

import androidx.annotation.NonNull;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.ImmutableCorrelationArray;

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
    public void finish(@NonNull ImmutableCorrelationArray<AlphabetId> correlationArray) {
        throw new UnsupportedOperationException("Unexpected");
    }
}
