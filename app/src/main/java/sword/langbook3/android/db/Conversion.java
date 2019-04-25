package sword.langbook3.android.db;

import sword.collections.ImmutableHashSet;
import sword.collections.ImmutableMap;
import sword.collections.ImmutableSet;
import sword.langbook3.android.collections.ImmutableIntPair;

public final class Conversion implements ConversionProposal {
    private final int _sourceAlphabet;
    private final int _targetAlphabet;
    private final ImmutableMap<String, String> _map;

    public Conversion(int sourceAlphabet, int targetAlphabet, sword.collections.Map<String, String> map) {
        if (sourceAlphabet == targetAlphabet) {
            throw new IllegalArgumentException();
        }

        _sourceAlphabet = sourceAlphabet;
        _targetAlphabet = targetAlphabet;
        _map = map.toImmutable().sort(LangbookReadableDatabase.conversionKeySortFunction);
    }

    @Override
    public int getSourceAlphabet() {
        return _sourceAlphabet;
    }

    public int getTargetAlphabet() {
        return _targetAlphabet;
    }

    public ImmutableIntPair getAlphabets() {
        return new ImmutableIntPair(_sourceAlphabet, _targetAlphabet);
    }

    public ImmutableMap<String, String> getMap() {
        return _map;
    }

    @Override
    public String convert(String text) {
        final int mapSize = _map.size();
        String result = "";
        while (text.length() > 0) {
            boolean found = false;
            for (int i = 0; i < mapSize; i++) {
                final String source = _map.keyAt(i);
                if (text.startsWith(source)) {
                    result += _map.valueAt(i);
                    text = text.substring(source.length());
                    found = true;
                    break;
                }
            }

            if (!found) {
                return null;
            }
        }

        return result;
    }

    /**
     * Apply the given conversion in the inverse order to find all original
     * strings that can be converted to the given text.
     *
     * @param text Converted text to be analyzed
     * @return A set with all source texts that results in the given text once the conversion is applied. This will be empty is none, but never null.
     */
    public ImmutableSet<String> findSourceTexts(String text) {
        final ImmutableSet.Builder<String> builder = new ImmutableHashSet.Builder<>();
        if (text == null) {
            return builder.build();
        }

        final int mapSize = _map.size();
        for (int i = 0; i < mapSize; i++) {
            final String source = _map.keyAt(i);
            final String target = _map.valueAt(i);
            if (target.equals(text)) {
                builder.add(source);
            }
            else if (text.startsWith(target)) {
                for (String result : findSourceTexts(text.substring(target.length()))) {
                    builder.add(source + result);
                }
            }
        }

        return builder.build();
    }
}
