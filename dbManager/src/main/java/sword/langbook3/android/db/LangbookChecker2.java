package sword.langbook3.android.db;

import sword.langbook3.android.models.AcceptationDetails2;

public interface LangbookChecker2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId extends BunchSetIdInterface, RuleId, AgentId, QuizId, SentenceId> extends QuizzesChecker2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, QuizId>, DefinitionsChecker<ConceptId>, RuledSentencesChecker2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, SentenceId>, LangbookChecker<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, QuizId, SentenceId> {

    AcceptationDetails2<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, RuleId, AgentId, SentenceId> getAcceptationsDetails(AcceptationId staticAcceptation, AlphabetId preferredAlphabet);
}
