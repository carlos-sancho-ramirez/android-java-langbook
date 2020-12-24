package sword.langbook3.android.models;

import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.LangbookDbSchema;

public final class QuestionFieldDetails {
    public final AlphabetId alphabet;
    public final int rule;
    public final int flags;

    public QuestionFieldDetails(AlphabetId alphabet, int rule, int flags) {
        this.alphabet = alphabet;
        this.rule = rule;
        this.flags = flags;
    }

    public int getType() {
        return flags & LangbookDbSchema.QuestionFieldFlags.TYPE_MASK;
    }

    public boolean isAnswer() {
        return (flags & LangbookDbSchema.QuestionFieldFlags.IS_ANSWER) != 0;
    }

    @Override
    public int hashCode() {
        return (flags * 37 + rule) * 37 + alphabet.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof QuestionFieldDetails)) {
            return false;
        }

        final QuestionFieldDetails that = (QuestionFieldDetails) other;
        return alphabet == that.alphabet && rule == that.rule && flags == that.flags;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + alphabet + ',' + rule + ',' + flags + ')';
    }
}
