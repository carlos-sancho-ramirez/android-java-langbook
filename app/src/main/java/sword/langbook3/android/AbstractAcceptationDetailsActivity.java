package sword.langbook3.android;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.Toast;

import sword.collections.ImmutableHashSet;
import sword.collections.ImmutableList;
import sword.collections.ImmutableMap;
import sword.collections.ImmutableSet;
import sword.collections.IntValueMap;
import sword.collections.Map;
import sword.langbook3.android.AcceptationDetailsAdapter.AcceptationNavigableItem;
import sword.langbook3.android.AcceptationDetailsAdapter.AgentNavigableItem;
import sword.langbook3.android.AcceptationDetailsAdapter.CorrelationArrayItem;
import sword.langbook3.android.AcceptationDetailsAdapter.HeaderItem;
import sword.langbook3.android.AcceptationDetailsAdapter.NonNavigableItem;
import sword.langbook3.android.AcceptationDetailsAdapter.SentenceNavigableItem;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AcceptationIdBundler;
import sword.langbook3.android.db.AgentId;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.ConceptId;
import sword.langbook3.android.db.CorrelationId;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.LanguageId;
import sword.langbook3.android.db.RuleId;
import sword.langbook3.android.db.SentenceId;
import sword.langbook3.android.models.AcceptationDetails.InvolvedAgentResultFlags;
import sword.langbook3.android.models.AcceptationDetails2;
import sword.langbook3.android.models.CharacterCompositionDefinitionRegister;
import sword.langbook3.android.models.DynamizableResult;
import sword.langbook3.android.models.IdTextPairResult;
import sword.langbook3.android.models.IdentifiableResult;
import sword.langbook3.android.models.SynonymTranslationResult;

public abstract class AbstractAcceptationDetailsActivity extends Activity {

    static final int REQUEST_CODE_CLICK_NAVIGATION = 1;
    static final int REQUEST_CODE_CREATE_SENTENCE = 2;
    static final int REQUEST_CODE_LINKED_ACCEPTATION = 3;
    static final int REQUEST_CODE_PICK_ACCEPTATION = 4;
    static final int REQUEST_CODE_PICK_BUNCH = 5;
    static final int REQUEST_CODE_PICK_DEFINITION = 6;

    interface ArgKeys {
        String ACCEPTATION = BundleKeys.ACCEPTATION;
    }

    public interface ResultKeys {
        String ACCEPTATION = BundleKeys.ACCEPTATION;
    }

    private AlphabetId _preferredAlphabet;
    AcceptationId _acceptation;
    AcceptationDetails2<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, RuleId, AgentId, SentenceId> _model;
    int _dbWriteVersion;

    boolean _hasDefinition;

    boolean _shouldShowBunchChildrenQuizMenuOption;
    ListView _listView;
    AcceptationDetailsAdapter _listAdapter;

    abstract boolean canNavigate();

