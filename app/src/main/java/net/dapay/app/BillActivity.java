package net.dapay.app;

import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import net.dapay.app.API.ExchangeAPI;

import static net.dapay.app.R.string.action_button_bill_received_owed;

public class BillActivity extends AppCompatActivity {

    String mWalletAddress;
    Bill mBill = null;

    DepositsListAdapter mLVAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bill);
        android.support.v7.app.ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);

        Bundle bundle = getIntent().getExtras();
        if(bundle != null)
            mWalletAddress = bundle.getString("wallet_address");

        if (ExchangeAPI.GetCurrentAPI() != null) {
            TextView label_fiat = ((TextView) findViewById(R.id.label_bill_fiat));
            label_fiat.setText(label_fiat.getText().toString().replace("$", ExchangeAPI.GetCurrentAPI().GetCurrencyActualSymbol()));
        }

        // get params
        mBill = DBHelper.getInstance(this).getBill(mWalletAddress);

        String str_action = "";
        if (mBill.getAction().equals(Bill.ACTION_PRE_SELL))
            str_action = "Presell";
        else if (mBill.getAction().equals(Bill.ACTION_TAKE))
            str_action = "Take";
        else if (mBill.getAction().equals(Bill.ACTION_MAKE))
            str_action = "Make";
        else if (mBill.getAction().equals(Bill.ACTION_HOLD))
            str_action = "Hold";
        ((TextView) findViewById(R.id.bill_label_action)).setText("(" + str_action + ")");

        TextView tv_amount_crypto     = ((TextView) findViewById(R.id.amount_crypto));
        TextView tv_amount_fiat       = ((TextView) findViewById(R.id.amount_fiat));
        TextView tv_amount_paid       = ((TextView) findViewById(R.id.amount_crypto_paid));
        TextView tv_amount_remaining  = ((TextView) findViewById(R.id.amount_crypto_remaining));
        TextView tv_label_amount_paid = ((TextView) findViewById(R.id.textview_recebido));
        Button button_qrcode = (Button)findViewById(R.id.button_show_qrcode);

        String str_amount_crypto    = String.format("%.8f", mBill.getCryptoAmount());
        String str_amount_fiat      = String.format("%.2f", mBill.getFiatAmount());
        String str_amount_paid      = String.format("%.8f", mBill.getAmountDepositedAndToBeDeposited());

        tv_amount_crypto.setText   (str_amount_crypto   );
        tv_amount_fiat.setText     (str_amount_fiat     );
        tv_amount_paid.setText     (str_amount_paid     );

        if (mBill.getAmountDeposited() != mBill.getAmountDepositedAndToBeDeposited())
            tv_label_amount_paid.setText(getApplicationContext().getString(R.string.label_bill_receiving)+"...");
        else
            tv_label_amount_paid.setText(getApplicationContext().getString(R.string.label_bill_received));

        tv_amount_remaining.setText(String.format("%.8f", mBill.getCryptoAmount() - mBill.getAmountDepositedAndToBeDeposited()));
        if (mBill.getCryptoAmount() - mBill.getAmountDepositedAndToBeDeposited() > 0.00000010) {
            tv_amount_remaining.setTextColor(Color.RED);
            button_qrcode.setText(getApplicationContext().getString(action_button_bill_received_owed));
        } else {
            tv_amount_remaining.setTextColor(tv_amount_paid.getCurrentTextColor());
            if (mBill.getCryptoAmount() == mBill.getAmountDepositedAndToBeDeposited())
                tv_amount_remaining.setText("0");
            button_qrcode.setText(getApplicationContext().getString(R.string.action_button_bill_show_qrcode));
        }

        button_qrcode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {Intent intent = new Intent(BillActivity.this, PayActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("wallet_address", mWalletAddress);
                bundle.putInt("from", PayActivity.FROM_BILL);
                intent.putExtras(bundle);
                startActivity(intent);
            }
        });

        mLVAdapter = new DepositsListAdapter(getApplicationContext(), mBill);
        ListView lv = ((ListView) findViewById(R.id.deposit_list));
        lv.setAdapter(mLVAdapter);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // app icon in action bar clicked; go home
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
