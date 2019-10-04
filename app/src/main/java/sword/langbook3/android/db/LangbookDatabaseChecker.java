package sword.langbook3.android.db;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntPairMap;
import sword.collections.ImmutableIntSet;
import sword.collections.MutableIntKeyMap;
import sword.database.DbExporter;
import sword.langbook3.android.collections.ImmutableIntPair;
import sword.langbook3.android.models.Conversion;
import sword.langbook3.android.models.DefinitionDetails;

abstract class LangbookDatabaseChecker implements AgentsChecker, DefinitionsChecker {

    abstract DbExporter.Database getDatabase();

    @Override
    public Integer findLanguageByCode(String code) {
        return LangbookReadableDatabase.findLanguageByCode(getDatabase(), code);
    }

    @Override
    public ImmutableIntSet findAlphabetsByLanguage(int language) {
        return LangbookReadableDatabase.findAlphabetsByLanguage(getDatabase(), language);
    }

    @Override
    public ImmutableIntKeyMap<String> getAcceptationTexts(int acceptation) {
        return LangbookReadableDatabase.getAcceptationTexts(getDatabase(), acceptation);
    }

    @Override
    public Conversion getConversion(ImmutableIntPair pair) {
        return LangbookReadableDatabase.getConversion(getDatabase(), pair);
    }

    @Override
    public int getMaxConcept() {
        return LangbookReadableDatabase.getMaxConcept(getDatabase());
    }

    @Override
    public ImmutableIntPairMap getConversionsMap() {
        return LangbookReadableDatabase.getConversionsMap(getDatabase());
    }

    @Override
    public ImmutableIntSet findAcceptationsByConcept(int concept) {
        return LangbookReadableDatabase.findAcceptationsByConcept(getDatabase(), concept);
    }

    @Override
    public ImmutableIntSet getAcceptationsInBunch(int bunch) {
        return LangbookReadableDatabase.getAcceptationsInBunch(getDatabase(), bunch);
    }

    @Override
    public ImmutableIntKeyMap<String> readAllMatchingBunches(ImmutableIntKeyMap<String> texts, int preferredAlphabet) {
        return LangbookReadableDatabase.readAllMatchingBunches(getDatabase(), texts, preferredAlphabet);
    }

    @Override
    public MutableIntKeyMap<String> readCorrelationArrayTexts(int correlationArrayId) {
        return LangbookReadableDatabase.readCorrelationArrayTexts(getDatabase(), correlationArrayId);
    }

    @Override
    public DefinitionDetails getDefinition(int concept) {
        return LangbookReadableDatabase.getDefinition(getDatabase(), concept);
    }
}
