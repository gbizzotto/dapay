package net.dapay.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import net.dapay.app.API.ExchangeAPI;
import net.dapay.app.API.IExchangeAPI;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static net.dapay.app.Bill.ACTION_PRE_SELL;

public class ProfilePinLoginActivity extends AppCompatActivity {

    private Broker mBroker;

    private View mProgressView;
    private View mLoginFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_pin_login);
        android.support.v7.app.ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setTitle(getString(R.string.prompt_login));

        if (Profile.currentProfile != null) {
            TextView profile_label_view = (TextView) findViewById(R.id.pin_login_label);
            profile_label_view.setText(Profile.currentProfile.label);
            mBroker = Broker.GetBrokerByID(Profile.currentProfile.broker_id);
            if (mBroker != null) {
                ImageView logo_view = (ImageView) findViewById(R.id.broker_row_picture);
                TextView name_view = (TextView) findViewById(R.id.broker_row_name);
                TextView country_view = (TextView) findViewById(R.id.broker_row_country);
                logo_view.setImageResource(mBroker.getImageResourceID());
                name_view.setText(mBroker.getName());
                country_view.setText(mBroker.getCoutry());
            }
        }

        mLoginFormView = findViewById(R.id.pin_login_scroll_form);
        mProgressView  = findViewById(R.id.pin_login_progress);

        ((Button) findViewById(R.id.pin_login_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        ((View) findViewById(R.id.pin_login_broker_desc)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(ExchangeAPI.GetCurrentAPI().GetURL()));
                    startActivity(myIntent);
                } catch (ActivityNotFoundException e) {
                    new AlertDialog.Builder(ProfilePinLoginActivity.this)
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
                Intent intent = new Intent(this, SelectProfileActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                showProgress(false);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private void attemptLogin() {
        EditText password_edit = (EditText) findViewById(R.id.pin_login_password);

        // Reset errors.
        password_edit.setError(null);

        // Store values at the time of the login attempt.
        String password = password_edit.getText().toString();

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password)) {
            password_edit.setError(getString(R.string.error_field_required));
            password_edit.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            new ProfilePinLoginActivity.PinLoginTask(password).execute((Void) null);
        }
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

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
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
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class PinLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mPassword;
        int mLoginError;

        PinLoginTask(String password) {
            mPassword = password;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            IExchangeAPI api = ExchangeAPI.GetCurrentAPI();
            if (api == null)
                return false;
            try {
                IExchangeAPI.ErrorOrPayload<Boolean> maybe_login =
                        api.Login(Profile.currentProfile.login, mPassword, null,
                                Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID))
                                .get(20, TimeUnit.SECONDS);
                mLoginError = maybe_login.getErrCode();
                if (mLoginError == IExchangeAPI.ErrorOrPayload.ERROR_NONE) {
                    ExchangeAPI.GetCurrentAPI().ReadInstrumentDescription();
                    ExchangeAPI.GetCurrentAPI().SignUpMarketData(Profile.currentProfile.action.equals(ACTION_PRE_SELL));
                    ExchangeAPI.GetCurrentAPI().GetDepositUpdates(DBHelper.getInstance(getApplicationContext()));
                    DBHelper.getInstance(getApplicationContext()).CacheBills(Profile.currentProfile);
                    return true;
                } else {
                    return false;
                }
            } catch (TimeoutException t) {
                mLoginError = IExchangeAPI.ErrorOrPayload.ERROR_TIMEOUT;
            } catch (Throwable t) {
                mLoginError = IExchangeAPI.ErrorOrPayload.ERROR_UNKNOWN;
            }
            return false;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            EditText password_edit = (EditText) findViewById(R.id.pin_login_password);
            if (success) {
                Intent intent = new Intent(ProfilePinLoginActivity.this, MainActivity.class);
                startActivity(intent);
                showProgress(false);
            } else {
                showProgress(false);
                if (mLoginError == IExchangeAPI.ErrorOrPayload.ERROR_NONE) {
                    password_edit.setError(getString(R.string.error_no_response));
                    password_edit.requestFocus();
                } else {
                    switch (mLoginError) {
                        case IExchangeAPI.ErrorOrPayload.ERROR_NOT_CONNECTED:
                            password_edit.setError(getString(R.string.error_no_connectivity));
                            password_edit.requestFocus();
                            break;
                        case IExchangeAPI.ErrorOrPayload.ERROR_LOGIN_PASS:
                            password_edit.setError(getString(R.string.error_incorrect_password));
                            password_edit.requestFocus();
                            break;
                        case IExchangeAPI.ErrorOrPayload.ERROR_UNAUTHORIZED:
                            password_edit.setError(getString(R.string.error_unauthorized));
                            password_edit.requestFocus();
                            break;
                        case IExchangeAPI.ErrorOrPayload.ERROR_UNKNOWN:
                            password_edit.setError(getString(R.string.error_unknown));
                            password_edit.requestFocus();
                            break;
                    }
                }
            }
        }

        @Override
        protected void onCancelled() {
            showProgress(false);
        }

    }
}
