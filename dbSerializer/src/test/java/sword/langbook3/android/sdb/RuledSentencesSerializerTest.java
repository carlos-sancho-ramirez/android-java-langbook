package sword.langbook3.android.sdb;

import sword.langbook3.android.db.BunchSetIdInterface;
import sword.langbook3.android.db.LanguageIdInterface;

interface RuledSentencesSerializerTest<ConceptId, LanguageId extends LanguageIdInterface<ConceptId>, AlphabetId, CharacterId, CharacterCompositionTypeId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId extends BunchSetIdInterface, RuleId, AgentId, SentenceId> extends AgentsSerializerTest<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId>, RuledSentencesSerializer1Test<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, SentenceId> {
}
