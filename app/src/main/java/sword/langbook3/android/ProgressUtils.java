package sword.langbook3.android;

import sword.langbook3.android.models.Progress;

public final class ProgressUtils {
    static String getCompletenessString(Progress progress) {
        final float completeness = (float) progress.getAnsweredQuestionsCount() * 100 / progress.getNumberOfQuestions();
        return String.format("%.1f%%", completeness);
    }

    static KnowledgeDrawable getDrawable(Progress progress) {
        return (progress.getAnsweredQuestionsCount() > 0)? new KnowledgeDrawable(progress.getAmountPerScore()) : null;
    }

    private ProgressUtils() {
    }
}
