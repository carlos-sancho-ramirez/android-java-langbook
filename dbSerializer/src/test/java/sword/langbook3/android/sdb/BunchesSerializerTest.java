package sword.langbook3.android.sdb;

import sword.database.MemoryDatabase;
import sword.langbook3.android.db.BunchesManager2;
import sword.langbook3.android.db.LanguageIdInterface;

/**
 * Include all test related to all responsibilities of a BunchesManager.
 *
 * BunchesManager responsibilities include all responsibilities from AcceptationsManager, and include the following ones:
 * <li>Bunches</li>
 */
interface BunchesSerializerTest<ConceptId, LanguageId extends LanguageIdInterface<ConceptId>, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId> extends AcceptationsSerializerTest<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId>, BunchesSerializer0Test<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId> {

    @Override
    BunchesManager2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId> createInManager(MemoryDatabase db);
}
