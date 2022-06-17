package sword.langbook3.android.presenters;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;
import sword.langbook3.android.BundleKeys;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AcceptationIdBundler;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.ImmutableCorrelationArray;
import sword.langbook3.android.db.ParcelableCorrelationArray;

public final class DefaultPresenter extends AbstractPresenter {

    public DefaultPresenter(@NonNull Activity activity) {
        super(activity);
    }

    @Override
    public void finish() {
        _activity.setResult(Activity.RESULT_OK);
        _activity.finish();
    }

    @Override
    public void finish(@NonNull AcceptationId acceptation) {
        final Intent intent = new Intent();
        AcceptationIdBundler.writeAsIntentExtra(intent, BundleKeys.ACCEPTATION, acceptation);
        _activity.setResult(Activity.RESULT_OK, intent);
        _activity.finish();
    }

    @Override
    public void finish(@NonNull ImmutableCorrelationArray<AlphabetId> correlationArray) {
        final Intent intent = new Intent();
        intent.putExtra(BundleKeys.CORRELATION_ARRAY, new ParcelableCorrelationArray(correlationArray));
        _activity.setResult(Activity.RESULT_OK, intent);
        _activity.finish();
    }
}
