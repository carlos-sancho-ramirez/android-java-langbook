package sword.langbook3.android.controllers;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;

import androidx.annotation.NonNull;

import sword.langbook3.android.DbManager;
import sword.langbook3.android.activities.delegates.AcceptationConfirmationActivityDelegate;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AcceptationIdParceler;
import sword.langbook3.android.db.CharacterCompositionTypeId;
import sword.langbook3.android.db.CharacterCompositionTypeIdManager;
import sword.langbook3.android.db.ConceptId;
import sword.langbook3.android.db.LangbookDbManager;
import sword.langbook3.android.interf.ActivityInterface;
import sword.langbook3.android.presenters.Presenter;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

public final class AddCharacterCompositionDefinitionAcceptationConfirmationController implements AcceptationConfirmationActivityDelegate.Controller {

    @NonNull
    private final AcceptationId _acceptation;

    public AddCharacterCompositionDefinitionAcceptationConfirmationController(@NonNull AcceptationId acceptation) {
        ensureNonNull(acceptation);
        _acceptation = acceptation;
    }

    @NonNull
    @Override
    public AcceptationId getAcceptation() {
        return _acceptation;
    }

    @Override
    public void confirm(@NonNull Presenter presenter) {
        final LangbookDbManager manager = DbManager.getInstance().getManager();
        final ConceptId concept = manager.conceptFromAcceptation(_acceptation);
        final CharacterCompositionTypeId typeId = CharacterCompositionTypeIdManager.conceptAsCharacterCompositionTypeId(concept);
        presenter.openCharacterCompositionDefinitionEditor(AcceptationConfirmationActivityDelegate.REQUEST_CODE_NEXT_STEP, new AddCharacterCompositionDefinitionWithSelectedAcceptationCharacterCompositionDefinitionEditorController(typeId));
    }

    @Override
    public void onActivityResult(@NonNull ActivityInterface activity, int requestCode, int resultCode, Intent data) {
        if (requestCode == AcceptationConfirmationActivityDelegate.REQUEST_CODE_NEXT_STEP && resultCode == Activity.RESULT_OK) {
            activity.setResult(Activity.RESULT_OK);
            activity.finish();
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        AcceptationIdParceler.write(dest, _acceptation);
    }

    public static final Creator<AddCharacterCompositionDefinitionAcceptationConfirmationController> CREATOR = new Creator<AddCharacterCompositionDefinitionAcceptationConfirmationController>() {

        @Override
        public AddCharacterCompositionDefinitionAcceptationConfirmationController createFromParcel(Parcel source) {
            final AcceptationId acceptation = AcceptationIdParceler.read(source);
            return new AddCharacterCompositionDefinitionAcceptationConfirmationController(acceptation);
        }

        @Override
        public AddCharacterCompositionDefinitionAcceptationConfirmationController[] newArray(int size) {
            return new AddCharacterCompositionDefinitionAcceptationConfirmationController[size];
        }
    };
}
