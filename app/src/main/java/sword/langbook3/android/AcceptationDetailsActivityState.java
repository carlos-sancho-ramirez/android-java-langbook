package sword.langbook3.android;

import android.os.Parcel;
import android.os.Parcelable;

import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AcceptationIdParceler;
import sword.langbook3.android.db.BunchId;
import sword.langbook3.android.db.BunchIdParceler;
import sword.langbook3.android.models.DisplayableItem;

public final class AcceptationDetailsActivityState implements Parcelable {

    public interface IntrinsicStates {
        int NORMAL = 0;
        int DELETE_ACCEPTATION = 1;
        int DELETE_DEFINITION = 2;
        int DELETING_ACCEPTATION_FROM_BUNCH = 3;
        int DELETING_FROM_BUNCH = 4;
    }

    private int _intrinsicState = IntrinsicStates.NORMAL;

    // Relevant for IntrinsicStates DELETING_ACCEPTATION_FROM_BUNCH
    private DisplayableItem<AcceptationId> _deleteTargetAcceptation;

    // Relevant for IntrinsicStates DELETING_FROM_BUNCH
    private DisplayableItem<BunchId> _deleteTargetBunch;

    public AcceptationDetailsActivityState() {
    }

    private AcceptationDetailsActivityState(Parcel in) {
        _intrinsicState = in.readInt();
        switch (_intrinsicState) {
            case IntrinsicStates.DELETING_FROM_BUNCH:
                final BunchId bunchId = BunchIdParceler.read(in);
                final String bunchText = in.readString();
                _deleteTargetBunch = new DisplayableItem<>(bunchId, bunchText);
                break;

            case IntrinsicStates.DELETING_ACCEPTATION_FROM_BUNCH:
                final AcceptationId acceptationId = AcceptationIdParceler.read(in);
                final String text = in.readString();
                _deleteTargetAcceptation = new DisplayableItem<>(acceptationId, text);
                break;
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(_intrinsicState);
        switch (_intrinsicState) {
            case IntrinsicStates.DELETING_FROM_BUNCH:
                BunchIdParceler.write(dest, _deleteTargetBunch.id);
                dest.writeString(_deleteTargetBunch.text);
                break;

            case IntrinsicStates.DELETING_ACCEPTATION_FROM_BUNCH:
                AcceptationIdParceler.write(dest, _deleteTargetAcceptation.id);
                dest.writeString(_deleteTargetAcceptation.text);
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

    void setDeletingSupertype() {
        assertState(IntrinsicStates.NORMAL);
        _intrinsicState = IntrinsicStates.DELETE_DEFINITION;
    }

    void clearDeletingDefinition() {
        assertState(IntrinsicStates.DELETE_DEFINITION);
        _intrinsicState = IntrinsicStates.NORMAL;
    }

    void setDeleteBunchTarget(DisplayableItem<BunchId> item) {
        if (item == null) {
            throw new IllegalArgumentException();
        }

        assertState(IntrinsicStates.NORMAL);
        _deleteTargetBunch = item;
        _intrinsicState = IntrinsicStates.DELETING_FROM_BUNCH;
    }

    void setDeleteAcceptationFromBunch(DisplayableItem<AcceptationId> item) {
        if (item == null) {
            throw new IllegalArgumentException();
        }

        assertState(IntrinsicStates.NORMAL);
        _deleteTargetAcceptation = item;
        _intrinsicState = IntrinsicStates.DELETING_ACCEPTATION_FROM_BUNCH;
    }

    DisplayableItem<AcceptationId> getDeleteTargetAcceptation() {
        if (_intrinsicState != IntrinsicStates.DELETING_ACCEPTATION_FROM_BUNCH) {
            throw new IllegalStateException();
        }

        return _deleteTargetAcceptation;
    }

    DisplayableItem<BunchId> getDeleteTargetBunch() {
        if (_intrinsicState != IntrinsicStates.DELETING_FROM_BUNCH) {
            throw new IllegalStateException();
        }

        return _deleteTargetBunch;
    }

    void clearDeleteTarget() {
        if (_intrinsicState == IntrinsicStates.DELETING_ACCEPTATION_FROM_BUNCH) {
            _deleteTargetAcceptation = null;
        }
        else if (_intrinsicState == IntrinsicStates.DELETING_FROM_BUNCH) {
            _deleteTargetBunch = null;
        }
        else {
            throw new IllegalStateException();
        }

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
