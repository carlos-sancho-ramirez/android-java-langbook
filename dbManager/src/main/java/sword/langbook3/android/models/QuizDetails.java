package sword.langbook3.android.models;

import sword.collections.ImmutableList;

import static sword.collections.SortUtils.equal;
import static sword.langbook3.android.util.PreconditionUtils.ensureValidArguments;

public final class QuizDetails<AlphabetId, BunchId, RuleId> {
    public final BunchId bunch;
    public final ImmutableList<QuestionFieldDetails<AlphabetId, RuleId>> fields;

    public QuizDetails(BunchId bunch, ImmutableList<QuestionFieldDetails<AlphabetId, RuleId>> fields) {
        ensureValidArguments(fields != null && fields.size() >= 2 && !fields.allMatch(QuestionFieldDetails::isAnswer) && fields.anyMatch(QuestionFieldDetails::isAnswer));

        this.bunch = bunch;
        this.fields = fields;
    }

    @Override
    public int hashCode() {
        return fields.hashCode() * 41 + ((bunch != null)? bunch.hashCode() : 0);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof QuizDetails)) {
            return false;
        }

        final QuizDetails that = (QuizDetails) other;
        return equal(bunch, that.bunch) && fields.equals(that.fields);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + bunch + ',' + fields.toString() + ')';
    }
}
