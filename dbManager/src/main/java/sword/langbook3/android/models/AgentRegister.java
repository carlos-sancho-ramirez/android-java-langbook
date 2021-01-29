package sword.langbook3.android.models;

import static sword.database.CommonUtils.equal;

public final class AgentRegister<CorrelationId> {
    public final int targetBunchSetId;
    public final int sourceBunchSetId;
    public final int diffBunchSetId;
    public final CorrelationId startMatcherId;
    public final CorrelationId startAdderId;
    public final CorrelationId endMatcherId;
    public final CorrelationId endAdderId;
    public final int rule;

    public AgentRegister(int targetBunchSetId, int sourceBunchSetId, int diffBunchSetId,
            CorrelationId startMatcherId, CorrelationId startAdderId, CorrelationId endMatcherId, CorrelationId endAdderId, int rule) {

        if (equal(startMatcherId, startAdderId) && equal(endMatcherId, endAdderId)) {
            if (targetBunchSetId == 0 || rule != 0) {
                throw new IllegalArgumentException();
            }
        }
        else if (rule == 0) {
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
