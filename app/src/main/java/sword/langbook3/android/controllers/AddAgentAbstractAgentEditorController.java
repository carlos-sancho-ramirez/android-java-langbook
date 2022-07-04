package sword.langbook3.android.controllers;

import androidx.annotation.NonNull;
import sword.collections.ImmutableSet;
import sword.langbook3.android.DbManager;
import sword.langbook3.android.R;
import sword.langbook3.android.db.AgentId;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.BunchId;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.ImmutableCorrelationArray;
import sword.langbook3.android.db.LangbookDbManager;
import sword.langbook3.android.db.RuleId;
import sword.langbook3.android.presenters.Presenter;

abstract class AddAgentAbstractAgentEditorController extends AbstractAgentEditorController {

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
}
