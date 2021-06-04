package sword.langbook3.android.models;

import sword.langbook3.android.db.BunchSetIdInterface;

public final class AgentRegister<CorrelationId, CorrelationArrayId, BunchSetId extends BunchSetIdInterface, RuleId> {
    public final BunchSetId targetBunchSetId;
    public final BunchSetId sourceBunchSetId;
    public final BunchSetId diffBunchSetId;
    public final CorrelationId startMatcherId;
    public final CorrelationArrayId startAdderId;
    public final CorrelationId endMatcherId;
    public final CorrelationArrayId endAdderId;
    public final RuleId rule;

    public AgentRegister(BunchSetId targetBunchSetId, BunchSetId sourceBunchSetId, BunchSetId diffBunchSetId,
            CorrelationId startMatcherId, CorrelationArrayId startAdderId, CorrelationId endMatcherId, CorrelationArrayId endAdderId, RuleId rule) {

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
