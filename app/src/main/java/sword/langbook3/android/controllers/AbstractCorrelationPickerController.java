package sword.langbook3.android.controllers;

import android.app.Activity;
import android.content.Intent;

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
import sword.langbook3.android.db.CorrelationId;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.ImmutableCorrelationArray;
import sword.langbook3.android.presenters.Presenter;

import static android.app.Activity.RESULT_OK;
import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

abstract class AbstractCorrelationPickerController implements CorrelationPickerActivity.Controller, Fireable {

    @NonNull
    final ImmutableCorrelation<AlphabetId> _texts;

    public AbstractCorrelationPickerController(
            @NonNull ImmutableCorrelation<AlphabetId> texts) {
        ensureNonNull(texts);
        _texts = texts;
    }

    @Override
    public void fire(@NonNull Presenter presenter, int requestCode) {
        // TODO: This can be optimised as we only need to know if the size of options is 1 or not
        final ImmutableSet<ImmutableCorrelationArray<AlphabetId>> options = _texts.checkPossibleCorrelationArrays(new AlphabetIdComparator());

        if (options.size() == 1) {
            complete(presenter, requestCode, options.first());
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
    public void load(@NonNull Presenter presenter, @NonNull Procedure2<ImmutableSet<ImmutableCorrelationArray<AlphabetId>>, ImmutableMap<ImmutableCorrelation<AlphabetId>, CorrelationId>> procedure) {
        final ImmutableSet<ImmutableCorrelationArray<AlphabetId>> options = _texts.checkPossibleCorrelationArrays(new AlphabetIdComparator());
        final ImmutableMap<ImmutableCorrelation<AlphabetId>, CorrelationId> knownCorrelations = findExistingCorrelations(options);
        procedure.apply(options, knownCorrelations);
    }

    abstract void complete(@NonNull Presenter presenter, int requestCode, @NonNull ImmutableCorrelationArray<AlphabetId> selectedOption);

    @Override
    public void complete(@NonNull Presenter presenter, @NonNull ImmutableCorrelationArray<AlphabetId> selectedOption) {
        complete(presenter, CorrelationPickerActivity.REQUEST_CODE_NEXT_STEP, selectedOption);
    }

    @Override
    public void onActivityResult(@NonNull Activity activity, @NonNull ImmutableSet<ImmutableCorrelationArray<AlphabetId>> options,  int selection, int requestCode, int resultCode, Intent data) {
        if (requestCode == CorrelationPickerActivity.REQUEST_CODE_NEXT_STEP && resultCode == RESULT_OK) {
            activity.setResult(RESULT_OK, data);
            activity.finish();
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
