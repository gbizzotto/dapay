package net.dapay.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Display;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Space;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import net.dapay.app.API.ExchangeAPI;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Observable;
import java.util.Observer;

public class PayActivity extends AppCompatActivity implements Observer {

    String mWalletAddress;
    Bill mBill = null;
    boolean mZoomed = false;
    static Bitmap s_bmp_qrcode_big;
    static Bitmap s_bmp_qrcode_normal;
    int mFrom = 0;

    public static final int FROM_CONFIG_PAY = 0;
    public static final int FROM_BILL       = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pay);
        android.support.v7.app.ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);

        Bundle bundle = getIntent().getExtras();
        if(bundle != null) {
            mWalletAddress = bundle.getString("wallet_address");
            mFrom = bundle.getInt("from");
        }

        RelativeLayout layout_activity = (RelativeLayout) findViewById(R.id.activity_pay);
        TextView title_tv = ((TextView) findViewById(R.id.pay_label_title));

        // get params
        mBill = DBHelper.getInstance(this).getBill(mWalletAddress);

        double exchange_rate = mBill.getFiatAmount() / mBill.getCryptoAmount();
        double amount_to_receive = mBill.getCryptoAmount() - mBill.getAmountDepositedAndToBeDeposited();
        if (amount_to_receive <= 0) {
            amount_to_receive = mBill.getCryptoAmount();
//            layout_activity.getRootView().setBackgroundColor(0xFFF);
            getWindow().getDecorView().setBackgroundColor(getResources().getColor(android.R.color.white));
            layout_activity.setBackgroundColor(0xAAA);
            title_tv.setText(getString(R.string.title_bill));
        } else {
            if (mFrom == FROM_CONFIG_PAY)
                title_tv.setText(getString(R.string.action_button_bill_new_account));
            else
                title_tv.setText(getString(R.string.action_button_bill_received_owed));
        }

        String str_amount_fiat   = String.format("%.2f", amount_to_receive * exchange_rate);
        String str_exchange_rate = String.format("%.2f", exchange_rate);
        String str_amount_btc    = String.format("%.8f", amount_to_receive);

        ((ImageView) findViewById(R.id.wallet_qrcode)).setImageBitmap(s_bmp_qrcode_normal);

        TextView wallet_address_tv = ((TextView) findViewById(R.id.wallet_address));
        wallet_address_tv.setText(
                mWalletAddress.substring(0, mWalletAddress.length()/2)
                +"\n"
                +mWalletAddress.substring(mWalletAddress.length()/2));
        TextView amount_btc_tv = ((TextView) findViewById(R.id.amount_btc));
        amount_btc_tv.setText(getString(R.string.cry)+" " + str_amount_btc);
        TextView amount_fiat_tv = ((TextView) findViewById(R.id.amount_fiat));

        if (ExchangeAPI.GetCurrentAPI() != null) {
            amount_fiat_tv.setText(ExchangeAPI.GetCurrentAPI().GetCurrencyActualSymbol() + " " + str_amount_fiat + " @ " + ExchangeAPI.GetCurrentAPI().GetCurrencyActualSymbol() + str_exchange_rate + "/" + getString(R.string.cry));
        } else {
            amount_fiat_tv.setText("$ " + str_amount_fiat + " @ $" + str_exchange_rate + "/" + getString(R.string.cry));
        }
        TextView label_tv = ((TextView) findViewById(R.id.label));
        label_tv.setText(mBill.getLabel());

        Button button_cancel = (Button)findViewById(R.id.pay_button_cancel);
        button_cancel.setVisibility(mBill.getAmountDepositedAndToBeDeposited()==0?View.VISIBLE:View.GONE);

        DBHelper.getInstance(getApplicationContext()).GetDepositUpdates(this, true);


        ((Button)findViewById(R.id.pay_button_cancel)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DBHelper.getInstance(PayActivity.this).disableBill(mBill);
                // Don't be a listener anymore, in order to get GCed
                DBHelper.getInstance(getApplicationContext()).GetDepositUpdates(PayActivity.this, false);
                startActivity(new Intent(PayActivity.this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            }
        });


        ImageView img_qrcode = (ImageView) findViewById(R.id.wallet_qrcode);
        img_qrcode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PayActivity.this.mZoomed = ! PayActivity.this.mZoomed;

                TextView amount_btc_tv     = (TextView) findViewById(R.id.amount_btc);
                TextView amount_fiat_tv    = (TextView) findViewById(R.id.amount_fiat);
                TextView label_tv          = (TextView) findViewById(R.id.label);
                TextView wallet_address_tv = (TextView) findViewById(R.id.wallet_address);
                TextView title_tv          = (TextView) findViewById(R.id.pay_label_title);
                Button button_cancel       = (Button)   findViewById(R.id.pay_button_cancel);
                Space space1 = ((Space) findViewById(R.id.space1));
                Space space2 = ((Space) findViewById(R.id.space2));
                Space space3 = ((Space) findViewById(R.id.space3));
                title_tv.setVisibility(mZoomed?View.GONE:View.VISIBLE);
                amount_btc_tv.setVisibility(mZoomed?View.GONE:View.VISIBLE);
                amount_fiat_tv.setVisibility(mZoomed?View.GONE:View.VISIBLE);
                label_tv.setVisibility(mZoomed?View.GONE:View.VISIBLE);
                wallet_address_tv.setVisibility(mZoomed?View.VISIBLE:View.GONE);
                space1.setVisibility(mZoomed?View.GONE:View.VISIBLE);
                space2.setVisibility(mZoomed?View.GONE:View.VISIBLE);
                space3.setVisibility(mZoomed?View.GONE:View.VISIBLE);
                button_cancel.setVisibility((mZoomed || mBill.getAmountDepositedAndToBeDeposited() > 0)?View.GONE:View.VISIBLE);

                ((ImageView) findViewById(R.id.wallet_qrcode)).setImageBitmap(
                        PayActivity.this.mZoomed? s_bmp_qrcode_big : s_bmp_qrcode_normal);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // app icon in action bar clicked; go home

                // Don't be a listener anymore, in order to get GCed
                DBHelper.getInstance(getApplicationContext()).GetDepositUpdates(this, false);

                Intent intent;
                if (mFrom == FROM_BILL) {
                    intent = new Intent(this, BillActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putString("wallet_address", mWalletAddress);
                    intent.putExtras(bundle);
                } else {
                    intent = new Intent(this, MainActivity.class);
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public static void GenerateQRCodes(Activity activity, Bill bill) {
        QRCodeWriter writer = new QRCodeWriter();
        try {
            String label_urlencoded = "";
            try {
                label_urlencoded = URLEncoder.encode(bill.getLabel(), "utf-8");
            } catch (UnsupportedEncodingException e) {
            }

            double amount_to_receive = bill.getCryptoAmount() - bill.getAmountDepositedAndToBeDeposited();
            String str_amount_btc = String.format("%.8f", amount_to_receive);
            String qrcode_address = "bitcoin:" + bill.getWalletID()
                    + "?amount=" + str_amount_btc.replace(',', '.')
                    + ((bill.getLabel()!=null && bill.getLabel().length()!=0)?("&label=" + label_urlencoded):"");

            View activity_layout = View.inflate(activity, net.dapay.app.R.layout.activity_pay, null);
//            RelativeLayout activity_layout = ((RelativeLayout) findViewById(R.id.activity_pay));
            int padding_left = activity_layout.getPaddingLeft();
            int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, padding_left, activity.getResources().getDisplayMetrics());

            Display display = activity.getWindowManager().getDefaultDisplay();
            Point screen_size = new Point();
            display.getSize(screen_size);

            int px_drawable_width = screen_size.x - 2*px;

            // "fullscreen" QRCode
            BitMatrix bitMatrix = writer.encode(qrcode_address, BarcodeFormat.QR_CODE, px_drawable_width, px_drawable_width);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            s_bmp_qrcode_big = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    s_bmp_qrcode_big.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }

            // Not fullscreen qrcode
            bitMatrix = writer.encode(qrcode_address, BarcodeFormat.QR_CODE, px_drawable_width*2/3, px_drawable_width*2/3);
            width = bitMatrix.getWidth();
            height = bitMatrix.getHeight();
            s_bmp_qrcode_normal = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    s_bmp_qrcode_normal.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }

        } catch (WriterException e) {
            e.printStackTrace();
        }
    }

    public void update(Observable obs, Object obj) {
        if ( ! (obj instanceof Bill.Deposit))
            return;
//        if (mBill.getStatus() != 0) {
            DBHelper.getInstance(getApplicationContext()).GetDepositUpdates(this, false);
            startActivity(new Intent(PayActivity.this, MainActivity.class));
//        }
    }
}
