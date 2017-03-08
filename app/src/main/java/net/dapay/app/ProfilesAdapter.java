package net.dapay.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import static net.dapay.app.R.mipmap.ic_blinktrade;

/**
 * Created by gabriel on 1/9/17.
 */

public class ProfilesAdapter extends ArrayAdapter<Profile> {

    private final Context context;

    public ProfilesAdapter(Context context) {
        super(context, -1, DBHelper.getInstance(context).getAllProfiles());
        this.context = context;
    }

    @Override
    public int getCount() {
        return DBHelper.getInstance(context).getAllProfiles().size();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View rowView = inflater.inflate(R.layout.layout_row_profile, parent, false);

        TextView name_view = (TextView) rowView.findViewById(R.id.profile_row_name);
        ImageView logo_view = (ImageView) rowView.findViewById(R.id.profile_row_broker_picture);

        Profile profile = this.getItem(position);
        Broker broker = Broker.GetBrokerByID(profile.broker_id);

        name_view.setText(profile.label);
        if (broker != null)
            logo_view.setImageResource(broker.getImageResourceID());
        else
            logo_view.setImageResource(ic_blinktrade);

        rowView.setTag(profile);
        return rowView;
    }
}
