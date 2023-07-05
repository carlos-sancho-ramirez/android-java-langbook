package sword.langbook3.android.sdb;

import sword.database.MemoryDatabase;
import sword.langbook3.android.db.BunchSetIdInterface;
import sword.langbook3.android.db.LanguageIdInterface;
import sword.langbook3.android.db.RuledSentencesManager2;

interface RuledSentencesSerializer1Test<ConceptId, LanguageId extends LanguageIdInterface<ConceptId>, AlphabetId, CharacterId, CharacterCompositionTypeId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId extends BunchSetIdInterface, RuleId, AgentId, SentenceId> extends AgentsSerializer1Test<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId>, RuledSentencesSerializer0Test<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, SentenceId> {

    @Override
    RuledSentencesManager2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, SentenceId> createInManager(MemoryDatabase db);
}
