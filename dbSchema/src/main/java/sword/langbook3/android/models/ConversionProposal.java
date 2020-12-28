package sword.langbook3.android.models;

public interface ConversionProposal<AlphabetId> {

    /**
     * Alphabet from where the conversion will be applied.
     */
    AlphabetId getSourceAlphabet();

    /**
     * Apply this conversion to the given text and returns its converted text.
     * @param text Text to be converted
     * @return The converted text, or null if text cannot be converted.
     */
    String convert(String text);
}
