package sword.langbook3.android.sdb;

import org.junit.jupiter.api.Test;

import sword.collections.ImmutableList;
import sword.database.MemoryDatabase;
import sword.langbook3.android.db.AcceptationsManager;
import sword.langbook3.android.db.LanguageIdInterface;
import sword.langbook3.android.models.CharacterCompositionDefinitionArea;
import sword.langbook3.android.models.CharacterCompositionDefinitionRegister;
import sword.langbook3.android.models.IdentifiableCharacterCompositionResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static sword.collections.SizableTestUtils.assertSize;
import static sword.langbook3.android.db.LangbookDbSchema.CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT;
import static sword.langbook3.android.sdb.AcceptationsSerializer0Test.addSimpleAcceptation;

/**
 * Include all test related to all responsibilities of an AcceptationsManager.
 *
 * AcceptationsManager responsibilities are limited to the management of:
 * <li>Languages</li>
 * <li>Alphabets</li>
 * <li>Symbol arrays</li>
 * <li>Correlations</li>
 * <li>Correlation arrays</li>
 * <li>Conversions</li>
 * <li>Acceptations</li>
 */
public interface AcceptationsSerializerTest<ConceptId, LanguageId extends LanguageIdInterface<ConceptId>, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> extends AcceptationsSerializer0Test<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> {

    CharacterCompositionTypeId conceptAsCharacterCompositionTypeId(ConceptId conceptId);

    @Test
    default void testAddAcceptationAlsoIncludeCharacters() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> inManager = createManager(inDb);

        final AlphabetId inAlphabet = inManager.addLanguage("es").mainAlphabet;
        final ConceptId inConcept = inManager.getNextAvailableConceptId();
        assertNotNull(addSimpleAcceptation(inManager, inAlphabet, inConcept, "aBc"));

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> outManager = createManager(outDb);

        final CharacterId aChar = outManager.findCharacter('a');
        assertNotNull(aChar);

        final CharacterId bChar = outManager.findCharacter('B');
        assertNotNull(bChar);
        assertNotEquals(aChar, bChar);

        final CharacterId cChar = outManager.findCharacter('c');
        assertNotNull(cChar);
        assertNotEquals(aChar, cChar);
        assertNotEquals(bChar, cChar);

        assertNull(outManager.findCharacter('b'));
        assertNull(outManager.findCharacter('d'));
    }

    @Test
    default void testUpdateCharacterCompositionDefinition() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> inManager = createManager(inDb);

        final AlphabetId inAlphabet = inManager.addLanguage("es").mainAlphabet;
        final ConceptId inCompositionTypeConcept = inManager.getNextAvailableConceptId();
        assertNotNull(addSimpleAcceptation(inManager, inAlphabet, inCompositionTypeConcept, "arriba-abajo"));

        final CharacterCompositionTypeId inCompositionTypeId = conceptAsCharacterCompositionTypeId(inCompositionTypeConcept);
        final CharacterCompositionDefinitionArea first = new CharacterCompositionDefinitionArea(0, 0, CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT, CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT / 2);
        final CharacterCompositionDefinitionArea second = new CharacterCompositionDefinitionArea(0, CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT / 2, CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT, CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT / 2);
        final CharacterCompositionDefinitionRegister register = new CharacterCompositionDefinitionRegister(first, second);
        assertTrue(inManager.updateCharacterCompositionDefinition(inCompositionTypeId, register));

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> outManager = createManager(outDb);

        final AlphabetId outAlphabet = outManager.findMainAlphabetForLanguage(outManager.findLanguageByCode("es"));
        final ImmutableList<IdentifiableCharacterCompositionResult<CharacterCompositionTypeId>> outCompositionTypes = outManager.getCharacterCompositionTypes(outAlphabet);
        assertSize(1, outCompositionTypes);
        assertEquals(register, outCompositionTypes.valueAt(0).register);
    }
}
