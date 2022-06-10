package sword.langbook3.android.controllers;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import sword.langbook3.android.CorrelationPickerActivity;
import sword.langbook3.android.WordEditorActivity;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AcceptationIdParceler;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.ConceptId;
import sword.langbook3.android.db.ConceptIdParceler;
import sword.langbook3.android.db.ImmutableCorrelation;

import static android.app.Activity.RESULT_OK;

public final class WordEditorController implements WordEditorActivity.Controller {

    private final ConceptId _concept;
    private final AcceptationId _existingAcceptation;
    private final boolean _mustEvaluateConversions;

    public WordEditorController(
            ConceptId concept,
            AcceptationId existingAcceptation,
            boolean mustEvaluateConversions) {
        _concept = concept;
        _existingAcceptation = existingAcceptation;
        _mustEvaluateConversions = mustEvaluateConversions;
    }

    @Override
    public void complete(@NonNull Activity activity, @NonNull ImmutableCorrelation<AlphabetId> texts) {
        final CorrelationPickerActivity.Controller controller;
        if (!_mustEvaluateConversions) {
            controller = new CorrelationPickerController(null, null, texts, false);
        }
        else if (_existingAcceptation == null) {
            controller = new CorrelationPickerController(null, _concept, texts, true);
        }
        else {
            controller = new CorrelationPickerController(_existingAcceptation, null, texts, true);
        }
        CorrelationPickerActivity.open(activity, WordEditorActivity.REQUEST_CODE_CORRELATION_PICKER, controller);
    }

    @Override
    public void onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data) {
        if (requestCode == WordEditorActivity.REQUEST_CODE_CORRELATION_PICKER && resultCode == RESULT_OK) {
            activity.setResult(RESULT_OK, data);
            activity.finish();
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        ConceptIdParceler.write(dest, _concept);
        AcceptationIdParceler.write(dest, _existingAcceptation);
        dest.writeInt(_mustEvaluateConversions? 1 : 0);
    }

    public static final Parcelable.Creator<WordEditorController> CREATOR = new Parcelable.Creator<WordEditorController>() {

        @Override
        public WordEditorController createFromParcel(Parcel source) {
            final ConceptId concept = ConceptIdParceler.read(source);
            final AcceptationId existingAcceptation = AcceptationIdParceler.read(source);
            final boolean mustEvaluateConversions = source.readInt() != 0;
            return new WordEditorController(concept, existingAcceptation, mustEvaluateConversions);
        }

        @Override
        public WordEditorController[] newArray(int size) {
            return new WordEditorController[size];
        }
    };
}
