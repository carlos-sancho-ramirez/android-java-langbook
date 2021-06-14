package sword.langbook3.android.db.check;

import sword.langbook3.android.db.BunchSetIdInterface;
import sword.langbook3.android.db.ConceptIdInterface;
import sword.langbook3.android.db.ConceptualizableIdInterface;
import sword.langbook3.android.db.LangbookManager;

public final class ThreeChainedAgentsPresentScenario<ConceptId extends ConceptIdInterface, LanguageId extends ConceptualizableIdInterface<ConceptId>, AlphabetId extends ConceptualizableIdInterface<ConceptId>, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId extends ConceptualizableIdInterface<ConceptId>, BunchSetId extends BunchSetIdInterface, RuleId extends ConceptualizableIdInterface<ConceptId>, AgentId, QuizId, SentenceId> {

    final LangbookManager<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, QuizId, SentenceId> manager;
    final Logger logger;

    ThreeChainedAgentsPresentScenario(
            LangbookManager<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, QuizId, SentenceId> manager,
            Logger logger) {
        this.manager = manager;
        this.logger = logger;

        logger.scenarioReached(getClass().getSimpleName());
    }
}
