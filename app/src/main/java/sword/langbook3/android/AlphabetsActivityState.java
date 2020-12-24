package sword.langbook3.android;

import android.os.Parcel;
import android.os.Parcelable;

import sword.langbook3.android.db.AlphabetId;

public final class AlphabetsActivityState implements Parcelable {

    private static final int DEFINE_CONVERSION_FLAG = 0x80000000;

    public AlphabetId startDefiningConversion() {
        if (_intrinsicState != IntrinsicStates.PICKING_SOURCE_ALPHABET) {
            throw new UnsupportedOperationException();
        }

        _intrinsicState = IntrinsicStates.DEFINING_CONVERSION;
        _sourceAlphabet &= ~DEFINE_CONVERSION_FLAG;

        return _newAlphabetConcept;
    }

    public void cancelDefiningConversion() {
        if (_intrinsicState != IntrinsicStates.DEFINING_CONVERSION) {
            throw new UnsupportedOperationException();
        }

        _sourceAlphabet |= DEFINE_CONVERSION_FLAG;
        _intrinsicState = IntrinsicStates.PICKING_SOURCE_ALPHABET;
    }

    public void completeDefiningConversion() {
        if (_intrinsicState != IntrinsicStates.DEFINING_CONVERSION) {
            throw new UnsupportedOperationException();
        }

        _intrinsicState = IntrinsicStates.NORMAL;
    }

    interface IntrinsicStates {
        int NORMAL = 0;
        int SHOWING_LANGUAGE_OPTIONS = 1;
        int SHOWING_ALPHABET_OPTIONS = 2;
        int LANGUAGE_DELETE_CONFIRMATION = 3;
        int ALPHABET_DELETE_CONFIRMATION = 4;
        int PICKING_NEW_ALPHABET_ACCEPTATION = 5;
        int PICKING_SOURCE_ALPHABET = 6;
        int DEFINING_CONVERSION = 7;
    }

    private int _intrinsicState;
    private int _id;
    private AlphabetId _newAlphabetConcept;
    private int _sourceAlphabet;

    private AlphabetsActivityState(Parcel in) {
        _intrinsicState = in.readInt();
        if (_intrinsicState != IntrinsicStates.NORMAL) {
            _id = in.readInt();
            if (_intrinsicState == IntrinsicStates.PICKING_SOURCE_ALPHABET) {
                _newAlphabetConcept = new AlphabetId(in.readInt());
                _sourceAlphabet = in.readInt();
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

    public void showAlphabetOptions(AlphabetId alphabet) {
        if (_intrinsicState != IntrinsicStates.NORMAL) {
            throw new UnsupportedOperationException();
        }

        _intrinsicState = IntrinsicStates.SHOWING_ALPHABET_OPTIONS;
        _id = alphabet.key;
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
            _id = alphabet.key;
        }
        else if (_intrinsicState == IntrinsicStates.SHOWING_ALPHABET_OPTIONS && _id == alphabet.key) {
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

    public AlphabetId cancelAlphabetOptions() {
        if (_intrinsicState != IntrinsicStates.SHOWING_ALPHABET_OPTIONS) {
            throw new UnsupportedOperationException();
        }

        _intrinsicState = IntrinsicStates.NORMAL;
        return new AlphabetId(_id);
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
        return new AlphabetId(_id);
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

    public AlphabetId getSelectedSourceAlphabet() {
        if (_intrinsicState != IntrinsicStates.PICKING_SOURCE_ALPHABET) {
            throw new UnsupportedOperationException();
        }

        return new AlphabetId(_sourceAlphabet & ~DEFINE_CONVERSION_FLAG);
    }

    public void setSelectedSourceAlphabet(AlphabetId alphabet) {
        if (_intrinsicState != IntrinsicStates.PICKING_SOURCE_ALPHABET) {
            throw new UnsupportedOperationException();
        }

        _sourceAlphabet = _sourceAlphabet & DEFINE_CONVERSION_FLAG | alphabet.key & ~DEFINE_CONVERSION_FLAG;
    }

    public boolean isDefineConversionChecked() {
        if (_intrinsicState != IntrinsicStates.PICKING_SOURCE_ALPHABET) {
            throw new UnsupportedOperationException();
        }

        return (_sourceAlphabet & DEFINE_CONVERSION_FLAG) != 0;
    }

    public void setDefinedConversionChecked(boolean checked) {
        if (_intrinsicState != IntrinsicStates.PICKING_SOURCE_ALPHABET) {
            throw new UnsupportedOperationException();
        }

        if (checked) {
            _sourceAlphabet |= DEFINE_CONVERSION_FLAG;
        }
        else {
            _sourceAlphabet &= ~DEFINE_CONVERSION_FLAG;
        }
    }

    public void cancelAcceptationForAlphabetPicking() {
        if (_intrinsicState != IntrinsicStates.PICKING_NEW_ALPHABET_ACCEPTATION) {
            throw new UnsupportedOperationException();
        }

        _intrinsicState = IntrinsicStates.NORMAL;
    }

    public void showSourceAlphabetPickingState(AlphabetId alphabet) {
        if (_intrinsicState != IntrinsicStates.PICKING_NEW_ALPHABET_ACCEPTATION) {
            throw new UnsupportedOperationException();
        }

        _intrinsicState = IntrinsicStates.PICKING_SOURCE_ALPHABET;
        _newAlphabetConcept = alphabet;
    }

    public AlphabetId cancelSourceAlphabetPicking() {
        if (_intrinsicState != IntrinsicStates.PICKING_SOURCE_ALPHABET) {
            throw new UnsupportedOperationException();
        }

        _intrinsicState = IntrinsicStates.NORMAL;
        return _newAlphabetConcept;
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
                dest.writeInt(_newAlphabetConcept.key);
                dest.writeInt(_sourceAlphabet);
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
