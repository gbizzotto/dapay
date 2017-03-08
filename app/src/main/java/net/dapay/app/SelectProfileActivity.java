package net.dapay.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import net.dapay.app.API.ExchangeAPI;
import net.dapay.app.API.IExchangeAPI;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class SelectProfileActivity extends AppCompatActivity {

    ProfilesAdapter mLVAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_profile);

        // hide status bar
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        Broker.RegisterBrokers(); // Do this at the very beginning of the program

        mLVAdapter = new ProfilesAdapter(getApplicationContext());
        final ListView lv = ((ListView) findViewById(R.id.select_profile_lv));
        lv.setAdapter(mLVAdapter);

        ((TextView) findViewById(R.id.no_profile)).setVisibility(mLVAdapter.getCount()==0?View.VISIBLE:View.GONE);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                showProgress(true);
                Profile profile = (Profile) view.getTag();
                new SelectProfileActivity.ConnectTask(profile).execute((Void)null);
            }
        });

        ((Button)findViewById(R.id.select_profile_new_profile)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SelectProfileActivity.this, SelectBrokerActivity.class);
                startActivity(intent);
                showProgress(false);
            }
        });

        lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View view, int pos, long id) {
                final Profile profile = (Profile) view.getTag();
                if (profile == null)
                    return true;
                new AlertDialog.Builder(SelectProfileActivity.this)
                        .setMessage(getString(R.string.prompt_remove)+" '" + profile.label + "'?")
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                DBHelper.getInstance(getApplicationContext()).disableProfile(profile);
                                mLVAdapter.notifyDataSetChanged();
                                ((TextView) findViewById(R.id.no_profile)).setVisibility(mLVAdapter.getCount()==0?View.VISIBLE:View.GONE);
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {}
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
                return true;
            }
        });
    }


    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.

        final View formView     = findViewById(R.id.select_profile_body);
        final View progressView = findViewById(R.id.select_profile_wait);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            formView.setVisibility(show ? View.GONE : View.VISIBLE);
            formView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    formView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            progressView.setVisibility(show ? View.VISIBLE : View.GONE);
            progressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    progressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            progressView.setVisibility(show ? View.VISIBLE : View.GONE);
            formView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class ConnectTask extends AsyncTask<Void, Void, Boolean> {

        Profile mProfile;

        public ConnectTask(Profile p) {
            mProfile = p;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            Broker broker = Broker.GetBrokerByID(mProfile.broker_id);
            ExchangeAPI.SetCurrentAPI(broker.GetAPIClass());
            try {
                Future<Boolean> connected_future = ExchangeAPI.GetCurrentAPI().Connect();
                return connected_future.get(5, TimeUnit.SECONDS).booleanValue();
            } catch (Exception e) {
                return false;
            }
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            if (success) {
                Intent intent = new Intent(SelectProfileActivity.this, ProfilePinLoginActivity.class);
                Profile.currentProfile = mProfile;
                startActivity(intent);
                showProgress(false);
            } else {
                showProgress(false);
                new AlertDialog.Builder(SelectProfileActivity.this)
                        .setMessage(getString(R.string.error_no_connectivity))
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {}
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }
        }

        @Override
        protected void onCancelled() {
            showProgress(false);
        }
    }
}
