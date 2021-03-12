package sword.langbook3.android.models;

import sword.collections.ImmutableList;

public final class QuizDetails<AlphabetId, BunchId, RuleId> {
    public final BunchId bunch;
    public final ImmutableList<QuestionFieldDetails<AlphabetId, RuleId>> fields;

    public QuizDetails(BunchId bunch, ImmutableList<QuestionFieldDetails<AlphabetId, RuleId>> fields) {
        if (bunch == null || fields == null || fields.size() < 2 || !fields.anyMatch(field -> !field.isAnswer()) || !fields.anyMatch(QuestionFieldDetails::isAnswer)) {
            throw new IllegalArgumentException();
        }

        this.bunch = bunch;
        this.fields = fields;
    }

    @Override
    public int hashCode() {
        return fields.hashCode() * 41 + bunch.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof QuizDetails)) {
            return false;
        }

        final QuizDetails that = (QuizDetails) other;
        return bunch.equals(that.bunch) && fields.equals(that.fields);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + bunch + ',' + fields.toString() + ')';
    }
}
