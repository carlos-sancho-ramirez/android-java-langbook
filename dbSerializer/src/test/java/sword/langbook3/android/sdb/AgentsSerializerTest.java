package sword.langbook3.android.sdb;

import sword.database.MemoryDatabase;
import sword.langbook3.android.db.AgentsManager2;
import sword.langbook3.android.db.BunchSetIdInterface;
import sword.langbook3.android.db.LanguageIdInterface;

/**
 * Include all test related to all values that a BunchesSerializer should serialize.
 *
 * Values the the AcceptationsSerializer should serialize are limited to:
 * <li>Bunches</li>
 */
interface AgentsSerializerTest<ConceptId, LanguageId extends LanguageIdInterface<ConceptId>, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId extends BunchSetIdInterface, RuleId, AgentId> extends BunchesSerializerTest<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId>, AgentsSerializer0Test<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> {

    @Override
    AgentsManager2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> createInManager(MemoryDatabase db);
}
