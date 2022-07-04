package sword.langbook3.android.controllers;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import sword.collections.ImmutableHashSet;
import sword.collections.ImmutableSet;
import sword.langbook3.android.DbManager;
import sword.langbook3.android.R;
import sword.langbook3.android.db.AgentId;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.BunchId;
import sword.langbook3.android.db.BunchIdParceler;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.ImmutableCorrelationArray;
import sword.langbook3.android.db.LangbookDbManager;
import sword.langbook3.android.db.RuleId;
import sword.langbook3.android.presenters.Presenter;

public final class AddAgentAgentEditorController extends AbstractAgentEditorController {

    private final BunchId _initialTargetBunch;
    private final BunchId _initialSourceBunch;
    private final BunchId _initialDiffBunch;

    public AddAgentAgentEditorController(BunchId initialTargetBunch, BunchId initialSourceBunch, BunchId initialDiffBunch) {
        _initialTargetBunch = initialTargetBunch;
        _initialSourceBunch = initialSourceBunch;
        _initialDiffBunch = initialDiffBunch;
    }

    @Override
    public void setup(@NonNull MutableState state) {
        if (_initialTargetBunch != null) {
            state.setTargetBunches(ImmutableHashSet.empty().add(_initialTargetBunch));
        }
        else if (_initialSourceBunch != null) {
            state.setSourceBunches(ImmutableHashSet.empty().add(_initialSourceBunch));
        }
        else if (_initialDiffBunch != null) {
            state.setDiffBunches(ImmutableHashSet.empty().add(_initialDiffBunch));
        }
    }

    @Override
    public void complete(@NonNull Presenter presenter, @NonNull State state) {
        if (checkState(presenter, state)) {
            final ImmutableCorrelation<AlphabetId> startMatcher = buildCorrelation(state.getStartMatcher());
            final ImmutableCorrelationArray<AlphabetId> startAdder = state.getStartAdder();
            final ImmutableCorrelation<AlphabetId> endMatcher = buildCorrelation(state.getEndMatcher());
            final ImmutableCorrelationArray<AlphabetId> endAdder = state.getEndAdder();

            final RuleId rule = (startMatcher.equals(startAdder.concatenateTexts()) && endMatcher.equals(endAdder.concatenateTexts()))? null : ensureRuleIsStored(state.getRule());

            final LangbookDbManager manager = DbManager.getInstance().getManager();
            final ImmutableSet<BunchId> targetBunches = state.getTargetBunches().map(this::ensureBunchIsStored).toSet();
            final ImmutableSet<BunchId> sourceBunches = state.getSourceBunches().map(this::ensureBunchIsStored).toSet();
            final ImmutableSet<BunchId> diffBunches = state.getDiffBunches().map(this::ensureBunchIsStored).toSet();

            final AgentId agentId = manager.addAgent(targetBunches, sourceBunches, diffBunches,
                    startMatcher, startAdder, endMatcher, endAdder, rule);
            presenter.displayFeedback((agentId != null)? R.string.newAgentFeedback : R.string.newAgentError);
            if (agentId != null) {
                presenter.finish();
            }
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        BunchIdParceler.write(dest, _initialTargetBunch);
        BunchIdParceler.write(dest, _initialSourceBunch);
        BunchIdParceler.write(dest, _initialDiffBunch);
    }

    public static final Parcelable.Creator<AddAgentAgentEditorController> CREATOR = new Parcelable.Creator<AddAgentAgentEditorController>() {

        @Override
        public AddAgentAgentEditorController createFromParcel(Parcel source) {
            final BunchId initialTargetBunch = BunchIdParceler.read(source);
            final BunchId initialSourceBunch = BunchIdParceler.read(source);
            final BunchId initialDiffBunch = BunchIdParceler.read(source);
            return new AddAgentAgentEditorController(initialTargetBunch, initialSourceBunch, initialDiffBunch);
        }

        @Override
        public AddAgentAgentEditorController[] newArray(int size) {
            return new AddAgentAgentEditorController[size];
        }
    };
}
