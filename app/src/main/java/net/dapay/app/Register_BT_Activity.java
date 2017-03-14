package net.dapay.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import net.dapay.app.API.ExchangeAPI;
import net.dapay.app.API.IExchangeAPI;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class Register_BT_Activity extends AppCompatActivity {

    private Broker mBroker = null;
    private SignUpTask mAuthTask = null;
    private EditText mEmailEdit;
    private EditText mPasswordEdit;
    private EditText mConfirmPasswordEdit;
    private EditText m2faEdit;
    private View mFormView;
    private View mProgressView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account_bt);
        android.support.v7.app.ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setTitle(getString(R.string.title_create_account));

        mEmailEdit           = (EditText) findViewById(R.id.create_acc_bt_email);
        mPasswordEdit        = (EditText) findViewById(R.id.create_acc_bt_password);
        mConfirmPasswordEdit = (EditText) findViewById(R.id.create_acc_bt_confirm_password);
        m2faEdit             = (EditText) findViewById(R.id.create_acc_bt_2fa);
        mFormView     = findViewById(R.id.create_acc_bt_form);
        mProgressView = findViewById(R.id.create_acc_bt_progress);

        int broker_id = -1;
        Bundle bundle = getIntent().getExtras();
        if(bundle != null) {
            broker_id = bundle.getInt("broker_id");
        }

        mBroker = Broker.GetBrokerByID(broker_id);

        if (mBroker != null) {
            ImageView logo_view    = (ImageView) findViewById(R.id.broker_row_picture);
            TextView  name_view    = (TextView)  findViewById(R.id.broker_row_name   );
            TextView  country_view = (TextView)  findViewById(R.id.broker_row_country);
            logo_view   .setImageResource(mBroker.getImageResourceID());
            name_view   .setText         (mBroker.getName           ());
            country_view.setText         (mBroker.getCoutry         ());
        } else {
            // TODO
        }

        Button confirm_button = (Button)  findViewById(R.id.create_acc_bt_button);
        confirm_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptCreateAccount();
            }
        });

        mEmailEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    mPasswordEdit.requestFocus();
                    return true;
                }
                return false;
            }
        });

        mPasswordEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    mConfirmPasswordEdit.requestFocus();
                    return true;
                }
                return false;
            }
        });

        mConfirmPasswordEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptCreateAccount();
                    return true;
                }
                return false;
            }
        });

        ((View) findViewById(R.id.create_acc_bt_broker_desc)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(ExchangeAPI.GetCurrentAPI().GetURL()));
                    startActivity(myIntent);
                } catch (ActivityNotFoundException e) {
                    new AlertDialog.Builder(Register_BT_Activity.this)
                            .setMessage(getString(R.string.error_no_browser))
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // app icon in action bar clicked; go home
                Intent intent = new Intent(this, SelectBrokerActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                showProgress(false);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void attemptCreateAccount() {
        if (mAuthTask != null)
            return;

        // Reset errors.
        mEmailEdit.setError(null);
        mPasswordEdit.setError(null);

        String email    = mEmailEdit.getText().toString();
        String password1 = mPasswordEdit.getText().toString();
        String password2 = mPasswordEdit.getText().toString();
        String _2fa     = ""; // TODO

        boolean cancel = false;
        View focusView = null;

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailEdit.setError(getString(R.string.error_field_required));
            focusView = mEmailEdit;
            cancel = true;
        } else if (!isEmailValid(email)) {
            mEmailEdit.setError(getString(R.string.error_invalid_email));
            focusView = mEmailEdit;
            cancel = true;
        }

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password1)) {
            mPasswordEdit.setError(getString(R.string.error_field_required));
            focusView = mPasswordEdit;
            cancel = true;
        } else if (!isPasswordValid(password1)) {
            mPasswordEdit.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordEdit;
            cancel = true;
        } else if (!password1.equals(password2)) {
            mPasswordEdit.setError(getString(R.string.error_passwords_dont_match));
            focusView = mPasswordEdit;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mAuthTask = new SignUpTask(email, password1, _2fa);
            mAuthTask.execute((Void) null);
        }
    }

    private boolean isEmailValid(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
    private boolean isPasswordValid(String pass) {
        return pass.length() >= 4;
    }


    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mFormView.setVisibility(show ? View.GONE : View.VISIBLE);
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
            mFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    public class SignUpTask extends AsyncTask<Void, Void, Boolean> {

        private final String mLogin;
        private final String mPassword;
        private final String m2fa;
        private int mLoginError;

        SignUpTask(String email, String password, String _2fa) {
            mLogin    = email;
            mPassword = password;
            m2fa      = _2fa;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            IExchangeAPI api = ExchangeAPI.GetCurrentAPI();
            if (api == null)
                return false;
            try {
                IExchangeAPI.ErrorOrPayload<Boolean> maybe_login =
                        api.Login(mLogin, mPassword, m2fa,
                                Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID))
                                .get(20, TimeUnit.SECONDS);
                mLoginError = maybe_login.getErrCode();
                return mLoginError == IExchangeAPI.ErrorOrPayload.ERROR_NONE;
            } catch (Throwable t) {
            }
            return false;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;

            if (success) {
                DBHelper.getInstance(getApplicationContext());
                Intent intent = new Intent(Register_BT_Activity.this, mBroker.GetNextActivity(Register_BT_Activity.class));
                Bundle bundle = new Bundle();
                bundle.putInt("broker_id", mBroker.getID());
                intent.putExtras(bundle);
                startActivity(intent);
                showProgress(false);
            } else {
                showProgress(false);
                if (mLoginError == IExchangeAPI.ErrorOrPayload.ERROR_NONE) {
                    m2faEdit.setText("");
                    m2faEdit.setVisibility(View.GONE);
                    mPasswordEdit.setError(getString(R.string.error_no_response));
                    mPasswordEdit.requestFocus();
                } else {
                    switch (mLoginError) {
                        case IExchangeAPI.ErrorOrPayload.ERROR_NEED_2FA:
                            m2faEdit = (EditText) findViewById(R.id._2fa);
                            if (m2faEdit.getVisibility() == View.VISIBLE) {
                                m2faEdit.setText("");
                                m2faEdit.setError(getString(R.string.error_incorrect_2fa));
                                m2faEdit.requestFocus();
                            } else {
                                m2faEdit.setVisibility(View.VISIBLE);
                                m2faEdit.setError(getString(R.string.error_needs_2fa));
                                m2faEdit.setText("");
                                m2faEdit.requestFocus();
                            }
                            break;
                        case IExchangeAPI.ErrorOrPayload.ERROR_NOT_CONNECTED:
                            m2faEdit.setText("");
                            m2faEdit.setVisibility(View.GONE);
                            mPasswordEdit.setError(getString(R.string.error_no_connectivity));
                            mPasswordEdit.requestFocus();
                            break;
                        case IExchangeAPI.ErrorOrPayload.ERROR_LOGIN_PASS:
                            m2faEdit.setText("");
                            m2faEdit.setVisibility(View.GONE);
                            mPasswordEdit.setError(getString(R.string.error_incorrect_password));
                            mPasswordEdit.requestFocus();
                            break;
                        case IExchangeAPI.ErrorOrPayload.ERROR_INVALID_2FA:
                            m2faEdit.setText("");
                            m2faEdit.setError(getString(R.string.error_incorrect_2fa));
                            m2faEdit.requestFocus();
                            break;
                        case IExchangeAPI.ErrorOrPayload.ERROR_UNAUTHORIZED:
                            m2faEdit.setText("");
                            m2faEdit.setVisibility(View.GONE);
                            mPasswordEdit.setError(getString(R.string.error_unauthorized));
                            mPasswordEdit.requestFocus();
                            break;
                        case IExchangeAPI.ErrorOrPayload.ERROR_UNKNOWN:
                            m2faEdit.setText("");
                            m2faEdit.setVisibility(View.GONE);
                            mPasswordEdit.setError(getString(R.string.error_unknown));
                            mPasswordEdit.requestFocus();
                            break;
                    }
                }
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }

    }
}
