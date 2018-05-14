package sword.langbook3.android;

import android.os.Parcel;
import android.os.Parcelable;

public final class AcceptationDetailsActivityState implements Parcelable {

    public interface IntrinsicStates {
        int NORMAL = 0;
        int DELETE_ACCEPTATION = 1;
        int DELETING_FROM_BUNCH = 2;
        int LINKING_CONCEPT = 3;
    }

    private int _intrinsicState = IntrinsicStates.NORMAL;

    // Only relevant for IntrinsicState LINKING_CONCEPT
    private int _linkedAcceptation;
    private int _linkDialogCheckedOption;

    // Only relevant for IntrinsicState DELETING_FROM_BUNCH
    private DisplayableItem _deleteBunchTarget;

    public AcceptationDetailsActivityState() {
    }

    private AcceptationDetailsActivityState(Parcel in) {
        _intrinsicState = in.readInt();
        switch (_intrinsicState) {
            case IntrinsicStates.LINKING_CONCEPT:
                _linkedAcceptation = in.readInt();
                _linkDialogCheckedOption = in.readInt();
                break;

            case IntrinsicStates.DELETING_FROM_BUNCH:
                _deleteBunchTarget = DisplayableItem.CREATOR.createFromParcel(in);
                break;
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(_intrinsicState);
        switch (_intrinsicState) {
            case IntrinsicStates.LINKING_CONCEPT:
                dest.writeInt(_linkedAcceptation);
                dest.writeInt(_linkDialogCheckedOption);
                break;

            case IntrinsicStates.DELETING_FROM_BUNCH:
                _deleteBunchTarget.writeToParcel(dest, flags);
                break;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    private void assertState(int intrinsicState) {
        if (_intrinsicState != intrinsicState) {
            throw new IllegalStateException();
        }
    }

    public int getIntrinsicState() {
        return _intrinsicState;
    }

    void setDeletingAcceptation() {
        assertState(IntrinsicStates.NORMAL);
        _intrinsicState = IntrinsicStates.DELETE_ACCEPTATION;
    }

    void clearDeletingAcceptation() {
        assertState(IntrinsicStates.DELETE_ACCEPTATION);
        _intrinsicState = IntrinsicStates.NORMAL;
    }

    void setLinkedAcceptation(int acceptation) {
        if (acceptation == 0) {
            throw new IllegalArgumentException();
        }

        assertState(IntrinsicStates.NORMAL);
        _linkedAcceptation = acceptation;
        _linkDialogCheckedOption = 0;
        _intrinsicState = IntrinsicStates.NORMAL;
    }

    void setDialogCheckedOption(int option) {
        assertState(IntrinsicStates.LINKING_CONCEPT);
        _linkDialogCheckedOption = option;
    }

    int getLinkedAcceptation() {
        assertState(IntrinsicStates.LINKING_CONCEPT);
        return _linkedAcceptation;
    }

    int getDialogCheckedOption() {
        assertState(IntrinsicStates.LINKING_CONCEPT);
        return _linkDialogCheckedOption;
    }

    void clearLinkedAcceptation() {
        assertState(IntrinsicStates.LINKING_CONCEPT);
        _intrinsicState = IntrinsicStates.NORMAL;
    }

    void setDeleteBunchTarget(DisplayableItem item) {
        if (item == null) {
            throw new IllegalArgumentException();
        }

        assertState(IntrinsicStates.NORMAL);
        _deleteBunchTarget = item;
        _intrinsicState = IntrinsicStates.DELETING_FROM_BUNCH;
    }

    DisplayableItem getDeleteBunchTarget() {
        assertState(IntrinsicStates.DELETING_FROM_BUNCH);
        return _deleteBunchTarget;
    }

    void clearDeleteBunchTarget() {
        assertState(IntrinsicStates.DELETING_FROM_BUNCH);
        _intrinsicState = IntrinsicStates.NORMAL;
    }

    public static final Parcelable.Creator<AcceptationDetailsActivityState> CREATOR = new Parcelable.Creator<AcceptationDetailsActivityState>() {
        @Override
        public AcceptationDetailsActivityState createFromParcel(Parcel in) {
            return new AcceptationDetailsActivityState(in);
        }

        @Override
        public AcceptationDetailsActivityState[] newArray(int size) {
            return new AcceptationDetailsActivityState[size];
        }
    };
}
