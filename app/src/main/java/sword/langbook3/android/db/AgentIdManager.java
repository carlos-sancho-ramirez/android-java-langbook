package sword.langbook3.android.db;

import sword.database.DbValue;

public final class AgentIdManager implements IntSetter<AgentId> {

    @Override
    public AgentId getKeyFromInt(int key) {
        return (key != 0)? new AgentId(key) : null;
    }

    @Override
    public AgentId getKeyFromDbValue(DbValue value) {
        return getKeyFromInt(value.toInt());
    }
}
