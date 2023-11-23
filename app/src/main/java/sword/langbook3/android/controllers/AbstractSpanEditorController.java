package sword.langbook3.android.controllers;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;

import sword.langbook3.android.AcceptationDefinition;
import sword.langbook3.android.BundleKeys;
import sword.langbook3.android.DbManager;
import sword.langbook3.android.activities.delegates.SpanEditorActivityDelegate;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AcceptationIdBundler;
import sword.langbook3.android.db.BunchId;
import sword.langbook3.android.db.ConceptId;
import sword.langbook3.android.db.LangbookDbManager;
import sword.langbook3.android.db.ParcelableBunchIdSet;
import sword.langbook3.android.db.ParcelableCorrelationArray;
import sword.langbook3.android.interf.ActivityInterface;
import sword.langbook3.android.models.SentenceSpan;
import sword.langbook3.android.presenters.Presenter;

abstract class AbstractSpanEditorController implements SpanEditorActivityDelegate.Controller {

    @NonNull
    final String _text;

    public AbstractSpanEditorController(@NonNull String text) {
        ensureNonNull(text);
        _text = text;
    }

    @NonNull
    @Override
    public String getText() {
        return _text;
    }

    void setPickedAcceptation(@NonNull MutableState state, @NonNull Object item) {
        state.putSpan(new SentenceSpan<>(state.getSelection(), item));
    }

    @Override
    public void pickAcceptation(@NonNull Presenter presenter, @NonNull MutableState state, @NonNull String query) {
        final Object immediateResult = presenter.fireFixedTextAcceptationPicker(SpanEditorActivityDelegate.REQUEST_CODE_PICK_ACCEPTATION, query);
        if (immediateResult != null) {
            setPickedAcceptation(state, immediateResult);
        }
    }

    static AcceptationId storeAcceptationDefinition(@NonNull AcceptationDefinition definition) {
        final LangbookDbManager manager = DbManager.getInstance().getManager();
        final ConceptId concept = manager.getNextAvailableConceptId();
        final AcceptationId acceptation = manager.addAcceptation(concept, definition.correlationArray);
        for (BunchId bunch : definition.bunchSet) {
            manager.addAcceptationInBunch(bunch, acceptation);
        }
        return acceptation;
    }

    @Override
    public void onActivityResult(@NonNull ActivityInterface activity, int requestCode, int resultCode, Intent data, @NonNull MutableState state) {
        if (requestCode == SpanEditorActivityDelegate.REQUEST_CODE_PICK_ACCEPTATION && resultCode == Activity.RESULT_OK && data != null) {
            final AcceptationId acceptation = AcceptationIdBundler.readAsIntentExtra(data, BundleKeys.ACCEPTATION);
            final Object item;
            if (acceptation != null) {
                item = acceptation;
            }
            else {
                final ParcelableCorrelationArray parcelableCorrelationArray = data.getParcelableExtra(BundleKeys.CORRELATION_ARRAY);
                final ParcelableBunchIdSet bunchIdSet = data.getParcelableExtra(BundleKeys.BUNCH_SET);
                item = new AcceptationDefinition(parcelableCorrelationArray.get(), bunchIdSet.get());
            }

            setPickedAcceptation(state, item);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
