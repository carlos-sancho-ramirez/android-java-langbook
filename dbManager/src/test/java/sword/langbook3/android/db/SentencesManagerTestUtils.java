package sword.langbook3.android.db;

import sword.langbook3.android.models.SentenceSpan;

import static sword.langbook3.android.db.StringTestUtils.rangeOf;

final class SentencesManagerTestUtils {

    static SentenceSpan newSpan(String text, String segment, int acceptation) {
        return new SentenceSpan(rangeOf(text, segment), acceptation);
    }
}
