package sword.langbook3.android.db;

import sword.langbook3.android.models.SentenceSpan;

import static sword.collections.StringTestUtils.rangeOf;

final class SentencesManagerTestUtils {

    static <AcceptationId> SentenceSpan<AcceptationId> newSpan(String text, String segment, AcceptationId acceptation) {
        return new SentenceSpan<>(rangeOf(text, segment), acceptation);
    }

    private SentencesManagerTestUtils() {
    }
}
