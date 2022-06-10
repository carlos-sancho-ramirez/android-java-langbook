package sword.langbook3.android.controllers;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import sword.collections.ImmutableHashMap;
import sword.collections.ImmutableHashSet;
import sword.collections.ImmutableMap;
import sword.collections.ImmutableSet;
import sword.langbook3.android.CorrelationPickerActivity;
import sword.langbook3.android.DbManager;
import sword.langbook3.android.MatchingBunchesPickerActivity;
import sword.langbook3.android.collections.Procedure2;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AcceptationIdBundler;
import sword.langbook3.android.db.AcceptationIdParceler;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.AlphabetIdComparator;
import sword.langbook3.android.db.ConceptId;
import sword.langbook3.android.db.ConceptIdParceler;
import sword.langbook3.android.db.CorrelationId;
import sword.langbook3.android.db.CorrelationParceler;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.ImmutableCorrelationArray;
import sword.langbook3.android.db.LangbookDbManager;
import sword.langbook3.android.db.ParcelableCorrelationArray;

import static android.app.Activity.RESULT_OK;

public final class CorrelationPickerController implements CorrelationPickerActivity.Controller {

    private final AcceptationId _existingAcceptation;
    private final ConceptId _concept;
    private final ImmutableCorrelation<AlphabetId> _texts;
    private final boolean _mustSaveAcceptation;

    public CorrelationPickerController(
            AcceptationId existingAcceptation,
            ConceptId concept,
            ImmutableCorrelation<AlphabetId> texts,
            boolean mustSaveAcceptation) {
        _existingAcceptation = existingAcceptation;
        _concept = concept;
        _texts = texts;
        _mustSaveAcceptation = mustSaveAcceptation;
    }

    private ImmutableMap<ImmutableCorrelation<AlphabetId>, CorrelationId> findExistingCorrelations(
            @NonNull ImmutableSet<ImmutableCorrelationArray<AlphabetId>> options) {
        final ImmutableSet.Builder<ImmutableCorrelation<AlphabetId>> correlationsBuilder = new ImmutableHashSet.Builder<>();
        for (ImmutableCorrelationArray<AlphabetId> option : options) {
            for (ImmutableCorrelation<AlphabetId> correlation : option) {
                correlationsBuilder.add(correlation);
            }
        }
        final ImmutableSet<ImmutableCorrelation<AlphabetId>> correlations = correlationsBuilder.build();

        final ImmutableMap.Builder<ImmutableCorrelation<AlphabetId>, CorrelationId> builder = new ImmutableHashMap.Builder<>();
        for (ImmutableCorrelation<AlphabetId> correlation : correlations) {
            final CorrelationId id = DbManager.getInstance().getManager().findCorrelation(correlation);
            if (id != null) {
                builder.put(correlation, id);
            }
        }

        return builder.build();
    }

    @Override
    public void load(@NonNull Activity activity, boolean firstTime, @NonNull Procedure2<ImmutableSet<ImmutableCorrelationArray<AlphabetId>>, ImmutableMap<ImmutableCorrelation<AlphabetId>, CorrelationId>> procedure) {
        final ImmutableSet<ImmutableCorrelationArray<AlphabetId>> options = _texts.checkPossibleCorrelationArrays(new AlphabetIdComparator());
        final ImmutableMap<ImmutableCorrelation<AlphabetId>, CorrelationId> knownCorrelations = findExistingCorrelations(options);

        if (firstTime && options.size() == 1) {
            complete(activity, options.valueAt(0));
        }
        else {
            procedure.apply(options, knownCorrelations);
        }
    }

    @Override
    public void complete(@NonNull Activity activity, @NonNull ImmutableCorrelationArray<AlphabetId> selectedOption) {
        if (!_mustSaveAcceptation) {
            final Intent intent = new Intent();
            intent.putExtra(CorrelationPickerActivity.ResultKeys.CORRELATION_ARRAY, new ParcelableCorrelationArray(selectedOption));
            activity.setResult(RESULT_OK, intent);
            activity.finish();
        }
        else if (_existingAcceptation == null) {
            final boolean allValidAlphabets = DbManager.getInstance().getManager().allValidAlphabets(_texts);
            final MatchingBunchesPickerActivity.Controller controller = allValidAlphabets? new MatchingBunchesPickerController(_concept, _texts, selectedOption) :
                    new NonValidAlphabetsMatchingBunchesPickerController(_texts);
            MatchingBunchesPickerActivity.open(activity, CorrelationPickerActivity.REQUEST_CODE_NEXT_STEP, controller);
        }
        else {
            DbManager.getInstance().getManager().updateAcceptationCorrelationArray(_existingAcceptation, selectedOption);
            activity.setResult(RESULT_OK);
            activity.finish();
        }
    }

    @Override
    public void onActivityResult(@NonNull Activity activity, @NonNull ImmutableSet<ImmutableCorrelationArray<AlphabetId>> options,  int selection, int requestCode, int resultCode, Intent data) {
        if (requestCode == CorrelationPickerActivity.REQUEST_CODE_NEXT_STEP) {
            if (resultCode == RESULT_OK) {
                final LangbookDbManager manager = DbManager.getInstance().getManager();
                final boolean allValidAlphabets = manager.allValidAlphabets(_texts);
                final Intent intent = new Intent();
                if (!allValidAlphabets) {
                    intent.putExtra(CorrelationPickerActivity.ResultKeys.CORRELATION_ARRAY, new ParcelableCorrelationArray(options.valueAt(selection)));
                }
                else if (data != null) {
                    final AcceptationId accId = AcceptationIdBundler.readAsIntentExtra(data, MatchingBunchesPickerActivity.ResultKeys.ACCEPTATION);
                    AcceptationIdBundler.writeAsIntentExtra(intent, CorrelationPickerActivity.ResultKeys.ACCEPTATION, accId);
                }
                activity.setResult(RESULT_OK, intent);
                activity.finish();
            }
            else if (options.size() == 1) {
                activity.finish();
            }
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        AcceptationIdParceler.write(dest, _existingAcceptation);
        ConceptIdParceler.write(dest, _concept);
        CorrelationParceler.write(dest, _texts);
        dest.writeInt(_mustSaveAcceptation? 1 : 0);
    }

    public static final Parcelable.Creator<CorrelationPickerController> CREATOR = new Parcelable.Creator<CorrelationPickerController>() {

        @Override
        public CorrelationPickerController createFromParcel(Parcel source) {
            final AcceptationId acceptationId = AcceptationIdParceler.read(source);
            final ConceptId conceptId = ConceptIdParceler.read(source);
            final ImmutableCorrelation<AlphabetId> texts = CorrelationParceler.read(source).toImmutable();
            final boolean mustSaveAcceptation = source.readInt() != 0;
            return new CorrelationPickerController(acceptationId, conceptId, texts, mustSaveAcceptation);
        }

        @Override
        public CorrelationPickerController[] newArray(int size) {
            return new CorrelationPickerController[size];
        }
    };
}
