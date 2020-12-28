package sword.langbook3.android.sdb;

import sword.langbook3.android.db.IntIdManager;

final class AlphabetIdManager implements IntIdManager<AlphabetIdHolder> {

    @Override
    public int getInt(AlphabetIdHolder id) {
        return id.key;
    }

    @Override
    public AlphabetIdHolder getKeyFromInt(int key) {
        return new AlphabetIdHolder(key);
    }
}
