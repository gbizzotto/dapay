package net.dapay.app;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;

import net.dapay.app.API.BlinktradeFoxbit;
import net.dapay.app.API.BlinktradeLocalhostFromDebugCellphone;
import net.dapay.app.API.BlinktradeLocalhostFromEmulator;
import net.dapay.app.API.BlinktradeTestnet;
import net.dapay.app.API.BlinktradeTestnetFoxbit;
import net.dapay.app.API.IExchangeAPI;

import java.util.ArrayList;
import java.util.List;

import static net.dapay.app.R.mipmap.ic_blinktrade;
import static net.dapay.app.R.mipmap.ic_foxbit;

/**
 * Created by gabriel on 12/29/16.
 */

public class Broker {

    private int mID;
    private int mImageResourceID;
    private String mName;
    private String mCountry;
    private Class<? extends IExchangeAPI> mAPIClass;
    private List<Class<? extends Activity>> mRegisterActivities;
    private List<Class<? extends Activity>> mLoginActivities;
    private List<Class<? extends Activity>> mActivitySeries;

    private static ArrayList<Broker> Brokers = new ArrayList<>();
    public static final ArrayList<Broker> GetBrokers() {
        return Brokers;
    }
    public static void RegisterBrokers() {
        if ( ! Brokers.isEmpty())
            return;
        List<Class<? extends Activity>> blinktrade_register_activities = new ArrayList<>();
        blinktrade_register_activities.add(Register_BT_Activity.class);
        blinktrade_register_activities.add(CreateProfile_BT_Activity.class);
        blinktrade_register_activities.add(ProfileSettingsActivity.class);
        blinktrade_register_activities.add(SelectProfileActivity.class);
        List<Class<? extends Activity>> blinktrade_login_activities = new ArrayList<>();
        blinktrade_login_activities.add(Login_BT_Activity.class);
        blinktrade_login_activities.add(CreateProfile_BT_Activity.class);
        blinktrade_login_activities.add(ProfileSettingsActivity.class);
        blinktrade_login_activities.add(SelectProfileActivity.class);
        Brokers.clear();
        Brokers.add(new Broker(ic_blinktrade, "BlinkTrade testnet"       , "-"     , BlinktradeTestnet                    .class, blinktrade_register_activities, blinktrade_login_activities));
        Brokers.add(new Broker(ic_blinktrade, "localhost from emulator"  , "-"     , BlinktradeLocalhostFromEmulator      .class, blinktrade_register_activities, blinktrade_login_activities));
        Brokers.add(new Broker(ic_blinktrade, "localhost from real phone", "-"     , BlinktradeLocalhostFromDebugCellphone.class, blinktrade_register_activities, blinktrade_login_activities));
        Brokers.add(new Broker(ic_foxbit    , "FoxBit"                   , "Brasil", BlinktradeFoxbit                     .class, blinktrade_register_activities, blinktrade_login_activities));
        Brokers.add(new Broker(ic_foxbit    , "FoxBit Testnet"           , "Brasil", BlinktradeTestnetFoxbit              .class, blinktrade_register_activities, blinktrade_login_activities));
    }

    private Broker(int resource_id, String name, String country,
                   Class<? extends IExchangeAPI> api_class,
                   List<Class<? extends Activity>> register_activities,
                   List<Class<? extends Activity>> login_activities) {
        int id = 1;
        for (Broker b: Brokers)
            if (b.getID() >= id)
                id = b.getID()+1;
        mID                 = id;
        mImageResourceID    = resource_id;
        mName               = name;
        mCountry            = country;
        mAPIClass           = api_class;
        mRegisterActivities = register_activities;
        mLoginActivities    = login_activities;
    }
    public int getID() { return mID; }
    public int getImageResourceID() {
        return mImageResourceID;
    }
    public String getName() {
        return mName;
    }
    public String getCoutry() {
        return mCountry;
    }
    public Class<? extends IExchangeAPI> GetAPIClass() { return mAPIClass; }
    public List<Class<? extends Activity>> getRegisterActivities() { return mRegisterActivities; }
    public List<Class<? extends Activity>> getLoginActivities   () { return mLoginActivities; }
    public void setActivitySeries(List<Class<? extends Activity>> activities) {
        mActivitySeries = activities;
    }
    public Class<? extends Activity> GetNextActivity(Class<? extends Activity> current_activity) {
        for (int i = 0 ; i<mActivitySeries.size() ; i++)
            if (mActivitySeries.get(i) == current_activity
                    && i < mActivitySeries.size() + 1)
                return mActivitySeries.get(i+1);
        return mActivitySeries.get(0);
    }
    static public Broker GetBrokerByID(int id) {
        for (Broker b: Brokers) {
            if (id == b.getID())
                return b;
        }
        return null;
    }
}
