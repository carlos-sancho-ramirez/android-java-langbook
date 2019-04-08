package sword.langbook3.android;

import android.os.Parcel;
import android.os.Parcelable;

public final class AlphabetsActivityState implements Parcelable {

    /**
     * State number for the state machine.
     * 0: NORMAL
     * 1: SHOWING_LANGUAGE_OPTIONS
     * 2: SHOWING_ALPHABET_OPTIONS
     * 3: LANGUAGE_DELETE_CONFIRMATION
     * 4: ALPHABET_DELETE_CONFIRMATION
     * 5: PICKING_NEW_ALPHABET_ACCEPTATION
     * 6: PICKING_SOURCE_ALPHABET
     */
    interface IntrinsicStates {
        int NORMAL = 0;
        int SHOWING_LANGUAGE_OPTIONS = 1;
        int SHOWING_ALPHABET_OPTIONS = 2;
        int LANGUAGE_DELETE_CONFIRMATION = 3;
        int ALPHABET_DELETE_CONFIRMATION = 4;
        int PICKING_NEW_ALPHABET_ACCEPTATION = 5;
        int PICKING_SOURCE_ALPHABET = 6;
    }

    private int _intrinsicState;
    private int _id;
    private int _id2;

    private AlphabetsActivityState(Parcel in) {
        _intrinsicState = in.readInt();
        if (_intrinsicState != IntrinsicStates.NORMAL) {
            _id = in.readInt();
            if (_intrinsicState == IntrinsicStates.PICKING_SOURCE_ALPHABET) {
                _id2 = in.readInt();
            }
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

    public boolean shouldShowSourceAlphabetPickerDialog() {
        return _intrinsicState == IntrinsicStates.PICKING_SOURCE_ALPHABET;
    }

    public boolean isRemovingLanguageConfirmationPresent() {
        return _intrinsicState == IntrinsicStates.LANGUAGE_DELETE_CONFIRMATION;
    }

    public boolean isRemovingAlphabetConfirmationPresent() {
        return _intrinsicState == IntrinsicStates.ALPHABET_DELETE_CONFIRMATION;
    }

    public void showLanguageOptions(int language) {
        if (_intrinsicState != IntrinsicStates.NORMAL) {
            throw new UnsupportedOperationException();
        }

        _intrinsicState = IntrinsicStates.SHOWING_LANGUAGE_OPTIONS;
        _id = language;
    }

    public void showAlphabetOptions(int alphabet) {
        if (_intrinsicState != IntrinsicStates.NORMAL) {
            throw new UnsupportedOperationException();
        }

        _intrinsicState = IntrinsicStates.SHOWING_ALPHABET_OPTIONS;
        _id = alphabet;
    }

    public void showLanguageRemovalConfirmation() {
        if (_intrinsicState != IntrinsicStates.SHOWING_LANGUAGE_OPTIONS) {
            throw new UnsupportedOperationException();
        }

        _intrinsicState = IntrinsicStates.LANGUAGE_DELETE_CONFIRMATION;
    }

    public void showAlphabetRemovalConfirmation(int alphabet) {
        if (_intrinsicState == IntrinsicStates.NORMAL) {
            _intrinsicState = IntrinsicStates.ALPHABET_DELETE_CONFIRMATION;
            _id = alphabet;
        }
        else if (_intrinsicState == IntrinsicStates.SHOWING_ALPHABET_OPTIONS && _id == alphabet) {
            _intrinsicState = IntrinsicStates.ALPHABET_DELETE_CONFIRMATION;
        }
        else {
            throw new UnsupportedOperationException();
        }
    }

    public int cancelLanguageOptions() {
        if (_intrinsicState != IntrinsicStates.SHOWING_LANGUAGE_OPTIONS) {
            throw new UnsupportedOperationException();
        }

        _intrinsicState = IntrinsicStates.NORMAL;
        return _id;
    }

    public void cancelDeleteConfirmation() {
        if (_intrinsicState != IntrinsicStates.LANGUAGE_DELETE_CONFIRMATION && _intrinsicState != IntrinsicStates.ALPHABET_DELETE_CONFIRMATION) {
            throw new UnsupportedOperationException();
        }

        _intrinsicState = IntrinsicStates.NORMAL;
    }

    public int cancelAlphabetOptions() {
        if (_intrinsicState != IntrinsicStates.SHOWING_ALPHABET_OPTIONS) {
            throw new UnsupportedOperationException();
        }

        _intrinsicState = IntrinsicStates.NORMAL;
        return _id;
    }

    public void showAlphabetRemovalConfirmation() {
        if (_intrinsicState != IntrinsicStates.SHOWING_ALPHABET_OPTIONS) {
            throw new UnsupportedOperationException();
        }

        _intrinsicState = IntrinsicStates.ALPHABET_DELETE_CONFIRMATION;
    }

    public int cancelAlphabetRemoval() {
        if (_intrinsicState != IntrinsicStates.ALPHABET_DELETE_CONFIRMATION) {
            throw new UnsupportedOperationException();
        }

        _intrinsicState = IntrinsicStates.NORMAL;
        return _id;
    }

    public int cancelLanguageRemoval() {
        if (_intrinsicState != IntrinsicStates.LANGUAGE_DELETE_CONFIRMATION) {
            throw new UnsupportedOperationException();
        }

        _intrinsicState = IntrinsicStates.NORMAL;
        return _id;
    }

    public void pickAcceptationForAlphabet() {
        if (_intrinsicState != IntrinsicStates.SHOWING_LANGUAGE_OPTIONS) {
            throw new UnsupportedOperationException();
        }

        _intrinsicState = IntrinsicStates.PICKING_NEW_ALPHABET_ACCEPTATION;
    }

    public int getNewAlphabetLanguage() {
        if (_intrinsicState != IntrinsicStates.PICKING_NEW_ALPHABET_ACCEPTATION && _intrinsicState != IntrinsicStates.PICKING_SOURCE_ALPHABET) {
            throw new UnsupportedOperationException();
        }

        return _id;
    }

    public void cancelAcceptationForAlphabetPicking() {
        if (_intrinsicState != IntrinsicStates.PICKING_NEW_ALPHABET_ACCEPTATION) {
            throw new UnsupportedOperationException();
        }

        _intrinsicState = IntrinsicStates.NORMAL;
    }

    public void showSourceAlphabetPickingState(int alphabet) {
        if (_intrinsicState != IntrinsicStates.PICKING_NEW_ALPHABET_ACCEPTATION) {
            throw new UnsupportedOperationException();
        }

        _intrinsicState = IntrinsicStates.PICKING_SOURCE_ALPHABET;
        _id2 = alphabet;
    }

    public int cancelSourceAlphabetPicking() {
        if (_intrinsicState != IntrinsicStates.PICKING_SOURCE_ALPHABET) {
            throw new UnsupportedOperationException();
        }

        _intrinsicState = IntrinsicStates.NORMAL;
        return _id2;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(_intrinsicState);
        if (_intrinsicState != IntrinsicStates.NORMAL) {
            dest.writeInt(_id);
            if (_intrinsicState == IntrinsicStates.PICKING_SOURCE_ALPHABET) {
                dest.writeInt(_id2);
            }
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
