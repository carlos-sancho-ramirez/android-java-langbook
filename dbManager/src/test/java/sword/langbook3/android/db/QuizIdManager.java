package sword.langbook3.android.db;

import sword.database.DbValue;

final class QuizIdManager implements IntSetter<QuizIdHolder> {

    @Override
    public QuizIdHolder getKeyFromInt(int key) {
        return (key == 0)? null : new QuizIdHolder(key);
    }

    @Override
    public QuizIdHolder getKeyFromDbValue(DbValue value) {
        return getKeyFromInt(value.toInt());
    }
}
