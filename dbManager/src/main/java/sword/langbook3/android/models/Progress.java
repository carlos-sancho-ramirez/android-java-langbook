package sword.langbook3.android.models;

import sword.collections.ImmutableIntList;

public final class Progress {
    private final ImmutableIntList amountPerScore;
    private final int answeredQuestionsCount;
    private final int numberOfQuestions;

    public Progress(ImmutableIntList amountPerScore, int numberOfQuestions) {
        final int answeredQuestions = amountPerScore.sum();

        if (answeredQuestions > numberOfQuestions) {
            throw new IllegalArgumentException();
        }

        this.amountPerScore = amountPerScore;
        this.answeredQuestionsCount = answeredQuestions;
        this.numberOfQuestions = numberOfQuestions;
    }

    public ImmutableIntList getAmountPerScore() {
        return amountPerScore;
    }

    public int getAnsweredQuestionsCount() {
        return answeredQuestionsCount;
    }

    public int getNumberOfQuestions() {
        return numberOfQuestions;
    }
}
