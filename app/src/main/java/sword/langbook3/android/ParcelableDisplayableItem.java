package sword.langbook3.android;

import android.os.Parcel;
import android.os.Parcelable;

import sword.langbook3.android.models.DisplayableItem;

public final class ParcelableDisplayableItem implements Parcelable {

    public final DisplayableItem item;

    public ParcelableDisplayableItem(DisplayableItem item) {
        if (item == null) {
            throw new IllegalArgumentException();
        }

        this.item = item;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(item.id);
        dest.writeString(item.text);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ParcelableDisplayableItem> CREATOR = new Creator<ParcelableDisplayableItem>() {
        @Override
        public ParcelableDisplayableItem createFromParcel(Parcel in) {
            final int id = in.readInt();
            final String text = in.readString();

            return new ParcelableDisplayableItem(new DisplayableItem(id, text));
        }

        @Override
        public ParcelableDisplayableItem[] newArray(int size) {
            return new ParcelableDisplayableItem[size];
        }
    };
}
