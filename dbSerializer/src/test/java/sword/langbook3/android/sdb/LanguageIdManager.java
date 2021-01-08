package sword.langbook3.android.sdb;

import sword.database.DbValue;

final class LanguageIdManager implements sword.langbook3.android.db.IntSetter<LanguageIdHolder> {

    @Override
    public LanguageIdHolder getKeyFromInt(int key) {
        return new LanguageIdHolder(key);
    }

    @Override
    public LanguageIdHolder getKeyFromDbValue(DbValue value) {
        return getKeyFromInt(value.toInt());
    }
}
