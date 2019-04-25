package sword.langbook3.android.db;

import sword.collections.ImmutableList;

public final class QuizDetails {
    public final int bunch;
    public final ImmutableList<LangbookReadableDatabase.QuestionFieldDetails> fields;

    QuizDetails(int bunch, ImmutableList<LangbookReadableDatabase.QuestionFieldDetails> fields) {
        if (fields == null || fields.size() < 2 || !fields.anyMatch(field -> !field.isAnswer()) || !fields.anyMatch(LangbookReadableDatabase.QuestionFieldDetails::isAnswer)) {
            throw new IllegalArgumentException();
        }

        this.bunch = bunch;
        this.fields = fields;
    }

    @Override
    public int hashCode() {
        return fields.hashCode() * 41 + bunch;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof QuizDetails)) {
            return false;
        }

        final QuizDetails that = (QuizDetails) other;
        return bunch == that.bunch && fields.equals(that.fields);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + bunch + ',' + fields.toString() + ')';
    }
}
