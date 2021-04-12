package sword.langbook3.android.db;

import sword.database.DbValue;

final class AgentIdManager implements IntSetter<AgentIdHolder> {

    @Override
    public AgentIdHolder getKeyFromInt(int key) {
        return (key == 0)? null : new AgentIdHolder(key);
    }

    @Override
    public AgentIdHolder getKeyFromDbValue(DbValue value) {
        return getKeyFromInt(value.toInt());
    }
}
