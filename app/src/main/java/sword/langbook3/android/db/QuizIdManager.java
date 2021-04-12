package sword.langbook3.android.db;

import sword.database.DbValue;

public final class QuizIdManager implements IntSetter<QuizId> {

    @Override
    public QuizId getKeyFromInt(int key) {
        return (key != 0)? new QuizId(key) : null;
    }

    @Override
    public QuizId getKeyFromDbValue(DbValue value) {
        return getKeyFromInt(value.toInt());
    }
}
