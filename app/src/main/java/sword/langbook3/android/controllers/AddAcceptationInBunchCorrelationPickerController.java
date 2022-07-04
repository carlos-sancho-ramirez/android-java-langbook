package sword.langbook3.android.controllers;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;

import androidx.annotation.NonNull;
import sword.collections.ImmutableHashMap;
import sword.collections.ImmutableHashSet;
import sword.collections.ImmutableMap;
import sword.collections.ImmutableSet;
import sword.langbook3.android.CorrelationPickerActivity;
import sword.langbook3.android.DbManager;
import sword.langbook3.android.collections.Procedure2;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.AlphabetIdComparator;
import sword.langbook3.android.db.BunchId;
import sword.langbook3.android.db.BunchIdParceler;
import sword.langbook3.android.db.CorrelationId;
import sword.langbook3.android.db.CorrelationParceler;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.ImmutableCorrelationArray;
import sword.langbook3.android.presenters.Presenter;

import static android.app.Activity.RESULT_OK;
import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

public final class AddAcceptationInBunchCorrelationPickerController implements CorrelationPickerActivity.Controller, Fireable {

    @NonNull
    private final BunchId _bunch;

    @NonNull
    private final ImmutableCorrelation<AlphabetId> _texts;

    public AddAcceptationInBunchCorrelationPickerController(
            @NonNull BunchId bunch,
            @NonNull ImmutableCorrelation<AlphabetId> texts) {
        ensureNonNull(bunch, texts);
        _bunch = bunch;
        _texts = texts;
    }

    @Override
    public void fire(@NonNull Presenter presenter, int requestCode) {
        // TODO: This can be optimised as we only need to know if the size of options is 1 or not
        final ImmutableSet<ImmutableCorrelationArray<AlphabetId>> options = _texts.checkPossibleCorrelationArrays(new AlphabetIdComparator());

        if (options.size() == 1) {
            complete(presenter, requestCode, options.valueAt(0));
        }
        else {
            presenter.openCorrelationPicker(requestCode, this);
        }
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
    public void load(@NonNull Presenter presenter, boolean firstTime, @NonNull Procedure2<ImmutableSet<ImmutableCorrelationArray<AlphabetId>>, ImmutableMap<ImmutableCorrelation<AlphabetId>, CorrelationId>> procedure) {
        final ImmutableSet<ImmutableCorrelationArray<AlphabetId>> options = _texts.checkPossibleCorrelationArrays(new AlphabetIdComparator());
        final ImmutableMap<ImmutableCorrelation<AlphabetId>, CorrelationId> knownCorrelations = findExistingCorrelations(options);

        if (firstTime && options.size() == 1) {
            complete(presenter, options.valueAt(0));
        }
        else {
            procedure.apply(options, knownCorrelations);
        }
    }

    private void complete(@NonNull Presenter presenter, int requestCode, @NonNull ImmutableCorrelationArray<AlphabetId> selectedOption) {
        new AddAcceptationInBunchMatchingBunchesPickerController(_bunch, selectedOption)
                .fire(presenter, requestCode);
    }

    @Override
    public void complete(@NonNull Presenter presenter, @NonNull ImmutableCorrelationArray<AlphabetId> selectedOption) {
        complete(presenter, CorrelationPickerActivity.REQUEST_CODE_NEXT_STEP, selectedOption);
    }

    @Override
    public void onActivityResult(@NonNull Activity activity, @NonNull ImmutableSet<ImmutableCorrelationArray<AlphabetId>> options,  int selection, int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
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
        BunchIdParceler.write(dest, _bunch);
        CorrelationParceler.write(dest, _texts);
    }

    public static final Creator<AddAcceptationInBunchCorrelationPickerController> CREATOR = new Creator<AddAcceptationInBunchCorrelationPickerController>() {

        @Override
        public AddAcceptationInBunchCorrelationPickerController createFromParcel(Parcel source) {
            final BunchId bunch = BunchIdParceler.read(source);
            final ImmutableCorrelation<AlphabetId> texts = CorrelationParceler.read(source).toImmutable();
            return new AddAcceptationInBunchCorrelationPickerController(bunch, texts);
        }

        @Override
        public AddAcceptationInBunchCorrelationPickerController[] newArray(int size) {
            return new AddAcceptationInBunchCorrelationPickerController[size];
        }
    };
}
