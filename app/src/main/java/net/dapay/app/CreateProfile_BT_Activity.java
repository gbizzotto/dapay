package net.dapay.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import net.dapay.app.API.BlinktradeAPI;
import net.dapay.app.API.ExchangeAPI;
import net.dapay.app.API.IExchangeAPI;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class CreateProfile_BT_Activity extends AppCompatActivity {

    AutoCompleteTextView mNameEdit;
    EditText mPasswordEdit;
    EditText mPasswordConfirmEdit;
    View mFormView;
    View mProgressView;
    Broker mBroker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_profile);
        android.support.v7.app.ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setTitle(getString(R.string.title_create_profile));

        findViewById(R.id.create_profile_bt_name).requestFocus();

        Bundle bundle = getIntent().getExtras();
        if(bundle != null)
            mBroker = Broker.GetBrokerByID(bundle.getInt("broker_id"));

        Button mEmailSignInButton = (Button) findViewById(R.id.create_profile_button);
        mEmailSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptCreateProfile();
            }
        });

        mNameEdit            = (AutoCompleteTextView) findViewById(R.id.create_profile_bt_name);
        mPasswordEdit        = (EditText) findViewById(R.id.create_profile_bt_password);
        mPasswordConfirmEdit = (EditText) findViewById(R.id.create_profile_bt_confirm_password);
        mFormView            = findViewById(R.id.create_profile_bt_form);
        mProgressView        = findViewById(R.id.create_profile_bt_wait);


        mNameEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
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
                    mPasswordConfirmEdit.requestFocus();
                    return true;
                }
                return false;
            }
        });

        mPasswordConfirmEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.password || id == EditorInfo.IME_NULL) {
                    attemptCreateProfile();
                    return true;
                }
                return false;
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

    private void attemptCreateProfile() {
        // Reset errors.
        mNameEdit.setError(null);
        mPasswordEdit.setError(null);
        mPasswordConfirmEdit.setError(null);

        // Store values at the time of the login attempt.
        String name = mNameEdit.getText().toString();
        String password = mPasswordEdit.getText().toString();
        String password_confirm = mPasswordConfirmEdit.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password)) {
            mPasswordEdit.setError(getString(R.string.error_field_required));
            focusView = mPasswordEdit;
            cancel = true;
        }

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password)) {
            mPasswordEdit.setError(getString(R.string.error_field_required));
            focusView = mPasswordEdit;
            cancel = true;
        } else if (!isPasswordValid(password)) {
            mPasswordEdit.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordEdit;
            cancel = true;
        } else if (!password.equals(password_confirm)) {
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
            new CreateProfile_BT_Activity.CreatePasswordTask(mBroker, name, password).execute((Void) null);
        }
    }

    private boolean isPasswordValid(String pass) {
        return pass.length() >= 4;
    }

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


    public class CreatePasswordTask extends AsyncTask<Void, Void, Boolean> {

        private final String mName;
        private final String mPassword;
        IExchangeAPI.ErrorOrPayload<Profile> mMaybeProfile;

        CreatePasswordTask(Broker broker, String name, String password) {
            mBroker = broker;
            mName = name;
            mPassword = password;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            IExchangeAPI tmp_api = ExchangeAPI.GetCurrentAPI();
            if (tmp_api == null || !(tmp_api instanceof BlinktradeAPI))
                return false;
            BlinktradeAPI api = (BlinktradeAPI) tmp_api;
            try {
                mMaybeProfile = api.CreateProfile(mName, mPassword).get(20, TimeUnit.SECONDS);
                return mMaybeProfile.getErrCode() == IExchangeAPI.ErrorOrPayload.ERROR_NONE;
            } catch (TimeoutException t) {
                mMaybeProfile = new IExchangeAPI.ErrorOrPayload<Profile>(IExchangeAPI.ErrorOrPayload.ERROR_TIMEOUT, null);
            } catch (Throwable t) {
            }
            return false;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            if (success) {
                mMaybeProfile.getPayload().broker_id = mBroker.getID();
                DBHelper.getInstance(getApplicationContext()).AddProfile(mMaybeProfile.getPayload());
                Profile.currentProfile = mMaybeProfile.getPayload();
                Intent intent = new Intent(CreateProfile_BT_Activity.this, mBroker.GetNextActivity(CreateProfile_BT_Activity.class));
                startActivity(intent.putExtra("broker_id", mBroker.getID()));
                showProgress(false);
            } else {
                int login_error = mMaybeProfile.getErrCode();
                showProgress(false);
                switch (login_error) {
                    case IExchangeAPI.ErrorOrPayload.ERROR_NONE:
                        System.out.println("Should not happen");
                        mPasswordEdit.setError(getString(R.string.error_no_response));
                        mPasswordEdit.requestFocus();
                        break;
                    case IExchangeAPI.ErrorOrPayload.ERROR_NOT_CONNECTED:
                        mPasswordEdit.setError(getString(R.string.error_no_connectivity));
                        mPasswordEdit.requestFocus();
                        break;
                    case IExchangeAPI.ErrorOrPayload.ERROR_LOGIN_PASS:
                        mPasswordEdit.setError(getString(R.string.error_incorrect_password));
                        mPasswordEdit.requestFocus();
                        break;
                    case IExchangeAPI.ErrorOrPayload.ERROR_UNAUTHORIZED:
                        mPasswordEdit.setError(getString(R.string.error_unauthorized));
                        mPasswordEdit.requestFocus();
                        break;
                    case IExchangeAPI.ErrorOrPayload.ERROR_UNKNOWN:
                        mPasswordEdit.setError(getString(R.string.error_unknown));
                        mPasswordEdit.requestFocus();
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
