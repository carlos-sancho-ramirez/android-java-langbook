package sword.langbook3.android.db;

import sword.database.DbValue;

public final class CharacterIdManager implements IntSetter<CharacterId> {

    @Override
    public CharacterId getKeyFromInt(int key) {
        return (key != 0)? new CharacterId(key) : null;
    }

    @Override
    public CharacterId getKeyFromDbValue(DbValue value) {
        return getKeyFromInt(value.toInt());
    }
}
