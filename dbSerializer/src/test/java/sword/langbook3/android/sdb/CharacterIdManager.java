package sword.langbook3.android.sdb;

import sword.database.DbValue;

final class CharacterIdManager implements sword.langbook3.android.db.IntSetter<CharacterIdHolder> {

    @Override
    public CharacterIdHolder getKeyFromInt(int key) {
        return new CharacterIdHolder(key);
    }

    @Override
    public CharacterIdHolder getKeyFromDbValue(DbValue value) {
        return getKeyFromInt(value.toInt());
    }
}
