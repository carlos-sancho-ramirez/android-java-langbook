package sword.langbook3.android.controllers;

import android.os.Parcel;

import androidx.annotation.NonNull;
import sword.collections.ImmutableSet;
import sword.langbook3.android.DbManager;
import sword.langbook3.android.R;
import sword.langbook3.android.db.AgentId;
import sword.langbook3.android.db.AgentIdParceler;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.BunchId;
import sword.langbook3.android.db.CorrelationId;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.ImmutableCorrelationArray;
import sword.langbook3.android.db.LangbookDbChecker;
import sword.langbook3.android.db.LangbookDbManager;
import sword.langbook3.android.db.RuleId;
import sword.langbook3.android.models.AgentDetails;
import sword.langbook3.android.presenters.Presenter;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

public final class EditAgentAgentEditorController extends AbstractAgentEditorController {

    @NonNull
    private final AgentId _agentId;

    public EditAgentAgentEditorController(@NonNull AgentId agentId) {
        ensureNonNull(agentId);
        _agentId = agentId;
    }

    @Override
    public void setup(@NonNull MutableState state) {
        final LangbookDbChecker checker = DbManager.getInstance().getManager();
        final AgentDetails<AlphabetId, CorrelationId, BunchId, RuleId> details = checker.getAgentDetails(_agentId);
        state.setTargetBunches((ImmutableSet<Object>) ((ImmutableSet) details.targetBunches));
        state.setSourceBunches((ImmutableSet<Object>) ((ImmutableSet) details.sourceBunches));
        state.setDiffBunches((ImmutableSet<Object>) ((ImmutableSet) details.diffBunches));
        state.setStartMatcher(details.startMatcher.toCorrelationEntryList().toImmutable());
        state.setStartAdder(details.startAdder);
        state.setEndMatcher(details.endMatcher.toCorrelationEntryList().toImmutable());
        state.setEndAdder(details.endAdder);
        state.setRule(details.rule);
    }

    @Override
    public void complete(@NonNull Presenter presenter, @NonNull State state) {
        if (checkState(presenter, state)) {
            final ImmutableCorrelation<AlphabetId> startMatcher = buildCorrelation(state.getStartMatcher());
            final ImmutableCorrelationArray<AlphabetId> startAdder = state.getStartAdder();
            final ImmutableCorrelation<AlphabetId> endMatcher = buildCorrelation(state.getEndMatcher());
            final ImmutableCorrelationArray<AlphabetId> endAdder = state.getEndAdder();

            final Object rawRule = state.getRule();
            final RuleId rule = (rawRule != null)? ensureRuleIsStored(rawRule) : null;

            final LangbookDbManager manager = DbManager.getInstance().getManager();
            final ImmutableSet<BunchId> targetBunches = state.getTargetBunches().map(this::ensureBunchIsStored).toSet();
            final ImmutableSet<BunchId> sourceBunches = state.getSourceBunches().map(this::ensureBunchIsStored).toSet();
            final ImmutableSet<BunchId> diffBunches = state.getDiffBunches().map(this::ensureBunchIsStored).toSet();

            final boolean success = manager.updateAgent(_agentId, targetBunches, sourceBunches, diffBunches,
                    startMatcher, startAdder, endMatcher, endAdder, rule);
            final int message = success? R.string.updateAgentFeedback : R.string.updateAgentError;

            presenter.displayFeedback(message);
            if (success) {
                presenter.finish();
            }
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        AgentIdParceler.write(dest, _agentId);
    }

    public static final Creator<EditAgentAgentEditorController> CREATOR = new Creator<EditAgentAgentEditorController>() {

        @Override
        public EditAgentAgentEditorController createFromParcel(Parcel source) {
            final AgentId agentId = AgentIdParceler.read(source);
            return new EditAgentAgentEditorController(agentId);
        }

        @Override
        public EditAgentAgentEditorController[] newArray(int size) {
            return new EditAgentAgentEditorController[size];
        }
    };
}
