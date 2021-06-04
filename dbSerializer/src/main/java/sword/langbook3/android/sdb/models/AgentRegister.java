package sword.langbook3.android.sdb.models;

public final class AgentRegister {
    public final int targetBunchSetId;
    public final int sourceBunchSetId;
    public final int diffBunchSetId;
    public final int startMatcherId;
    public final int startAdderId;
    public final int endMatcherId;
    public final int endAdderId;
    public final int rule;

    public AgentRegister(int targetBunchSetId, int sourceBunchSetId, int diffBunchSetId,
            int startMatcherId, int startAdderId, int endMatcherId, int endAdderId, int rule) {

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
