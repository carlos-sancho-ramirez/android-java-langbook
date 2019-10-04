package sword.langbook3.android.db;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.MutableIntKeyMap;

public interface AgentsChecker extends BunchesChecker {
    ImmutableIntKeyMap<String> readAllMatchingBunches(ImmutableIntKeyMap<String> texts, int preferredAlphabet);
    MutableIntKeyMap<String> readCorrelationArrayTexts(int correlationArrayId);
}
