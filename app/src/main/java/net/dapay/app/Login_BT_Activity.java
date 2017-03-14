package net.dapay.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.app.LoaderManager.LoaderCallbacks;

import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;

import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import android.content.Intent;

import net.dapay.app.API.BlinktradeAPI;
import net.dapay.app.API.ExchangeAPI;
import net.dapay.app.API.IExchangeAPI;

/**
 * A login screen that offers login via email/password.
 */
public class Login_BT_Activity extends AppCompatActivity implements LoaderCallbacks<Cursor> {

    // Id to identity READ_CONTACTS permission request.
    private static final int REQUEST_READ_CONTACTS = 0;

    // Keep track of the login task to ensure we can cancel it if requested.
    private UserLoginTask mAuthTask = null;

    // UI references.
    private AutoCompleteTextView mLoginEdit;
    private EditText mPasswordEdit;
    private EditText m2faEdit;
    private View mProgressView;
    private View mLoginFormView;

    private Broker mBroker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        android.support.v7.app.ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setTitle(getString(R.string.prompt_login));

        Bundle bundle = getIntent().getExtras();
        if(bundle != null) {
            int broker_id = bundle.getInt("broker_id");
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
        }

        // Init singleton WITH CONTEXT
        Notifier.getInstance(getApplicationContext(), this);

        // Set up the login form.
        mLoginEdit = (AutoCompleteTextView) findViewById(R.id.login);

        mPasswordEdit = (EditText) findViewById(R.id.password);
        m2faEdit = (EditText) findViewById(R.id._2fa);


        mLoginEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
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
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        m2faEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.password || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        ((Button) findViewById(R.id.button_login)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        ((View) findViewById(R.id.login_bt_broker_desc)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(ExchangeAPI.GetCurrentAPI().GetURL()));
                    startActivity(myIntent);
                } catch (ActivityNotFoundException e) {
                    new AlertDialog.Builder(Login_BT_Activity.this)
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

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);

        mLoginEdit.requestFocus();
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


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        if (mAuthTask != null)
            return;

        // Reset errors.
        mLoginEdit.setError(null);
        mPasswordEdit.setError(null);

        // Store values at the time of the login attempt.
        String email = mLoginEdit.getText().toString();
        String password = mPasswordEdit.getText().toString();
        String _2fa = (m2faEdit.getVisibility() == View.GONE) ? null : m2faEdit.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password)) {
            mPasswordEdit.setError(getString(R.string.error_field_required));
            focusView = mPasswordEdit;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mLoginEdit.setError(getString(R.string.error_field_required));
            focusView = mLoginEdit;
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
            mAuthTask = new UserLoginTask(email, password, _2fa);
            mAuthTask.execute((Void) null);
        }
    }

    private boolean isEmailValid(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isPasswordValid(String password) {
        return password.length() > 0;
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

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this,
                // Retrieve data rows for the device user's 'profile' contact.
                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

                // Select only email addresses.
                ContactsContract.Contacts.Data.MIMETYPE +
                        " = ?", new String[]{ContactsContract.CommonDataKinds.Email
                .CONTENT_ITEM_TYPE},

                // Show primary email addresses first. Note that there won't be
                // a primary email address if the user hasn't specified one.
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        List<String> emails = new ArrayList<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            emails.add(cursor.getString(ProfileQuery.ADDRESS));
            cursor.moveToNext();
        }

        addEmailsToAutoComplete(emails);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
        //Create mLVAdapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(Login_BT_Activity.this,
                        android.R.layout.simple_dropdown_item_1line, emailAddressCollection);

        mLoginEdit.setAdapter(adapter);
    }


    private interface ProfileQuery {
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
        };

        int ADDRESS = 0;
        int IS_PRIMARY = 1;
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mLogin;
        private final String mPassword;
        private final String m2fa;
        int mLoginError;

        UserLoginTask(String email, String password, String _2fa) {
            mLogin = email;
            mPassword = password;
            m2fa = _2fa;
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
            } catch (TimeoutException t) {
                mLoginError = IExchangeAPI.ErrorOrPayload.ERROR_TIMEOUT;
            } catch (Throwable t) {
                mLoginError = IExchangeAPI.ErrorOrPayload.ERROR_UNKNOWN;
            }
            return false;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;

            if (success) {
                DBHelper.getInstance(getApplicationContext());
                Intent intent = new Intent(Login_BT_Activity.this, mBroker.GetNextActivity(Login_BT_Activity.class));
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
                        default:
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

