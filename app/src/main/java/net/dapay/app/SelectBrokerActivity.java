package net.dapay.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import net.dapay.app.API.ExchangeAPI;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;


public class SelectBrokerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_broker);
        android.support.v7.app.ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setTitle(getString(R.string.prompt_create_profile));

        final ListView lv = (ListView) findViewById(R.id.select_broker_lv);
        final Button button_login  = (Button) findViewById (R.id.select_broker_login);
        final Button button_create = (Button) findViewById (R.id.select_broker_create_account);

        lv.setAdapter(new BrokersAdapter(getApplicationContext()));
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Broker broker = (Broker) view.getTag();
                button_login.setTag(broker);
                button_create.setTag(broker);
                button_login.setEnabled(true);
                button_create.setEnabled(true);
            }
        });

        button_create.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Broker broker = (Broker) view.getTag();
                if (broker != null) {
                    showProgress(true);
                    broker.setActivitySeries(broker.getRegisterActivities());
                    new ConnectTask(broker, broker.GetNextActivity(null)).execute((Void) null);
                }
            }
        });

        button_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Broker broker = (Broker) view.getTag();
                if (broker != null) {
                    showProgress(true);
                    broker.setActivitySeries(broker.getLoginActivities());
                    new ConnectTask(broker, broker.GetNextActivity(null)).execute((Void) null);
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                final EditText label_edit = ((EditText) findViewById(R.id.transaction_label));

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

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.

        final View formView     = findViewById(R.id.activity_broker);
        final View progressView = findViewById(R.id.broker_wait_layout);

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

        Broker mBroker;
        Class<? extends Activity> mNextActivityClass;

        public ConnectTask(Broker b, Class<? extends Activity> next_activity_class) {
            mBroker = b;
            mNextActivityClass = next_activity_class;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            ExchangeAPI.SetCurrentAPI(mBroker.GetAPIClass());
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
                Intent intent = new Intent(SelectBrokerActivity.this, mNextActivityClass);
                Bundle bundle = new Bundle();
                bundle.putInt("broker_id", mBroker.getID());
                intent.putExtras(bundle);
                startActivity(intent);
                showProgress(false);
            } else {
                showProgress(false);
                new AlertDialog.Builder(SelectBrokerActivity.this)
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
