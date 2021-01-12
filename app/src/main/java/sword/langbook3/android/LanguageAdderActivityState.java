package sword.langbook3.android;

import android.os.Parcel;
import android.os.Parcelable;

import sword.collections.ImmutableList;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.AlphabetIdManager;
import sword.langbook3.android.db.CorrelationArrayParceler;
import sword.langbook3.android.db.CorrelationComposer;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.ImmutableCorrelationArray;
import sword.langbook3.android.db.LangbookDbManager;
import sword.langbook3.android.db.LanguageId;
import sword.langbook3.android.db.LanguageIdManager;
import sword.langbook3.android.db.LanguageIdParceler;
import sword.langbook3.android.models.LanguageCreationResult;

import static sword.langbook3.android.collections.EqualUtils.equal;

public final class LanguageAdderActivityState implements Parcelable {

    private String _languageCode;
    private LanguageId _newLanguageId;
    private int _alphabetCount;

    private ImmutableCorrelationArray<AlphabetId> _languageCorrelationArray;
    private ImmutableList<ImmutableCorrelationArray<AlphabetId>> _alphabetCorrelationArrays;

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(_languageCode);
        if (_languageCode != null) {
            LanguageIdParceler.write(dest, _newLanguageId);
            dest.writeInt(_alphabetCount);

            final int correlationArrayCount = (_alphabetCorrelationArrays != null)? _alphabetCorrelationArrays.size() + 1 : 0;
            dest.writeInt(correlationArrayCount);

            if (correlationArrayCount > 0) {
                CorrelationArrayParceler.write(dest, _languageCorrelationArray);
            }

            for (int i = 0; i < correlationArrayCount - 1; i++) {
                CorrelationArrayParceler.write(dest, _alphabetCorrelationArrays.valueAt(i));
            }
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    boolean missingAlphabetCorrelationArray() {
        return _languageCode == null || _alphabetCorrelationArrays == null || _alphabetCorrelationArrays.size() < _alphabetCount;
    }

    int getCurrentConcept() {
        final int languageConcept = LanguageIdManager.getConceptId(_newLanguageId);
        return (_alphabetCorrelationArrays != null)? languageConcept + _alphabetCorrelationArrays.size() + 1 : languageConcept;
    }

    ImmutableCorrelation<AlphabetId> getEmptyCorrelation() {
        return CorrelationComposer.getEmptyCorrelation(_newLanguageId, _alphabetCount);
    }

    void reset() {
        _languageCode = null;
        _newLanguageId = null;
        _alphabetCount = 0;

        _languageCorrelationArray = null;
        _alphabetCorrelationArrays = null;
    }

    void setBasicDetails(String code, LanguageId newLanguageId, int alphabetCount) {
        if (_languageCode != null) {
            throw new UnsupportedOperationException("Code already set");
        }

        if (code == null || alphabetCount <= 0 || newLanguageId == null) {
            throw new IllegalArgumentException();
        }

        _languageCode = code;
        _newLanguageId = newLanguageId;
        _alphabetCount = alphabetCount;
    }

    void setLanguageCorrelationArray(ImmutableCorrelationArray<AlphabetId> correlationArray) {
        if (correlationArray == null || correlationArray.isEmpty()) {
            throw new IllegalArgumentException();
        }

        if (_alphabetCorrelationArrays != null) {
            throw new UnsupportedOperationException("Code already set");
        }

        _languageCorrelationArray = correlationArray;
        _alphabetCorrelationArrays = ImmutableList.empty();
    }

    ImmutableCorrelationArray<AlphabetId> popLanguageCorrelationArray() {
        if (_languageCorrelationArray == null || _alphabetCorrelationArrays == null) {
            throw new UnsupportedOperationException("Language correlation not set");
        }

        if (!_alphabetCorrelationArrays.isEmpty()) {
            throw new UnsupportedOperationException("Unable to remove language correlation without removing alphabet correlation first");
        }

        final ImmutableCorrelationArray<AlphabetId> correlationArray = _languageCorrelationArray;
        _languageCorrelationArray = null;
        _alphabetCorrelationArrays = null;
        return correlationArray;
    }

    void setNextAlphabetCorrelationArray(ImmutableCorrelationArray<AlphabetId> correlationArray) {
        if (correlationArray == null || correlationArray.isEmpty()) {
            throw new IllegalArgumentException();
        }

        if (_alphabetCorrelationArrays == null) {
            throw new UnsupportedOperationException("Code must be set first");
        }

        if (_alphabetCorrelationArrays.size() >= _alphabetCount) {
            throw new UnsupportedOperationException("All alphabets are set");
        }

        _alphabetCorrelationArrays = _alphabetCorrelationArrays.append(correlationArray);
    }

    boolean hasAtLeastOneAlphabetCorrelationArray() {
        return _alphabetCorrelationArrays != null && !_alphabetCorrelationArrays.isEmpty();
    }

    ImmutableCorrelationArray<AlphabetId> popLastAlphabetCorrelationArray() {
        final int index = _alphabetCorrelationArrays.size() - 1;
        final ImmutableCorrelationArray<AlphabetId> correlationArray = _alphabetCorrelationArrays.valueAt(index);
        _alphabetCorrelationArrays = _alphabetCorrelationArrays.removeAt(index);
        return correlationArray;
    }

    public static final Creator<LanguageAdderActivityState> CREATOR = new Creator<LanguageAdderActivityState>() {
        @Override
        public LanguageAdderActivityState createFromParcel(Parcel in) {
            final LanguageAdderActivityState state = new LanguageAdderActivityState();
            final String languageCode = in.readString();
            if (languageCode != null) {
                final LanguageId newLanguageId = LanguageIdParceler.read(in);
                final int alphabetCount = in.readInt();
                state.setBasicDetails(languageCode, newLanguageId, alphabetCount);

                final int correlationArrayCount = in.readInt();
                if (correlationArrayCount > 0) {
                    state.setLanguageCorrelationArray(CorrelationArrayParceler.read(in));

                    for (int i = 1; i < correlationArrayCount; i++) {
                        state.setNextAlphabetCorrelationArray(CorrelationArrayParceler.read(in));
                    }
                }
            }

            return state;
        }

        @Override
        public LanguageAdderActivityState[] newArray(int size) {
            return new LanguageAdderActivityState[size];
        }
    };

    void storeIntoDatabase(LangbookDbManager manager) {
        if (missingAlphabetCorrelationArray()) {
            throw new UnsupportedOperationException();
        }

        final LanguageCreationResult<LanguageId, AlphabetId> langPair = manager.addLanguage(_languageCode);
        if (!equal(langPair.language, _newLanguageId)) {
            throw new AssertionError();
        }

        final AlphabetId sourceAlphabet = langPair.mainAlphabet;
        for (int i = 1; i < _alphabetCount; i++) {
            final AlphabetId alphabet = AlphabetIdManager.getNextAvailableId(manager);
            if (!manager.addAlphabetCopyingFromOther(alphabet, sourceAlphabet)) {
                throw new AssertionError();
            }
        }

        final int newLanguageConcept = LanguageIdManager.getConceptId(_newLanguageId);
        if (manager.addAcceptation(newLanguageConcept, _languageCorrelationArray) == null) {
            throw new AssertionError();
        }

        for (int i = 0; i < _alphabetCount; i++) {
            if (manager.addAcceptation(newLanguageConcept + i + 1, _alphabetCorrelationArrays.valueAt(i)) == null) {
                throw new AssertionError();
            }
        }
    }
}
