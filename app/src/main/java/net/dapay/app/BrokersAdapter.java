package net.dapay.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by gabriel on 12/29/16.
 */

public class BrokersAdapter extends ArrayAdapter<Broker> {

    private final Context context;

    public BrokersAdapter(Context context) {
        super(context, -1, Broker.GetBrokers());
        this.context = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView;
        if (null == convertView) {
            rowView = ((LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.layout_row_broker, null);
        } else {
            rowView = convertView;
        }

        ImageView logo_view = (ImageView) rowView.findViewById(R.id.broker_row_picture);
        TextView name_view = (TextView) rowView.findViewById(R.id.broker_row_name);
        TextView country_view = (TextView) rowView.findViewById(R.id.broker_row_country);

        Broker broker = this.getItem(position);

        logo_view.setImageResource(broker.getImageResourceID());
        name_view.setText         (broker.getName());
        country_view.setText      (broker.getCoutry());

        rowView.setTag(broker);
        return rowView;
    }
}
