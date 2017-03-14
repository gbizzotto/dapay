package net.dapay.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.content.Intent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.math.BigDecimal;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import net.dapay.app.API.ExchangeAPI;
import net.dapay.app.API.IExchangeAPI;


public class ConfigPayActivity extends ActionBarActivity implements Observer {

    private View mProgressView;
    private View mConfigPayFormView;
    private EditText mFiatEdit;
    private EditText mIdentifierEdit;
    private TextView mValueCrypto;
    Future<IExchangeAPI.ErrorOrPayload<Float>> mCryptoBalance = null;
    int mValueCryptoTextColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config_pay);
        android.support.v7.app.ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);

        if (ExchangeAPI.GetCurrentAPI() != null) {
            TextView label_bill = ((TextView) findViewById(R.id.label_bill));
            label_bill.setText(label_bill.getText().toString().replace("$", ExchangeAPI.GetCurrentAPI().GetCurrencyActualSymbol()));
        }

        mFiatEdit       = ((EditText) findViewById(R.id.value_fiat));
        mIdentifierEdit = ((EditText) findViewById(R.id.transaction_label));
        mValueCrypto    = ((TextView) findViewById(R.id.value_btc));
        mValueCryptoTextColor = mValueCrypto.getCurrentTextColor();

        mFiatEdit.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void afterTextChanged(Editable s) {
                ConfigPayActivity.this.UpdateBTCPreview(false);
                // If there's both . and , separators, remove the one that appears first
                if (mFiatEdit.getText().toString().contains(".") && mFiatEdit.getText().toString().contains(",")) {
                    if (mFiatEdit.getText().toString().indexOf('.') < mFiatEdit.getText().toString().indexOf(','))
                        mFiatEdit.setText(mFiatEdit.getText().toString().replace(".", ""));
                    else
                        mFiatEdit.setText(mFiatEdit.getText().toString().replace(",", ""));
                    // place cursor at the end
                    mFiatEdit.setSelection(mFiatEdit.toString().length());
                }
            }
        });

        mIdentifierEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.transaction_label || id == EditorInfo.IME_NULL) {
                    showProgress(true);
                    new ConfigPayActivity.SettlePayTask(getFiatAmount()).execute((Void) null);
                    return true;
                }
                return false;
            }
        });

        String str_action = "";
        if (Profile.currentProfile.action.equals(Bill.ACTION_PRE_SELL))
            str_action = "Presell";
        else if (Profile.currentProfile.action.equals(Bill.ACTION_TAKE))
            str_action = "Take";
        else if (Profile.currentProfile.action.equals(Bill.ACTION_MAKE))
            str_action = "Make";
        else if (Profile.currentProfile.action.equals(Bill.ACTION_HOLD))
            str_action = "Hold";
        ((TextView) findViewById(R.id.label_action)).setText("(" + str_action + ")");

        ((Button)findViewById(R.id.button_confirm)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showProgress(true);
                new ConfigPayActivity.SettlePayTask(getFiatAmount()).execute((Void) null);
            }
        });

        mConfigPayFormView = findViewById(R.id.config_pay_form);
        mProgressView = findViewById(R.id.config_pay_progress);

        final IExchangeAPI api = ExchangeAPI.GetCurrentAPI();
        if (api != null) {
            api.GetBookUpdates(this);
            if (Profile.currentProfile.action.equals(Bill.ACTION_PRE_SELL))
                mCryptoBalance = api.GetCryptoBalance();
        }
    }


    public void update(Observable o, Object arg) {
        UpdateBTCPreview(true);
    }
    public void UpdateBTCPreview(boolean fromBook) {
        IExchangeAPI api = ExchangeAPI.GetCurrentAPI();
        if (api == null)
            return;

        TextView warning_view = ((TextView) findViewById(R.id.config_pay_warning));

        double fiat_amount = getFiatAmount();

        double crypto_amount = 0.0d; // error by default
        double exchange_rate = api.GetMakerPrice();
        if (exchange_rate != 0.0d)
            crypto_amount = fiat_amount / exchange_rate;

        if (Profile.currentProfile.action.equals(Bill.ACTION_PRE_SELL)) {
            double balance = -1.0;
            try {
                if (mCryptoBalance != null && mCryptoBalance.isDone() && mCryptoBalance.get().getErrCode() == IExchangeAPI.ErrorOrPayload.ERROR_NONE) {
                    balance = mCryptoBalance.get().getPayload().doubleValue();
                }
            } catch (Exception e) {
            }

            if (crypto_amount != 0.0) {
                if (Profile.currentProfile.action.equals(Bill.ACTION_PRE_SELL)) {
                    if (balance == -1.0) {
                        // We don't know if we have enough CRY, use maker price
                        warning_view.setVisibility(View.VISIBLE);
                        warning_view.setText(getString(R.string.warning_unknown_balance) + "\n"+ getString(R.string.balance) +": "+getString(R.string.cry)+" " + balance);
                        mValueCrypto.setTextColor(warning_view.getCurrentTextColor());
                    } else if (balance < crypto_amount) {
                        warning_view.setVisibility(View.VISIBLE);
                        warning_view.setText(getString(R.string.warning_insufficient_crypto) + "\n"+ getString(R.string.balance) +": "+ balance);
                        mValueCrypto.setTextColor(warning_view.getCurrentTextColor());
                    } else {
                        // balance >= crypto_amount
                        try {
                            double[] crypto_amount_and_exchange_rate = api.GetQuotation(fiat_amount);
                            crypto_amount = crypto_amount_and_exchange_rate[0];
                            exchange_rate = crypto_amount_and_exchange_rate[1];
                            warning_view.setVisibility(View.GONE);
                            mValueCrypto.setTextColor(mValueCryptoTextColor);
                        } catch (ArithmeticException e) {
                            warning_view.setVisibility(View.VISIBLE);
                            warning_view.setText(getString(R.string.warning_unknown_price));
                            mValueCrypto.setTextColor(warning_view.getCurrentTextColor());
                        }
                    }
                } else if (Profile.currentProfile.action.equals(Bill.ACTION_TAKE)) {
                    // We'll have the balance then
                    warning_view.setVisibility(View.GONE);
                    mValueCrypto.setTextColor(mValueCryptoTextColor);
                }
            }
        }

        String str_new_amount_tmp;
        String str_exchange_rate_tmp;
        if (fiat_amount == 0.0d) {
            str_new_amount_tmp    = getString(R.string.not_available);
            str_exchange_rate_tmp = getString(R.string.not_available);
        } else if (crypto_amount == 0.0d) {
            str_new_amount_tmp    = getString(R.string.unknown_exchange_rate);
            str_exchange_rate_tmp = getString(R.string.not_available);
        } else {
            str_new_amount_tmp    = String.format("%.8f", crypto_amount);
            if (api != null)
                str_exchange_rate_tmp = String.format("%.2f", exchange_rate) + "   "+api.GetCurrencyActualSymbol()+" / "+getString(R.string.cry);
            else
                str_exchange_rate_tmp = String.format("%.2f", exchange_rate) + "   $ / "+getString(R.string.cry);
        }

        final String str_new_amount    = str_new_amount_tmp;
        final String str_exchange_rate = str_exchange_rate_tmp;

        TextView btc_text = ((TextView) findViewById(R.id.value_btc));
        if (str_new_amount.equals(btc_text.getText().toString()))
            return;
        btc_text.post(new Runnable() {
            public void run() {
                TextView btc_text = ((TextView) findViewById(R.id.value_btc));
                btc_text.setText(str_new_amount);
            }
        });
        TextView exchange_rate_text = ((TextView) findViewById(R.id.exchange_rate));
        exchange_rate_text.post(new Runnable() {
            public void run() {
                TextView exchange_rate_text = ((TextView) findViewById(R.id.exchange_rate));
                exchange_rate_text.setText(str_exchange_rate);
            }
        });




        if (fromBook) {
            // show color fading
            new Thread() {
                public void run() {
                    TextView btc_text = ((TextView) findViewById(R.id.value_btc));
                    try {
                        for (int opacity = 256-8; opacity != 00; opacity-=8) {
                            final int o = opacity;
                            btc_text.post(new Runnable() {
                                public void run() {
                                    TextView btc_text = ((TextView) findViewById(R.id.value_btc));
                                    TextView cambio_text = ((TextView) findViewById(R.id.exchange_rate));
                                    btc_text.setBackgroundColor(Color.argb(o, 255, 255, 0));
                                    cambio_text.setBackgroundColor(Color.argb(o, 255, 255, 0));
                                }
                            });
                            try {
                                Thread.sleep(16);
                            } catch (InterruptedException ex) {
                                return;
                            }
                        }
                    } finally {
                        btc_text.post(new Runnable() {
                            public void run() {
                                TextView btc_text = ((TextView) findViewById(R.id.value_btc));
                                TextView cambio_text = ((TextView) findViewById(R.id.exchange_rate));
                                btc_text.setBackgroundColor(Color.argb(0, 255, 255, 0));
                                cambio_text.setBackgroundColor(Color.argb(0, 255, 255, 0));
                            }
                        });
                    }
                }
            }.start();
        }
    }

    public double getFiatAmount()
    {
        final EditText fiat_edit = ((EditText) findViewById(R.id.value_fiat));
        if (fiat_edit.getText().toString().length() == 0)
            return 0.0;

        if (fiat_edit == null)
            return 0.0;

        try {
            // Accept . and , as decimal separators
            String str = fiat_edit.getText().toString();
            return Double.parseDouble(str.toString().replace(',', '.'));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                final EditText label_edit = ((EditText) findViewById(R.id.transaction_label));
                // app icon in action bar clicked; go home
                startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mConfigPayFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mConfigPayFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mConfigPayFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mConfigPayFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    public class SettlePayTask extends AsyncTask<Void, Void, Boolean> {

        double mFiatAmount   = 0.0d;
        double mCryptoAmount = 0.0d;
        String mWalletAddress = "";
        String mLabel = "";
        Bill mBill;

        private final int ERRCODE_NONE                 = 0;
        private final int ERRCODE_UNKNOWN              = 1;
        private final int ERRCODE_CANT_CREATE_WALLET   = 2;
        private final int ERRCODE_PROBLEM_SELLING_FIAT = 3;
        private final int ERRCODE_API_NULL             = 4;
        private final int ERRCODE_BILL_NOT_FOUND       = 5;
        private final int ERRCODE_INSUFFICIENT_FUNDS   = 6;
        private final int ERRCODE_NO_BUYER             = 7;
        private final int ERRCODE_NOT_ENOUGH_BUYERS    = 8;

        int mErrCode = ERRCODE_NONE;

        SettlePayTask(double fiat_amount) {
            mFiatAmount = fiat_amount;
            final EditText label_edit = ((EditText) findViewById(R.id.transaction_label));
            mLabel = label_edit.getText().toString();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            mErrCode = ERRCODE_NONE;

            IExchangeAPI api = ExchangeAPI.GetCurrentAPI();
            if (api == null) {
                mErrCode = ERRCODE_API_NULL;
                return false;
            }

            try {
                IExchangeAPI.ErrorOrPayload<String> maybe_deposit_address = api.GetDepositAddress().get(20, TimeUnit.SECONDS);
                if (maybe_deposit_address.getErrCode() == IExchangeAPI.ErrorOrPayload.ERROR_NONE) {
                    mWalletAddress = maybe_deposit_address.getPayload();
                } else {
                    mErrCode = ERRCODE_CANT_CREATE_WALLET;
                    return false;
                }
            } catch (Throwable t) {
                mErrCode = ERRCODE_CANT_CREATE_WALLET;
                return false;
            }

            double fiat_amount = getFiatAmount();
            mCryptoAmount = 0.0d; // error by default
            String action = Profile.currentProfile.action;

            double exchange_rate = api.GetMakerPrice();
            if (exchange_rate != 0.0d)
                mCryptoAmount = fiat_amount / exchange_rate;

            if (action.equals(Bill.ACTION_PRE_SELL)) {
                double balance = -1.0;
                try {
                    if (mCryptoBalance != null && mCryptoBalance.isDone() && mCryptoBalance.get().getErrCode() == IExchangeAPI.ErrorOrPayload.ERROR_NONE) {
                        balance = mCryptoBalance.get().getPayload().doubleValue();
                    }
                } catch (Exception e) {
                }

                if (mCryptoAmount != 0.0) {
                    if (action.equals(Bill.ACTION_PRE_SELL)) {
                        if (balance == -1.0) {
                            // We don't know if we have enough CRY, use maker price
                            action = Bill.ACTION_TAKE;
                        } else if (balance < mCryptoAmount) {
                            action = Bill.ACTION_TAKE;
                        } else {
                            // balance >= crypto_amount, sell the BTC now
                            IExchangeAPI.ErrorOrPayload<Double> maybe_crypto_amount = api.Convert(mFiatAmount);
                            if (maybe_crypto_amount.getErrCode() == IExchangeAPI.ErrorOrPayload.ERROR_NONE) {
                                try {
                                    double[] crypto_amount_and_exchange_rate = api.GetQuotation(fiat_amount);
                                    mCryptoAmount = crypto_amount_and_exchange_rate[0];
                                } catch (ArithmeticException e) {
                                    mErrCode = ERRCODE_NOT_ENOUGH_BUYERS;
                                    return false;
                                }
                            } else if (maybe_crypto_amount.getErrCode() == IExchangeAPI.ErrorOrPayload.ERROR_INSUFFICIENT_FUNDS) {
                                action = Bill.ACTION_TAKE;
                            } else if (maybe_crypto_amount.getErrCode() == IExchangeAPI.ErrorOrPayload.ERROR_NO_BUYER) {
                                mErrCode = ERRCODE_NO_BUYER;
                                return false;
                            } else if (maybe_crypto_amount.getErrCode() == IExchangeAPI.ErrorOrPayload.ERROR_NOT_ENOUGH_BUYERS) {
                                mErrCode = ERRCODE_NOT_ENOUGH_BUYERS;
                                return false;
                            } else {
                                mErrCode = ERRCODE_PROBLEM_SELLING_FIAT;
                                return false;
                            }
                        }
                    }
                }
            }

            // trunk to 8 decimal places
            BigDecimal bd = new BigDecimal(mCryptoAmount);
            bd = bd.setScale(8, BigDecimal.ROUND_HALF_DOWN);
            mCryptoAmount = bd.doubleValue();

            mBill = DBHelper.getInstance(getApplicationContext())
                    .insertBill(System.currentTimeMillis(),
                                Profile.currentProfile.id, mWalletAddress, mFiatAmount, mCryptoAmount, mLabel,
                                action, action.equals(Bill.ACTION_PRE_SELL) || action.equals(Bill.ACTION_HOLD));
            if (mBill == null) {
                mErrCode = ERRCODE_BILL_NOT_FOUND;
                return false;
            }

            PayActivity.GenerateQRCodes(ConfigPayActivity.this, mBill);
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            if (success) {
                Intent intent = new Intent(ConfigPayActivity.this, PayActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("wallet_address", mWalletAddress);
                bundle.putInt("from", PayActivity.FROM_CONFIG_PAY);
                intent.putExtras(bundle);
                startActivity(intent);
                showProgress(false);
            } else {
                showProgress(false);
                switch (mErrCode){
                    case ERRCODE_CANT_CREATE_WALLET:
                        new AlertDialog.Builder(ConfigPayActivity.this)
                                .setMessage(getString(R.string.error_create_deposit))
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                })
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .show();
                        break;
                    case ERRCODE_INSUFFICIENT_FUNDS:
                        new AlertDialog.Builder(ConfigPayActivity.this)
                                .setMessage(getString(R.string.error_insufficient_funds))
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                })
                                .show();
                        break;
                    case ERRCODE_NO_BUYER:
                        new AlertDialog.Builder(ConfigPayActivity.this)
                                .setMessage(getString(R.string.error_no_buyers))
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                })
                                .show();
                        break;
                    case ERRCODE_NOT_ENOUGH_BUYERS:
                        new AlertDialog.Builder(ConfigPayActivity.this)
                                .setMessage(getString(R.string.error_not_enough_buyers))
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        Intent intent = new Intent(ConfigPayActivity.this, PayActivity.class);
                                        Bundle bundle = new Bundle();
                                        bundle.putString("wallet_address", mWalletAddress);
                                        bundle.putInt("from", PayActivity.FROM_CONFIG_PAY);
                                        intent.putExtras(bundle);
                                        startActivity(intent);
                                        showProgress(false);
                                    }
                                })
                                .show();
                        break;
                    case ERRCODE_PROBLEM_SELLING_FIAT:
                        new AlertDialog.Builder(ConfigPayActivity.this)
                                .setMessage(getString(R.string.error_during_conversion))
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                })
                                .show();
                        break;
                    case ERRCODE_API_NULL:
                        new AlertDialog.Builder(ConfigPayActivity.this)
                                .setMessage(getString(R.string.error_internal_1))
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                })
                                .show();
                        break;
                    case ERRCODE_UNKNOWN:
                        new AlertDialog.Builder(ConfigPayActivity.this)
                                .setMessage(getString(R.string.error_unknown))
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                })
                                .show();
                        break;
                }
            }
        }

        @Override
        protected void onCancelled() {
            showProgress(false);
        }

    }
}