    private ImmutableList<AcceptationDetailsAdapter.Item> getAdapterItems() {
        final ImmutableList.Builder<AcceptationDetailsAdapter.Item> result = new ImmutableList.Builder<>();

        final ImmutableList<CorrelationId> correlationIds = _model.getCorrelationIds();
        final ImmutableMap<CorrelationId, ImmutableCorrelation<AlphabetId>> correlations = _model.getCorrelations();
        final ImmutableSet<AlphabetId> commonAlphabets = correlationIds
                .map(id -> correlations.get(id).keySet())
                .reduce((set1, set2) -> set1.filter(set2::contains), ImmutableHashSet.empty());
        if (commonAlphabets.size() > 1) {
            final AlphabetId mainAlphabet = commonAlphabets.valueAt(0);
            final AlphabetId pronunciationAlphabet = commonAlphabets.valueAt(1);
            result.add(new CorrelationArrayItem(correlationIds, correlations, mainAlphabet, pronunciationAlphabet, canNavigate()));
        }

        final IdTextPairResult<LanguageId> language = _model.getLanguage();
        result.add(new HeaderItem(getString(R.string.accDetailsSectionSummary, _acceptation)));
        result.add(new NonNavigableItem(getString(R.string.accDetailsSectionLanguage) + ": " + language.text));

        final AcceptationId baseConceptAcceptationId = _model.getBaseConceptAcceptationId();
        _hasDefinition = baseConceptAcceptationId != null;
        if (_hasDefinition) {
            String baseText = getString(R.string.accDetailsSectionDefinition) + ": " + _model.getBaseConceptText();
            String complementsText = _model.getDefinitionComplementTexts().reduce((a, b) -> a + ", " + b, null);
            String definitionText = (complementsText != null)? baseText + " (" + complementsText + ")" : baseText;
            result.add(new AcceptationNavigableItem(baseConceptAcceptationId, definitionText, false));
            _hasDefinition = true;
        }

        final CharacterCompositionDefinitionRegister characterCompositionDefinitionRegister = _model.getCharacterCompositionDefinitionRegister();
        if (characterCompositionDefinitionRegister != null) {
            result.add(new HeaderItem(getString(R.string.accDetailsSectionCharacterCompositionDefinition)));
            result.add(new AcceptationDetailsAdapter.CharacterCompositionDefinitionItem(characterCompositionDefinitionRegister));
        }

        final String agentTextPrefix = "Agent #";
        final AcceptationId originalAcceptationId = _model.getOriginalAcceptationId();
        final ImmutableMap<RuleId, String> ruleTexts = _model.getRuleTexts();
        if (originalAcceptationId != null) {
            final String text = getString(R.string.accDetailsSectionOrigin) + ": " + _model.getOriginalAcceptationText();
            result.add(new AcceptationNavigableItem(originalAcceptationId, text, false));

            final String ruleText = getString(R.string.accDetailsSectionAppliedRule) + ": " + ruleTexts.get(_model.getAppliedRuleId());
            result.add(new AcceptationNavigableItem(_model.getAppliedRuleAcceptationId(), ruleText, false));

            final AgentId appliedAgentId = _model.getAppliedAgentId();
            final String agentText = getString(R.string.accDetailsSectionAppliedAgent) + ": " + agentTextPrefix + appliedAgentId;
            result.add(new AgentNavigableItem(appliedAgentId, agentText));
        }

        boolean subTypeFound = false;
        for (ImmutableMap.Entry<AcceptationId, String> subtype : _model.getSubtypes().entries()) {
            if (!subTypeFound) {
                result.add(new HeaderItem(getString(R.string.accDetailsSectionSubtypes)));
                subTypeFound = true;
            }

            result.add(new AcceptationNavigableItem(subtype.key(), subtype.value(), false));
        }

        final ImmutableMap<AcceptationId, SynonymTranslationResult<LanguageId>> synonymsAndTranslations = _model.getSynonymsAndTranslations();
        boolean synonymFound = false;
        for (Map.Entry<AcceptationId, SynonymTranslationResult<LanguageId>> entry : synonymsAndTranslations.entries()) {
            if (language.id.equals(entry.value().language)) {
                if (!synonymFound) {
                    result.add(new HeaderItem(getString(R.string.accDetailsSectionSynonyms)));
                    synonymFound = true;
                }

                result.add(new AcceptationNavigableItem(entry.key(), entry.value().text, entry.value().dynamic));
            }
        }

        boolean translationFound = false;
        for (Map.Entry<AcceptationId, SynonymTranslationResult<LanguageId>> entry : synonymsAndTranslations.entries()) {
            final LanguageId entryLanguage = entry.value().language;
            if (!language.id.equals(entryLanguage)) {
                if (!translationFound) {
                    result.add(new HeaderItem(getString(R.string.accDetailsSectionTranslations)));
                    translationFound = true;
                }

                final String langStr = _model.getLanguageTexts().get(entryLanguage, null);
                result.add(new AcceptationNavigableItem(entry.key(), "" + langStr + " -> " + entry.value().text, entry.value().dynamic));
            }
        }

        final ImmutableSet<AlphabetId> alphabets = _model.getTexts().keySet();
        final ImmutableMap<AcceptationId, ImmutableSet<AlphabetId>> acceptationsSharingTexts = _model.getAcceptationsSharingTexts();
        boolean acceptationSharingCorrelationArrayFound = false;
        final ImmutableSet<AcceptationId> accsSharingCorrelationArray = acceptationsSharingTexts.filter(alphabets::equalSet).keySet();
        for (AcceptationId acc : accsSharingCorrelationArray) {
            if (!acceptationSharingCorrelationArrayFound) {
                result.add(new HeaderItem(getString(R.string.accDetailsSectionAcceptationsSharingCorrelationArray)));
                acceptationSharingCorrelationArrayFound = true;
            }

            result.add(new AcceptationNavigableItem(acc, _model.getTitle(_preferredAlphabet), false));
        }

        boolean acceptationSharingTextsFound = false;
        for (AcceptationId acc : acceptationsSharingTexts.keySet().filterNot(accsSharingCorrelationArray::contains)) {
            if (!acceptationSharingTextsFound) {
                result.add(new HeaderItem(getString(R.string.accDetailsSectionAcceptationsSharingTexts)));
                acceptationSharingTextsFound = true;
            }

            final String text = _model.getAcceptationsSharingTextsDisplayableTexts().get(acc);
            result.add(new AcceptationNavigableItem(acc, text, false));
        }

        boolean parentBunchFound = false;
        for (DynamizableResult<AcceptationId> r : _model.getBunchesWhereAcceptationIsIncluded()) {
            if (!parentBunchFound) {
                result.add(new HeaderItem(getString(R.string.accDetailsSectionBunchesWhereIncluded)));
                parentBunchFound = true;
            }

            result.add(new AcceptationNavigableItem(AcceptationDetailsAdapter.ItemTypes.BUNCH_WHERE_INCLUDED,
                    r.id, r.text, r.dynamic));
        }

        boolean morphologyFound = false;
        final ImmutableMap<AcceptationId, IdentifiableResult<AgentId>> derivedAcceptations = _model.getDerivedAcceptations();
        final ImmutableMap<AgentId, RuleId> agentRules = _model.getAgentRules();
        final int derivedAcceptationsCount = derivedAcceptations.size();
        for (int i = 0; i < derivedAcceptationsCount; i++) {
            final AcceptationId accId = derivedAcceptations.keyAt(i);
            final IdentifiableResult<AgentId> r = derivedAcceptations.valueAt(i);
            if (!morphologyFound) {
                result.add(new HeaderItem(getString(R.string.accDetailsSectionDerivedAcceptations)));
                morphologyFound = true;
            }

            final String ruleText = ruleTexts.get(agentRules.get(r.id));
            result.add(new AcceptationNavigableItem(accId, ruleText + " -> " + r.text, true));
        }

        _shouldShowBunchChildrenQuizMenuOption = false;
        boolean bunchChildFound = false;
        for (DynamizableResult<AcceptationId> r : _model.getBunchChildren()) {
            if (!bunchChildFound) {
                result.add(new HeaderItem(getString(R.string.accDetailsSectionAcceptationsInThisBunch)));
                bunchChildFound = true;
                _shouldShowBunchChildrenQuizMenuOption = true;
            }

            result.add(new AcceptationNavigableItem(AcceptationDetailsAdapter.ItemTypes.ACCEPTATION_INCLUDED, r.id, r.text, r.dynamic));
        }

        final ImmutableMap<SentenceId, String> sampleSentences = _model.getSampleSentences();
        boolean sentenceFound = false;
        final int sampleSentenceCount = sampleSentences.size();
        for (int i = 0; i < sampleSentenceCount; i++) {
            final SentenceId sentenceId = sampleSentences.keyAt(i);
            final String sentence = sampleSentences.valueAt(i);

            if (!sentenceFound) {
                result.add(new HeaderItem(getString(R.string.accDetailsSectionSampleSentences)));
                sentenceFound = true;
            }

            result.add(new SentenceNavigableItem(sentenceId, sentence));
        }

        boolean agentFound = false;
        for (IntValueMap.Entry<AgentId> entry : _model.getInvolvedAgents().entries()) {
            if (!agentFound) {
                result.add(new HeaderItem(getString(R.string.accDetailsSectionInvolvedAgents)));
                agentFound = true;
            }

            final StringBuilder s = new StringBuilder(agentTextPrefix);
            s.append(entry.key()).append(" (");
            final int flags = entry.value();
            s.append(((flags & InvolvedAgentResultFlags.target) != 0)? 'T' : '-');
            s.append(((flags & InvolvedAgentResultFlags.source) != 0)? 'S' : '-');
            s.append(((flags & InvolvedAgentResultFlags.diff) != 0)? 'D' : '-');
            s.append(((flags & InvolvedAgentResultFlags.rule) != 0)? 'R' : '-');
            s.append(((flags & InvolvedAgentResultFlags.processed) != 0)? 'P' : '-');
            s.append(')');

            result.add(new AgentNavigableItem(entry.key(), s.toString()));
        }

        for (Map.Entry<AgentId, RuleId> entry : agentRules.entries()) {
            if (!agentFound) {
                result.add(new HeaderItem(getString(R.string.accDetailsSectionInvolvedAgents)));
                agentFound = true;
            }

            final String text = "Agent #" + entry.key() + " (" + ruleTexts.get(entry.value()) + ')';
            result.add(new AgentNavigableItem(entry.key(), text));
        }

        return result.build();
    }

    boolean updateModelAndUi() {
        _model = DbManager.getInstance().getManager().getAcceptationsDetails(_acceptation, _preferredAlphabet);
        _dbWriteVersion = DbManager.getInstance().getDatabase().getWriteVersion();
        if (_model != null) {
            setTitle(_model.getTitle(_preferredAlphabet));
            _listAdapter = new AcceptationDetailsAdapter(this, REQUEST_CODE_CLICK_NAVIGATION, getAdapterItems());
            _listView.setAdapter(_listAdapter);
            return true;
        }
        else {
            finish();
            return false;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_activity);

        _preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        _acceptation = AcceptationIdBundler.readAsIntentExtra(getIntent(), ArgKeys.ACCEPTATION);
        _listView = findViewById(R.id.listView);

        if (!getIntent().hasExtra(ArgKeys.ACCEPTATION)) {
            throw new IllegalArgumentException("acceptation not provided");
        }
    }

    void showFeedback(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
