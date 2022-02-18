package sword.langbook3.android.db;

import sword.database.DbValue;

final class CharacterIdManager implements IntSetter<CharacterIdHolder> {

    @Override
    public CharacterIdHolder getKeyFromInt(int key) {
        return new CharacterIdHolder(key);
    }

    @Override
    public CharacterIdHolder getKeyFromDbValue(DbValue value) {
        return getKeyFromInt(value.toInt());
    }
}
