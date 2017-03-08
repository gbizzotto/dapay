package net.dapay.app;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;

public class ProfileSettingsActivity extends AppCompatActivity {

    boolean mCameFromMenu = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_settings);
        android.support.v7.app.ActionBar ab = getSupportActionBar();
        ab.setTitle(getString(R.string.prompt_preferences));

        Bundle bundle = getIntent().getExtras();
        if(bundle != null && bundle.containsKey("from"))
            mCameFromMenu = bundle.getString("from").equals("menu");

        if (mCameFromMenu) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        // Select the right radio
        ((RadioButton) findViewById(R.id.pref_radio_action_presell)).setChecked(Profile.currentProfile.action.equals(Bill.ACTION_PRE_SELL));
        ((RadioButton) findViewById(R.id.pref_radio_action_take   )).setChecked(Profile.currentProfile.action.equals(Bill.ACTION_TAKE    ));
        ((RadioButton) findViewById(R.id.pref_radio_action_make   )).setChecked(Profile.currentProfile.action.equals(Bill.ACTION_MAKE    ));
        ((RadioButton) findViewById(R.id.pref_radio_action_hold   )).setChecked(Profile.currentProfile.action.equals(Bill.ACTION_HOLD    ));

        ((Button) findViewById(R.id.pref_button_ok)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (((RadioButton) findViewById(R.id.pref_radio_action_presell)).isChecked())
                    Profile.currentProfile.action = Bill.ACTION_PRE_SELL;
                else if (((RadioButton) findViewById(R.id.pref_radio_action_take)).isChecked())
                    Profile.currentProfile.action = Bill.ACTION_TAKE;
                else if (((RadioButton) findViewById(R.id.pref_radio_action_make)).isChecked())
                    Profile.currentProfile.action = Bill.ACTION_MAKE;
                else if (((RadioButton) findViewById(R.id.pref_radio_action_hold)).isChecked())
                    Profile.currentProfile.action = Bill.ACTION_HOLD;
                DBHelper.getInstance(getApplicationContext()).updateProfileAction(Profile.currentProfile);
                Broker broker = Broker.GetBrokerByID(Profile.currentProfile.broker_id);
                Intent intent;
                if (mCameFromMenu)
                    intent = new Intent(ProfileSettingsActivity.this, MainActivity.class);
                else
                    intent = new Intent(ProfileSettingsActivity.this, broker.GetNextActivity(ProfileSettingsActivity.class));
                startActivity(intent.putExtra("broker_id", broker.getID()));
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // app icon in action bar clicked; go home
                startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
