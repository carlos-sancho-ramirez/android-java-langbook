package sword.langbook3.android.sdb;

import org.junit.jupiter.api.Test;

import sword.collections.ImmutableList;
import sword.database.MemoryDatabase;
import sword.langbook3.android.db.AcceptationsChecker2;
import sword.langbook3.android.db.AcceptationsManager2;
import sword.langbook3.android.db.LanguageIdInterface;
import sword.langbook3.android.models.CharacterCompositionDefinitionArea;
import sword.langbook3.android.models.CharacterCompositionDefinitionRegister;
import sword.langbook3.android.models.CharacterCompositionEditorModel;
import sword.langbook3.android.models.CharacterCompositionRepresentation;
import sword.langbook3.android.models.IdentifiableCharacterCompositionResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static sword.collections.SizableTestUtils.assertSize;
import static sword.langbook3.android.db.LangbookDbSchema.CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT;
import static sword.langbook3.android.models.CharacterCompositionRepresentation.INVALID_CHARACTER;
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

    @Override
    AcceptationsManager2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> createInManager(MemoryDatabase db);
    CharacterCompositionTypeId conceptAsCharacterCompositionTypeId(ConceptId conceptId);

    static <ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> void insertUpDownCharacterCompositionDefinition(
            AcceptationsManager2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> manager, CharacterCompositionTypeId typeId) {
        final CharacterCompositionDefinitionArea first = new CharacterCompositionDefinitionArea(0, 0, CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT, CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT / 2);
        final CharacterCompositionDefinitionArea second = new CharacterCompositionDefinitionArea(0, CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT / 2, CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT, CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT / 2);
        final CharacterCompositionDefinitionRegister register = new CharacterCompositionDefinitionRegister(first, second);
        assertTrue(manager.updateCharacterCompositionDefinition(typeId, register));
    }

    static <ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> void insertLeftRightCharacterCompositionDefinition(
            AcceptationsManager2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> manager, CharacterCompositionTypeId typeId) {
        final CharacterCompositionDefinitionArea first = new CharacterCompositionDefinitionArea(0, 0, CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT / 2, CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT);
        final CharacterCompositionDefinitionArea second = new CharacterCompositionDefinitionArea(CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT / 2, 0, CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT / 2, CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT);
        final CharacterCompositionDefinitionRegister register = new CharacterCompositionDefinitionRegister(first, second);
        assertTrue(manager.updateCharacterCompositionDefinition(typeId, register));
    }

    @Test
    default void testUpdateCharacterCompositionDefinition() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AcceptationsManager2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> inManager = createInManager(inDb);

        final AlphabetId inAlphabet = inManager.addLanguage("es").mainAlphabet;
        final ConceptId inCompositionTypeConcept = inManager.getNextAvailableConceptId();
        assertNotNull(addSimpleAcceptation(inManager, inAlphabet, inCompositionTypeConcept, "arriba-abajo"));

        final CharacterCompositionTypeId inCompositionTypeId = conceptAsCharacterCompositionTypeId(inCompositionTypeConcept);
        final CharacterCompositionDefinitionArea first = new CharacterCompositionDefinitionArea(0, 0, CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT, CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT / 2);
        final CharacterCompositionDefinitionArea second = new CharacterCompositionDefinitionArea(0, CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT / 2, CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT, CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT / 2);
        final CharacterCompositionDefinitionRegister register = new CharacterCompositionDefinitionRegister(first, second);
        assertTrue(inManager.updateCharacterCompositionDefinition(inCompositionTypeId, register));

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AcceptationsChecker2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> outManager = createOutChecker(outDb);

        final AlphabetId outAlphabet = outManager.findMainAlphabetForLanguage(outManager.findLanguageByCode("es"));
        final ImmutableList<IdentifiableCharacterCompositionResult<CharacterCompositionTypeId>> outCompositionTypes = outManager.getCharacterCompositionTypes(outAlphabet);
        assertSize(1, outCompositionTypes);
        assertEquals(register, outCompositionTypes.valueAt(0).register);
    }

    @Test
    default void testUpdateCharacterComposition() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AcceptationsManager2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> inManager = createInManager(inDb);

        final AlphabetId alphabet = inManager.addLanguage("es").mainAlphabet;
        final ConceptId moreConcept = inManager.getNextAvailableConceptId();
        assertNotNull(addSimpleAcceptation(inManager, alphabet, moreConcept, "más"));

        final CharacterId composed = inManager.findCharacter('á');
        assertNotNull(composed);

        final ConceptId compositionTypeConcept = inManager.getNextAvailableConceptId();
        assertNotNull(addSimpleAcceptation(inManager, alphabet, compositionTypeConcept, "arriba-abajo"));

        final CharacterCompositionTypeId compositionTypeId = conceptAsCharacterCompositionTypeId(compositionTypeConcept);
        insertUpDownCharacterCompositionDefinition(inManager, compositionTypeId);

        final CharacterCompositionRepresentation firstRepresentation = new CharacterCompositionRepresentation('´', null);
        final CharacterCompositionRepresentation secondRepresentation = new CharacterCompositionRepresentation('a', null);
        assertTrue(inManager.updateCharacterComposition(composed, firstRepresentation, secondRepresentation, compositionTypeId));

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AcceptationsChecker2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> outManager = createOutChecker(outDb);

        final CharacterId outComposed = outManager.findCharacter('á');
        assertNotNull(outComposed);

        final CharacterCompositionEditorModel<CharacterId, CharacterCompositionTypeId> model = outManager.getCharacterCompositionDetails(outComposed);
        assertEquals('´', model.first.representation.character);
        assertNull(model.first.representation.token);
        assertEquals('a', model.second.representation.character);
        assertNull(model.second.representation.token);
        assertEquals(compositionTypeId, model.compositionType);
    }

    @Test
    default void testUpdateCharacterCompositionWithOneToken() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AcceptationsManager2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> inManager = createInManager(inDb);

        final AlphabetId alphabet = inManager.addLanguage("es").mainAlphabet;
        final ConceptId moreConcept = inManager.getNextAvailableConceptId();
        assertNotNull(addSimpleAcceptation(inManager, alphabet, moreConcept, "más"));

        final CharacterId composed = inManager.findCharacter('á');
        assertNotNull(composed);

        final ConceptId compositionTypeConcept = inManager.getNextAvailableConceptId();
        assertNotNull(addSimpleAcceptation(inManager, alphabet, compositionTypeConcept, "arriba-abajo"));

        final CharacterCompositionTypeId compositionTypeId = conceptAsCharacterCompositionTypeId(compositionTypeConcept);
        insertUpDownCharacterCompositionDefinition(inManager, compositionTypeId);

        final CharacterCompositionRepresentation firstRepresentation = new CharacterCompositionRepresentation(INVALID_CHARACTER, "tilde");
        final CharacterCompositionRepresentation secondRepresentation = new CharacterCompositionRepresentation('a', null);
        assertTrue(inManager.updateCharacterComposition(composed, firstRepresentation, secondRepresentation, compositionTypeId));

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AcceptationsChecker2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> outManager = createOutChecker(outDb);

        final CharacterId outComposed = outManager.findCharacter('á');
        assertNotNull(outComposed);

        final CharacterCompositionEditorModel<CharacterId, CharacterCompositionTypeId> model = outManager.getCharacterCompositionDetails(outComposed);
        assertEquals(INVALID_CHARACTER, model.first.representation.character);
        assertEquals("tilde", model.first.representation.token);
        assertEquals('a', model.second.representation.character);
        assertNull(model.second.representation.token);
        assertEquals(compositionTypeId, model.compositionType);
    }

    @Test
    default void testUpdateCharacterCompositionWithAllTokens() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AcceptationsManager2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> inManager = createInManager(inDb);

        final AlphabetId alphabet = inManager.addLanguage("es").mainAlphabet;
        final ConceptId moreConcept = inManager.getNextAvailableConceptId();
        assertNotNull(addSimpleAcceptation(inManager, alphabet, moreConcept, "más"));

        final CharacterId composed = inManager.findCharacter('á');
        assertNotNull(composed);

        final ConceptId compositionTypeConcept = inManager.getNextAvailableConceptId();
        assertNotNull(addSimpleAcceptation(inManager, alphabet, compositionTypeConcept, "arriba-abajo"));

        final CharacterCompositionTypeId compositionTypeId = conceptAsCharacterCompositionTypeId(compositionTypeConcept);
        insertUpDownCharacterCompositionDefinition(inManager, compositionTypeId);

        final CharacterCompositionRepresentation firstRepresentation = new CharacterCompositionRepresentation(INVALID_CHARACTER, "tildeTop");
        final CharacterCompositionRepresentation secondRepresentation = new CharacterCompositionRepresentation(INVALID_CHARACTER, "tildeBottom");
        assertTrue(inManager.updateCharacterComposition(composed, firstRepresentation, secondRepresentation, compositionTypeId));

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AcceptationsChecker2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> outManager = createOutChecker(outDb);

        final CharacterId outComposed = outManager.findCharacter('á');
        assertNotNull(outComposed);

        final CharacterCompositionEditorModel<CharacterId, CharacterCompositionTypeId> model = outManager.getCharacterCompositionDetails(outComposed);
        assertEquals(INVALID_CHARACTER, model.first.representation.character);
        assertEquals("tildeTop", model.first.representation.token);
        assertEquals(INVALID_CHARACTER, model.second.representation.character);
        assertEquals("tildeBottom", model.second.representation.token);
        assertEquals(compositionTypeId, model.compositionType);
    }

    @Test
    default void testUpdateCharacterCompositionMultipleTimesEnsuringTokenBeforeNewCharacters() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AcceptationsManager2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> inManager = createInManager(inDb);

        final AlphabetId alphabet = inManager.addLanguage("es").mainAlphabet;
        final ConceptId moreConcept = inManager.getNextAvailableConceptId();
        assertNotNull(addSimpleAcceptation(inManager, alphabet, moreConcept, "más"));

        final CharacterId aComposed = inManager.findCharacter('á');
        assertNotNull(aComposed);

        final ConceptId compositionTypeConcept = inManager.getNextAvailableConceptId();
        assertNotNull(addSimpleAcceptation(inManager, alphabet, compositionTypeConcept, "arriba-abajo"));

        final CharacterCompositionTypeId compositionTypeId = conceptAsCharacterCompositionTypeId(compositionTypeConcept);
        insertUpDownCharacterCompositionDefinition(inManager, compositionTypeId);

        CharacterCompositionRepresentation firstRepresentation = new CharacterCompositionRepresentation(INVALID_CHARACTER, "tilde");
        CharacterCompositionRepresentation secondRepresentation = new CharacterCompositionRepresentation('a', null);
        assertTrue(inManager.updateCharacterComposition(aComposed, firstRepresentation, secondRepresentation, compositionTypeId));

        final ConceptId compositionType2Concept = inManager.getNextAvailableConceptId();
        assertNotNull(addSimpleAcceptation(inManager, alphabet, compositionType2Concept, "izquierda-derecha"));

        final CharacterCompositionTypeId compositionType2Id = conceptAsCharacterCompositionTypeId(compositionType2Concept);
        insertLeftRightCharacterCompositionDefinition(inManager, compositionType2Id);

        final CharacterId tildeId = inManager.getCharacterCompositionDetails(aComposed).first.id;
        firstRepresentation = new CharacterCompositionRepresentation(INVALID_CHARACTER, "tildeFirst");
        secondRepresentation = new CharacterCompositionRepresentation(INVALID_CHARACTER, "tildeSecond");
        assertTrue(inManager.updateCharacterComposition(tildeId, firstRepresentation, secondRepresentation, compositionType2Id));

        final ConceptId songConcept = inManager.getNextAvailableConceptId();
        assertNotNull(addSimpleAcceptation(inManager, alphabet, songConcept, "canción"));

        final CharacterId oComposed = inManager.findCharacter('ó');
        assertNotNull(oComposed);

        firstRepresentation = new CharacterCompositionRepresentation(INVALID_CHARACTER, "tilde");
        secondRepresentation = new CharacterCompositionRepresentation('o', null);
        assertTrue(inManager.updateCharacterComposition(oComposed, firstRepresentation, secondRepresentation, compositionTypeId));

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AcceptationsChecker2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> outManager = createOutChecker(outDb);

        final CharacterId outAComposed = outManager.findCharacter('á');
        assertNotNull(outAComposed);

        final CharacterId outOComposed = outManager.findCharacter('ó');
        assertNotNull(outOComposed);

        CharacterCompositionEditorModel<CharacterId, CharacterCompositionTypeId> model = outManager.getCharacterCompositionDetails(outAComposed);
        final CharacterId outTildeId = model.first.id;
        final CharacterCompositionTypeId outUpDownTypeId = model.compositionType;
        assertEquals(INVALID_CHARACTER, model.first.representation.character);
        assertEquals("tilde", model.first.representation.token);
        assertEquals('a', model.second.representation.character);
        assertNull(model.second.representation.token);

        model = outManager.getCharacterCompositionDetails(outTildeId);
        assertEquals(INVALID_CHARACTER, model.first.representation.character);
        assertEquals("tildeFirst", model.first.representation.token);
        assertEquals(INVALID_CHARACTER, model.second.representation.character);
        assertEquals("tildeSecond", model.second.representation.token);
        assertNotEquals(outUpDownTypeId, model.compositionType);

        model = outManager.getCharacterCompositionDetails(outOComposed);
        assertEquals(INVALID_CHARACTER, model.first.representation.character);
        assertEquals("tilde", model.first.representation.token);
        assertEquals('o', model.second.representation.character);
        assertNull(model.second.representation.token);
        assertEquals(outTildeId, model.first.id);
        assertEquals(outUpDownTypeId, model.compositionType);
    }
}
