package sword.langbook3.android.db;

import sword.database.DbValue;

final class SymbolArrayIdManager implements IntSetter<SymbolArrayIdHolder> {

    @Override
    public SymbolArrayIdHolder getKeyFromInt(int key) {
        return new SymbolArrayIdHolder(key);
    }

    @Override
    public SymbolArrayIdHolder getKeyFromDbValue(DbValue value) {
        return getKeyFromInt(value.toInt());
    }
}
