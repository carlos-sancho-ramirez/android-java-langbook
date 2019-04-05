package sword.langbook3.android;

import android.os.Parcel;
import android.os.Parcelable;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntList;
import sword.collections.ImmutableIntRange;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableList;
import sword.database.Database;

public final class LanguageAdderActivityState implements Parcelable {

    private String _languageCode;
    private int _newLanguageId;
    private int _alphabetCount;

    private ImmutableList<ImmutableIntKeyMap<String>> _languageCorrelationArray;
    private ImmutableList<ImmutableList<ImmutableIntKeyMap<String>>> _alphabetCorrelationArrays;

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(_languageCode);
        if (_languageCode != null) {
            dest.writeInt(_newLanguageId);
            dest.writeInt(_alphabetCount);

            final int correlationArrayCount = (_alphabetCorrelationArrays != null)? _alphabetCorrelationArrays.size() + 1 : 0;
            dest.writeInt(correlationArrayCount);

            if (correlationArrayCount > 0) {
                ParcelableCorrelationArray.write(dest, _languageCorrelationArray);
            }

            for (int i = 0; i < correlationArrayCount - 1; i++) {
                ParcelableCorrelationArray.write(dest, _alphabetCorrelationArrays.valueAt(i));
            }
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    boolean missingBasicDetails() {
        return _languageCode == null;
    }

    boolean missingLanguageCorrelationArray() {
        return _languageCode == null || _alphabetCorrelationArrays == null;
    }

    boolean missingAlphabetCorrelationArray() {
        return _languageCode == null || _alphabetCorrelationArrays == null || _alphabetCorrelationArrays.size() < _alphabetCount;
    }

    int getCurrentConcept() {
        return (_alphabetCorrelationArrays != null)? _newLanguageId + _alphabetCorrelationArrays.size() + 1 : _newLanguageId;
    }

    ImmutableIntSet getAlphabets() {
        return new ImmutableIntRange(_newLanguageId + 1, _newLanguageId + _alphabetCount);
    }

    void setBasicDetails(String code, int newLanguageId, int alphabetCount) {
        if (_languageCode != null) {
            throw new UnsupportedOperationException("Code already set");
        }

        if (code == null || alphabetCount <= 0 || newLanguageId == 0) {
            throw new IllegalArgumentException();
        }

        _languageCode = code;
        _newLanguageId = newLanguageId;
        _alphabetCount = alphabetCount;
    }

    void setLanguageCorrelationArray(ImmutableList<ImmutableIntKeyMap<String>> correlationArray) {
        if (correlationArray == null || correlationArray.isEmpty()) {
            throw new IllegalArgumentException();
        }

        if (_alphabetCorrelationArrays != null) {
            throw new UnsupportedOperationException("Code already set");
        }

        _languageCorrelationArray = correlationArray;
        _alphabetCorrelationArrays = ImmutableList.empty();
    }

    void setNextAlphabetCorrelationArray(ImmutableList<ImmutableIntKeyMap<String>> correlationArray) {
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

    public static final Creator<LanguageAdderActivityState> CREATOR = new Creator<LanguageAdderActivityState>() {
        @Override
        public LanguageAdderActivityState createFromParcel(Parcel in) {
            final LanguageAdderActivityState state = new LanguageAdderActivityState();
            final String languageCode = in.readString();
            if (languageCode != null) {
                final int newAlphabetId = in.readInt();
                final int alphabetCount = in.readInt();
                state.setBasicDetails(languageCode, newAlphabetId, alphabetCount);

                final int correlationArrayCount = in.readInt();
                if (correlationArrayCount > 0) {
                    state.setLanguageCorrelationArray(ParcelableCorrelationArray.read(in));

                    for (int i = 1; i < correlationArrayCount; i++) {
                        state.setNextAlphabetCorrelationArray(ParcelableCorrelationArray.read(in));
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

    void storeIntoDatabase(Database db) {
        if (missingAlphabetCorrelationArray()) {
            throw new UnsupportedOperationException();
        }

        final ImmutableIntPair langPair = LangbookDatabase.addLanguage(db, _languageCode);
        if (langPair.left != _newLanguageId || langPair.right != _newLanguageId + 1) {
            throw new AssertionError();
        }

        for (int i = 1; i < _alphabetCount; i++) {
            final int alphabet = LangbookDatabase.addAlphabet(db, _newLanguageId);
            if (alphabet != _newLanguageId + i + 1) {
                throw new AssertionError();
            }
        }

        final ImmutableIntList langArray = _languageCorrelationArray.mapToInt(correlation -> LangbookDatabase.obtainCorrelation(db, correlation));
        final int langArrayId = LangbookDatabase.obtainCorrelationArray(db, langArray);
        if (LangbookDatabase.addAcceptation(db, _newLanguageId, langArrayId) == null) {
            throw new AssertionError();
        }

        for (int i = 0; i < _alphabetCount; i++) {
            final ImmutableList<ImmutableIntKeyMap<String>> array = _alphabetCorrelationArrays.valueAt(i);
            final ImmutableIntList alphabetArray = array.mapToInt(correlation -> LangbookDatabase.obtainCorrelation(db, correlation));
            final int alphabetArrayId = LangbookDatabase.obtainCorrelationArray(db, alphabetArray);
            if (LangbookDatabase.addAcceptation(db, _newLanguageId + i + 1, alphabetArrayId) == null) {
                throw new AssertionError();
            }
        }
    }
}
