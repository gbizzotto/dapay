package net.dapay.app;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import net.dapay.app.API.ExchangeAPI;
import net.dapay.app.API.IExchangeAPI;

import java.util.Observable;
import java.util.Observer;

public class MainActivity extends AppCompatActivity implements Observer {

    BillsListAdapter mLVAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DBHelper.getInstance(getApplicationContext()).GetDepositUpdates(MainActivity.this, false);
                Intent intent = new Intent(MainActivity.this, ConfigPayActivity.class);
                startActivity(intent);
            }
        });

        mLVAdapter = new BillsListAdapter(getApplicationContext());
        ListView lv = ((ListView) findViewById(R.id.bill_list));
        lv.setAdapter(mLVAdapter);

        DBHelper.getInstance(getApplicationContext()).GetDepositUpdates(this, true);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                DBHelper.getInstance(getApplicationContext()).GetDepositUpdates(MainActivity.this, false);
                Bill bill = (Bill) view.getTag();
                Intent intent = new Intent(MainActivity.this, BillActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("wallet_address", bill.getWalletID());
                intent.putExtras(bundle);
                startActivity(intent);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
//        menu.findItem(R.id.action_settings).setIntent(new Intent(this, SettingsActivity.class));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_logoff) {
            super.onOptionsItemSelected(item);
            this.closeOptionsMenu();
            IExchangeAPI api = ExchangeAPI.GetCurrentAPI();
            if (api != null)
                api.Logout();
            DBHelper.getInstance(getApplicationContext()).GetDepositUpdates(this, false);
            startActivity(new Intent(MainActivity.this, SelectProfileActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            return true;
        } else if (id == R.id.action_prefs) {
            startActivity(new Intent(MainActivity.this, ProfileSettingsActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP).putExtra("from", "menu"));
        } else if (id == R.id.action_open_exchange) {
            try {
                Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(ExchangeAPI.GetCurrentAPI().GetURL()));
                startActivity(myIntent);
            } catch (ActivityNotFoundException e) {
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage(getString(R.string.error_no_browser))
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    public void update(Observable obs, Object obj) {
        new Thread() {
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mLVAdapter.notifyDataSetChanged();
                    }
                });
            }
        }.start();
    }
}
