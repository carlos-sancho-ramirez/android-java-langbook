package sword.langbook3.android.db.check;

import sword.langbook3.android.db.ImmutableCorrelation;

final class SingleAlphabetCorrelationComposer<AlphabetId> {
    final AlphabetId alphabet;

    SingleAlphabetCorrelationComposer(AlphabetId alphabet) {
        this.alphabet = alphabet;
    }

    ImmutableCorrelation<AlphabetId> compose(String text) {
        return new ImmutableCorrelation.Builder<AlphabetId>()
                .put(alphabet, text)
                .build();
    }
}
