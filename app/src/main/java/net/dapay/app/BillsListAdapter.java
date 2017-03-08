package net.dapay.app;

/**
 * Created by gabriel on 12/8/16.
 */

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
        import android.view.View;
        import android.view.ViewGroup;
import android.widget.ArrayAdapter;
        import android.widget.ImageView;
        import android.widget.TextView;

import net.dapay.app.API.ExchangeAPI;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static android.R.drawable.ic_notification_overlay;
import static android.R.drawable.presence_invisible;
import static android.R.drawable.presence_online;
import static net.dapay.app.Bill.STATUS_CONFIRMED;
import static net.dapay.app.Bill.STATUS_UNCONFIRMED;

public class BillsListAdapter extends ArrayAdapter<Bill> {
    private final Context context;
    private Set<String> displayedBills = new HashSet<String>();

    public BillsListAdapter(Context context) {
        super(context, -1, DBHelper.getInstance(context).getAllBills());
        this.context = context;
    }

    @Override
    public int getCount() {
        return DBHelper.getInstance(context).getAllBills().size();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.layout_row_bill, parent, false);

        ArrayList<Bill> values = DBHelper.getInstance(context).getAllBills();
        synchronized (values) {
            if (position >= values.size())
                return rowView;
            Bill bill = values.get(position);
            Date date = new Date(bill.getTimestamp());
            SimpleDateFormat datetime_format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            ImageView icon = (ImageView) rowView.findViewById(R.id.bill_icon);
            TextView fiat_view = (TextView) rowView.findViewById(R.id.fiat_view);
            TextView crypto_view = (TextView) rowView.findViewById(R.id.crypto_view);
            TextView timestamp_view = (TextView) rowView.findViewById(R.id.timestamp_view);
            TextView label_view = (TextView) rowView.findViewById(R.id.bill_label);
            TextView paid_view = (TextView) rowView.findViewById(R.id.paid_amount);

            if (ExchangeAPI.GetCurrentAPI() != null) {
                fiat_view.setText(String.format(ExchangeAPI.GetCurrentAPI().GetCurrencyActualSymbol() + "  %.2f", bill.getFiatAmount()));
            } else {
                fiat_view.setText(String.format("$  %.2f", bill.getFiatAmount()));
            }
            crypto_view.setText(String.format(context.getString(R.string.cry)+" %.8f", bill.getCryptoAmount()));
            timestamp_view.setText(datetime_format.format(date));
            label_view.setText(bill.getLabel());

            if (bill.getAmountDepositedAndToBeDeposited() == bill.getAmountDeposited()) {
                paid_view.setText(String.format(context.getString(R.string.label_bill_received)+" %.8f", bill.getAmountDeposited()));
                if (bill.getAmountDeposited() > 0 && bill.getAmountDeposited() < bill.getCryptoAmount()-0.00000010)
                    paid_view.setTextColor(Color.RED);
            } else {
                paid_view.setText(String.format(context.getString(R.string.label_bill_receiving)+" %.8f", bill.getAmountDepositedAndToBeDeposited()));
                if (bill.getAmountDepositedAndToBeDeposited() > 0 && bill.getAmountDepositedAndToBeDeposited() < bill.getCryptoAmount()-0.00000010)
                    paid_view.setTextColor(Color.RED);
            }

            switch (bill.getStatus()) {
                case STATUS_UNCONFIRMED:
                    icon.setImageResource(presence_invisible);
                    break;
                case STATUS_CONFIRMED:
                    icon.setImageResource(presence_online);
                    break;
                default:
                    icon.setImageResource(ic_notification_overlay);
                    break;
            }

            if ( ! displayedBills.contains(bill.getWalletID())) {
                // TODO: Animate
                displayedBills.add(bill.getWalletID());
            }
            rowView.setTag(bill);
        }
        return rowView;
    }
}