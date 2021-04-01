package sword.langbook3.android.models;

import sword.langbook3.android.db.BunchSetIdInterface;

import static sword.database.CommonUtils.equal;

public final class AgentRegister<CorrelationId, BunchSetId extends BunchSetIdInterface, RuleId> {
    public final BunchSetId targetBunchSetId;
    public final BunchSetId sourceBunchSetId;
    public final BunchSetId diffBunchSetId;
    public final CorrelationId startMatcherId;
    public final CorrelationId startAdderId;
    public final CorrelationId endMatcherId;
    public final CorrelationId endAdderId;
    public final RuleId rule;

    public AgentRegister(BunchSetId targetBunchSetId, BunchSetId sourceBunchSetId, BunchSetId diffBunchSetId,
            CorrelationId startMatcherId, CorrelationId startAdderId, CorrelationId endMatcherId, CorrelationId endAdderId, RuleId rule) {

        if (equal(startMatcherId, startAdderId) && equal(endMatcherId, endAdderId)) {
            if (targetBunchSetId.isDeclaredEmpty() || rule != null) {
                throw new IllegalArgumentException();
            }
        }
        else if (rule == null) {
            throw new IllegalArgumentException();
        }

        this.targetBunchSetId = targetBunchSetId;
        this.sourceBunchSetId = sourceBunchSetId;
        this.diffBunchSetId = diffBunchSetId;
        this.startMatcherId = startMatcherId;
        this.startAdderId = startAdderId;
        this.endMatcherId = endMatcherId;
        this.endAdderId = endAdderId;
        this.rule = rule;
    }
}
