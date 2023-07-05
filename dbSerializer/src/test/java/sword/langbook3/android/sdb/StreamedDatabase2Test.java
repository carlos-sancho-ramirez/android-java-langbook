package sword.langbook3.android.sdb;

import java.io.IOException;

import sword.database.MemoryDatabase;
import sword.langbook3.android.db.ConceptsChecker;
import sword.langbook3.android.db.IntSetter;
import sword.langbook3.android.db.LangbookChecker2;
import sword.langbook3.android.db.LangbookDatabaseManager2;
import sword.langbook3.android.db.LangbookManager2;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class StreamedDatabase2Test implements RuledSentencesSerializerTest<ConceptIdHolder, LanguageIdHolder, AlphabetIdHolder, CharacterIdHolder, CharacterCompositionTypeIdHolder, SymbolArrayIdHolder, CorrelationIdHolder, CorrelationArrayIdHolder, AcceptationIdHolder, BunchIdHolder, BunchSetIdHolder, RuleIdHolder, AgentIdHolder, SentenceIdHolder> {

    private final AlphabetIdManager alphabetIdManager = new AlphabetIdManager();
    private final AcceptationIdManager acceptationIdManager = new AcceptationIdManager();
    private final BunchIdManager bunchIdManager = new BunchIdManager();
    private final RuleIdManager ruleIdManager = new RuleIdManager();
    private final CharacterCompositionTypeIdManager characterCompositionTypeIdManager = new CharacterCompositionTypeIdManager();

    @Override
    public LangbookManager2<ConceptIdHolder, LanguageIdHolder, AlphabetIdHolder, CharacterIdHolder, CharacterCompositionTypeIdHolder, SymbolArrayIdHolder, CorrelationIdHolder, CorrelationArrayIdHolder, AcceptationIdHolder, BunchIdHolder, BunchSetIdHolder, RuleIdHolder, AgentIdHolder, QuizIdHolder, SentenceIdHolder> createInManager(MemoryDatabase db) {
        return new LangbookDatabaseManager2<>(db, new ConceptIdManager(), new LanguageIdManager(), alphabetIdManager, new CharacterIdManager(), new CharacterCompositionTypeIdManager(), new SymbolArrayIdManager(), new CorrelationIdManager(), new CorrelationArrayIdManager(), acceptationIdManager, bunchIdManager, new BunchSetIdManager(), ruleIdManager, new AgentIdManager(), new QuizIdManager(), new SentenceIdManager());
    }

    @Override
    public LangbookChecker2<ConceptIdHolder, LanguageIdHolder, AlphabetIdHolder, CharacterIdHolder, CharacterCompositionTypeIdHolder, SymbolArrayIdHolder, CorrelationIdHolder, CorrelationArrayIdHolder, AcceptationIdHolder, BunchIdHolder, BunchSetIdHolder, RuleIdHolder, AgentIdHolder, QuizIdHolder, SentenceIdHolder> createOutChecker(MemoryDatabase db) {
        return new LangbookDatabaseManager2<>(db, new ConceptIdManager(), new LanguageIdManager(), alphabetIdManager, new CharacterIdManager(), new CharacterCompositionTypeIdManager(), new SymbolArrayIdManager(), new CorrelationIdManager(), new CorrelationArrayIdManager(), acceptationIdManager, bunchIdManager, new BunchSetIdManager(), ruleIdManager, new AgentIdManager(), new QuizIdManager(), new SentenceIdManager());
    }

    @Override
    public MemoryDatabase cloneBySerializing(MemoryDatabase inDb) {
        final TestStream outStream = new TestStream();
        try {
            new StreamedDatabaseWriter(inDb, outStream, null).write();
            outStream.close();
            final AssertStream inStream = outStream.toInputStream();

            final MemoryDatabase newDb = new MemoryDatabase();
            final StreamedDatabaseReaderInterface dbReader = new StreamedDatabaseReader(newDb, inStream, null);
            new DatabaseInflater(newDb, dbReader, null).read();
            assertTrue(inStream.allBytesRead());
            return newDb;
        }
        catch (IOException e) {
            throw new AssertionError("IOException thrown");
        }
    }

    @Override
    public CharacterCompositionTypeIdHolder conceptAsCharacterCompositionTypeId(ConceptIdHolder conceptId) {
        return characterCompositionTypeIdManager.getKeyFromConceptId(conceptId);
    }

    @Override
    public RuleIdHolder conceptAsRuleId(ConceptIdHolder conceptId) {
        return ruleIdManager.getKeyFromConceptId(conceptId);
    }

    @Override
    public BunchIdHolder conceptAsBunchId(ConceptIdHolder conceptId) {
        return bunchIdManager.getKeyFromConceptId(conceptId);
    }

    @Override
    public AlphabetIdHolder getNextAvailableId(ConceptsChecker<ConceptIdHolder> manager) {
        return alphabetIdManager.getKeyFromConceptId(manager.getNextAvailableConceptId());
    }

    @Override
    public IntSetter<AcceptationIdHolder> getAcceptationIdManager() {
        return acceptationIdManager;
    }
}
