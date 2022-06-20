package sword.langbook3.android;

import android.os.Parcel;
import android.os.Parcelable;

import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.AlphabetIdParceler;
import sword.langbook3.android.db.LanguageId;
import sword.langbook3.android.db.LanguageIdParceler;

public final class AlphabetsActivityState implements Parcelable {

    interface IntrinsicStates {
        int NORMAL = 0;
        int SHOWING_LANGUAGE_OPTIONS = 1;
        int SHOWING_ALPHABET_OPTIONS = 2;
        int LANGUAGE_DELETE_CONFIRMATION = 3;
        int ALPHABET_DELETE_CONFIRMATION = 4;
    }

    private int _intrinsicState;
    private LanguageId _languageId;
    private AlphabetId _alphabetId;

    private AlphabetsActivityState(Parcel in) {
        _intrinsicState = in.readInt();

        if (_intrinsicState == IntrinsicStates.SHOWING_LANGUAGE_OPTIONS || _intrinsicState == IntrinsicStates.LANGUAGE_DELETE_CONFIRMATION) {
            _languageId = LanguageIdParceler.read(in);
        }

        if (_intrinsicState != IntrinsicStates.NORMAL) {
            _alphabetId = AlphabetIdParceler.read(in);
        }
    }

    public AlphabetsActivityState() {
        // All to 0 by default;
    }

    public boolean shouldShowDeleteConfirmationDialog() {
        return _intrinsicState == IntrinsicStates.LANGUAGE_DELETE_CONFIRMATION || _intrinsicState == IntrinsicStates.ALPHABET_DELETE_CONFIRMATION;
    }

    public boolean shouldShowLanguageOptionsDialog() {
        return _intrinsicState == IntrinsicStates.SHOWING_LANGUAGE_OPTIONS;
    }

    public boolean shouldShowAlphabetOptionsDialog() {
        return _intrinsicState == IntrinsicStates.SHOWING_ALPHABET_OPTIONS;
    }

    public boolean isRemovingLanguageConfirmationPresent() {
        return _intrinsicState == IntrinsicStates.LANGUAGE_DELETE_CONFIRMATION;
    }

    public boolean isRemovingAlphabetConfirmationPresent() {
        return _intrinsicState == IntrinsicStates.ALPHABET_DELETE_CONFIRMATION;
    }

    public void showLanguageOptions(LanguageId language) {
        if (_intrinsicState != IntrinsicStates.NORMAL) {
            throw new UnsupportedOperationException();
        }

        _intrinsicState = IntrinsicStates.SHOWING_LANGUAGE_OPTIONS;
        _languageId = language;
    }

    public void showAlphabetOptions(AlphabetId alphabet) {
        if (_intrinsicState != IntrinsicStates.NORMAL) {
            throw new UnsupportedOperationException();
        }

        _intrinsicState = IntrinsicStates.SHOWING_ALPHABET_OPTIONS;
        _alphabetId = alphabet;
    }

    public void showLanguageRemovalConfirmation() {
        if (_intrinsicState != IntrinsicStates.SHOWING_LANGUAGE_OPTIONS) {
            throw new UnsupportedOperationException();
        }

        _intrinsicState = IntrinsicStates.LANGUAGE_DELETE_CONFIRMATION;
    }

    public void showAlphabetRemovalConfirmation(AlphabetId alphabet) {
        if (_intrinsicState == IntrinsicStates.NORMAL) {
            _intrinsicState = IntrinsicStates.ALPHABET_DELETE_CONFIRMATION;
            _alphabetId = alphabet;
        }
        else if (_intrinsicState == IntrinsicStates.SHOWING_ALPHABET_OPTIONS && _alphabetId.equals(alphabet)) {
            _intrinsicState = IntrinsicStates.ALPHABET_DELETE_CONFIRMATION;
        }
        else {
            throw new UnsupportedOperationException();
        }
    }

    public void cancelLanguageOptions() {
        if (_intrinsicState != IntrinsicStates.SHOWING_LANGUAGE_OPTIONS) {
            throw new UnsupportedOperationException();
        }

        _intrinsicState = IntrinsicStates.NORMAL;
    }

    public void cancelDeleteConfirmation() {
        if (_intrinsicState != IntrinsicStates.LANGUAGE_DELETE_CONFIRMATION && _intrinsicState != IntrinsicStates.ALPHABET_DELETE_CONFIRMATION) {
            throw new UnsupportedOperationException();
        }

        _intrinsicState = IntrinsicStates.NORMAL;
    }

    public AlphabetId cancelAlphabetOptions() {
        if (_intrinsicState != IntrinsicStates.SHOWING_ALPHABET_OPTIONS) {
            throw new UnsupportedOperationException();
        }

        _intrinsicState = IntrinsicStates.NORMAL;
        return _alphabetId;
    }

    public void showAlphabetRemovalConfirmation() {
        if (_intrinsicState != IntrinsicStates.SHOWING_ALPHABET_OPTIONS) {
            throw new UnsupportedOperationException();
        }

        _intrinsicState = IntrinsicStates.ALPHABET_DELETE_CONFIRMATION;
    }

    public AlphabetId cancelAlphabetRemoval() {
        if (_intrinsicState != IntrinsicStates.ALPHABET_DELETE_CONFIRMATION) {
            throw new UnsupportedOperationException();
        }

        _intrinsicState = IntrinsicStates.NORMAL;
        return _alphabetId;
    }

    public LanguageId cancelLanguageRemoval() {
        if (_intrinsicState != IntrinsicStates.LANGUAGE_DELETE_CONFIRMATION) {
            throw new UnsupportedOperationException();
        }

        _intrinsicState = IntrinsicStates.NORMAL;
        return _languageId;
    }

    public LanguageId startAddAlphabetIntention() {
        if (_intrinsicState != IntrinsicStates.SHOWING_LANGUAGE_OPTIONS) {
            throw new UnsupportedOperationException();
        }

        _intrinsicState = IntrinsicStates.NORMAL;
        return _languageId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(_intrinsicState);
        if (_intrinsicState == IntrinsicStates.SHOWING_LANGUAGE_OPTIONS || _intrinsicState == IntrinsicStates.LANGUAGE_DELETE_CONFIRMATION) {
            LanguageIdParceler.write(dest, _languageId);
        }

        if (_intrinsicState != IntrinsicStates.NORMAL) {
            AlphabetIdParceler.write(dest, _alphabetId);
        }
    }

    public static final Creator<AlphabetsActivityState> CREATOR = new Creator<AlphabetsActivityState>() {
        @Override
        public AlphabetsActivityState createFromParcel(Parcel in) {
            return new AlphabetsActivityState(in);
        }

        @Override
        public AlphabetsActivityState[] newArray(int size) {
            return new AlphabetsActivityState[size];
        }
    };
}
