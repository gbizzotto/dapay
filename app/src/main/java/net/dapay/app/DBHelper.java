package net.dapay.app;

import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import net.dapay.app.API.ExchangeAPI;
import net.dapay.app.API.IExchangeAPI;
import net.dapay.app.util.CustomFuture;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * Created by gabriel on 12/7/16.
 */

public class DBHelper extends SQLiteOpenHelper implements Observer {

    Context mContext;
    NotificationManager mNotificationManager;

    private static DBHelper INSTANCE = null;
    private DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.mContext = context;
        mNotificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
    }
    public static DBHelper getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new DBHelper(context);
            INSTANCE.CacheProfiles();
        }
        return INSTANCE;
    }

    IExchangeAPI.LastDepositData mLastDepositData = new IExchangeAPI.LastDepositData();

    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "dapay.db";

    public static final String TABLE_NAME_PROFILE = "profiles";
    public static final String PROFILE_COLUMN_NAME_ID        = "id";
    public static final String PROFILE_COLUMN_NAME_LABEL     = "label";
    public static final String PROFILE_COLUMN_NAME_LOGIN     = "login";
    public static final String PROFILE_COLUMN_NAME_MARKET_ID = "market_id";
    public static final String PROFILE_COLUMN_NAME_ENABLED   = "enabled";
    public static final String PROFILE_COLUMN_NAME_ACTION    = "action";

    public static final String TABLE_NAME_BILL = "bills";
    public static final String BILL_COLUMN_NAME_ID = "id";
    public static final String BILL_COLUMN_NAME_PROFILE_ID    = "profile_id";
    public static final String BILL_COLUMN_NAME_WALLET_ID     = "wallet_id";
    public static final String BILL_COLUMN_NAME_TIMESTAMP     = "created_at";
    public static final String BILL_COLUMN_NAME_FIAT_AMOUNT   = "fiat_amount";
    public static final String BILL_COLUMN_NAME_CRYPTO_AMOUNT = "crypto_amount";
    public static final String BILL_COLUMN_NAME_LABEL         = "label";
    public static final String BILL_COLUMN_NAME_ACTION        = "action";
    public static final String BILL_COLUMN_NAME_ACTION_DONE   = "action_done";
    public static final String BILL_COLUMN_NAME_ENABLED       = "enabled";

    public static final String TABLE_NAME_DEPOSIT = "deposits";
    public static final String DEPOSIT_COLUMN_NAME_INTERNAL_ID   = "internal_id";
    public static final String DEPOSIT_COLUMN_NAME_WALLET_ID     = "wallet_id";
    public static final String DEPOSIT_COLUMN_NAME_DEPOSIT_ID    = "id";
    public static final String DEPOSIT_COLUMN_NAME_CRYPTO_AMOUNT = "deposited_amount";
    public static final String DEPOSIT_COLUMN_NAME_TIMESTAMP     = "timestamp";
    public static final String DEPOSIT_COLUMN_NAME_STATUS        = "status";

    ArrayList<Profile> mProfileList = new ArrayList<>();
    ArrayList<Bill> mBillList = new ArrayList<>();
    HashMap<String, Bill> mBillMap = new HashMap<>();

    private static final String SQL_CREATE_TABLE_PROFILES = "CREATE TABLE " + TABLE_NAME_PROFILE + " (" +
                                PROFILE_COLUMN_NAME_ID        + " INTEGER PRIMARY KEY ," +
                                PROFILE_COLUMN_NAME_LABEL     + " TEXT                ," +
                                PROFILE_COLUMN_NAME_LOGIN     + " TEXT                ," +
                                PROFILE_COLUMN_NAME_MARKET_ID + " INTEGER             ," +
                                PROFILE_COLUMN_NAME_ENABLED   + " BOOLEAN             ," +
                                PROFILE_COLUMN_NAME_ACTION    + " VARCHAR(10)         );";

    private static final String SQL_CREATE_TABLE_BILLS = "CREATE TABLE " + TABLE_NAME_BILL + " (" +
                                BILL_COLUMN_NAME_ID            + " INTEGER PRIMARY KEY," +
                                BILL_COLUMN_NAME_PROFILE_ID    + " INTEGER            ," +
                                BILL_COLUMN_NAME_WALLET_ID     + " TEXT               ," +
                                BILL_COLUMN_NAME_TIMESTAMP     + " INTEGER            ," +
                                BILL_COLUMN_NAME_FIAT_AMOUNT   + " REAL               ," +
                                BILL_COLUMN_NAME_CRYPTO_AMOUNT + " REAL               ," +
                                BILL_COLUMN_NAME_LABEL         + " TEXT               ," +
                                BILL_COLUMN_NAME_ACTION        + " VARCHAR(10)        ," +
                                BILL_COLUMN_NAME_ACTION_DONE   + " BOOLEAN            ," +
                                BILL_COLUMN_NAME_ENABLED       + " BOOLEAN            );";
    private static final String SQL_CREATE_TABLE_DEPOSITS = "CREATE TABLE " + TABLE_NAME_DEPOSIT + " (" +
                                DEPOSIT_COLUMN_NAME_INTERNAL_ID   + " INTEGER PRIMARY KEY," +
                                DEPOSIT_COLUMN_NAME_WALLET_ID     + " TEXT     ," +
                                DEPOSIT_COLUMN_NAME_DEPOSIT_ID    + " TEXT     ," +
                                DEPOSIT_COLUMN_NAME_TIMESTAMP     + " INTEGER  ," +
                                DEPOSIT_COLUMN_NAME_STATUS        + " INTEGER  ," +
                                DEPOSIT_COLUMN_NAME_CRYPTO_AMOUNT + " REAL     )";

    private static final String SQL_DELETE_PROFILES = "DROP TABLE IF EXISTS " + TABLE_NAME_PROFILE;
    private static final String SQL_DELETE_BILLS    = "DROP TABLE IF EXISTS " + TABLE_NAME_BILL;
    private static final String SQL_DELETE_DEPOSITS = "DROP TABLE IF EXISTS " + TABLE_NAME_DEPOSIT;

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE_PROFILES);
        db.execSQL(SQL_CREATE_TABLE_BILLS   );
        db.execSQL(SQL_CREATE_TABLE_DEPOSITS);
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public Bill insertBill(long timestamp, long profile_id, String wallet_id, double fiat_amount, double crypto_amount, String label, String action, boolean action_done)
    {
        if (mBillMap.containsKey(wallet_id))
            return null;

        SQLiteDatabase db = getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put(BILL_COLUMN_NAME_TIMESTAMP,     timestamp    );
        cv.put(BILL_COLUMN_NAME_PROFILE_ID,    profile_id   );
        cv.put(BILL_COLUMN_NAME_WALLET_ID,     wallet_id    );
        cv.put(BILL_COLUMN_NAME_FIAT_AMOUNT,   fiat_amount  );
        cv.put(BILL_COLUMN_NAME_CRYPTO_AMOUNT, crypto_amount);
        cv.put(BILL_COLUMN_NAME_LABEL,         label        );
        cv.put(BILL_COLUMN_NAME_ACTION,        action       );
        cv.put(BILL_COLUMN_NAME_ACTION_DONE,   action_done  );
        cv.put(BILL_COLUMN_NAME_ENABLED,       true         );

        Bill bill = new Bill(timestamp, profile_id, wallet_id, fiat_amount, crypto_amount, label, action, action_done);
        synchronized (mBillList) {
            int i;
            for (i=0 ; i<mBillList.size() ; i++) {
                if (timestamp > mBillList.get(i).getTimestamp())
                    break;
            }
            mBillList.add(i, bill);
            mBillMap.put(wallet_id, bill);
        }

        long row_id = db.insert(TABLE_NAME_BILL, null, cv);
        if (row_id != -1) {
            bill.setId(row_id);
            mLastDepositData.SetChanged();
            mLastDepositData.notifyObservers(bill);
            return bill;
        } else {
            return null;
        }
    }

    private void CacheProfiles() {
        SQLiteDatabase db = getReadableDatabase();

        { // scope variables
            String selectQuery = " SELECT  * FROM " + TABLE_NAME_PROFILE +
                    " WHERE "+ PROFILE_COLUMN_NAME_ENABLED +"=1" +
                    " ORDER BY " + PROFILE_COLUMN_NAME_ID + " DESC";
            Cursor cursor = db.rawQuery(selectQuery, null);
            mProfileList.clear();
            mProfileList.ensureCapacity(cursor.getCount());
            if (cursor.moveToFirst()) {
                do {
                    Profile profile = new Profile(cursor.getInt(cursor.getColumnIndex(PROFILE_COLUMN_NAME_ID)),
                            cursor.getString(cursor.getColumnIndex(PROFILE_COLUMN_NAME_LABEL    )),
                            cursor.getString(cursor.getColumnIndex(PROFILE_COLUMN_NAME_LOGIN    )),
                            cursor.getInt   (cursor.getColumnIndex(PROFILE_COLUMN_NAME_MARKET_ID)),
                            cursor.getString(cursor.getColumnIndex(PROFILE_COLUMN_NAME_ACTION   )));
                    mProfileList.add(profile);
                }
                while (cursor.moveToNext());
            }
            cursor.close();
        }
    }

    public void CacheBills(Profile profile)
    {
        SQLiteDatabase db = getWritableDatabase();

        // Getting whatever we have from DB
        synchronized (mBillList) {
            // Get Bills
            String selectQuery = "SELECT  * FROM " + TABLE_NAME_BILL +
                    " WHERE " + BILL_COLUMN_NAME_PROFILE_ID + "=" + profile.id +
                    " AND "   + BILL_COLUMN_NAME_ENABLED    + "= 1" +
                    " ORDER BY " + BILL_COLUMN_NAME_TIMESTAMP + " DESC";
            Cursor cursor = db.rawQuery(selectQuery, null);
            mBillList.clear();
            mBillMap.clear();
            mBillList.ensureCapacity(cursor.getCount());
            if (cursor.moveToFirst()) {
                do {
                    Bill bill = new Bill(cursor.getLong  (cursor.getColumnIndex(BILL_COLUMN_NAME_TIMESTAMP    )),
                                         cursor.getLong  (cursor.getColumnIndex(BILL_COLUMN_NAME_PROFILE_ID   )),
                                         cursor.getString(cursor.getColumnIndex(BILL_COLUMN_NAME_WALLET_ID    )),
                                         cursor.getDouble(cursor.getColumnIndex(BILL_COLUMN_NAME_FIAT_AMOUNT  )),
                                         cursor.getDouble(cursor.getColumnIndex(BILL_COLUMN_NAME_CRYPTO_AMOUNT)),
                                         cursor.getString(cursor.getColumnIndex(BILL_COLUMN_NAME_LABEL        )),
                                         cursor.getString(cursor.getColumnIndex(BILL_COLUMN_NAME_ACTION       )),
                                         cursor.getInt   (cursor.getColumnIndex(BILL_COLUMN_NAME_ACTION_DONE  ))==1);
                    mBillList.add(cursor.getPosition(), bill);
                    mBillMap.put(bill.getWalletID(), bill);
                }
                while (cursor.moveToNext());
            }
            cursor.close();

            // Get Deposits for each Bill
            for (Bill b: mBillList) {
                selectQuery = "SELECT  * FROM " + TABLE_NAME_DEPOSIT +
                              " WHERE " + DEPOSIT_COLUMN_NAME_WALLET_ID + "='"+b.getWalletID()+"'";
                cursor = db.rawQuery(selectQuery, null);
                if (cursor.moveToFirst()) {
                    do {
                        if (cursor.getDouble(cursor.getColumnIndex(DEPOSIT_COLUMN_NAME_CRYPTO_AMOUNT)) == 0.0d)
                            continue;
                        Bill.Deposit deposit = new Bill.Deposit(b.getWalletID(),
                                                                cursor.getString(cursor.getColumnIndex(DEPOSIT_COLUMN_NAME_DEPOSIT_ID   )),
                                                                cursor.getLong  (cursor.getColumnIndex(DEPOSIT_COLUMN_NAME_TIMESTAMP    )),
                                                                cursor.getDouble(cursor.getColumnIndex(DEPOSIT_COLUMN_NAME_CRYPTO_AMOUNT)),
                                                                cursor.getInt   (cursor.getColumnIndex(DEPOSIT_COLUMN_NAME_STATUS)));
                        if (b.InsertDeposit(deposit, false)) {
                            InsertDepositIntoDB(deposit);
                        } else if (b.UpdateDeposit(deposit, false)) {
                            UpdateDepositInDB(deposit);
                        }
                    }
                    while (cursor.moveToNext());
                }
                cursor.close();
            }

            // Updating what we got from DB with what we get from server
            Future<IExchangeAPI.ErrorOrPayload<Hashtable<String, ArrayList<Bill.Deposit>>>> deposits_future = ExchangeAPI.GetCurrentAPI().GetDepositStatus();
            IExchangeAPI.ErrorOrPayload<Hashtable<String, ArrayList<Bill.Deposit>>> maybe_deposits = null;
            try {
                maybe_deposits = deposits_future.get(20, TimeUnit.SECONDS);
            } catch (TimeoutException e){
                if (deposits_future instanceof CustomFuture) {
                    if (((CustomFuture)deposits_future).isOnItsWay()) {
                        // Data received but being processed locally, give it more time
                        try {
                            maybe_deposits = deposits_future.get();
                        } catch (Throwable t){
                        }
                    }
                }
            } catch (Throwable t){
            }

            if (maybe_deposits == null || maybe_deposits.getErrCode() != IExchangeAPI.ErrorOrPayload.ERROR_NONE)
                return;
            Hashtable<String, ArrayList<Bill.Deposit>> deposits_per_bill = maybe_deposits.getPayload();
            Enumeration<String> enumWallets = deposits_per_bill.keys();
            while(enumWallets.hasMoreElements()) {
                String wallet_id = enumWallets.nextElement();
                ArrayList<Bill.Deposit> deposits = deposits_per_bill.get(wallet_id);

                if ( ! mBillMap.containsKey(wallet_id))
                    continue;
                Bill bill = mBillMap.get(wallet_id);

                for (Bill.Deposit deposit: deposits)
                    if (bill.InsertDeposit(deposit, false)) {
                        InsertDepositIntoDB(deposit);
                    } else if (bill.UpdateDeposit(deposit, false)) {
                        UpdateDepositInDB(deposit);
                    }
            }
        }
    }
    public ArrayList<Bill> getAllBills() {
        return mBillList;
    }
    public Bill getBill(String wallet_address) {
        synchronized (mBillList) {
            return mBillMap.get(wallet_address);
        }
    }

    public ArrayList<Profile> getAllProfiles() { return mProfileList; }
    public Profile getProfile(long id) {
        for (Profile p: mProfileList) {
            if (id == p.id)
                return p;
        }
        return null;
    }

    public void update(Observable obs, Object obj) {
        if ( ! (obj instanceof Bill.Deposit))
            return;

        Bill.Deposit deposit = (Bill.Deposit)obj;
        if ( ! mBillMap.containsKey(deposit.wallet_id))
            return;

        Bill bill = mBillMap.get(deposit.wallet_id);
        if (bill.InsertDeposit(deposit, true)) {
            InsertDepositIntoDB(deposit);
            mLastDepositData.SetChanged();
            mLastDepositData.notifyObservers(deposit);
        } else if (bill.UpdateDeposit(deposit, true)) {
            UpdateDepositInDB(deposit);
            mLastDepositData.SetChanged();
            mLastDepositData.notifyObservers(deposit);
        }
        if (bill.getStatus() == Bill.STATUS_CONFIRMED) {
            IExchangeAPI api = ExchangeAPI.GetCurrentAPI();
            if (api != null &&  ! bill.getActionDone()) {
                switch (bill.getAction()) {
                    case Bill.ACTION_TAKE:
                        api.TakeMarket(bill.getCryptoAmount());
                        bill.setActionDone(true);
                        break;
                    case Bill.ACTION_MAKE:
                        api.MakeMarket(bill.getCryptoAmount());
                        bill.setActionDone(true);
                        break;
                }
            }
        }
    }

    private void InsertDepositIntoDB(Bill.Deposit deposit) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("INSERT INTO " + TABLE_NAME_DEPOSIT + "("+DEPOSIT_COLUMN_NAME_WALLET_ID    +","+
                                                             DEPOSIT_COLUMN_NAME_DEPOSIT_ID   +","+
                                                             DEPOSIT_COLUMN_NAME_TIMESTAMP    +","+
                                                             DEPOSIT_COLUMN_NAME_STATUS       +","+
                                                             DEPOSIT_COLUMN_NAME_CRYPTO_AMOUNT+")"+
                    " VALUES('" + deposit.wallet_id     + "', " +
                            "'" + deposit.id            + "', " +
                             "" + deposit.timestamp     + ", " +
                             "" + deposit.status        + ", " +
                             "" + deposit.crypto_amount + ")");
    }
    private void UpdateDepositInDB(Bill.Deposit deposit) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("UPDATE " + TABLE_NAME_DEPOSIT +
                " SET "   + DEPOSIT_COLUMN_NAME_STATUS       + "="  + deposit.status +
                ", "      + DEPOSIT_COLUMN_NAME_CRYPTO_AMOUNT + "="  + deposit.crypto_amount +
                " WHERE " + DEPOSIT_COLUMN_NAME_DEPOSIT_ID    + "='" + deposit.id + "'");
    }

    public void GetDepositUpdates(Observer o, boolean get_updates) {
        if (get_updates)
            mLastDepositData.addObserver(o);
        else
            mLastDepositData.deleteObserver(o);
    }

    public void AddProfile(Profile profile) {
        profile.id = (int) InsertProfileIntoDB(profile);
        mProfileList.add(0, profile);
    }

    protected long InsertProfileIntoDB(Profile profile) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues content_values = new ContentValues(3);
        content_values.put(PROFILE_COLUMN_NAME_LABEL    , profile.label);
        content_values.put(PROFILE_COLUMN_NAME_LOGIN    , profile.login);
        content_values.put(PROFILE_COLUMN_NAME_MARKET_ID, profile.broker_id);
        content_values.put(PROFILE_COLUMN_NAME_ENABLED  , true);
        content_values.put(PROFILE_COLUMN_NAME_ACTION   , profile.action);
        return db.insert(TABLE_NAME_PROFILE, null, content_values);
    }

    public void disableProfile(Profile profile) {
        mProfileList.remove(profile);

        SQLiteDatabase db = getWritableDatabase();
        ContentValues content_values = new ContentValues(3);
        content_values.put(PROFILE_COLUMN_NAME_ENABLED, false);
        db.update(TABLE_NAME_PROFILE, content_values, PROFILE_COLUMN_NAME_ID+"=?", new String[]{""+profile.id});
    }

    public void disableBill(Bill bill) {
        mBillList.remove(bill);
        mBillMap.remove(bill.getWalletID());

        SQLiteDatabase db = getWritableDatabase();
        ContentValues content_values = new ContentValues(3);
        content_values.put(BILL_COLUMN_NAME_ENABLED, false);
        db.update(TABLE_NAME_BILL, content_values, BILL_COLUMN_NAME_ID+"=?", new String[]{""+bill.getId()});
    }

    public void updateProfileAction(Profile profile) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues content_values = new ContentValues(3);
        content_values.put(PROFILE_COLUMN_NAME_ACTION, profile.action);
        db.update(TABLE_NAME_PROFILE, content_values, PROFILE_COLUMN_NAME_ID+"=?", new String[]{""+profile.id});
    }
}
