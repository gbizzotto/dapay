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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

public class DepositsListAdapter extends ArrayAdapter<Bill> implements Observer {
    private final Context context;
    private Set<String> displayedBills = new HashSet<String>();
    private ArrayList<Bill.Deposit> mDeposits;

    public DepositsListAdapter(Context context, Bill bill) {
        super(context, -1, DBHelper.getInstance(context).getAllBills());
        this.context = context;
        this.update(bill, null);
    }

    @Override
    public int getCount() {
        return mDeposits.size();
    }

    public void update(Observable obs, Object obj) {
        if ( ! (obs instanceof Bill))
            return;
        Bill bill = (Bill) obs;
        mDeposits = new ArrayList<Bill.Deposit>(bill.getDeposits().values());
        Comparator<Bill.Deposit> comparator = new Comparator<Bill.Deposit>() {
            @Override
            public int compare(Bill.Deposit left, Bill.Deposit right) {
                return (int)(right.timestamp - left.timestamp);
            }
        };
        Collections.sort(mDeposits, comparator); // use the comparator as much as u want
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.layout_row_deposit, parent, false);

        if (position >= mDeposits.size())
            return rowView;
        Bill.Deposit deposit = mDeposits.get(position);
        Date date = new Date(deposit.timestamp);
        SimpleDateFormat datetime_format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        ImageView icon = (ImageView) rowView.findViewById(R.id.deposit_icon);
        TextView tv_amount = (TextView) rowView.findViewById(R.id.deposit_amount);
        TextView tv_timestamp = (TextView) rowView.findViewById(R.id.deposit_timestamp);
        TextView tv_status = (TextView) rowView.findViewById(R.id.deposit_status);

        tv_amount.setText(String.format("BTC %.8f", deposit.crypto_amount));
        tv_timestamp.setText(datetime_format.format(date));

        switch (deposit.status) {
            case Bill.STATUS_UNCONFIRMED:
                tv_status.setText(context.getString(R.string.label_bill_receiving)+"...");
                tv_status.setTextColor(Color.DKGRAY);
                break;
            case Bill.STATUS_CONFIRMED:
                tv_status.setText(context.getString(R.string.label_bill_received));
                tv_status.setTextColor(tv_timestamp.getCurrentTextColor());
                break;
            case Bill.STATUS_CANCELLED:
                tv_status.setText(context.getString(R.string.cancelled));
                tv_status.setTextColor(Color.RED);
                break;
            case Bill.STATUS_DOUBLE_SPENT:
                tv_status.setText(context.getString(R.string.fraud));
                tv_status.setTextColor(Color.RED);
                break;
        }
        //rowView.setTag(new Integer(deposit.id));
        return rowView;
    }
}