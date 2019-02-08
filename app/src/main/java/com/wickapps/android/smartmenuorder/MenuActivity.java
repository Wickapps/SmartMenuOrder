/*
 * Copyright (C) 2019 Mark Wickham
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wickapps.android.smartmenuorder;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ScaleXSpan;
import android.text.style.StyleSpan;
import android.view.*;
import android.view.View.OnClickListener;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import com.epson.EpsonCom.EpsonCom;
import com.epson.EpsonCom.EpsonCom.ALIGNMENT;
import com.epson.EpsonCom.EpsonCom.ERROR_CODE;
import com.epson.EpsonCom.EpsonCom.FONT;
import com.epson.EpsonCom.EpsonComDevice;
import com.epson.EpsonCom.EpsonComDeviceParameters;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.internal.MemoryPersistence;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.squareup.picasso.Picasso.get;

public class MenuActivity extends Activity {

    private WifiLock mWifiLock;

    SharedPreferences prefs;
    Editor prefEdit;

    // MQTT params
    private MemoryPersistence mMemStore; // On Fail reverts to MemoryStore
    private MqttConnectOptions mOpts; // Connection Options
    private MqttClient mClient; // Mqtt Client
    private static final boolean MQTT_CLEAN_SESSION = true; // Start a clean session?
    private static final String MQTT_USER = "mqttuser";
    private static final char[] MQTT_PASSWORD = new char[]{'a', 'b', 'c', '1', '2', '3'};
    private static final int MQTT_QOS_0 = 0; // QOS Level 0 (Delivery Once no confirmation)
    private static final int MQTT_QOS_1 = 1; // QOS Level 1 (Delivery at least Once with confirmation)
    private static final int MQTT_QOS_2 = 2; // QOS Level 2 (Delivery only once with confirmation with handshake)

    private static ListView list;
    private static ListView listCat;

    private static PicSimpleAdapter adapter1;
    private static ArrayAdapter<?> adapter4;
    private static Locale lc;

    private static LinearLayout lltop, llMain, llOption, llExtra, lldish;
    private static TextView txt1, txt2;
    private static EditText specET;

    private static String OrderTotalString;

    private static ImageView icon;

    private StateListDrawable states;

    private EditText etPassword1, etPassword2, etPassword3, etPassword4;
    private GenericTextWatcher watcher1, watcher2, watcher3, watcher4;

    private ViewFlipper vfTop;
    public static final int TOP_MENU = 0;
    public static final int TOP_SENT = 1;
    public static final int TOP_TABLES = 2;
    public static final int TOP_SPECIALS = 3;
    public static final int TOP_GUESTS = 4;
    public static final int TOP_NOT_SENT = 5;
    public static final int TOP_DISH_PIC = 6;

    public static ViewFlipper vfMenu;
    public static final int FLIP_MENU = 0;
    public static final int FLIP_ORDER_LIST = 1;
    public static final int FLIP_DISH_DETAIL = 2;
    public static final int FLIP_DISH_PIC = 3;

    private static String orderItem, orderItemAlt, orderDesc, priceOptionName, priceOptionNameAlt;
    private static String orderSpecIns = "";
    private static int priceUnitBase, priceUnitTotal, priceQtyTotal, dishQty, priceUnitTotalFull, priceDiscount;

    private static String formatTicket1;
    private static String formatTicket1Tot;
    private static String formatTicket2;
    private static String formatTicket3;

    public static ArrayList<Boolean> P2Filter = new ArrayList<Boolean>();
    public static ArrayList<Boolean> P3Filter = new ArrayList<Boolean>();

    private static JSONArray JSONOrderAry = null;
    private static JSONArray JSONDishAry = null;
    private static JSONArray JSONOptionsAry = null;
    private static JSONArray JSONExtrasAry = null;

    private static String JSONOrderStr = "";
    private static ArrayList<JSONArray> JSONORDERLIST = new ArrayList<JSONArray>();

    private File ordersDir;
    private File retryDir;

    private ConnectionLog mLog;

    //EpsonCom Objects
    private static EpsonComDevice POSDev;
    private static EpsonComDeviceParameters POSParams;
    private static ERROR_CODE err;

    private static String[] menuItem = new String[]{};
    private static String[] rmbItem = new String[]{};
    private static String[] rmbItemAlt = new String[]{};
    private static String[] optionsItem = new String[]{};
    private static String[] extrasItem = new String[]{};
    private static String[] optionsAll = new String[]{};
    private static String[] extrasAll = new String[]{};
    private static String[] categoryAll = new String[]{};
    private static String[] kitchenLines = new String[]{};

    private static String itemCat;
    private static String itemCatAlt;
    private static Integer itemCatId;
    private static Boolean ItemCounterOnly;

    public static final ArrayList<Integer> MenuPosition = new ArrayList<Integer>();
    public static ArrayList<String> CategoryEng = new ArrayList<String>();
    public static ArrayList<String> CategoryAlt = new ArrayList<String>();

    private static ListView listOrder;
    private static OrderAdapter orderAdapter;

    private static Boolean sentPOS1, sentPOS2, sentPOS3, feedbackSent, orderFullySent, orderPartialSent;

    private static int mainFontSize;

    private static int MaxTABLES = 45;
    private static int TakeOutTable = 44;
    private static int MaxGuests = 15;
    private static String[] guestNumbers = new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15"};
    private static Boolean allowAccess;

    private static RadioGroup mRadioGroupMain;
    private static RadioGroup[] mRadioGroupOption = new RadioGroup[]{};
    private static RadioButton[][] rbO = new RadioButton[][]{};
    private static CheckBox[][] cb0 = new CheckBox[][]{};
    private static int numOptions;
    private static int numExtras;

    @Override
    protected void onDestroy() {
        list.setOnItemClickListener(null);
        list.setAdapter(null);
        Global.MenuStarted = false;
        mWifiLock.release();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        prefEdit.commit();
        super.onPause();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.INVISIBLE);
        }
    }

    //	Need handler for callbacks to the UI thread
    final Handler mHandler = new Handler();

    //	Create runnable for posting
    final Runnable mUpdateResults = new Runnable() {
        public void run() {
            updateOrderView();
            if (orderFullySent) {
                orderFullySent = false;
                // display the ORDER SENT View
                updateOrderSentView();
                vfTop.setDisplayedChild(TOP_SENT);
            }
            if (feedbackSent) {
                feedbackSent = false;
                // display the MENU View
                menuSelect();
                orderUnselect();
                infoUnselect();
                msgUnselect();
                vfMenu.setDisplayedChild(FLIP_MENU);
            }
        }
    };

    final Runnable feedbackNotSent = new Runnable() {
        public void run() {
            failedAuth0();
        }
    };

    final Runnable mUpdateNotSent = new Runnable() {
        public void run() {
            updateOrderNotSentView();
            vfTop.setDisplayedChild(TOP_NOT_SENT);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefEdit = prefs.edit();

        Global.MenuStarted = true;
        allowAccess = false;

        try {
            mLog = new ConnectionLog(this);
        } catch (IOException e) {
        }

        WifiManager lWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            mWifiLock = lWifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "LockTag");
        } else {
            mWifiLock = lWifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, "LockTag");
        }
        mWifiLock.acquire();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            View mDecorView = getWindow().getDecorView();
            mDecorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.INVISIBLE);
            mDecorView.setOnSystemUiVisibilityChangeListener(
                    new View.OnSystemUiVisibilityChangeListener() {
                        @Override
                        public void onSystemUiVisibilityChange(int flags) {
                            getWindow().getDecorView().setSystemUiVisibility(
                                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                            | View.INVISIBLE);
                        }
                    });
        }

        // grab the directory where orders and retrys will be stored
        ordersDir = new File(getFilesDir(), "SmartMenuOrders");
        if (!ordersDir.exists()) ordersDir.mkdirs();
        retryDir = new File(getFilesDir(), "SmartMenuRetry");
        if (!retryDir.exists()) retryDir.mkdirs();

        optionsAll = Global.OPTIONSTXT.split("\\n");
        extrasAll = Global.EXTRASTXT.split("\\n");
        categoryAll = Global.CATEGORYTXT.split("\\n");
        menuItem = Global.MENUTXT.split("\\n");
        kitchenLines = Global.KITCHENTXT.split("\\n");

        orderFullySent = false;
        orderPartialSent = false;
        feedbackSent = false;

        mainFontSize = Utils.getFontSize(MenuActivity.this);

        setContentView(R.layout.menu_layout);

        // start with the TABLES View
        vfTop = (ViewFlipper) findViewById(R.id.master);
        vfTop.setDisplayedChild(TOP_TABLES);

        // start with the MENU VIEW
        vfMenu = (ViewFlipper) findViewById(R.id.details);
        vfMenu.setDisplayedChild(FLIP_MENU);

        processMenu();
        setupTablesView();
        setupHeaders();

        lltop = (LinearLayout) findViewById(R.id.LLTab_Menu);
        lltop.setBackgroundColor(Global.BackColor);
        LinearLayout ll1 = (LinearLayout) findViewById(R.id.LLmenutabsM);
        LinearLayout ll2 = (LinearLayout) ll1.findViewById(R.id.LLmenuHeader);
        ll2.setBackgroundColor(Global.HeaderColor);

        // grab the icon from the server
        String fetchURL = Global.ProtocolPrefix + Global.ServerIP + "/" + Global.SMID + "/" + Global.LogoName;
        icon = (ImageView) findViewById(R.id.ImageView_Header);
        // Lazy load the image with Picasso
        get()
                .load(fetchURL)
                .placeholder(R.drawable.nopic)
                .error(R.drawable.nopic)
                .into(icon);

        menuSelect();
        orderUnselect();
        infoUnselect();
        msgUnselect();

        Button butExit = (Button) findViewById(R.id.butInnerExit);
        butExit.setTextSize(mainFontSize);
        butExit.setTextColor(Global.ButFontColor);
        butExit.setText(getString(R.string.tab3_exit));
        states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Global.LabelFontColor));
        states.addState(new int[]{}, new ColorDrawable(Global.ButColor));
        butExit.setBackgroundDrawable(states);
        butExit.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!JSONORDERLIST.isEmpty() & (!orderFullySent)) {
                    AlertDialog alertDialog = new AlertDialog.Builder(MenuActivity.this).create();
                    alertDialog.setTitle(getString(R.string.tab3_contains_items_title));
                    alertDialog.setMessage(getString(R.string.tab3_contains_items_text));
                    alertDialog.setButton2(getString(R.string.tab3_exit), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            setupNewOrder();
                            vfTop.setDisplayedChild(TOP_TABLES);
                            vfMenu.setDisplayedChild(FLIP_MENU);
                        }
                    });
                    alertDialog.setButton(getString(R.string.tab3_back), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
                    alertDialog.show();
                } else {
                    setupNewOrder();
                    vfTop.setDisplayedChild(TOP_TABLES);
                }
            }
        });

        // Set up the BIG MENU Button
        LinearLayout butMenu = (LinearLayout) findViewById(R.id.LLmenu);
        butMenu.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                vfMenu.setDisplayedChild(FLIP_MENU);
                menuSelect();
                orderUnselect();
                infoUnselect();
                msgUnselect();
            }
        });

        // Set up the BIG ORDER LIST Button
        LinearLayout butOrd = (LinearLayout) findViewById(R.id.LLorder);
        butOrd.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                updateOrderView();
                vfMenu.setDisplayedChild(FLIP_ORDER_LIST);
                orderSelect();
                menuUnselect();
                infoUnselect();
                msgUnselect();
            }
        });

        Button btn1 = (Button) findViewById(R.id.butLangEng);
        btn1.setTextSize(mainFontSize);
        btn1.setTextColor(Global.ButFontColor);
        states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Global.LabelFontColor));
        states.addState(new int[]{}, new ColorDrawable(Global.ButColor));
        btn1.setBackgroundDrawable(states);
        btn1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Configuration config = getBaseContext().getResources().getConfiguration();
                lc = new Locale("en");
                Locale.setDefault(lc);
                config.locale = lc;
                getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
                Global.EnglishLang = true;
                updateMenuView();
                updateOrderView();
                setupHeaders();
                menuSelect();
                orderUnselect();
                infoUnselect();
                msgUnselect();
                vfMenu.setDisplayedChild(FLIP_MENU);
            }
        });

        btn1 = (Button) findViewById(R.id.butLangAlt);
        btn1.setTextSize(mainFontSize);
        btn1.setTextColor(Global.ButFontColor);
        states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Global.LabelFontColor));
        states.addState(new int[]{}, new ColorDrawable(Global.ButColor));
        btn1.setBackgroundDrawable(states);
        btn1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Configuration config = getBaseContext().getResources().getConfiguration();
                lc = new Locale("zh");
                Locale.setDefault(lc);
                config.locale = lc;
                getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
                Global.EnglishLang = false;
                updateMenuView();
                updateOrderView();
                setupHeaders();
                menuSelect();
                orderUnselect();
                infoUnselect();
                msgUnselect();
                vfMenu.setDisplayedChild(FLIP_MENU);
            }
        });

        // set up the SEND button
        Button send = (Button) findViewById(R.id.sendbutton);
        send.setTextSize((float) (mainFontSize * 1.4));
        send.setText(getString(R.string.tab3_send));
        states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Global.ButColor));
        states.addState(new int[]{}, new ColorDrawable(Global.SelButColor));
        send.setBackgroundDrawable(states);
        send.setTextColor(Global.SelButFontColor);

        updateMenuView();
        updateOrderView();

        itemSetup(0);

        // add all the category buttons to the CatList on the left
        listCat = (ListView) findViewById(R.id.CatList);

        // set the Cat List on the left to be 21% of screen width
        LinearLayout llCatList = (LinearLayout) findViewById(R.id.LLCatList);
        final float WIDE = this.getResources().getDisplayMetrics().widthPixels;
        int valueWide = (int) (WIDE * 0.21f);
        llCatList.setLayoutParams(new LinearLayout.LayoutParams(valueWide, LayoutParams.WRAP_CONTENT));

        listCat.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView parent, View v, final int position, long id) {
                list.setSelection(MenuPosition.get(position));
            }
        });


        list = (ListView) findViewById(R.id.list);
        list.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView parent, View v, final int position, long id) {
                itemSetup(position);
                showPopup(position);
            }
        });

        //send.setOnClickListener(new View.OnClickListener() {
        send.setOnClickListener(new DebouncedOnClickListener(2500) {
            public void onDebouncedClick(View v) {
                if (JSONORDERLIST.isEmpty()) {
                    AlertDialog alertDialog = new AlertDialog.Builder(MenuActivity.this).create();
                    alertDialog.setTitle(getString(R.string.tab3_empty_title));
                    alertDialog.setMessage(getString(R.string.tab3_empty_text));
                    alertDialog.setButton(getString(R.string.tab3_back), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
                    alertDialog.show();
                } else {
                    // Disable the button to prevent double sends
                    //Button tmpsend =(Button) findViewById(R.id.sendbutton);
                    //tmpsend.setEnabled(false);
                    // build the tickets
                    formatTicket1 = formatTicket(JSONOrderAry.toString(), 1);
                    formatTicket1Tot = "Total: " + getOrderTotalRMB() + ".00";
                    formatTicket2 = formatTicket(JSONOrderAry.toString(), 2);
                    formatTicket3 = formatTicket(JSONOrderAry.toString(), 3);

                    // update the time on the ORDER ID
                    String curTime = Utils.GetTime();
                    Global.SendTime = curTime;

                    final ProgressDialog pd = ProgressDialog.show(MenuActivity.this, getString(R.string.tab3_sending_title), getString(R.string.tab3_sending_p1), true, false);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                        pd.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                | View.INVISIBLE);
                    new Thread(new Runnable() {
                        public void run() {
                            // pack the order details into JSON for network sending mode or storage
                            try {
                                // Update the Order JSON with potential updated informations
                                jsonSetter(JSONOrderAry, "sendtime", Global.SendTime);
                                jsonSetter(JSONOrderAry, "ticketnum", "N/A");
                                jsonSetter(JSONOrderAry, "guests", Global.Guests);

                                if (Global.TableID == TakeOutTable) {
                                    // Flag this as a TO order so it can be printed properly by the POS App
                                    // Order App orders usually get sent up as source=2
                                    // See OrderSource[] in TakeOut App for all values
                                    jsonSetter(JSONOrderAry, "source", 5);
                                    // No reliable guest info for the instore Take Out so override it
                                    Global.Guests = "";
                                    jsonSetter(JSONOrderAry, "guests", "");
                                }

                                JSONObject JSONdishObj = JSONOrderAry.getJSONObject(jsonGetter3(JSONOrderAry, "dishes"));
                                //log("JSON dishes Obj=" + JSONdishObj.toString(1));
                                JSONArray JSONdishesAry = JSONdishObj.getJSONArray("dishes");
                                int numdish = JSONdishesAry.length();
                                //log("Number of dishes=" + numdish);

                                JSONOrderStr = JSONOrderAry.toString();
                                JSONOrderStr = JSONOrderStr + "\r\n";
                                //log("JSONOrderStr=" + JSONOrderStr);
                            } catch (JSONException e) {
                                log("JSONOrder Exception");
                            }
                            // Local Order Print
                            if (Global.SendOrderMode == 1) {
                                // print the order directly to the printers
                                sendPrinter1();

                                int count2 = unprintedDishCount(true, 2);
                                int count3 = unprintedDishCount(true, 3);

                                if (count2 > 0) sendPrinter2();
                                    // Fake it cause we didnt need to print on P2/P3
                                else sentPOS2 = true;

                                if (count3 > 0) sendPrinter3();
                                else sentPOS3 = true;

                                // send the order after all prints completed
                                try {
                                    if ((!Global.POS1Enable || sentPOS1) & (!Global.POS2Enable || sentPOS2) & (!Global.POS3Enable || sentPOS3)) {
                                        //log("all printed- sending");
                                        sendOrderThread();
                                        JSONORDERLIST.clear();
                                        orderFullySent = true;
                                        orderPartialSent = false;
                                        mHandler.post(mUpdateResults);
                                    } else {
                                        //log("Partial- not sent");
                                        orderPartialSent = true;
                                        mHandler.post(mUpdateResults);
                                    }
                                } catch (Exception e) {
                                    log("Network send failed");
                                    orderPartialSent = true;
                                    mHandler.post(mUpdateResults);
                                }
                            } else if (Global.SendOrderMode == 2) {
                                // Network Send
                                // Loop for socket retrys with Break to exit instead of Finally
                                int count = 0;
                                while (true) {
                                    Socket s = null;
                                    try {
                                        // Check the Table to see where to send the order
                                        if (Global.TableID != TakeOutTable) {
                                            // get the ticket number from the network
                                            s = new Socket(Global.POSIp, Global.POSSocket);
                                        } else {
                                            s = new Socket(Global.TakeOutIp, Global.POSSocket);
                                        }
                                        s.setSoTimeout(Global.ConnectTimeout);
                                        s.setKeepAlive(false);
                                        PrintWriter output = new PrintWriter(s.getOutputStream(), true);
                                        //log("Socket send=" + JSONOrderStr);
                                        output.println(JSONOrderStr);
                                        //output.flush();
                                        // Check if we have the ACK from POS or TO
                                        BufferedReader input = new BufferedReader(new InputStreamReader(s.getInputStream()));
                                        String st = input.readLine();
                                        if (st.equalsIgnoreCase("SMACK")) {
                                            //log("Recieved expected SMACK");
                                            // clear the order
                                            JSONORDERLIST.clear();
                                            orderFullySent = true;
                                            orderPartialSent = false;
                                            pd.dismiss();
                                            mHandler.post(mUpdateResults);
                                            // Cleanup the socket before we break
                                            if (s != null) {
                                                try {
                                                    s.close();
                                                } catch (IOException eeee) {
                                                    log("Close exception ee=" + eeee);
                                                }
                                            }
                                            break;
                                        } else {
                                            log("expected SMACK not received");
                                            pd.dismiss();
                                            mHandler.post(mUpdateNotSent);
                                            // Clean up the socket before the Break
                                            if (s != null) {
                                                try {
                                                    s.close();
                                                } catch (IOException ee) {
                                                    log("Close exception ee=" + ee);
                                                }
                                            }
                                            break;
                                        }
                                    } catch (Exception e) {
                                        count++;
                                        if (count < Global.SocketRetry) {
                                            log("Socket2 Retry count=" + count + " e=" + e);
                                            try {
                                                Thread.sleep(Global.SocketRetrySleep);
                                            } catch (InterruptedException e1) {
                                                log("Sleep exception2");
                                            }
                                        } else {
                                            log("Network send failed after retrys=" + count + " e=" + e);
                                            orderPartialSent = true;
                                            mHandler.post(mUpdateNotSent);
                                            // Cleanup the socket before we break
                                            if (s != null) {
                                                try {
                                                    s.close();
                                                } catch (IOException ee) {
                                                    log("Close exception ee=" + ee);
                                                }
                                            }
                                            break;
                                        }
                                    }
                                }
                            } else if (Global.SendOrderMode == 3) {
                                // MMQT send
                                try {
                                    String url = String.format(Locale.US, Global.BrokerURLFormat, Global.ServerIP, Global.BrokerPort);
                                    mMemStore = new MemoryPersistence();
                                    mClient = new MqttClient(url, Global.DeviceId, mMemStore);
                                    //log("MQTT Connecting with URL=" + url + " DeviceId=" + Global.DeviceId);
                                    // publish the order to the topic
                                    MqttTopic orderTopic = mClient.getTopic(Global.BrokerOrderTopic);
                                    //log("MQTT orderTopic=" + Global.BrokerTopic);
                                    MqttMessage message = new MqttMessage(JSONOrderStr.getBytes());
                                    message.setQos(MQTT_QOS_2);
                                    //log("MQTT message.tostr=" + message.toString());
                                    //mClient.connect();
                                    mOpts = new MqttConnectOptions();
                                    mOpts.setCleanSession(MQTT_CLEAN_SESSION);
                                    mOpts.setUserName(MQTT_USER);
                                    mOpts.setPassword(MQTT_PASSWORD);
                                    mClient.connect(mOpts);
                                    orderTopic.publish(message);
                                    mClient.disconnect();
                                    log("Sent MQTT Order. Topic=" + orderTopic.getName() + " length=" + JSONOrderStr.length());
                                    // success
                                    JSONORDERLIST.clear();
                                    orderFullySent = true;
                                    orderPartialSent = false;
                                    mHandler.post(mUpdateResults);
                                } catch (MqttException e) {
                                    log("MQTT Exception=" + e);
                                    orderPartialSent = true;
                                    mHandler.post(mUpdateNotSent);
                                }
                            }
                            pd.dismiss();
                        } // thread close
                    }).start();
                } // has items close
            } // send onClick close
        });
    }

    private void sendOrderThread() {
        new Thread(new Runnable() {
            public void run() {
                int statusCode = -1;
                if (haveNetworkConnection()) {
                    String postURL = Global.ProtocolPrefix + Global.ServerIP + Global.PosSaveOrderJsonURL;
                    statusCode = Utils.SendMultipartJsonOrder(postURL, JSONOrderStr, Global.SMID);
                    // write the file
                    String savefname = Global.OrderId + ".txt";
                    if (statusCode == 200) {
                        writeOutFile(ordersDir, savefname, JSONOrderStr);
                    } else {
                        writeOutFile(retryDir, savefname, JSONOrderStr);
                    }
                } else {
                    // add it to the retry directory
                    String fname = Global.OrderId + ".txt";
                    writeOutFile(retryDir, fname, JSONOrderStr);
                }
                //log("sent sc=" + statusCode);
            }
        }).start();
    }

    private void itemSetup(final int position) {
        // setup the Global Strings so the popup options have what they need
        String[] menuItem = Global.MENUTXT.split("\\n");
        String line = menuItem[position];
        String[] menuColumns = line.split("\\|");

        String cat = menuColumns[1].trim();
        itemCatId = categoryGetIndex(cat);
        itemCat = CategoryEng.get(categoryGetIndex(cat)).trim();
        itemCatAlt = CategoryAlt.get(categoryGetIndex(cat)).trim();

        // If they want to over ride the category filters to select printer, store the flag
        String typeFlags = menuColumns[0];
        if (typeFlags.substring(5, 6).equals("1")) ItemCounterOnly = true;
        else ItemCounterOnly = false;

        // we have our array of columns for the selected line, now set up the language specific fields using the divider "\"
        String[] itemColumns = menuColumns[2].split("\\\\");
        String[] descColumns = menuColumns[4].split("\\\\");
        String[] rmbColumns = menuColumns[5].split("\\\\");

        if (!Global.EnglishLang) {
            orderDesc = descColumns[1];
        } else {
            orderDesc = descColumns[0];
        }
        orderItem = itemColumns[0];
        orderItemAlt = itemColumns[1];
        rmbItem = rmbColumns[0].split("%");
        rmbItemAlt = rmbColumns[1].split("%");

        String optionColumns = menuColumns[7];
        String extraColumns = menuColumns[8];
        // need to get rid of the /r at the end of the last column
        extraColumns = extraColumns.substring(0, extraColumns.length() - 1);

        optionsItem = optionColumns.split("%");
        extrasItem = extraColumns.split("%");

        //Log.v("SETUP", "optionCol=" + optionColumns);
        //Log.v("SETUP", "optionsItem[0]=" + optionsItem[0]);
        //Log.v("SETUP", "len=" + optionsItem.length);
        //Log.v("SETUP", "extraCol=" + extraColumns + " extrasItem[0]=" + extrasItem[0]+ " len=" + extrasItem.length);
        //Log.v("SETUP", "itemCat=" + itemCat);
        //Log.v("SETUP", "-----");
    }

    public void showPopup(final int position) {
        lldish = (LinearLayout) findViewById(R.id.LLShowDish);
        lldish.setBackgroundColor(Global.BackColor);

        // lets scale the title on the popup box
        String tit = orderItem;
        if (!Global.EnglishLang) tit = orderItemAlt;

        // set a device specific line height spacing for radio buttons and font size
        int lheight = Utils.getLineHeight(MenuActivity.this);
        int fsize = mainFontSize;
        int fsizebig = (int) (mainFontSize * 1.3);
        int fsizesmall = (int) (mainFontSize / 1.4);

        txt1 = (TextView) findViewById(R.id.TextDishName);
        txt1.setText(tit);
        txt1.setTextSize(fsizebig);
        txt1.setTextColor(Global.FontColor);
        txt1.setVisibility(View.VISIBLE);

        txt2 = (TextView) findViewById(R.id.TextDishDesc);
        txt2.setText(orderDesc);
        txt2.setTextSize(fsizesmall);
        txt2.setTextColor(Global.FontColor);
        if (Global.ShowDishDescriptions) {
            txt2.setVisibility(View.VISIBLE);
        } else {
            txt2.setVisibility(View.GONE);
        }

        // load the pic
        String fetchURL = Global.fetchURL200.get(position);
        String clickURL = Global.fetchURL800.get(position);
        ImageView img = (ImageView) findViewById(R.id.ImgDishPic);
        img.setLayoutParams(new LinearLayout.LayoutParams((int) (Utils.getWidth(MenuActivity.this) / 6.0), (int) (Utils.getWidth(MenuActivity.this) / 4.0)));
        // Lazy load the image with Picasso
        get()
                .load(fetchURL)
                .placeholder(R.drawable.nopic)
                .error(R.drawable.nopic)
                .into(img);

        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // set up the single picture ZOOM view
                final ImageView bigimage = (ImageView) findViewById(R.id.singleImg);

                vfMenu.setDisplayedChild(FLIP_DISH_PIC);
                // set image
                String imageurl = Global.fetchURL800.get(position);
                // Lazy load the image with Picasso
                get()
                        .load(imageurl)
                        .into(bigimage);

                bigimage.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        //Animation slide_out_left = AnimationUtils.loadAnimation(MenuActivity.this, R.anim.outtoleft);
                        //MenuActivity.vfMenu.setOutAnimation(slide_out_left);
                        MenuActivity.vfMenu.setDisplayedChild(FLIP_DISH_DETAIL);
                    }
                });
                // Text at the bottom of the Image is not needed since we have an icon in the top right
                //TextView backtxt = (TextView) findViewById(R.id.singleImageBack);
                //backtxt.setTextSize((float) (mainFontSize*1.4));
                //backtxt.setText(getString(R.string.tab3_backtext));
                //backtxt.setTextColor(Color.parseColor("#FFFFFF"));
                //backtxt.setShadowLayer((float) 0.01,3,3,Color.parseColor("#000000"));
            }
        });

        if (!Global.ShowPics) {
            img.setVisibility(View.GONE);
        }

        LinearLayout.LayoutParams layoutParams = new RadioGroup.LayoutParams(RadioGroup.LayoutParams.WRAP_CONTENT, lheight);
        LinearLayout.LayoutParams layoutParams2 = new RadioGroup.LayoutParams(RadioGroup.LayoutParams.WRAP_CONTENT, (int) (lheight * 1.5));

        // set up the main dish radiogroup
        llMain = (LinearLayout) findViewById(R.id.llMain);
        llMain.removeAllViews();
        addDividerLine(llMain);
        mRadioGroupMain = new RadioGroup(MenuActivity.this);
        llMain.addView(mRadioGroupMain, 0);
        // set up the buttons
        RadioButton[] rbM = new RadioButton[rmbItem.length];
        // work through the items backwards so the first item "index=0" is top of the radio group
        for (int i = rmbItem.length - 1; i >= 0; i--) {
            rbM[i] = new RadioButton(MenuActivity.this);
            rbM[i].setId(i);
            rbM[i].setButtonDrawable(R.drawable.resize_button);
            rbM[i].setChecked(false);
            if (!Global.EnglishLang) {
                rbM[i].setText(rmbItemAlt[i]);
            } else rbM[i].setText(rmbItem[i]);
            rbM[i].setTextSize(fsize);
            rbM[i].setTextColor(Global.FontColor);
            mRadioGroupMain.addView(rbM[i], 0, layoutParams);
        }
        rbM[0].setChecked(true);
        addTextHeader(llMain, getString(R.string.tab3_chooseone));

        // set up the Options
        //Log.v("WHY0", "optionsItem[0]=" + optionsItem[0]);
        numOptions = 0;        // support multiple option groups per dish
        llOption = (LinearLayout) findViewById(R.id.llOptions);
        llOption.removeAllViews();

        if (!optionsItem[0].equalsIgnoreCase("none")) {
            numOptions = optionsItem.length;

            //Log.v("WHY1", "numOptions=" + numOptions);
            // set up radio buttons for each of the groups
            rbO = new RadioButton[5][25];
            mRadioGroupOption = new RadioGroup[5];
            for (int j = 0; j < numOptions; j++) {
                addDividerLine(llOption);
                mRadioGroupOption[j] = new RadioGroup(MenuActivity.this);
                llOption.addView(mRadioGroupOption[j], 0);
                // get the index of the option
                int oo = optionsGetIndex(optionsItem[j]);
                //Log.v("WHY1", "oo=" + oo);
                // get the options into an array parsing by the %
                String line = optionsAll[oo];
                String[] optColumns = line.split("\\|");
                String[] Opt = optColumns[1].split("\\\\");
                String[] OptDetail = Opt[0].split("%");        // english
                String[] OptDetailAlt = Opt[1].split("%");    // alt language
                //Log.v("WHY1", "OPTDETAIL[0]=" + OptDetail[0]);
                //Log.v("WHY2", "LEN OPTDETAIL[0]=" + OptDetail[0].length());
                // set up the buttons
                for (int i = OptDetail.length - 1; i >= 0; i--) {
                    rbO[j][i] = new RadioButton(MenuActivity.this);
                    rbO[j][i].setId(i);
                    rbO[j][i].setButtonDrawable(R.drawable.resize_button);
                    rbO[j][i].setChecked(false);
                    if (!Global.EnglishLang) {
                        rbO[j][i].setText(OptDetailAlt[i]);
                    } else rbO[j][i].setText(OptDetail[i]);
                    rbO[j][i].setTextSize(fsize);
                    rbO[j][i].setTextColor(Global.FontColor);
                    mRadioGroupOption[j].addView(rbO[j][i], 0, layoutParams);
                }
                rbO[j][0].setChecked(true);
                addTextHeader(llOption, getString(R.string.tab3_chooseone));
            }
        }

        // set up the EXTRAS check boxes
        numExtras = 0;
        llExtra = (LinearLayout) findViewById(R.id.llExtras);
        llExtra.removeAllViews();

        if (!extrasItem[0].equalsIgnoreCase("none")) {
            numExtras = extrasItem.length;

            // set up CHECKboxes for Extras
            cb0 = new CheckBox[5][25];
            //addDividerLine(llExtra);
            for (int j = 0; j < numExtras; j++) {
                // get the index of the extra
                int ee = extrasGetIndex(extrasItem[j]);
                // get the options into an array parsing by the %
                String line = extrasAll[ee];
                String[] extColumns = line.split("\\|");
                String[] Ext = extColumns[1].split("\\\\");
                String[] ExtDetail = Ext[0].split("%");        // english
                String[] ExtDetailAlt = Ext[1].split("%");    // alt language
                // set up the checkboxes
                for (int i = ExtDetail.length - 1; i >= 0; i--) {
                    cb0[j][i] = new CheckBox(MenuActivity.this);
                    cb0[j][i].setTextSize(fsize);
                    cb0[j][i].setTextColor(Global.FontColor);
                    if (!Global.EnglishLang) {
                        cb0[j][i].setText(ExtDetailAlt[i]);
                    } else cb0[j][i].setText(ExtDetail[i]);
                    llExtra.addView(cb0[j][i], 0, layoutParams);
                }
            }
            addTextHeader(llExtra, getString(R.string.tab3_chooseany));
        }

        //set up button for DISH QTY
        dishQty = 1;
        final Button buttonq = (Button) findViewById(R.id.RightButtonQTY);
        buttonq.setTextSize(mainFontSize);
        buttonq.setText(getString(R.string.tab3_dishqty) + " " + "\u2460");
        states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Global.SelButColor));
        states.addState(new int[]{}, new ColorDrawable(Global.ButColor));
        buttonq.setBackgroundDrawable(states);
        buttonq.setTextColor(Global.ButFontColor);
        buttonq.setMaxWidth((int) (Utils.getWidth(MenuActivity.this) / 5.0));
        buttonq.setMinimumWidth((int) (Utils.getWidth(MenuActivity.this) / 5.0));
        //buttonq.setTag("1");	// default to QTY=1

        buttonq.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                final Dialog dialogqty = new Dialog(MenuActivity.this);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                    dialogqty.getWindow().getDecorView().setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                    | View.INVISIBLE);

                dialogqty.setContentView(R.layout.qty_popup);
                dialogqty.setCanceledOnTouchOutside(true);
                dialogqty.setCancelable(true);

                final RadioButton rbut1 = (RadioButton) dialogqty.findViewById(R.id.but_qty1);
                final RadioButton rbut2 = (RadioButton) dialogqty.findViewById(R.id.but_qty2);
                final RadioButton rbut3 = (RadioButton) dialogqty.findViewById(R.id.but_qty3);
                final RadioButton rbut4 = (RadioButton) dialogqty.findViewById(R.id.but_qty4);
                final RadioButton rbut5 = (RadioButton) dialogqty.findViewById(R.id.but_qty5);

                if (dishQty == 1) rbut1.setChecked(true);
                if (dishQty == 2) rbut2.setChecked(true);
                if (dishQty == 3) rbut3.setChecked(true);
                if (dishQty == 4) rbut4.setChecked(true);
                if (dishQty == 5) rbut5.setChecked(true);

                String tit = getString(R.string.tab3_select_qty);
                SpannableStringBuilder ssBuilser = new SpannableStringBuilder(tit);
                StyleSpan span = new StyleSpan(Typeface.NORMAL);
                ScaleXSpan span1 = new ScaleXSpan(2);
                ssBuilser.setSpan(span, 0, tit.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                ssBuilser.setSpan(span1, 0, tit.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                dialogqty.setTitle(ssBuilser);

                //set up button for UPDATE QTY
                Button buttonqty = (Button) dialogqty.findViewById(R.id.update_but);
                buttonqty.setText(getString(R.string.tab3_update));
                buttonqty.setTextSize(mainFontSize);
                buttonqty.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        RadioGroup qtyRadioGroup = (RadioGroup) dialogqty.findViewById(R.id.GroupQTY);
                        RadioButton butsel = (RadioButton) dialogqty.findViewById(qtyRadioGroup.getCheckedRadioButtonId());
                        dishQty = Integer.valueOf(butsel.getText().toString());
                        if (dishQty == 1) {
                            rbut1.setChecked(true);
                            buttonq.setText(getString(R.string.tab3_dishqty) + " " + "\u2460");
                        }
                        if (dishQty == 2) {
                            rbut2.setChecked(true);
                            buttonq.setText(getString(R.string.tab3_dishqty) + " " + "\u2461");
                        }
                        if (dishQty == 3) {
                            rbut3.setChecked(true);
                            buttonq.setText(getString(R.string.tab3_dishqty) + " " + "\u2462");
                        }
                        if (dishQty == 4) {
                            rbut4.setChecked(true);
                            buttonq.setText(getString(R.string.tab3_dishqty) + " " + "\u2463");
                        }
                        if (dishQty == 5) {
                            rbut5.setChecked(true);
                            buttonq.setText(getString(R.string.tab3_dishqty) + " " + "\u2464");
                        }
                        dialogqty.dismiss();
                    }
                });
                dialogqty.show();
            }
        });

        //set up button for SPECIAL INS
        orderSpecIns = "";
        final Button buttonsi = (Button) findViewById(R.id.RightButtonSI);
        buttonsi.setTextSize(mainFontSize);
        buttonsi.setText(getString(R.string.tab3_dishspecialinstructions));
        states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Global.SelButColor));
        states.addState(new int[]{}, new ColorDrawable(Global.ButColor));
        buttonsi.setBackgroundDrawable(states);
        buttonsi.setTextColor(Global.ButFontColor);
        buttonsi.setTag("");
        buttonsi.setMaxWidth((int) (Utils.getWidth(MenuActivity.this) / 5.0));
        buttonsi.setMinimumWidth((int) (Utils.getWidth(MenuActivity.this) / 5.0));
        buttonsi.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                final Dialog dialogAS = new Dialog(MenuActivity.this);

                dialogAS.setContentView(R.layout.special_instruction);
                dialogAS.setCancelable(true);
                dialogAS.setCanceledOnTouchOutside(true);

                // lets scale the title on the popup box
                String tit = getString(R.string.special_ins_title);
                int fsize = mainFontSize;
                SpannableStringBuilder ssBuilser = new SpannableStringBuilder(tit);
                StyleSpan span = new StyleSpan(Typeface.NORMAL);
                ScaleXSpan span1 = new ScaleXSpan(2);
                ssBuilser.setSpan(span, 0, tit.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                ssBuilser.setSpan(span1, 0, tit.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                dialogAS.setTitle(ssBuilser);

                TextView AStext = (TextView) dialogAS.findViewById(R.id.SItext);
                if (!Global.EnglishLang) {
                    AStext.setText(getString(R.string.special_ins_text1) + orderItemAlt);
                } else {
                    AStext.setText(getString(R.string.special_ins_text1) + orderItem);
                }
                AStext.setTextSize(mainFontSize);
                AStext.setTextColor(Global.FontColor);
                // edit text box is next
                Button AScancel = (Button) dialogAS.findViewById(R.id.SIcancel);
                AScancel.setTextSize(mainFontSize);
                AScancel.setText(getString(R.string.tab2_si_cancel));
                AScancel.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        dialogAS.dismiss();
                    }
                });
                Button ASsave = (Button) dialogAS.findViewById(R.id.SIadd);
                ASsave.setTextSize(mainFontSize);
                ASsave.setText(getString(R.string.tab2_si_save));
                ASsave.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        specET = (EditText) dialogAS.findViewById(R.id.SIedit);
                        String specins = specET.getText().toString();
                        specins = specins.replaceAll("[^\\p{L}\\p{N}\\s]", "");
                        orderSpecIns = specins;
                        buttonsi.setTag(orderSpecIns);
                        //buttonsi.setText(getString(R.string.tab3_dishspecialinstructions) + " " + "\u26AB");	// medium middle dot
                        buttonsi.setText(getString(R.string.tab3_dishspecialinstructions) + " " + "\u25CF");    // large black circle
                        // Clear soft keyboard
                        specET.clearFocus();
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (imm != null) imm.hideSoftInputFromWindow(specET.getWindowToken(), 0);
                        dialogAS.dismiss();
                    }
                });
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                    dialogAS.getWindow().getDecorView().setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                    | View.INVISIBLE);
                specET = (EditText) dialogAS.findViewById(R.id.SIedit);
                specET.setText(buttonsi.getTag().toString());
                dialogAS.show();
            }
        });

        //set up button for BACK TO MENU
        Button button1 = (Button) findViewById(R.id.RightButtonBack);
        button1.setTextSize(mainFontSize);
        button1.setText(getString(R.string.tab3_backtomenu));
        states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Global.ButColor));
        states.addState(new int[]{}, new ColorDrawable(Global.SelButColor));
        button1.setBackgroundDrawable(states);
        button1.setTextColor(Global.SelButFontColor);
        button1.setMaxWidth((int) (Utils.getWidth(MenuActivity.this) / 5.0));
        button1.setMinimumWidth((int) (Utils.getWidth(MenuActivity.this) / 5.0));
        button1.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                ///dialog.dismiss();
                vfMenu.setDisplayedChild(FLIP_MENU);
            }
        });

        //set up button for DISH INFORMATION
        Button buttoni = (Button) findViewById(R.id.LeftButtonInformation);
        buttoni.setTextSize(mainFontSize);
        buttoni.setText(getString(R.string.tab3_dishinfo));
        states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Global.SelButColor));
        states.addState(new int[]{}, new ColorDrawable(Global.ButColor));
        buttoni.setBackgroundDrawable(states);
        buttoni.setTextColor(Global.ButFontColor);
        buttoni.setMinimumWidth((int) (Utils.getWidth(MenuActivity.this) / 6.0));
        buttoni.setMaxWidth((int) (Utils.getWidth(MenuActivity.this) / 6.0));
        buttoni.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                //vfMenu.setDisplayedChild(0);
            }
        });
        if (!Global.ShowDishInfo) buttoni.setVisibility(View.GONE);

        //set up button for DISH FEEDBACK
        Button buttonf = (Button) findViewById(R.id.LeftButtonFeedback);
        buttonf.setTextSize(mainFontSize);
        buttonf.setText(getString(R.string.tab3_dishfeedback));
        states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Global.SelButColor));
        states.addState(new int[]{}, new ColorDrawable(Global.ButColor));
        buttonf.setBackgroundDrawable(states);
        buttonf.setTextColor(Global.ButFontColor);
        buttonf.setMinimumWidth((int) (Utils.getWidth(MenuActivity.this) / 6.0));
        buttonf.setMaxWidth((int) (Utils.getWidth(MenuActivity.this) / 6.0));
        buttonf.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                //vfMenu.setDisplayedChild(0);
            }
        });
        if (!Global.ShowDishFeedback) buttonf.setVisibility(View.GONE);

        //set up the button for ADD TO ORDER
        Button button = (Button) findViewById(R.id.RightButtonAdd);
        button.setTextSize(mainFontSize);
        button.setText(getString(R.string.tab3_addtoorder));
        states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Global.ButColor));
        states.addState(new int[]{}, new ColorDrawable(Global.SelButColor));
        button.setBackgroundDrawable(states);
        button.setTextColor(Global.SelButFontColor);
        button.setMaxWidth((int) (Utils.getWidth(MenuActivity.this) / 5.0));
        button.setMinimumWidth((int) (Utils.getWidth(MenuActivity.this) / 5.0));
        button.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // Create the initial JSON order and Options and Extras holders
                JSONOptionsAry = new JSONArray();
                JSONExtrasAry = new JSONArray();

                // Reset the dish price
                priceUnitTotal = 0;

                // load up the dish main option (price selector)
                int orderChoice = mRadioGroupMain.getCheckedRadioButtonId();
                // until we have a JSON menu, we need to use the following functions to get our pricing numbers
                priceOptionName = removeRMBnumber(rmbItem[orderChoice]);
                priceOptionNameAlt = removeRMBnumber(rmbItemAlt[orderChoice]);

                // load up the dish options (non-price) if available
                if (numOptions > 0) {
                    for (int j = 0; j < numOptions; j++) {
                        int orderSecondary = mRadioGroupOption[j].getCheckedRadioButtonId();
                        int oo = optionsGetIndex(optionsItem[j]);
                        String line = optionsAll[oo];
                        String[] optColumns = line.split("\\|");
                        String[] Opt = optColumns[1].split("\\\\");
                        String[] OptDetail = Opt[0].split("%");        // english only for the ticket
                        String[] OptDetailAlt = Opt[1].split("%");    // alt language
                        String orderSecondaryTxt = OptDetail[orderSecondary].trim();
                        String orderSecondaryTxtAlt = OptDetailAlt[orderSecondary].trim();
                        try {
                            // Append each Dish Option to the global array
                            JSONArray aryO = new JSONArray();
                            aryO.put(createInt("optionId", orderSecondary));
                            aryO.put(createInt("optionPrice", getRMBnumber(orderSecondaryTxt)));
                            priceUnitTotal = priceUnitTotal + getRMBnumber(orderSecondaryTxt);
                            aryO.put(createStr("optionName", removeRMBnumber(orderSecondaryTxt)));
                            aryO.put(createStr("optionNameAlt", removeRMBnumber(orderSecondaryTxtAlt)));
                            JSONOptionsAry.put(aryO);    // append the dish options
                        } catch (JSONException e) {
                            log("JSON Add Dish Options Exception=" + e);
                        }
                    }
                }

                // load up the dish extras
                if (numExtras > 0) {
                    for (int j = 0; j < numExtras; j++) {
                        int ee = extrasGetIndex(extrasItem[j]);
                        String line = extrasAll[ee];
                        String[] extColumns = line.split("\\|");
                        String[] Ext = extColumns[1].split("\\\\");
                        String[] ExtDetail = Ext[0].split("%");        // english
                        String[] ExtDetailAlt = Ext[1].split("%");    // alt language
                        for (int i = ExtDetail.length - 1; i >= 0; i--) {
                            if (cb0[j][i].isChecked()) {
                                String ExtDet = ExtDetail[i].trim();            // english only for the ticket
                                String ExtDetAlt = ExtDetailAlt[i].trim();    // english only for the ticket
                                try {
                                    // Append selected Dish Extras to the global array
                                    JSONArray aryE = new JSONArray();
                                    aryE.put(createInt("extraId", j));
                                    aryE.put(createStr("extraItem", extrasItem[j]));
                                    aryE.put(createInt("extraIndex", ee));
                                    aryE.put(createInt("extraPrice", getRMBnumber(ExtDet)));
                                    priceUnitTotal = priceUnitTotal + getRMBnumber(ExtDet);
                                    aryE.put(createStr("extraName", removeRMBnumber(ExtDet)));
                                    aryE.put(createStr("extraNameAlt", removeRMBnumber(ExtDetAlt)));
                                    JSONExtrasAry.put(aryE);    // append the dish extras
                                } catch (JSONException e) {
                                    log("JSON Add Dish Extras Exception=" + e);
                                }
                            }
                        }
                    }
                }

                // add it to the order list
                Toast.makeText(MenuActivity.this, "Added to Order:\n" + orderItem, Toast.LENGTH_SHORT).show();

                // note that priceUnitTotal may already contain value from the Options and Extras processing
                priceUnitBase = getRMBnumber(rmbItem[orderChoice]);
                priceUnitTotal = priceUnitTotal + priceUnitBase;
                priceUnitTotalFull = priceUnitTotal;    // Undiscounted price for future discount calculations in the POS app
                priceDiscount = 100;                    // no discounts in this app
                priceQtyTotal = priceUnitTotal * dishQty;

                vfMenu.setDisplayedChild(FLIP_MENU);

                // update the number of items on the Order Tab counter
                ((TextView) findViewById(R.id.numItems)).setText("" + JSONORDERLIST.size());

                try {
                    // update the JSON with this item
                    JSONObject JSONdishObj = JSONOrderAry.getJSONObject(jsonGetter3(JSONOrderAry, "dishes"));
                    JSONArray JSONdishesAry = JSONdishObj.getJSONArray("dishes");

                    JSONDishAry = new JSONArray();
                    JSONDishAry.put(createInt("dishId", position));
                    JSONDishAry.put(createStr("dishName", orderItem));
                    JSONDishAry.put(createStr("dishNameAlt", orderItemAlt));
                    JSONDishAry.put(createStr("categoryName", itemCat));
                    JSONDishAry.put(createStr("categoryNameAlt", itemCatAlt));
                    JSONDishAry.put(createInt("categoryId", itemCatId));
                    JSONDishAry.put(createInt("priceOptionId", orderChoice));
                    JSONDishAry.put(createStr("priceOptionName", priceOptionName));
                    JSONDishAry.put(createStr("priceOptionNameAlt", priceOptionNameAlt));
                    JSONDishAry.put(createInt("qty", dishQty));
                    JSONDishAry.put(createInt("priceUnitBase", priceUnitBase));
                    JSONDishAry.put(createInt("priceUnitTotal", priceUnitTotal));
                    JSONDishAry.put(createInt("priceUnitTotalFull", priceUnitTotalFull));
                    JSONDishAry.put(createInt("priceDiscount", priceDiscount));
                    JSONDishAry.put(createInt("priceQtyTotal", priceQtyTotal));
                    JSONDishAry.put(createStr("specIns", orderSpecIns));
                    JSONDishAry.put(createBoolean("dishPrinted", false));
                    JSONDishAry.put(createBoolean("counterOnly", ItemCounterOnly));

                    // Add the dish Options which were built when they were selected ...
                    JSONObject aryO = new JSONObject();
                    aryO.put("options", JSONOptionsAry);
                    JSONDishAry.put(aryO);

                    // Add the dish Extras which were built when they were selected ...
                    JSONObject aryE = new JSONObject();
                    aryE.put("extras", JSONExtrasAry);
                    JSONDishAry.put(aryE);

                    JSONdishesAry.put(JSONDishAry);    // append this dish to the JSON dishes
                    JSONObject ary = new JSONObject();    // new object to store the new dishes
                    ary.put("dishes", JSONdishesAry);
                    // Replace the JSON dishes Object in the JSON order
                    JSONOrderAry.put(jsonGetter3(JSONOrderAry, "dishes"), ary);

                    // update the total price of the order
                    ary = new JSONObject();
                    ary.put("ordertotal", updateOrderTotalRMB());
                    JSONOrderAry.put(jsonGetter3(JSONOrderAry, "ordertotal"), ary);

                    // update the number of items on the Order Tab counter
                    ((TextView) findViewById(R.id.numItems)).setText("" + JSONdishesAry.length());
                } catch (JSONException e) {
                    log("JSON Add Dish Exception=" + e);
                }
            }
        });
        vfMenu.setDisplayedChild(FLIP_DISH_DETAIL);
    }

    private void setUpCatList() {
        String tmpCat = "";
        MenuPosition.clear();

        // Loop through each line and populate the the Menu Positions
        for (int i = 0; i < menuItem.length; i++) {
            // parse each line into columns using the divider character "|"
            String[] menuColumns = menuItem[i].split("\\|");
            // need to get rid of the /r at the end of the last column
            ///////menuColumns[7] = menuColumns[7].substring(0,menuColumns[7].length()-1);
            // get category
            String catColumns = menuColumns[1];
            // if we have a new category, then add to the linked list
            if (!tmpCat.equals(catColumns)) {
                // add the new cat to the linked list
                MenuPosition.add(i);
                tmpCat = catColumns;
            }
        }
        // Loop through each line of cat file and populate both the Category String Arrays
        CategoryEng.clear();
        CategoryAlt.clear();
        // also populate the printer filter arrays
        P2Filter.clear();
        P3Filter.clear();
        for (int i = 0; i < categoryAll.length; i++) {
            String line = categoryAll[i];
            String[] linColumns = line.split("\\|");
            String[] linLang = linColumns[1].split("\\\\");
            // if there are no special, then we dont want to add the special to the category selector
            // even though the specials category still always resides in the TXT file
            if ((Global.NumSpecials > 0) || !(linLang[0].equalsIgnoreCase("Specials"))) {
                CategoryEng.add(linLang[0]);
                CategoryAlt.add(linLang[1]);
                // print filters arrays
                if (Global.P2FilterCats.contains(linColumns[0])) {
                    P2Filter.add(i, true);
                } else {
                    P2Filter.add(i, false);
                }
                if (Global.P3FilterCats.contains(linColumns[0])) {
                    P3Filter.add(i, true);
                } else {
                    P3Filter.add(i, false);
                }
            }
        }
        //log("p2f=" + P2Filter);
        //log("p3f=" + P3Filter);
    }

    // below is the over ride that will disable the back button
    public void onBackPressed() {
        if (vfMenu.getDisplayedChild() == FLIP_DISH_DETAIL) vfMenu.setDisplayedChild(FLIP_MENU);
        if (vfMenu.getDisplayedChild() == FLIP_DISH_PIC) vfMenu.setDisplayedChild(FLIP_DISH_DETAIL);
    }

    private class CatListAdapter extends ArrayAdapter<String> {
        private ArrayList<String> items;

        public CatListAdapter(MenuActivity menuActivity, int textViewResourceId, ArrayList<String> items) {
            super(getBaseContext(), textViewResourceId, items);
            this.items = items;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.cat_item, null);
            }
            String o = items.get(position);
            if (o != null) {
                TextView tt = (TextView) v.findViewById(R.id.cat_item_title);
                if (tt != null) {
                    tt.setText(o);
                    tt.setTextSize((float) (mainFontSize / 1.2));
                    tt.setPadding(2, 2, 2, 2);
                    tt.setTextColor(Global.LabelFontColor);
                    tt.setSingleLine();
                }
            }
            return v;
        }
    }

    private void addDividerLine(LinearLayout ll) {
        LinearLayout mll;
        mll = ll;
        // add divider line
        ImageView imageLine = new ImageView(MenuActivity.this);
        imageLine.setBackgroundResource(R.drawable.bar_white);
        mll.addView(imageLine, 0);
    }

    private void addTextHeader(LinearLayout ll, String str) {
        LinearLayout mll;
        String mstr;
        mll = ll;
        mstr = str;
        // add text view
        TextView tv = new TextView(MenuActivity.this);
        tv.setText(mstr);
        tv.setTextSize(mainFontSize);
        tv.setTextColor(Global.FontColor);
        tv.setPadding(0, 10, 0, 10);
        mll.addView(tv, 0);
    }

    // Scan though the optionsItem array, find the str, return the location index
    private int optionsGetIndex(String str) {
        int found = 0;
        for (int i = 0; i < optionsAll.length; i++) {
            if (str.equalsIgnoreCase(optionsAll[i].substring(0, str.length()))) {
                found = i;
                break;
            }
        }
        return found;
    }

    // Scan though the extrasItem array, find the str, return the location index
    private int extrasGetIndex(String str) {
        int found = 0;
        for (int i = 0; i < extrasAll.length; i++) {
            if (str.equalsIgnoreCase(extrasAll[i].substring(0, str.length()))) {
                found = i;
                break;
            }
        }
        return found;
    }

    // Scan though the Category array, find the str, return the location index
    private int categoryGetIndex(String str) {
        int found = 0;
        for (int i = 0; i < categoryAll.length; i++) {
            if (str.equalsIgnoreCase(categoryAll[i].substring(0, str.length()))) {
                found = i;
                break;
            }
        }
        return found;
    }

    // set the MENU button as Selected
    private void menuSelect() {
        states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Global.ButColor));
        states.addState(new int[]{}, new ColorDrawable(Global.SelButColor));
        ((LinearLayout) findViewById(R.id.LLmenu)).setBackgroundDrawable(states);

        TextView text = (TextView) findViewById(R.id.TextMenu);
        text.setText(getString(R.string.tab2name));
        text.setTextSize((float) (mainFontSize * 1.4));
        text.setTextColor(Global.SelButFontColor);
    }

    // set the MENU button as Unselected
    private void menuUnselect() {
        states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Global.SelButColor));
        states.addState(new int[]{}, new ColorDrawable(Global.ButColor));
        ((LinearLayout) findViewById(R.id.LLmenu)).setBackgroundDrawable(states);

        TextView text = (TextView) findViewById(R.id.TextMenu);
        text.setText(getString(R.string.tab2name));
        text.setTextSize((float) (mainFontSize * 1.4));
        text.setTextColor(Global.ButFontColor);
    }

    // set the ORDER button as Selected
    private void orderSelect() {
        states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Global.ButColor));
        states.addState(new int[]{}, new ColorDrawable(Global.SelButColor));
        ((LinearLayout) findViewById(R.id.LLorder)).setBackgroundDrawable(states);

        TextView text = (TextView) findViewById(R.id.TextOrder);
        text.setText(getString(R.string.tab3name));
        text.setTextSize((float) (mainFontSize * 1.4));
        text.setTextColor(Global.SelButFontColor);

        text = (TextView) findViewById(R.id.numItems);
        text.setTextSize((float) (mainFontSize * 1.4));
        text.setTextColor(Global.SelButFontColor);
    }

    // set the ORDER button as Unselected
    private void orderUnselect() {
        states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Global.SelButColor));
        states.addState(new int[]{}, new ColorDrawable(Global.ButColor));
        ((LinearLayout) findViewById(R.id.LLorder)).setBackgroundDrawable(states);

        TextView text = (TextView) findViewById(R.id.TextOrder);
        text.setText(getString(R.string.tab3name));
        text.setTextSize((float) (mainFontSize * 1.4));
        text.setTextColor(Global.ButFontColor);

        text = (TextView) findViewById(R.id.numItems);
        text.setTextSize((float) (mainFontSize * 1.4));
        text.setTextColor(Global.ButFontColor);
    }

    // set the INFO button as Selected
    private void infoSelect() {
        states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Global.ButColor));
        states.addState(new int[]{}, new ColorDrawable(Global.SelButColor));
        ((Button) findViewById(R.id.butInfo)).setBackgroundDrawable(states);
        Button but = (Button) findViewById(R.id.butInfo);
        but.setTextColor(Global.SelButFontColor);
    }

    // set the INFO button as Unselected
    private void infoUnselect() {
        states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Global.SelButColor));
        states.addState(new int[]{}, new ColorDrawable(Global.ButColor));
        ((Button) findViewById(R.id.butInfo)).setBackgroundDrawable(states);
        Button but = (Button) findViewById(R.id.butInfo);
        but.setTextColor(Global.ButFontColor);
    }

    // set the MSG button as Selected
    private void msgSelect() {
        states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Global.ButColor));
        states.addState(new int[]{}, new ColorDrawable(Global.SelButColor));
        ((Button) findViewById(R.id.butQuickMsg)).setBackgroundDrawable(states);
        Button but = (Button) findViewById(R.id.butQuickMsg);
        but.setTextColor(Global.SelButFontColor);
    }

    // set the MSG button as Unselected
    private void msgUnselect() {
        states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Global.SelButColor));
        states.addState(new int[]{}, new ColorDrawable(Global.ButColor));
        ((Button) findViewById(R.id.butQuickMsg)).setBackgroundDrawable(states);
        Button but = (Button) findViewById(R.id.butQuickMsg);
        but.setTextColor(Global.ButFontColor);
    }

    private class OrderAdapter extends ArrayAdapter<JSONArray> {

        private ArrayList<JSONArray> items;
        private int orderFontSize;
        private int orderFontSizeSmall;
        private int orderFontSizeLarge;

        public OrderAdapter(MenuActivity placeOrder, int textViewResourceId, ArrayList<JSONArray> items) {
            super(getBaseContext(), textViewResourceId, items);
            this.items = items;
            orderFontSize = (mainFontSize);
            orderFontSizeSmall = (int) (mainFontSize / 1.2);
            orderFontSizeLarge = (int) (mainFontSize * 1.4);
        }

        public View getView(final int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.list_item, null);
            }
            // Setup for the dialog items
            JSONArray o = items.get(position);

            if (o != null) {
                TextView qt = (TextView) v.findViewById(R.id.list_item_qty);
                String quantity = jsonGetter2(o, "qty").toString();
                qt.setText("x " + quantity);
                qt.setTextSize(orderFontSizeSmall);
                qt.setTextColor(Global.FontColor);

                TextView up = (TextView) v.findViewById(R.id.list_item_unitprice);
                up.setText(jsonGetter2(o, "priceUnitTotal").toString());
                up.setTextSize(orderFontSizeSmall);
                up.setTextColor(Global.FontColor);

                TextView qp = (TextView) v.findViewById(R.id.list_item_qtyprice);
                qp.setText(jsonGetter2(o, "priceQtyTotal").toString() + ".00");
                qp.setTextSize(orderFontSizeSmall);
                qp.setTextColor(Global.FontColor);

                // Set the main dish title with price option + options + extras + special instructions
                TextView tt = (TextView) v.findViewById(R.id.list_item_title);
                TextView st = (TextView) v.findViewById(R.id.list_item_subtitle);
                if ((tt != null) & (st != null)) {
                    tt.setTextSize(orderFontSizeSmall);
                    tt.setTextColor(Global.FontColor);
                    st.setTextSize(orderFontSizeSmall);
                    st.setTextColor(Global.FontColor);

                    // Start with dish name
                    String dishtext;
                    if (Global.EnglishLang) dishtext = jsonGetter2(o, "dishName").toString();
                    else dishtext = jsonGetter2(o, "dishNameAlt").toString();
                    tt.setText(dishtext);

                    // Handle price option
                    String dishsubtext = "";
                    String priceopt;
                    if (Global.EnglishLang) priceopt = jsonGetter2(o, "priceOptionName").toString();
                    else priceopt = jsonGetter2(o, "priceOptionNameAlt").toString();
                    if (priceopt.length() > 0) {
                        dishsubtext = dishsubtext + priceopt;
                        dishsubtext = dishsubtext + "\n";
                    }

                    try {
                        // Add all the Option choices
                        JSONObject dishopt = new JSONObject();
                        dishopt = o.getJSONObject(jsonGetter3(o, "options"));
                        JSONArray dishoptAry = dishopt.getJSONArray("options");
                        //log("opt=" + dishoptAry.toString(1));
                        if (dishoptAry.length() > 0) {
                            // Loop print
                            for (int i = 0; i < dishoptAry.length(); i++) {
                                //dishtext = dishtext + dishoptAry.getString(i);
                                // Grab just the optionName
                                if (Global.EnglishLang)
                                    dishsubtext = dishsubtext + jsonGetter2(dishoptAry.getJSONArray(i), "optionName").toString();
                                else
                                    dishsubtext = dishsubtext + jsonGetter2(dishoptAry.getJSONArray(i), "optionNameAlt").toString();
                                if (i != dishoptAry.length() - 1) dishsubtext = dishsubtext + ", ";
                            }
                            dishsubtext = dishsubtext + "\n";
                        }
                        // Add selected Extra choices
                        JSONObject dishext = new JSONObject();
                        dishext = o.getJSONObject(jsonGetter3(o, "extras"));
                        JSONArray dishextAry = dishext.getJSONArray("extras");
                        //log("ext=" + dishextAry.toString(1));
                        if (dishextAry.length() > 0) {
                            // Loop print
                            for (int i = 0; i < dishextAry.length(); i++) {
                                //dishtext = dishtext + dishextAry.getString(i);
                                // Grab just the extraName
                                if (Global.EnglishLang)
                                    dishsubtext = dishsubtext + jsonGetter2(dishextAry.getJSONArray(i), "extraName").toString();
                                else
                                    dishsubtext = dishsubtext + jsonGetter2(dishextAry.getJSONArray(i), "extraNameAlt").toString();
                                if (i != dishextAry.length() - 1) dishsubtext = dishsubtext + ", ";
                            }
                            dishsubtext = dishsubtext + "\n";
                        }
                    } catch (JSONException e) {
                        log("JSON Opt+Ext Exception=" + e);
                    }

                    // Handle special Instructions
                    String specins = jsonGetter2(o, "specIns").toString();
                    if (specins.length() > 0) {
                        //dishsubtext = dishsubtext + "\n";
                        dishsubtext = dishsubtext + "Special: " + specins;
                    }
                    st.setText(dishsubtext);
                }

                final Button butEdit = (Button) v.findViewById(R.id.OrderButtonEdit);
                states = new StateListDrawable();
                states.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Global.SelButColor));
                states.addState(new int[]{}, new ColorDrawable(Global.ButColor));
                butEdit.setTextSize((float) (orderFontSize / 1.3));
                butEdit.setBackgroundDrawable(states);
                butEdit.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        // update the dish selections
                        final Dialog dialogedit = new Dialog(MenuActivity.this);

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                            dialogedit.getWindow().getDecorView().setSystemUiVisibility(
                                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                            | View.INVISIBLE);

                        dialogedit.setContentView(R.layout.edit_item_popup);
                        dialogedit.setCanceledOnTouchOutside(true);
                        dialogedit.setCancelable(true);

                        // title of the Edit item popup
                        String tit = getString(R.string.tab3_item_edit);
                        SpannableStringBuilder ssBuilser = new SpannableStringBuilder(tit);
                        StyleSpan span = new StyleSpan(Typeface.NORMAL);
                        ScaleXSpan span1 = new ScaleXSpan(2);
                        ssBuilser.setSpan(span, 0, tit.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                        ssBuilser.setSpan(span1, 0, tit.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                        dialogedit.setTitle(ssBuilser);

                        JSONArray o = items.get(position);

                        Integer did = (Integer) jsonGetter2(o, "dishId");
                        itemSetup(did);
                        //log("did=" + did);

                        int lheight = Utils.getLineHeight(MenuActivity.this);
                        int fsize = mainFontSize;
                        LinearLayout.LayoutParams layoutParams = new RadioGroup.LayoutParams(RadioGroup.LayoutParams.WRAP_CONTENT, lheight);

                        // Display Dish Name
                        String dname = "";
                        if (Global.EnglishLang) dname = jsonGetter2(o, "dishName").toString();
                        else dname = jsonGetter2(o, "dishNameAlt").toString();
                        final TextView tvname = (TextView) dialogedit.findViewById(R.id.TextEditItemDishName);
                        tvname.setText(dname);
                        tvname.setTextSize(orderFontSizeLarge);
                        tvname.setTextColor(Global.FontColor);

                        // Display the price option state
                        llMain = (LinearLayout) dialogedit.findViewById(R.id.llEditItemMain);
                        llMain.removeAllViews();
                        addDividerLine(llMain);
                        mRadioGroupMain = new RadioGroup(MenuActivity.this);
                        llMain.addView(mRadioGroupMain, 0);
                        // set up the buttons
                        RadioButton[] rbM = new RadioButton[rmbItem.length];
                        Integer priceoptionid = (Integer) jsonGetter2(o, "priceOptionId");
                        // work through the items backwards so the first item "index=0" is top of the radio group
                        for (int i = rmbItem.length - 1; i >= 0; i--) {
                            rbM[i] = new RadioButton(MenuActivity.this);
                            rbM[i].setId(i);
                            rbM[i].setButtonDrawable(R.drawable.resize_button);
                            rbM[i].setChecked(false);
                            if (!Global.EnglishLang) {
                                rbM[i].setText(rmbItemAlt[i]);
                            } else rbM[i].setText(rmbItem[i]);
                            rbM[i].setTextSize(fsize);
                            rbM[i].setTextColor(Global.FontColor);
                            mRadioGroupMain.addView(rbM[i], 0, layoutParams);
                        }
                        rbM[priceoptionid].setChecked(true);

                        // Display the options
                        numOptions = 0;        // support multiple option groups per dish
                        llOption = (LinearLayout) dialogedit.findViewById(R.id.llEditItemOptions);
                        llOption.removeAllViews();
                        if (!optionsItem[0].equalsIgnoreCase("none")) {
                            numOptions = optionsItem.length;
                            // set up radio buttons for each of the groups
                            rbO = new RadioButton[5][25];
                            mRadioGroupOption = new RadioGroup[5];
                            for (int j = 0; j < numOptions; j++) {
                                addDividerLine(llOption);
                                mRadioGroupOption[j] = new RadioGroup(MenuActivity.this);
                                llOption.addView(mRadioGroupOption[j], 0);
                                // get the index of the option
                                int oo = optionsGetIndex(optionsItem[j]);
                                // get the options into an array parsing by the %
                                String line = optionsAll[oo];
                                String[] optColumns = line.split("\\|");
                                String[] Opt = optColumns[1].split("\\\\");
                                String[] OptDetail = Opt[0].split("%");        // english
                                String[] OptDetailAlt = Opt[1].split("%");    // alt language
                                // set up the buttons
                                try {
                                    // Get selected item first
                                    JSONObject dishopt = new JSONObject();
                                    dishopt = o.getJSONObject(jsonGetter3(o, "options"));
                                    JSONArray dishoptAry = dishopt.getJSONArray("options");
                                    JSONArray selOptItm = dishoptAry.getJSONArray(j);
                                    //log("j=" + j + " selOptItm=" + selOptItm.toString());
                                    Integer optionid = (Integer) jsonGetter2(selOptItm, "optionId");

                                    for (int i = OptDetail.length - 1; i >= 0; i--) {
                                        rbO[j][i] = new RadioButton(MenuActivity.this);
                                        rbO[j][i].setId(i);
                                        rbO[j][i].setButtonDrawable(R.drawable.resize_button);
                                        rbO[j][i].setChecked(false);
                                        if (!Global.EnglishLang) {
                                            rbO[j][i].setText(OptDetailAlt[i]);
                                        } else rbO[j][i].setText(OptDetail[i]);
                                        rbO[j][i].setTextSize(fsize);
                                        rbO[j][i].setTextColor(Global.FontColor);
                                        mRadioGroupOption[j].addView(rbO[j][i], 0, layoutParams);
                                    }
                                    rbO[j][optionid].setChecked(true);
                                } catch (JSONException e) {
                                    log("JSON Update Qty Exception=" + e);
                                }
                            }
                        }

                        // Display all the extras groups and check the ones currently stored in the order JSON
                        numExtras = 0;
                        llExtra = (LinearLayout) dialogedit.findViewById(R.id.llEditItemExtras);
                        llExtra.removeAllViews();
                        if (!extrasItem[0].equalsIgnoreCase("none")) {
                            numExtras = extrasItem.length;
                            // set up CHECKboxes for Extras
                            cb0 = new CheckBox[5][25];
                            for (int j = 0; j < numExtras; j++) {
                                try {
                                    addDividerLine(llExtra);
                                    // get the index of the extra
                                    int ee = extrasGetIndex(extrasItem[j]);
                                    // get the options into an array parsing by the %
                                    String line = extrasAll[ee];
                                    String[] extColumns = line.split("\\|");
                                    String[] Ext = extColumns[1].split("\\\\");
                                    String[] ExtDetail = Ext[0].split("%");        // english
                                    String[] ExtDetailAlt = Ext[1].split("%");    // alt language
                                    // set up the checkboxes
                                    for (int i = ExtDetail.length - 1; i >= 0; i--) {
                                        cb0[j][i] = new CheckBox(MenuActivity.this);
                                        cb0[j][i].setTextSize(fsize);
                                        cb0[j][i].setTextColor(Global.FontColor);
                                        if (!Global.EnglishLang) {
                                            cb0[j][i].setText(ExtDetailAlt[i]);
                                        } else cb0[j][i].setText(ExtDetail[i]);
                                        // Figure out if it should be checked by seeing if the JSON order contains it
                                        Boolean result = false;
                                        // Get selected JSON item
                                        JSONObject dishext = new JSONObject();
                                        dishext = o.getJSONObject(jsonGetter3(o, "extras"));
                                        JSONArray dishextAry = dishext.getJSONArray("extras");
                                        // See if it is currently selected
                                        result = jsonExtraContain(dishextAry, ee, removeRMBnumber(ExtDetail[i]));
                                        cb0[j][i].setChecked(result);
                                        llExtra.addView(cb0[j][i], 0, layoutParams);
                                    }
                                } catch (JSONException e) {
                                    log("JSON Extras Item Setup Exception=" + e);
                                }
                            }
                        }

                        // Display the quantity state
                        final TextView qname = (TextView) dialogedit.findViewById(R.id.TextEditQtyName);
                        qname.setText(getString(R.string.qty_title));
                        qname.setTextSize(orderFontSizeLarge);
                        qname.setTextColor(Global.FontColor);
                        final RadioButton rbut1 = (RadioButton) dialogedit.findViewById(R.id.but_qty1);
                        final RadioButton rbut2 = (RadioButton) dialogedit.findViewById(R.id.but_qty2);
                        final RadioButton rbut3 = (RadioButton) dialogedit.findViewById(R.id.but_qty3);
                        final RadioButton rbut4 = (RadioButton) dialogedit.findViewById(R.id.but_qty4);
                        final RadioButton rbut5 = (RadioButton) dialogedit.findViewById(R.id.but_qty5);
                        String quantity = jsonGetter2(o, "qty").toString();
                        if (quantity.equalsIgnoreCase("1")) rbut1.setChecked(true);
                        if (quantity.equalsIgnoreCase("2")) rbut2.setChecked(true);
                        if (quantity.equalsIgnoreCase("3")) rbut3.setChecked(true);
                        if (quantity.equalsIgnoreCase("4")) rbut4.setChecked(true);
                        if (quantity.equalsIgnoreCase("5")) rbut5.setChecked(true);

                        //set up button for EDIT UPDATing
                        Button buttonEditUpdate = (Button) dialogedit.findViewById(R.id.update_but);
                        buttonEditUpdate.setText(getString(R.string.tab3_update));
                        buttonEditUpdate.setTextSize(mainFontSize);
                        buttonEditUpdate.setOnClickListener(new OnClickListener() {
                            public void onClick(View v) {
                                // Create the initial JSON order and Options and Extras holders
                                JSONOptionsAry = new JSONArray();
                                JSONExtrasAry = new JSONArray();

                                // Reset the dish price
                                priceUnitTotal = 0;

                                // load up the dish main option (price selector)
                                int orderChoice = mRadioGroupMain.getCheckedRadioButtonId();
                                // until we have a JSON menu, we need to use the following functions to get our pricing numbers
                                priceOptionName = removeRMBnumber(rmbItem[orderChoice]);
                                priceOptionNameAlt = removeRMBnumber(rmbItemAlt[orderChoice]);

                                // load up the dish options (non-price) if available
                                if (numOptions > 0) {
                                    for (int j = 0; j < numOptions; j++) {
                                        int orderSecondary = mRadioGroupOption[j].getCheckedRadioButtonId();
                                        int oo = optionsGetIndex(optionsItem[j]);
                                        String line = optionsAll[oo];
                                        String[] optColumns = line.split("\\|");
                                        String[] Opt = optColumns[1].split("\\\\");
                                        String[] OptDetail = Opt[0].split("%");        // english only for the ticket
                                        String[] OptDetailAlt = Opt[1].split("%");    // alt language
                                        String orderSecondaryTxt = OptDetail[orderSecondary].trim();
                                        String orderSecondaryTxtAlt = OptDetailAlt[orderSecondary].trim();
                                        try {
                                            // Append each Dish Option to the global array
                                            JSONArray aryO = new JSONArray();
                                            aryO.put(createInt("optionId", orderSecondary));
                                            aryO.put(createInt("optionPrice", getRMBnumber(orderSecondaryTxt)));
                                            priceUnitTotal = priceUnitTotal + getRMBnumber(orderSecondaryTxt);
                                            aryO.put(createStr("optionName", removeRMBnumber(orderSecondaryTxt)));
                                            aryO.put(createStr("optionNameAlt", removeRMBnumber(orderSecondaryTxtAlt)));
                                            JSONOptionsAry.put(aryO);    // append the dish options
                                        } catch (JSONException e) {
                                            log("JSON Add Dish Options Exception=" + e);
                                        }
                                    }
                                }

                                // load up the dish extras
                                if (numExtras > 0) {
                                    for (int j = 0; j < numExtras; j++) {
                                        int ee = extrasGetIndex(extrasItem[j]);
                                        String line = extrasAll[ee];
                                        String[] extColumns = line.split("\\|");
                                        String[] Ext = extColumns[1].split("\\\\");
                                        String[] ExtDetail = Ext[0].split("%");        // english
                                        String[] ExtDetailAlt = Ext[1].split("%");    // alt language
                                        for (int i = ExtDetail.length - 1; i >= 0; i--) {
                                            if (cb0[j][i].isChecked()) {
                                                String ExtDet = ExtDetail[i].trim();            // english only for the ticket
                                                String ExtDetAlt = ExtDetailAlt[i].trim();    // english only for the ticket
                                                try {
                                                    // Append selected Dish Extras to the global array
                                                    JSONArray aryE = new JSONArray();
                                                    aryE.put(createInt("extraId", j));
                                                    aryE.put(createStr("extraItem", extrasItem[j]));
                                                    aryE.put(createInt("extraIndex", ee));
                                                    aryE.put(createInt("extraPrice", getRMBnumber(ExtDet)));
                                                    priceUnitTotal = priceUnitTotal + getRMBnumber(ExtDet);
                                                    aryE.put(createStr("extraName", removeRMBnumber(ExtDet)));
                                                    aryE.put(createStr("extraNameAlt", removeRMBnumber(ExtDetAlt)));
                                                    JSONExtrasAry.put(aryE);    // append the dish extras
                                                } catch (JSONException e) {
                                                    log("JSON Add Dish Extras Exception=" + e);
                                                }
                                            }
                                        }
                                    }
                                }

                                // add it to the order list
                                Toast.makeText(MenuActivity.this, "Updated Order Item:\n" + orderItem, Toast.LENGTH_SHORT).show();

                                // Handle the quantity
                                RadioGroup qtyRadioGroup = (RadioGroup) dialogedit.findViewById(R.id.GroupQTY);
                                RadioButton butsel = (RadioButton) dialogedit.findViewById(qtyRadioGroup.getCheckedRadioButtonId());
                                Integer outQty = Integer.valueOf(butsel.getText().toString());

                                // note that priceUnitTotal may already contain value from the Options and Extras processing
                                priceUnitBase = getRMBnumber(rmbItem[orderChoice]);
                                priceUnitTotal = priceUnitTotal + priceUnitBase;
                                priceUnitTotalFull = priceUnitTotal;    // Undiscounted price for future discount calculations in the POS app
                                priceDiscount = 100;                    // no discounts in this app
                                priceQtyTotal = priceUnitTotal * outQty;

                                try {
                                    // update the JSON with this item
                                    JSONArray o = items.get(position);

                                    JSONObject ary = new JSONObject();
                                    ary.put("priceOptionId", orderChoice);
                                    o.put(jsonGetter3(o, "priceOptionId"), ary);

                                    ary = new JSONObject();
                                    ary.put("priceOptionName", priceOptionName);
                                    o.put(jsonGetter3(o, "priceOptionName"), ary);

                                    ary = new JSONObject();
                                    ary.put("priceOptionNameAlt", priceOptionNameAlt);
                                    o.put(jsonGetter3(o, "priceOptionNameAlt"), ary);

                                    ary = new JSONObject();
                                    ary.put("priceUnitBase", priceUnitBase);
                                    o.put(jsonGetter3(o, "priceUnitBase"), ary);

                                    ary = new JSONObject();
                                    ary.put("priceUnitTotalFull", priceUnitTotalFull);
                                    o.put(jsonGetter3(o, "priceUnitTotalFull"), ary);

                                    ary = new JSONObject();
                                    ary.put("priceDiscount", priceDiscount);
                                    o.put(jsonGetter3(o, "priceDiscount"), ary);

                                    // Add the dish Options which were built when they were selected ...
                                    JSONObject aryO = new JSONObject();
                                    aryO.put("options", JSONOptionsAry);
                                    o.put(jsonGetter3(o, "options"), aryO);

                                    // Add the dish Extras which were built when they were selected ...
                                    JSONObject aryE = new JSONObject();
                                    aryE.put("extras", JSONExtrasAry);
                                    o.put(jsonGetter3(o, "extras"), aryE);

                                    // Update for the new QTY
                                    ary = new JSONObject();
                                    ary.put("priceUnitTotal", priceUnitTotal);
                                    o.put(jsonGetter3(o, "priceUnitTotal"), ary);

                                    int put = Integer.parseInt(jsonGetter2(o, "priceUnitTotal").toString());

                                    // Store the new Qty
                                    ary = new JSONObject();
                                    ary.put("qty", outQty);
                                    o.put(jsonGetter3(o, "qty"), ary);

                                    // Store the new PriceUnitQty
                                    int pqt = put * outQty;
                                    ary = new JSONObject();
                                    ary.put("priceQtyTotal", pqt);
                                    o.put(jsonGetter3(o, "priceQtyTotal"), ary);

                                    // Store the total
                                    ary = new JSONObject();
                                    ary.put("ordertotal", updateOrderTotalRMB());
                                    JSONOrderAry.put(jsonGetter3(JSONOrderAry, "ordertotal"), ary);
                                } catch (JSONException e) {
                                    log("JSON Add Dish Exception=" + e);
                                }
                                // Update the OrderList
                                updateOrderView();
                                dialogedit.dismiss();
                            }
                        });
                        dialogedit.show();
                    }
                });
                butEdit.setMinimumWidth((int) (Utils.getWidth(MenuActivity.this) / 10.0));
                butEdit.setMaxWidth((int) (Utils.getWidth(MenuActivity.this) / 10.0));
                butEdit.setMinimumHeight((int) (Utils.getWidth(MenuActivity.this) / 10.0));
                butEdit.setMaxHeight((int) (Utils.getWidth(MenuActivity.this) / 10.0));

                Button del = (Button) v.findViewById(R.id.OrderButtonDelete);
                states = new StateListDrawable();
                states.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Global.SelButColor));
                states.addState(new int[]{}, new ColorDrawable(Global.ButColor));
                del.setBackgroundDrawable(states);
                del.setTextSize((float) (orderFontSize / 1.3));
                del.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        AlertDialog alertDialog = new AlertDialog.Builder(MenuActivity.this).create();
                        alertDialog.setTitle(getString(R.string.tab3_delete_title));
                        alertDialog.setMessage(getString(R.string.tab3_delete_text));
                        alertDialog.setButton2(getString(R.string.tab3_delete), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // delete the item
                                try {
                                    JSONObject JSONdishObj = JSONOrderAry.getJSONObject(jsonGetter3(JSONOrderAry, "dishes"));
                                    JSONArray JSONdishesAry = JSONdishObj.getJSONArray("dishes");
                                    // Remove the selected item
                                    // Check for SDK version to see if we can use the JSON function directly
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                        JSONdishesAry.remove(position);
                                    } else {
                                        // Do it the old-school way
                                        JSONdishesAry = RemoveJSONArray(JSONdishesAry, position);
                                    }
                                    // replace it
                                    JSONObject ary = new JSONObject();    // new object to store the new dishes
                                    ary.put("dishes", JSONdishesAry);
                                    // Replace the JSON dishes Object in the JSON order
                                    JSONOrderAry.put(jsonGetter3(JSONOrderAry, "dishes"), ary);
                                    //log("Removed position=" + position);
                                    //log("new dish cnt=" + JSONdishesAry.length());
                                    // Update total price
                                    updateOrderTotalRMB();
                                } catch (JSONException e) {
                                    log("JSON Delete Dish Exception=" + e);
                                }
                                updateOrderView();
                            }
                        });
                        alertDialog.setButton(getString(R.string.tab3_back), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        });
                        alertDialog.show();

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                            alertDialog.getWindow().getDecorView().setSystemUiVisibility(
                                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                            | View.INVISIBLE);
                    }
                });
                del.setMinimumWidth((int) (Utils.getWidth(MenuActivity.this) / 10.0));
                del.setMaxWidth((int) (Utils.getWidth(MenuActivity.this) / 10.0));
                del.setMinimumHeight((int) (Utils.getWidth(MenuActivity.this) / 10.0));
                del.setMaxHeight((int) (Utils.getWidth(MenuActivity.this) / 10.0));

                // load the pic
                final String dishid = jsonGetter2(o, "dishId").toString();
                final String fetchURLsmall = Global.fetchURL200.get(Integer.parseInt(dishid));
                ImageView img = (ImageView) v.findViewById(R.id.OrderDishPic);
                img.setLayoutParams(new LinearLayout.LayoutParams((int) (Utils.getWidth(MenuActivity.this) / 10.0), (int) (Utils.getWidth(MenuActivity.this) / 6.7)));
                // Lazy load the image with Picasso
                get()
                        .load(fetchURLsmall)
                        .placeholder(R.drawable.nopic)
                        .error(R.drawable.nopic)
                        .into(img);
                if (!Global.ShowPics) {
                    img.setVisibility(View.GONE);
                }
                img.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        final ImageView bigimage = (ImageView) findViewById(R.id.singleImg);
                        vfMenu.setDisplayedChild(FLIP_DISH_PIC);
                        final String fetchURLbig = Global.fetchURL800.get(Integer.parseInt(dishid));
                        // Lazy load the image with Picasso
                        get()
                                .load(fetchURLbig)
                                .into(bigimage);
                        bigimage.setOnClickListener(new OnClickListener() {
                            public void onClick(View v) {
                                vfMenu.setDisplayedChild(FLIP_ORDER_LIST);
                            }
                        });
                    }
                });
            }
            return v;
        }
    }

    private void sendPrinter3() {
        try {
            // Choose print size based on KitchenCodes Mode
            Boolean largeSize = false;
            if (Global.P2KitchenCodes) largeSize = true;

            if ((Global.POS3Enable) && (!sentPOS3)) {
                // open the device
                POSDev = new EpsonComDevice();
                POSParams = new EpsonComDeviceParameters();
                POSParams.PortType = EpsonCom.PORT_TYPE.ETHERNET;
                POSParams.IPAddress = Global.POS3Ip;
                POSParams.PortNumber = 9100;
                POSDev.setDeviceParameters(POSParams);
                err = POSDev.openDevice();
                while (err == ERROR_CODE.SUCCESS) {
                    err = POSDev.selectAlignment(ALIGNMENT.LEFT);
                    err = POSDev.sendCommand("ESC d 5");
                    // print out the time sent on the first line
                    if (Global.P3PrintSentTime) {
                        err = POSDev.printString("Sent: " + Global.SendTime, FONT.FONT_A, true, false, largeSize, largeSize);
                    }
                    //err = POSDev.sendCommand("ESC d 1");
                    err = POSDev.printString("Id: " + Global.OrderId, FONT.FONT_A, true, false, largeSize, largeSize);
                    //err = POSDev.printString("Num: " + Global.TicketNum, FONT.FONT_A, true, false, largeSize, largeSize);
                    //err = POSDev.sendCommand("ESC d 1");
                    // print the table and guests in BIG
                    err = POSDev.printString("Table: " + Global.TableName, FONT.FONT_A, true, false, true, true);
                    err = POSDev.printString("Guests: " + Global.Guests, FONT.FONT_A, true, false, largeSize, largeSize);
                    //POSDev.sendCommand("ESC d 1");
                    err = POSDev.sendCommand("ESC d 2");

                    // Use Vector mode so Chinese in Special instructions can print correctly
                    //err = POSDev.printString(formatTicket23, FONT.FONT_A, true, false, true, true);
                    err = printVector(formatTicket3, POSDev);

                    err = POSDev.cutPaper();
                    if (!Global.Printer3Type)
                        err = POSDev.sendCommand("ESC B 4 2"); // Send the Beep command which is supported by the Gprinter, 4 beep 200mils
                    err = POSDev.closeDevice();
                    sentPOS3 = true;
                    break;
                }
                if (err != ERROR_CODE.SUCCESS) {
                    String errorString = "";
                    if (err != null) errorString = EpsonCom.getErrorText(err);
                    messageBox(MenuActivity.this,
                            getString(R.string.tab3_pos_err_1) + errorString +
                                    getString(R.string.tab3_pos_err_2), "Connection problem 3");
                    err = POSDev.closeDevice();
                    sentPOS3 = false;
                }
            }
        } catch (Exception ex) {
            String errorString = "";
            if (err != null) errorString = EpsonCom.getErrorText(err);
            messageBox(MenuActivity.this,
                    getString(R.string.tab3_pos_err_1) + errorString +
                            getString(R.string.tab3_pos_err_2), "Connection problem 3b");
            err = POSDev.closeDevice();
            sentPOS3 = false;
        }
    }

    private void sendPrinter2() {
        try {
            // Choose print size based on KitchenCodes Mode
            Boolean largeSize = false;
            if (Global.P2KitchenCodes) largeSize = true;

            if ((Global.POS2Enable) && (!sentPOS2)) {
                // open the device
                POSDev = new EpsonComDevice();
                POSParams = new EpsonComDeviceParameters();
                POSParams.PortType = EpsonCom.PORT_TYPE.ETHERNET;
                POSParams.IPAddress = Global.POS2Ip;
                POSParams.PortNumber = 9100;
                POSDev.setDeviceParameters(POSParams);
                err = POSDev.openDevice();
                while (err == ERROR_CODE.SUCCESS) {
                    err = POSDev.selectAlignment(ALIGNMENT.LEFT);
                    err = POSDev.sendCommand("ESC d 5");
                    // print out the time sent on the first line
                    if (Global.P2PrintSentTime) {
                        err = POSDev.printString("Sent: " + Global.SendTime, FONT.FONT_A, true, false, largeSize, largeSize);
                    }
                    //err = POSDev.sendCommand("ESC d 1");
                    err = POSDev.printString("Id: " + Global.OrderId, FONT.FONT_A, true, false, largeSize, largeSize);
                    //err = POSDev.printString("Num: " + Global.TicketNum, FONT.FONT_A, true, false, largeSize, largeSize);
                    //err = POSDev.sendCommand("ESC d 1");
                    // print the table and guests in BIG
                    err = POSDev.printString("Table: " + Global.TableName, FONT.FONT_A, true, false, true, true);
                    err = POSDev.printString("Guests: " + Global.Guests, FONT.FONT_A, true, false, largeSize, largeSize);
                    //POSDev.sendCommand("ESC d 1");
                    err = POSDev.sendCommand("ESC d 2");

                    // Use Vector mode so Chinese in Special instructions can print correctly
                    //err = POSDev.printString(formatTicket23, FONT.FONT_A, true, false, true, true);
                    err = printVector(formatTicket2, POSDev);

                    err = POSDev.cutPaper();
                    if (!Global.Printer2Type)
                        err = POSDev.sendCommand("ESC B 4 2"); // Send the Beep command which is supported by the Gprinter, 4 beep 200 mils
                    err = POSDev.closeDevice();
                    sentPOS2 = true;
                    break;
                }
                if (err != ERROR_CODE.SUCCESS) {
                    String errorString = "";
                    if (err != null) errorString = EpsonCom.getErrorText(err);
                    messageBox(MenuActivity.this,
                            getString(R.string.tab3_pos_err_1) + errorString +
                                    getString(R.string.tab3_pos_err_2), "Connection problem 2");
                    err = POSDev.closeDevice();
                    sentPOS2 = false;
                }
            }
        } catch (Exception ex) {
            String errorString = "";
            if (err != null) errorString = EpsonCom.getErrorText(err);
            messageBox(MenuActivity.this,
                    getString(R.string.tab3_pos_err_1) + errorString +
                            getString(R.string.tab3_pos_err_2), "Connection problem 2b");
            err = POSDev.closeDevice();
            sentPOS2 = false;
        }
    }

    private void sendPrinter1() {
        try {
            if ((Global.POS1Enable) && (!sentPOS1)) {
                // open the device
                POSDev = new EpsonComDevice();
                POSParams = new EpsonComDeviceParameters();
                POSParams.PortType = EpsonCom.PORT_TYPE.ETHERNET;
                POSParams.IPAddress = Global.POS1Ip;
                POSParams.PortNumber = 9100;
                POSDev.setDeviceParameters(POSParams);
                err = POSDev.openDevice();
                // ready to print
                while (err == ERROR_CODE.SUCCESS) {
                    // Loop over the number of copies
                    int copies = Global.Printer1Copy;
                    if (Global.TableName.equalsIgnoreCase("Take Out")) copies = copies + 1;
                    for (int i = 0; i < copies; i++) {
                        err = POSDev.selectAlignment(ALIGNMENT.CENTER);
                        // print the logo or a title
                        if (Global.POS1Logo) {
                            err = POSDev.sendCommand("FS p 1 0");        // print the lilys logo
                            //err = POS1Dev.sendCommand("FS p 2 0");	// print the 3sum logo
                        } else {
                            err = POSDev.printString(Global.CustomerName, FONT.FONT_A, true, false, true, true);
                        }
                        err = POSDev.sendCommand("ESC d 1");
                        err = POSDev.printString(Global.OrderId, FONT.FONT_A, true, false, false, false);
                        //err = POSDev.printString("Number: " + Global.TicketNum, FONT.FONT_A, true, false, false, false);
                        // print the table and guests in BIG
                        POSDev.printString("Table " + Global.TableName, FONT.FONT_A, true, false, true, true);
                        POSDev.printString("Guest " + Global.Guests, FONT.FONT_A, true, false, false, false);
                        POSDev.sendCommand("ESC d 1");
                        if (Global.P1PrintSentTime) {
                            err = POSDev.printString("Sent: " + Global.SendTime, FONT.FONT_A, true, false, false, false);
                        }
                        err = POSDev.sendCommand("ESC d 1");
                        err = POSDev.selectAlignment(ALIGNMENT.LEFT);

                        // Use Vector mode so Chinese in Special instructions can print correctly
                        //err = POSDev.printString(formatTicket1, FONT.FONT_A, true, false, false, false);
                        err = printVector(formatTicket1, POSDev);

                        err = POSDev.selectAlignment(ALIGNMENT.RIGHT);
                        err = POSDev.printString(formatTicket1Tot, FONT.FONT_A, true, false, true, true);
                        err = POSDev.sendCommand("ESC d 1");
                        err = POSDev.selectAlignment(ALIGNMENT.CENTER);
                        err = POSDev.printString("Thanks for visiting " + Global.CustomerNameBrief, FONT.FONT_A, true, false, false, false);
                        err = POSDev.sendCommand("ESC d 1");
                        err = POSDev.printString(Global.StoreAddress, FONT.FONT_A, true, false, false, false);
                        err = POSDev.cutPaper();
                    }
                    if (!Global.Printer1Type)
                        err = POSDev.sendCommand("ESC B 1 4"); // Send the Beep command to the Gprinter, 1 beep 200mils
                    // Close the connection so others can use it
                    POSDev.closeDevice();
                    sentPOS1 = true;
                    if (Global.AutoOpenDrawer) openDrawer();
                    break;
                }
                if (err != ERROR_CODE.SUCCESS) {
                    String errorString = "";
                    if (err != null) errorString = EpsonCom.getErrorText(err);
                    messageBox(MenuActivity.this,
                            getString(R.string.tab3_pos_err_1) + errorString +
                                    getString(R.string.tab3_pos_err_2), "Connection problem 1");
                    err = POSDev.closeDevice();
                    sentPOS1 = false;
                }
            }
        } catch (Exception ex) {
            String errorString = "";
            if (err != null) errorString = EpsonCom.getErrorText(err);
            messageBox(MenuActivity.this,
                    getString(R.string.tab3_pos_err_1) + errorString +
                            getString(R.string.tab3_pos_err_2), "Connection problem 1b");
            err = POSDev.closeDevice();
            sentPOS1 = false;
        }
    }

    private ERROR_CODE printVector(String str, EpsonComDevice pr) {
        byte[] bytesOut = null;
        Vector<Byte> sendit = null;
        try {
            //String sendString = editTextPrintString.getText().toString();
            String sendString = str;
            String sendStringEncoded = new String(sendString.getBytes("UTF-8"));
            bytesOut = sendStringEncoded.getBytes("CP936");
            sendit = new Vector<Byte>();
            for (int i = 0; i < bytesOut.length; i++) {
                sendit.add(bytesOut[i]);
            }
        } catch (UnsupportedEncodingException e) {
            log("Unsupported Encoding Exception");
        }
        err = pr.sendData(sendit);
        err = pr.sendCommand("ESC d 1");
        return err;
    }

    private void openDrawer() {
        if (isOnline()) {
            try {
                POSDev = new EpsonComDevice();
                POSParams = new EpsonComDeviceParameters();
                POSParams.PortType = EpsonCom.PORT_TYPE.ETHERNET;
                POSParams.IPAddress = Global.POS1Ip;
                POSParams.PortNumber = 9100;
                POSDev.setDeviceParameters(POSParams);
                err = POSDev.openDevice();
                // ready to print
                err = POSDev.sendCommand("ESC p 0 2 2");    // open the money kick pin2 4ms on 2ms off
                err = POSDev.sendCommand("ESC p 1 2 2");    // open the money kick pin5 4ms on 2ms off
                err = POSDev.closeDevice();
            } catch (Exception ex) {
                String errorString = "";
                if (err != null) errorString = EpsonCom.getErrorText(err);
                messageBox(MenuActivity.this,
                        "Sorry, Cash Drawer cannot be opened. " + errorString,
                        "Connection problem 1");
                err = POSDev.closeDevice();
            }
        } else {
            String errorString = "Sorry, Cash Drawer cannot be opened. ";
            messageBox(MenuActivity.this, errorString, "Connection problem 1b");
            err = POSDev.closeDevice();
        }
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return (ni != null && ni.isAvailable() && ni.isConnected());
    }

    private boolean haveNetworkConnection() {
        boolean HaveConnectedWifi = false;
        boolean HaveConnectedMobile = false;

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] netInfo = cm.getAllNetworkInfo();
        for (NetworkInfo ni : netInfo) {
            if (ni.getTypeName().equalsIgnoreCase("WIFI"))
                if (ni.isConnected())
                    HaveConnectedWifi = true;
            if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
                if (ni.isConnected())
                    HaveConnectedMobile = true;
        }
        return HaveConnectedWifi || HaveConnectedMobile;
    }

    private void messageBox(final Context context, final String message, final String title) {
        this.runOnUiThread(
                new Runnable() {
                    public void run() {
                        final AlertDialog alertDialog = new AlertDialog.Builder(context).create();
                        alertDialog.setTitle(title);
                        alertDialog.setIcon(android.R.drawable.stat_sys_warning);
                        alertDialog.setMessage(message);
                        alertDialog.setCancelable(false);
                        alertDialog.setButton("Back", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                alertDialog.cancel();
                                //finish();
                            }
                        });
                        alertDialog.show();
                    }
                }
        );
    }

    private boolean isKitKat() {
        boolean kk = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) kk = true;
        return kk;
    }

    private String formatTicket(String json, Integer pnum) {
        // String   Json order
        // Integer	printer number to determine filter and kitchen codes
        // returns  String  formatted ticket

        ArrayList<JSONArray> JSONOrderList = new ArrayList<JSONArray>();
        // First, build an ArrayList of the dishes in this JSON order
        try {
            JSONArray JSONOrderAry = new JSONArray(json);
            JSONObject JSONdishObj = JSONOrderAry.getJSONObject(jsonGetter3(JSONOrderAry, "dishes"));
            JSONArray jda = JSONdishObj.getJSONArray("dishes");
            int numDish = jda.length();
            //log("d1: Number of dishes=" + numDish);
            JSONOrderList.clear();
            if (numDish > 0) {
                JSONArray JSONdishesAry = JSONdishObj.getJSONArray("dishes");
                for (int i = 0; i < JSONdishesAry.length(); i++) {
                    JSONArray jd = JSONdishesAry.getJSONArray(i);
                    // Build a list of the unprinted dishes if they want partial, otherwise all the dishes will get printed
                    // Check if it passes the filter first
                    Integer dishcatid = (Integer) jsonGetter2(jd, "categoryId");
                    // Also dont include the dish on Kitchen Printer P2 if they override it with the flag
                    Boolean counterOnly = (Boolean) jsonGetter2(jd, "counterOnly");
                    Boolean includeDish = true;
                    if (pnum == 2) if (!P2Filter.get(dishcatid)) includeDish = false;
                    if (pnum == 2) if (true == counterOnly) includeDish = false;
                    if (pnum == 3) if (!P3Filter.get(dishcatid)) includeDish = false;
                    if (pnum == 3) if (true == counterOnly) includeDish = true;
                    if (includeDish) {
                        JSONOrderList.add(jd);
                    }
                }
            }
        } catch (JSONException e) {
            log("json formatTicket Exception=" + e);
        }

        // Printer 1 will not use Kitchen Codes
        // Printer 2 will check Global.P2KitchenCodes
        // Printer 3 will check Global.P3KitchenCodes
        String formattedTicket = "";
        Boolean kitchenFormat = false;
        if ((pnum == 2) && (Global.P2KitchenCodes)) kitchenFormat = true;
        if ((pnum == 3) && (Global.P3KitchenCodes)) kitchenFormat = true;

        int dishCount = JSONOrderList.size();

        // Loop over each dish and build the printer strings
        //
        // COUNTER TICKET FORMAT:
        // 123456789-123456789-123456789-123456789-12
        // Id Name                      unt  Q qtytot
        // 99 aaaaaaaaaaaaaaaaaaaaaaaaa 999 x9 999.00
        //    priceOpt+options
        //    extras
        //    Special:aaaaaaaaaaaaaaaaa
        //    Discount: 99%
        //
        // Update, if no Seq Num, so the Name can be longer (28 chars)
        // aaaaaaaaaaaaaaaaaaaaaaaaaaaa
        //
        // KITCHEN TICKET FORMAT (double horizontal font size)
        // 123456789-123456789-1
        // Substituted(DishName + PriceOpt + Options + Extras) + Special:aaaaaa /r/n
        // Append "#9999" for Quantity > 1

        for (int i = 0; i < dishCount; i++) {
            try {
                JSONArray o = JSONOrderList.get(i);
                if (o != null) {
                    // Grab the print strings from JSON dish and format
                    String dishtext;

                    if (Global.EnglishLang) dishtext = jsonGetter2(o, "dishName").toString();
                    else dishtext = jsonGetter2(o, "dishName").toString();
                    if (kitchenFormat) formattedTicket = formattedTicket + kitchenSubstitute(dishtext) + " ";

                    if (Global.PrintDishID) {
                        if (dishtext.length() > 25) dishtext = dishtext.substring(0, 24);
                        dishtext = addPadNum(25, dishtext);
                    } else {
                        if (dishtext.length() > 28) dishtext = dishtext.substring(0, 27);
                        dishtext = addPadNum(28, dishtext);
                    }

                    String quantity = jsonGetter2(o, "qty").toString();
                    String up = jsonGetter2(o, "priceUnitTotal").toString();
                    String qp = jsonGetter2(o, "priceQtyTotal").toString();

                    String formatid = String.format("%2d", Integer.valueOf(i + 1));
                    String formatqty = String.format("%1d", Integer.valueOf(quantity));
                    String formatup = String.format("%3d", Integer.valueOf(up));
                    String formatqp = String.format("%3d", Integer.valueOf(qp)) + ".00";

                    if (!kitchenFormat) {
                        if (Global.PrintDishID) {
                            formattedTicket = formattedTicket + formatid + "." +
                                    dishtext + " " +
                                    formatup + " x" +
                                    formatqty + " " +
                                    formatqp;
                        } else {
                            formattedTicket = formattedTicket + dishtext + " " +
                                    formatup + " x" +
                                    formatqty + " " +
                                    formatqp;
                        }
                    }

                    // Grab the Price Option + Options for the first line
                    String dishsub1a = "";
                    // Handle price option
                    String priceopt;
                    if (Global.EnglishLang) priceopt = jsonGetter2(o, "priceOptionName").toString();
                    else priceopt = jsonGetter2(o, "priceOptionName").toString();
                    if (priceopt.length() > 0) {
                        if (kitchenFormat) formattedTicket = formattedTicket + kitchenSubstitute(priceopt) + " ";
                        dishsub1a = priceopt + " ";
                    }
                    // Add all the Option choices
                    String options = "";
                    String option = "";
                    JSONObject dishopt = new JSONObject();
                    dishopt = o.getJSONObject(jsonGetter3(o, "options"));
                    JSONArray dishoptAry = dishopt.getJSONArray("options");
                    if (dishoptAry.length() > 0) {
                        for (int j = 0; j < dishoptAry.length(); j++) {
                            // Grab just the optionName
                            if (Global.EnglishLang)
                                option = jsonGetter2(dishoptAry.getJSONArray(j), "optionName").toString();
                            else option = jsonGetter2(dishoptAry.getJSONArray(j), "optionName").toString();
                            //if (j!=dishoptAry.length()-1) dishsubtext = dishsubtext + " ";
                            if (kitchenFormat) formattedTicket = formattedTicket + kitchenSubstitute(option) + " ";
                            options = options + option + " ";
                        }
                        dishsub1a = dishsub1a + options;
                    }
                    //if ( dishsub1a.length() > 39 ) dishsub1a = dishsub1a.substring(0, 38);	// truncate for 1 line only
                    if (!kitchenFormat)
                        if (dishsub1a.length() > 0) formattedTicket = formattedTicket + addPad("   " + dishsub1a);

                    // Add selected Extra choices for the next line
                    String extras = "";
                    String extra = "";
                    JSONObject dishext = new JSONObject();
                    dishext = o.getJSONObject(jsonGetter3(o, "extras"));
                    JSONArray dishextAry = dishext.getJSONArray("extras");
                    if (dishextAry.length() > 0) {
                        for (int j = 0; j < dishextAry.length(); j++) {
                            // Grab just the extraName
                            if (Global.EnglishLang)
                                extra = jsonGetter2(dishextAry.getJSONArray(j), "extraName").toString();
                            else extra = jsonGetter2(dishextAry.getJSONArray(j), "extraName").toString();
                            //if (j!=dishextAry.length()-1) dishsubtext = dishsubtext + " ";
                            if (kitchenFormat) formattedTicket = formattedTicket + kitchenSubstitute(extra) + " ";
                            extras = extras + extra + " ";
                        }
                        //if ( extras.length() > 39 ) extras = extras.substring(0, 38);	// truncate for 1 line only
                        if (!kitchenFormat) formattedTicket = formattedTicket + addPad("   " + extras);
                    }

                    if (kitchenFormat) formattedTicket = formattedTicket.trim();

                    // Handle special Instructions on a new line
                    String specins = jsonGetter2(o, "specIns").toString();
                    if (specins.length() > 0) {
                        if (specins.length() > 31) specins = specins.substring(0, 30);
                        if (kitchenFormat)
                            formattedTicket = addPadKitc(formattedTicket) + addPadKitc("Spec:" + specins);
                        if (!kitchenFormat) formattedTicket = formattedTicket + addPad("   Special: " + specins);
                    }
                    //formattedKitchenTicket = formattedKitchenTicket.trim();

                    // Handle discount information on a new line
                    String discount = jsonGetter2(o, "priceDiscount").toString();
                    if (discount.equalsIgnoreCase("100")) {
                        // No discount so don't do anything
                    } else if (discount.equalsIgnoreCase("0")) {
                        if (!kitchenFormat) formattedTicket = formattedTicket + addPad("   Discount: Free Dish");
                    } else {
                        if (!kitchenFormat)
                            formattedTicket = formattedTicket + addPad("   Discount: " + discount + "%");
                    }

                    // If QTY > 1 then add indicator to the Kitchen ticket
                    if (kitchenFormat && Integer.valueOf(quantity) > 1) {
                        formattedTicket = formattedTicket + " #" + quantity + "#";
                    }

                    // Add a blank line or dash line between dishes
                    //if (i < dishCount-1) {    // in between
                    if (i < dishCount) {        // after each
                        //formattedCounterTicket = addBlankLine(formattedCounterTicket);
                        if (!kitchenFormat) formattedTicket = addDashLine(formattedTicket);
                        // pad out and then double space the kitchen ticket
                        if (kitchenFormat) formattedTicket = addPadKitc(formattedTicket);
                        if (kitchenFormat) formattedTicket = addBlankLineKitc(formattedTicket);
                    }
                }
            } catch (JSONException e) {
                log("Building Tickets Exception=" + e + " string=" + json);
                return "Print Error 1";
            }
        }
        return formattedTicket;
    }

    private String kitchenSubstitute(String str) {
        // Loop over each kitchen file entry
        int kitcCount = kitchenLines.length;
        String resultStr = str;
        for (int i = 0; i < kitcCount; i++) {
            // parse each line into columns using the divider character "|"
            String[] kitchenColumns = kitchenLines[i].split("\\|");
            // make sure the codes are valid length
            if (kitchenColumns.length == 2) {
                String s1 = kitchenColumns[0].trim();
                String s2 = kitchenColumns[1].trim();
                //log("HMMM: str=" + str + " s1=" + s1 + " s2=" + s2);
                //resultStr = str.replaceAll(s1,s2);
                if (str.equalsIgnoreCase(s1)) {
                    resultStr = s2;
                    break;
                }
            }
        }
        return resultStr;
    }

    // Some string padding functions for the printers
    private String addDashLine(String str) {
        String strDash = "";
        for (int k = 1; k <= Global.TicketCharWidth; k++) {
            strDash = strDash + "-";
        }
        str = str + strDash;
        return str;
    }

    private String addBlankLine(String str) {
        String strSpace = "";
        for (int k = 1; k <= Global.TicketCharWidth; k++) {
            strSpace = strSpace + " ";
        }
        str = str + strSpace;
        return str;
    }

    private String addBlankLineKitc(String str) {
        String strSpace = "";
        for (int k = 1; k <= Global.KitcTicketCharWidth; k++) {
            strSpace = strSpace + " ";
        }
        str = str + strSpace;
        return str;
    }

    private String addPad(String str) {
        //int addPad = Global.TicketCharWidth - str.length() + 1;
        // find the len of the str
        int length = str.length();
        int chars = 0;
        for (int i = 0; i < length; i++) {
            char ch = str.charAt(i);
            Character.UnicodeBlock block = Character.UnicodeBlock.of(ch);
            if (Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS.equals(block) ||
                    Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS.equals(block) ||
                    Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A.equals(block)) {
                chars = chars + 2;    // Double wide
            } else {
                chars = chars + 1;
            }
        }
        int intRem = ((chars - 1) % Global.TicketCharWidth);
        int intSpaces = Global.TicketCharWidth - intRem;
        for (int k = 1; k < intSpaces; k++) {
            str = str + " ";
        }
        //str = str + "\\r\\n";
        return str;
    }

    private String addPadKitc(String str) {
        // find the len of the str
        int length = str.length();
        int chars = 0;
        for (int i = 0; i < length; i++) {
            char ch = str.charAt(i);
            Character.UnicodeBlock block = Character.UnicodeBlock.of(ch);
            if (Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS.equals(block) ||
                    Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS.equals(block) ||
                    Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A.equals(block)) {
                chars = chars + 2;    // Double wide
            } else {
                chars = chars + 1;
            }
        }
        int intRem = ((chars - 1) % Global.KitcTicketCharWidth);
        int intSpaces = Global.KitcTicketCharWidth - intRem;
        for (int k = 1; k < intSpaces; k++) {
            str = str + " ";
        }
        return str;
    }

    private String addPadNum(int num, String str) {
        int addPad = num - str.length() + 1;
        for (int k = 1; k < addPad; k++) {
            str = str + " ";
        }
        //str = str + "\\r\\n";
        return str;
    }

	/*
	private String addLeftSplit(String str, int leftindent, int chunksize) {
		// Break the str up into multiple lines with left indent spaces
		//int addPad = num - str.length() + 1;
		//for(int k=1; k<addPad; k++)
        //{
		//	str = str + " ";
        //}
		//str = str + "\\r\\n";
		return str;
	}

	private String addBlankLine(String str) {
		String strDash = "";
		for(int k=1; k<=Global.TicketCharWidth; k++)
        {
			strDash = strDash + " ";
        }
		str = str + strDash;
		return str;
	}

	private String addBlankLineAfterKitc(String str) {
		//String strBlanks = "";
		//for(int k=1; k<=Global.KitcTicketCharWidth; k++)
        //{
		//	strBlanks = strBlanks + " ";
        //}
		//str = str + strBlanks;
		str = str + "\\r\\n";
		return str;
	}
	*/

    private void setupNewOrder() {
        JSONORDERLIST.clear();

        orderPartialSent = false;
        orderFullySent = false;

        sentPOS1 = false;
        sentPOS2 = false;
        sentPOS3 = false;

        vfMenu.setDisplayedChild(FLIP_MENU);
        menuSelect();
        orderUnselect();
        infoUnselect();
        msgUnselect();

        updateOrderView();
        updateMenuView();

        String curTime = Utils.GetDateTime();
        Global.OrderId = curTime + "-" + Global.TableName;

        Global.SaleType = "0"; //cash
        Global.Guests = "0";

        // Create the initial JSON order and Options and Extras holders
        try {
            JSONOrderAry = getNewJSONOrder();
        } catch (JSONException e) {
            log("JSONException Intial e=" + e);
        }
    }

    private int getOrderTotalRMB() {
        // Grab the ordertotal from the JSON
        int TOTALRMB = 0;
        if (JSONOrderAry != null) {
            String ordertotal = "0";
            ordertotal = jsonGetter2(JSONOrderAry, "ordertotal").toString();
            TOTALRMB = Integer.parseInt(ordertotal);
        }
        return TOTALRMB;
    }

    private int updateOrderTotalRMB() {
        int TOTALRMB = 0;
        if (JSONOrderAry != null) {
            try {
                JSONObject JSONdishObj = JSONOrderAry.getJSONObject(jsonGetter3(JSONOrderAry, "dishes"));
                JSONArray JSONdishesAry = JSONdishObj.getJSONArray("dishes");

                // Loop through each dish and add the Qty Total for the dish to the Order Total
                for (int i = 0; i < JSONdishesAry.length(); i++) {
                    JSONArray jd = JSONdishesAry.getJSONArray(i);
                    // Grab the PriceQty from the dish
                    int priceqty = Integer.parseInt(jsonGetter2(jd, "priceQtyTotal").toString());
                    // Running total ...
                    TOTALRMB = TOTALRMB + priceqty;
                }
                //log("dish cnt=" + JSONdishesAry.length());
                //log("new dish price=" + TOTALRMB);

                // update total price
                JSONObject ary = new JSONObject();
                ary.put("ordertotal", TOTALRMB);
                JSONOrderAry.put(jsonGetter3(JSONOrderAry, "ordertotal"), ary);

                // replace it
                ary = new JSONObject();    // new object to store the new dishes
                ary.put("dishes", JSONdishesAry);
                // Replace the JSON dishes Object in the JSON order
                JSONOrderAry.put(jsonGetter3(JSONOrderAry, "dishes"), ary);
            } catch (JSONException e) {
                log("JSON updateOrderTotalRMB Exception=" + e);
            }
        }
        return TOTALRMB;
    }

    private boolean jsonExtraContain(JSONArray ary, int indx, String nam) {
        boolean result = false;
        try {
            //log("ary=" + ary.toString() + ",indx=" + indx + ",nam=" + nam);
            // Walk the json Extra Ary and see if there is a matching item
            if (ary != null) {
                for (int i = 0; i < ary.length(); i++) {
                    JSONArray tmp = ary.getJSONArray(i);
                    int extraIndex = (Integer) jsonGetter2(tmp, "extraIndex");
                    String extraName = jsonGetter2(tmp, "extraName").toString();
                    //log("i,indx,nam=" + i + "," + indx + "," + nam);
                    if ((extraIndex == indx) && (extraName.equalsIgnoreCase(nam))) {
                        result = true;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log("jsonExtraContain Exception=" + e);
        }
        return result;
    }

    // Until we have JSON menu, we need to extract the price from the dish name
    private static int getRMBnumber(String in) {
        int newRMB = 0;
        String foundRMB = "none";
        Pattern p = Pattern.compile("(RMB )(\\d*)");
        Matcher m = p.matcher(in);
        while (m.find()) {
            foundRMB = m.group(2).trim();    // load up the money
            newRMB = newRMB + Integer.valueOf(foundRMB);
        }
        return newRMB;
    }

    // Until we have JSON menu, we need to remove the price from the dish name
    private static String removeRMBnumber(String in) {
        String newName = in;
        Pattern p = Pattern.compile("(.*)(RMB )(\\d*)");
        Matcher m = p.matcher(in);
        while (m.find()) {
            newName = m.group(1).trim();    // load up the dish name before the RMB 99
        }
        return newName;
    }

    private void updateOrderSentView() {
        // set up the ORDER SENT View
        LinearLayout lltop = (LinearLayout) findViewById(R.id.LLOrderSent);
        lltop.setBackgroundColor(Global.BackColor);

        TextView tv = (TextView) findViewById(R.id.t1);
        tv.setText(getString(R.string.ordersent_t1));
        tv.setTextColor(Global.FontColor);
        tv.setTextSize((float) (mainFontSize * 1.8));

        tv = (TextView) findViewById(R.id.t2);
        tv.setText(getString(R.string.ordersent_t2));
        tv.setTextColor(Global.FontColor);
        tv.setTextSize((float) (mainFontSize * 1.5));

        Button getback = (Button) findViewById(R.id.sentExitBut);
        getback.setText(getString(R.string.ordersent_new));
        getback.setTextColor(Global.SelButFontColor);
        getback.setTextSize((float) (mainFontSize * 1.4));

        states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Global.ButColor));
        states.addState(new int[]{}, new ColorDrawable(Global.SelButColor));

        getback.setBackgroundDrawable(states);
        getback.setTextColor(Global.SelButFontColor);
        getback.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                setupNewOrder();
                // display the SPECIALS or the TABLES view
                vfTop.setDisplayedChild(TOP_TABLES);
            }
        });
    }

    private void updateOrderNotSentView() {
        // set up the ORDER SENT View
        LinearLayout lltop = (LinearLayout) findViewById(R.id.LLOrderNotSent);
        lltop.setBackgroundColor(Global.BackColor);

        TextView tv = (TextView) findViewById(R.id.nst1);
        tv.setText(getString(R.string.ordernotsent_t1));
        tv.setTextColor(Global.FontColor);
        tv.setTextSize((float) (mainFontSize * 1.8));

        tv = (TextView) findViewById(R.id.nst2);
        tv.setText(getString(R.string.ordernotsent_t2));
        tv.setTextColor(Global.FontColor);
        tv.setTextSize((float) (mainFontSize * 1.5));

        Button badback = (Button) findViewById(R.id.notSentExitBut);
        badback.setText(getString(R.string.tab3_backtomenu));
        badback.setTextColor(Global.SelButFontColor);
        badback.setTextSize((float) (mainFontSize * 1.4));

        states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Global.ButColor));
        states.addState(new int[]{}, new ColorDrawable(Global.SelButColor));

        badback.setBackgroundDrawable(states);
        badback.setTextColor(Global.SelButFontColor);
        badback.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                vfMenu.setDisplayedChild(FLIP_ORDER_LIST);
                vfTop.setDisplayedChild(TOP_MENU);
            }
        });
    }

    private void updateMenuView() {
        list = (ListView) findViewById(R.id.list);
        adapter1 = new PicSimpleAdapter(MenuActivity.this, R.layout.list_complex, Global.fetchURL200, MenuActivity.this);
        list.setPadding(2, 2, 2, 2);
        list.setCacheColorHint(0);
        list.setFadingEdgeLength(0);
        list.setSelector(R.drawable.list_selector_background);
        list.setItemsCanFocus(true);
        list.setLongClickable(true);
        list.setSelection(0);
        list.setAdapter(adapter1);
        setUpCatList();
        list.setSelection(MenuPosition.get(0));

        listCat = (ListView) findViewById(R.id.CatList);
        //listCat.setSelector(R.drawable.list_selector_background_pur);
        //listCat.getBackground().setColorFilter(Global.LabelColor, PorterDuff.Mode.SRC_OVER);
        listCat.setBackgroundColor(Global.LabelColor);
        listCat.setLongClickable(false);
        listCat.setPadding(4, 4, 4, 4);
        if (!Global.EnglishLang) {
            adapter4 = new CatListAdapter(MenuActivity.this, R.layout.cat_item, CategoryAlt);
        } else {
            adapter4 = new CatListAdapter(MenuActivity.this, R.layout.cat_item, CategoryEng);
        }
        listCat.setAdapter(adapter4);

        TextView text = (TextView) findViewById(R.id.TextMenu);
        text.setText(getString(R.string.tab2name));

        text = (TextView) findViewById(R.id.TextOrder);
        text.setText(getString(R.string.tab3name));

        Button butExit = (Button) findViewById(R.id.butInnerExit);
        butExit.setText(getString(R.string.tab3_exit));

        Button butInfo = (Button) findViewById(R.id.butInfo);
        butInfo.setText(getString(R.string.tab3_info));

        Button butMsg = (Button) findViewById(R.id.butQuickMsg);
        butMsg.setText(getString(R.string.tab3_msg));

        // update the number of items on the Order Tab counter
        try {
            if (JSONDishAry != null) {
                JSONObject JSONdishObj = JSONOrderAry.getJSONObject(jsonGetter3(JSONOrderAry, "dishes"));
                JSONArray JSONdishesAry = JSONdishObj.getJSONArray("dishes");
                ((TextView) findViewById(R.id.numItems)).setText("" + JSONdishesAry.length());
            }
        } catch (Exception e) {
            log("json updateOrderView Exception=" + e);
        }
    }

    private void updateOrderView() {
        // set the name of the Order
        final TextView oName = (TextView) findViewById(R.id.textheader);
        oName.setText(Global.OrderId);
        oName.setTextSize(mainFontSize);
        oName.setTextColor(Global.FontColor);
        // Allow them to change the table
        if (Global.AllowChangeTable) {
            oName.setOnLongClickListener(new View.OnLongClickListener() {
                public boolean onLongClick(View v) {
                    final Dialog dialog;
                    dialog = new Dialog(MenuActivity.this);
                    dialog.setContentView(R.layout.tables_grid);
                    dialog.show();
                    dialog.setCancelable(true);
                    dialog.setCanceledOnTouchOutside(true);

                    // Kill the normal header and set up a SPANNABLE
                    TextView tv = (TextView) dialog.findViewById(R.id.greetingTable);
                    tv.setVisibility(View.GONE);

                    String tit = getString(R.string.tab1_table);
                    SpannableStringBuilder ssBuilser = new SpannableStringBuilder(tit);
                    StyleSpan span = new StyleSpan(Typeface.NORMAL);
                    ScaleXSpan span1 = new ScaleXSpan(2);
                    ssBuilser.setSpan(span, 0, tit.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                    ssBuilser.setSpan(span1, 0, tit.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                    dialog.setTitle(ssBuilser);

                    // setup table buttons
                    Button[] buttons = new Button[]{};
                    buttons = new Button[MaxTABLES];
                    final int fsize = (int) (mainFontSize * 1.5);
                    final int butSize = Utils.getWidth(MenuActivity.this) / 8;

                    for (int i = 0; i < MaxTABLES; i++) {
                        String buttonID = "butt" + (i);
                        int resID = getResources().getIdentifier(buttonID, "id", getPackageName());
                        buttons[i] = ((Button) dialog.findViewById(resID));
                        String tmp = Global.tablenames.get(i);
                        if (tmp.equalsIgnoreCase("blank")) {
                            buttons[i].setVisibility(View.INVISIBLE);
                        } else if (tmp.equalsIgnoreCase("gone")) {
                            buttons[i].setVisibility(View.GONE);
                        } else {
                            buttons[i].setText(tmp);
                        }
                        buttons[i].setTextColor(Global.ButFontColor);
                        buttons[i].setMinWidth(butSize);
                        buttons[i].setMaxWidth(butSize);
                        states = new StateListDrawable();
                        states.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Global.LabelFontColor));
                        if (Global.TableID == i) {
                            Integer color = Color.parseColor("#F7F6A6");    // mellow YELLOW
                            states.addState(new int[]{}, new ColorDrawable(color));
                        } else {
                            states.addState(new int[]{}, new ColorDrawable(Global.ButColor));
                        }
                        buttons[i].setBackgroundDrawable(states);
                        buttons[i].setTextSize(fsize);
                        buttons[i].setOnClickListener(new View.OnClickListener() {
                            public void onClick(View v) {
                                String value = v.getTag().toString();
                                Global.TableID = Integer.valueOf(value);
                                Global.TableName = Global.tablenames.get(Global.TableID);
                                Global.TableTime = Utils.GetTime();
                                // Update the OrderID
                                String curTime = Utils.GetDateTime();
                                Global.OrderId = curTime + "-" + Global.TableName;
                                oName.setText(Global.OrderId);
                                // Update the JSON
                                try {
                                    jsonSetter(JSONOrderAry, "tablename", Global.TableName);
                                    jsonSetter(JSONOrderAry, "orderid", Global.OrderId);
                                    jsonSetter(JSONOrderAry, "tabletime", Global.TableTime);
                                    jsonSetter(JSONOrderAry, "currenttableid", Global.TableID);
                                } catch (Exception e) {
                                    log("JSONOrder Exception=" + e);
                                }

                                dialog.dismiss();
                            }
                        });
                    }
                    return true;
                }
            });
        }

        // update the order list
        JSONORDERLIST.clear();

        // Build an ArrayList for the orderAdapter from the Json dishes
        try {
            if (JSONDishAry != null) {
                JSONObject JSONdishObj = JSONOrderAry.getJSONObject(jsonGetter3(JSONOrderAry, "dishes"));
                JSONArray JSONdishesAry = JSONdishObj.getJSONArray("dishes");
                for (int i = 0; i < JSONdishesAry.length(); i++) {
                    JSONArray jd = JSONdishesAry.getJSONArray(i);
                    JSONORDERLIST.add(jd);
                }
            }
        } catch (Exception e) {
            log("json updateOrderView Exception=" + e);
        }
        listOrder = (ListView) findViewById(R.id.listOrder);
        orderAdapter = new OrderAdapter(MenuActivity.this, R.layout.list_item, JSONORDERLIST);
        listOrder.setAdapter(orderAdapter);

        // update the text for the Order Total RMB
        TextView text = (TextView) findViewById(R.id.textTotal);
        text.setText(getString(R.string.tab3_rmb) + " " + Integer.toString(getOrderTotalRMB()));
        text.setTextColor(Global.FontColor);
        OrderTotalString = "Total : RMB " + Integer.toString(getOrderTotalRMB());
        Global.TotalRMB = getOrderTotalRMB();

        Button send = (Button) findViewById(R.id.sendbutton);
        send.setTextSize((float) (mainFontSize * 1.4));
        send.setText(getString(R.string.tab3_send));

        // update the number of items on the Order Tab counter
        ((TextView) findViewById(R.id.numItems)).setText("" + JSONORDERLIST.size());
    }

    private void setupTablesView() {
        // setup table buttons
        Button[] buttons = new Button[]{};
        buttons = new Button[MaxTABLES];
        final int fsize = (int) (mainFontSize * 1.5);

        Button out = (Button) findViewById(R.id.butOuterExit);
        out.setText(getString(R.string.tab3_exit));
        out.setTextColor(Global.ButFontColor);
        states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Global.LabelFontColor));
        states.addState(new int[]{}, new ColorDrawable(Global.ButColor));
        out.setBackgroundDrawable(states);
        out.setTextSize(mainFontSize);
        out.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
                Global.MenuStarted = false;
            }
        });
        if (!Global.ShowBigExit) out.setVisibility(View.INVISIBLE);

        // set up the inner language switcher button
        Button langButA = (Button) findViewById(R.id.langA);
        langButA.setTextSize(mainFontSize);
        langButA.setTextColor(Global.ButFontColor);
        states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Global.LabelFontColor));
        states.addState(new int[]{}, new ColorDrawable(Global.ButColor));
        langButA.setBackgroundDrawable(states);
        langButA.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Configuration config = getBaseContext().getResources().getConfiguration();
                lc = new Locale("zh");
                Locale.setDefault(lc);
                config.locale = lc;
                getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
                Global.EnglishLang = false;
                setupHeaders();
            }
        });

        // set up the outer language switcher button
        Button langButE = (Button) findViewById(R.id.langE);
        langButE.setTextSize(mainFontSize);
        langButE.setTextColor(Global.ButFontColor);
        states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Global.LabelFontColor));
        states.addState(new int[]{}, new ColorDrawable(Global.ButColor));
        langButE.setBackgroundDrawable(states);
        langButE.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Configuration config = getBaseContext().getResources().getConfiguration();
                lc = new Locale("en");
                Locale.setDefault(lc);
                config.locale = lc;
                getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
                Global.EnglishLang = true;
                setupHeaders();
            }
        });

        // Set up for TABLE CHOOSING
        lltop = (LinearLayout) findViewById(R.id.LLChooseTable);
        lltop.setBackgroundColor(Global.BackColor);

        TextView tv = (TextView) findViewById(R.id.greetingTable);
        tv.setText(getString(R.string.tab1_table));
        tv.setTextColor(Global.FontColor);
        tv.setTextSize(fsize);

        int butSize = Utils.getWidth(MenuActivity.this) / 8;

        for (int i = 0; i < MaxTABLES; i++) {
            String buttonID = "butt" + (i);
            int resID = getResources().getIdentifier(buttonID, "id", getPackageName());
            buttons[i] = ((Button) findViewById(resID));
            String tmp = Global.tablenames.get(i);
            if (tmp.equalsIgnoreCase("blank")) {
                buttons[i].setVisibility(View.INVISIBLE);
            } else if (tmp.equalsIgnoreCase("gone")) {
                buttons[i].setVisibility(View.GONE);
            } else {
                buttons[i].setText(tmp);
            }
            buttons[i].setTextColor(Global.ButFontColor);
            buttons[i].setMinWidth(butSize);
            buttons[i].setMaxWidth(butSize);
            states = new StateListDrawable();
            states.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Global.LabelFontColor));
            states.addState(new int[]{}, new ColorDrawable(Global.ButColor));
            buttons[i].setBackgroundDrawable(states);
            buttons[i].setTextSize(fsize);
            buttons[i].setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    String value = v.getTag().toString();
                    Global.TableID = Integer.valueOf(value);
                    Global.TableName = Global.tablenames.get(Global.TableID);
                    Global.TableTime = Utils.GetTime();
                    setupNewOrder();
                    if (Global.ChooseGuests) {
                        vfTop.setDisplayedChild(TOP_GUESTS);
                    } else if (Global.DisplaySpecials) {
                        vfTop.setDisplayedChild(TOP_SPECIALS);
                    } else {
                        vfTop.setDisplayedChild(TOP_MENU);
                    }
                }
            });
        }
        buttons[TakeOutTable].setMinWidth(butSize);
        buttons[TakeOutTable].setMaxWidth(butSize);
        buttons[TakeOutTable].setTextSize((float) (fsize / 1.2));

        // Set up for GUEST NUMBER CHOOSING
        Button[] buttonsg = new Button[]{};
        buttonsg = new Button[MaxGuests];
        lltop = (LinearLayout) findViewById(R.id.LLChooseGuests);
        lltop.setBackgroundColor(Global.BackColor);

        tv = (TextView) findViewById(R.id.greetingGuests);
        tv.setText(getString(R.string.tab1_guests));
        tv.setTextColor(Global.FontColor);
        tv.setTextSize(fsize);

        for (int i = 0; i < MaxGuests; i++) {
            String buttonID = "buttg" + (i);
            int resID = getResources().getIdentifier(buttonID, "id", getPackageName());
            buttonsg[i] = ((Button) findViewById(resID));
            buttonsg[i].setText(guestNumbers[i]);
            buttonsg[i].setTextColor(Global.ButFontColor);
            buttonsg[i].setMinWidth((int) (Utils.getWidth(MenuActivity.this) / 8.0));
            buttonsg[i].setMaxWidth((int) (Utils.getWidth(MenuActivity.this) / 8.0));
            states = new StateListDrawable();
            states.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Global.LabelFontColor));
            states.addState(new int[]{}, new ColorDrawable(Global.ButColor));
            buttonsg[i].setBackgroundDrawable(states);
            buttonsg[i].setTextSize(fsize);
            buttonsg[i].setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    String value = v.getTag().toString();
                    Global.Guests = value;
                    if (Global.DisplaySpecials) {
                        vfTop.setDisplayedChild(TOP_SPECIALS);
                    } else {
                        vfTop.setDisplayedChild(TOP_MENU);
                    }
                }
            });
        }
        buttons[0].setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                getPassword(Global.AdminPin, 1);
                return true;
            }
        });
        /*
        buttons[TakeOutTable].setOnLongClickListener(new View.OnLongClickListener() {
        	public boolean onLongClick(View v) {
        		if (Global.AllowBackdoor) {
        			// they long pressed on last table, so let them in if they next long press on first Table
        			allowAccess = true;
        		}
        		return true;
        	}
        });
        */
    }

    private void processMenu() {
        Global.MenuMaxItems = menuItem.length;
        Global.NumCategory = categoryAll.length;
        Global.NumSpecials = 0;
        Global.fetchURL200.clear();

        // Loop through each line and populate the URL strings for image loading
        for (int i = 0; i < menuItem.length; i++) {
            // parse each line into columns using the divider character "|"
            String[] menuColumns = menuItem[i].split("\\|");
            // if it is a special, then bump the counter
            if (menuColumns[1].equals("specials")) Global.NumSpecials++;
            // build the picture array lists
            String menuPic = menuColumns[3];
            String menuPic200 = Global.ProtocolPrefix + Global.ServerIP + "/" + Global.SMID + "/" + Global.PICBASE200 + menuPic;
            String menuPic800 = Global.ProtocolPrefix + Global.ServerIP + "/" + Global.SMID + "/" + Global.PICBASE800 + menuPic;
            Global.fetchURL200.add(i, menuPic200);
            Global.fetchURL800.add(i, menuPic800);
        }
    }

    private void setupHeaders() {
        // We have headers in the TABLES, GUESTS and SPECIALS screens which can be displayed initially
        // This will update the headers in the case of a language switch
        TextView tv = (TextView) findViewById(R.id.greetingTable);
        tv.setText(getString(R.string.tab1_table));

        tv = (TextView) findViewById(R.id.greetingGuests);
        tv.setText(getString(R.string.tab1_guests));

        Button out = (Button) findViewById(R.id.butOuterExit);
        out.setText(getString(R.string.tab3_exit));

        buildSpecials();
    }

    private void buildSpecials() {
        if (Global.NumSpecials > 0) {
            // build the specials view layout
            lltop = (LinearLayout) findViewById(R.id.LLShowSpecials);
            lltop.setBackgroundColor(Global.BackColor);

            TextView tvtop = (TextView) findViewById(R.id.specials_title);
            tvtop.setTextColor(Global.FontColor);
            tvtop.setText(getString(R.string.tab2_dailys));
            tvtop.setTextSize((float) (mainFontSize * 1.5));

            Button bt = (Button) findViewById(R.id.goMenu);
            bt.setText(getString(R.string.tab1_gotomenu));
            bt.setTextSize((float) (mainFontSize * 1.3));

            LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            int wide = Utils.getPicHeight(MenuActivity.this) / 8;  // size up the pic to % of screen width
            int wide2 = (int) (Utils.getPicHeight(MenuActivity.this) / 1.5);  // size up the pic to % of screen width
            LinearLayout.LayoutParams params2 = new LinearLayout.LayoutParams(wide, (int) (wide * 1.5));
            LinearLayout.LayoutParams params3 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            LinearLayout.LayoutParams params4 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

            LinearLayout llSI = (LinearLayout) findViewById(R.id.LLSpecItems);
            llSI.removeAllViews();

            // the Specials are first in menu list, so loop through them and add views with the details
            for (int i = Global.NumSpecials - 1; i >= 0; i--) {
                String[] specCol = menuItem[i].split("\\|");
                String catColumns = specCol[1];
                String[] itemColumns = specCol[2].split("\\\\");
                String[] descColumns = specCol[4].split("\\\\");
                String price = specCol[6];

                String dishtitle, dishdesc;

                if (!Global.EnglishLang) {
                    dishtitle = itemColumns[1];
                    dishdesc = descColumns[1];
                } else {
                    dishtitle = itemColumns[0];
                    dishdesc = descColumns[0];
                }

                if (catColumns.equals("specials")) {
                    // add a LL H for this special item
                    // add the pic
                    llSI.setOrientation(LinearLayout.VERTICAL);
                    llSI.setLayoutParams(params1);
                    llSI.setGravity(Gravity.LEFT);
                    addDividerLine(llSI);

                    LinearLayout llH = new LinearLayout(MenuActivity.this);
                    llH.setOrientation(LinearLayout.HORIZONTAL);
                    llH.setPadding(10, 4, 10, 4);
                    llH.setGravity(Gravity.CENTER);
                    llH.setLayoutParams(params1);

                    ImageView mImg = new ImageView(MenuActivity.this);
                    mImg.setPadding(4, 4, 4, 4);
                    mImg.setClickable(true);
                    mImg.setFocusable(false);
                    mImg.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    mImg.setBackgroundResource(R.drawable.popuplt);
                    mImg.setLayoutParams(params2);
                    // set up the Specials single picture ZOOM view if they click on the image
                    final Integer finalI = i;
                    mImg.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            final ImageView bigimage = (ImageView) findViewById(R.id.specialSingleImg);
                            vfTop.setDisplayedChild(TOP_DISH_PIC);
                            String imageurl = Global.fetchURL800.get(finalI);
                            // Lazy load the image with Picasso
                            get()
                                    .load(imageurl)
                                    .into(bigimage);
                            bigimage.setOnClickListener(new OnClickListener() {
                                public void onClick(View v) {
                                    vfTop.setDisplayedChild(TOP_SPECIALS);
                                }
                            });
                        }
                    });
                    llH.addView(mImg);

                    // add a LL V (llV) for the text items
                    LinearLayout llV = new LinearLayout(MenuActivity.this);
                    llV.setOrientation(LinearLayout.VERTICAL);
                    llV.setPadding(20, 2, 10, 2);
                    llV.setGravity(Gravity.LEFT);
                    llV.setLayoutParams(params4);

                    // add the text items
                    TextView tit = new TextView(MenuActivity.this);
                    tit.setText(dishtitle);
                    tit.setTextColor(Global.FontColor);
                    tit.setTextSize((float) (mainFontSize * 1.0));
                    tit.setPadding(3, 1, 3, 1);
                    tit.setGravity(Gravity.LEFT);
                    tit.setLayoutParams(params4);
                    llV.addView(tit);
                    TextView desc = new TextView(MenuActivity.this);
                    desc.setText(dishdesc);
                    desc.setTextColor(Global.FontColor);
                    desc.setTextSize((float) (mainFontSize / 1.25));
                    desc.setPadding(3, 1, 3, 1);
                    desc.setMaxWidth(wide2);
                    desc.setGravity(Gravity.LEFT);
                    desc.setLayoutParams(params4);
                    llV.addView(desc);
                    TextView pric = new TextView(MenuActivity.this);
                    pric.setText(price + " ");
                    pric.setTextColor(Global.FontColor);
                    pric.setTextSize((float) (mainFontSize / 1.25));
                    pric.setPadding(3, 1, 3, 1);
                    pric.setGravity(Gravity.LEFT);
                    pric.setLayoutParams(params4);
                    llV.addView(pric);
                    llH.addView(llV);

                    // Add a Selector for direct selection of a Special item
                    LinearLayout llSelect = new LinearLayout(MenuActivity.this);
                    llSelect.setOrientation(LinearLayout.VERTICAL);
                    llSelect.setPadding(10, 2, 10, 2);
                    llSelect.setGravity(Gravity.RIGHT);
                    llSelect.setLayoutParams(params3);

                    Button target = new Button(MenuActivity.this);
                    target.setBackgroundResource(android.R.drawable.ic_menu_add);
                    //target.setBackgroundResource(R.drawable.popuplt);
                    //target.setBackgroundResource(R.drawable.text_but_selector_light);
                    target.setPadding(6, 6, 6, 6);
                    target.setMinimumWidth((int) (Utils.getWidth(MenuActivity.this) / 10.0));
                    target.setMaxWidth((int) (Utils.getWidth(MenuActivity.this) / 10.0));
                    target.setMinimumHeight((int) (Utils.getWidth(MenuActivity.this) / 10.0));
                    target.setMaxHeight((int) (Utils.getWidth(MenuActivity.this) / 10.0));
                    target.setGravity(Gravity.CENTER);
                    target.setLayoutParams(params4);
                    target.setTag(i);
                    target.setOnClickListener(new OnClickListener() {
                        public void onClick(View v) {
                            // Go to the Menu Flipper
                            vfTop.setDisplayedChild(TOP_MENU);
                            // Handle the clicked dish
                            Integer value = (Integer) v.getTag();
                            itemSetup(value);
                            showPopup(value);
                        }
                    });
                    llSelect.addView(target);
                    llH.addView(llSelect);

                    // Add the special item
                    llSI.addView(llH, 0);
                    picLoad(i, mImg);
                }
            }
            addDividerLine(llSI);

            Button tomenu = (Button) findViewById(R.id.goMenu);
            states = new StateListDrawable();
            states.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Global.ButColor));
            states.addState(new int[]{}, new ColorDrawable(Global.SelButColor));
            tomenu.setBackgroundDrawable(states);
            tomenu.setTextColor(Global.SelButFontColor);
            tomenu.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    vfTop.setDisplayedChild(TOP_MENU);
                }
            });
        } else {
            vfTop.setDisplayedChild(TOP_MENU);
        }
    }

    private void picLoad(int position, final ImageView img) {
        String fetchURL = Global.fetchURL200.get(position);
        img.setVisibility(View.VISIBLE);
        img.setTag(fetchURL);
        // Lazy load the image with Picasso
        get()
                .load(fetchURL)
                .placeholder(R.drawable.nopic)
                .error(R.drawable.nopic)
                .into(img);
        if (!Global.ShowPics) {
            img.setVisibility(View.GONE);
        }
    }

    private void failedAuth0() {
        AlertDialog alertDialog = new AlertDialog.Builder(MenuActivity.this).create();
        alertDialog.setTitle("Feedback");
        alertDialog.setIcon(android.R.drawable.stat_sys_warning);
        alertDialog.setMessage("Your feedback could not be sent at this time.");
        alertDialog.setCancelable(false);
        alertDialog.setButton("Back", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        alertDialog.show();
    }

    // Set up the intial blank JSON representation of the order.
    private JSONArray getNewJSONOrder() throws JSONException {
        JSONArray orderArr = new JSONArray();
        orderArr.put(createStr("orderid", Global.OrderId));
        orderArr.put(createInt("source", 2));        // see the orderSource[] for values
        orderArr.put(createStr("storeid", Global.SMID));
        orderArr.put(createStr("customername", ""));
        orderArr.put(createStr("response", ""));
        orderArr.put(createStr("tablename", Global.TableName));
        orderArr.put(createStr("waiter", Global.DeviceId));
        orderArr.put(createStr("guests", ""));
        orderArr.put(createStr("saletype", Global.SaleType));
        orderArr.put(createStr("date", ""));
        orderArr.put(createStr("tabletime", Global.TableTime));
        orderArr.put(createStr("sendtime", ""));
        orderArr.put(createStr("servertime", ""));    // gets set when received on the server side
        orderArr.put(createStr("sendtype", "1"));    // 1=normal, 2=resent
        orderArr.put(createStr("ticketnum", ""));
        orderArr.put(createInt("ordertotal", 0));
        orderArr.put(createStr("deliverynumber", ""));
        orderArr.put(createStr("deliveryaddress", ""));
        orderArr.put(createStr("deliveryaddress2", ""));
        orderArr.put(createInt("currenttableid", Global.TableID));

        // And the tabstate goes last, the PHP script on the server assumes this, so be warned....
        orderArr.put(createInt("tabstate", 0));

        //orderArr.put(createInt("printer1status",0));
        //orderArr.put(createInt("printer2status",0));
        //orderArr.put(createInt("printer3status",0));

        // Add the dish to the order ...
        // The dish information will be left blank and updated as the order is built
        orderArr.put(createArrayDishes());

        return orderArr;
    }

    private JSONObject createArrayDishes() throws JSONException {
        JSONDishAry = new JSONArray();

        JSONObject ary = new JSONObject();

        /* Each of the dishes look like this in JSON
        {  "dishes": [ { "dishId":  99 },
                       { "dishName":  aaa },
                       { "dishNameAlt":  aaa },
                       { "categoryName":  aaa },
                       { "categoryNameAlt":  aaa },
                       { "priceOptionId": 99 },
                       { "priceOptionName": aaa },
                       { "priceOptionNameAlt": aaa },

                       { "options": [ { "optionId": 99 },
                                      { "optionPrice": 99 },
                                      { "optionName": aaa },
                                      { "optionNameAlt": aaa } ]

                                    ,[ ... ]

                                    }

                       { "extras": [ { "extraId": 99 },
                             	     { "extraItem", 99 },
      	      	            	     { "extraIndex", 99 },
                                     { "extraPrice": 99 },
                                     { "extraName": aaa },
                                     { "extraNameAlt": aaa } ]

                                   ,[ ... ]

                                   }

                       { "qty":  99 },
                       { "priceUnitBase": 99 },
                       { "priceUnitTotal": 99 },
                       { "priceQtyTotal": 99 },
                       { "specIns":  aaa },
                       { "dishPrinted":  boolean }
        */

        ary.put("dishes", JSONDishAry);

        return ary;
    }

    private JSONObject createStr(String nam, String val) throws JSONException {
        JSONObject ary = new JSONObject();
        ary.put(nam, val);
        return ary;
    }

    private JSONObject createInt(String nam, Integer val) throws JSONException {
        JSONObject ary = new JSONObject();
        ary.put(nam, val);
        return ary;
    }

    private JSONObject createBoolean(String nam, Boolean val) throws JSONException {
        JSONObject ary = new JSONObject();
        ary.put(nam, val);
        return ary;
    }

    private Object jsonGetter(JSONArray json, String key) {
        Object value = null;
        for (int i = 0; i < json.length(); i++) {
            try {
                JSONObject obj = json.getJSONObject(i);
                String name = obj.getString("name");
                if (name.equalsIgnoreCase(key)) {
                    value = obj.get("value");
                }
            } catch (JSONException e) {
            }
        }
        return value;
    }

    private Object jsonGetter2(JSONArray json, String key) {
        Object value = null;
        for (int i = 0; i < json.length(); i++) {
            try {
                JSONObject obj = json.getJSONObject(i);
                if (obj.has(key)) {
                    value = obj.get(key);
                }
            } catch (JSONException e) {
                log("jsonGetter2 Exception=" + e);
            }
        }
        return value;
    }

    private int jsonGetter3(JSONArray json, String key) {
        int v = -1;
        for (int i = 0; i < json.length(); i++) {
            try {
                JSONObject obj = json.getJSONObject(i);
                if (obj.has(key)) {
                    v = i;
                }
            } catch (JSONException e) {
                log("jsonGetter3 Exception=" + e);
            }
        }
        return v;
    }

    private void jsonSetter(JSONArray array, String key, Object replace) {
        for (int i = 0; i < array.length(); i++) {
            try {
                JSONObject obj = array.getJSONObject(i);
                if (obj.has(key)) {
                    obj.putOpt(key, replace);
                }
            } catch (JSONException e) {
                log("jsonSetter exception");
            }
        }
    }

    public static JSONArray RemoveJSONArray(JSONArray jarray, int pos) {
        JSONArray Njarray = new JSONArray();
        try {
            for (int i = 0; i < jarray.length(); i++) {
                if (i != pos)
                    Njarray.put(jarray.get(i));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Njarray;
    }

    // Log helper function
    private void log(String message) {
        log(message, null);
    }

    private void log(String message, Throwable e) {
        if (mLog != null) {
            try {
                mLog.println(message);
            } catch (IOException ex) {
            }
        }
    }

    private void writeOutFile(File fildir, String fname, String fcontent) {
        File writeFile = new File(fildir, fname);
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(writeFile, false), "UTF-8"));
            writer.write(fcontent);
            writer.flush();
            writer.close();
        } catch (Exception e) {
            log("MenuActivity: WriteOutFile Exception: Dir=" + fildir + " fname=" + fname);
        }
    }

    private void pushItOut(final String msg) {
        final ProgressDialog pd = ProgressDialog.show(MenuActivity.this, "Sending", "Sending message...", true, false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            pd.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.INVISIBLE);
        new Thread(new Runnable() {
            public void run() {
                try {
                    if (haveNetworkConnection()) {
                        // publish the message
                        String url = String.format(Locale.US, Global.BrokerURLFormat, Global.ServerIP, Global.BrokerPort);
                        mMemStore = new MemoryPersistence();
                        mClient = new MqttClient(url, Global.DeviceId, mMemStore);
                        // publish the msg to the topic
                        MqttTopic orderTopic = mClient.getTopic(Global.BrokerMsgTopic);
                        MqttMessage message = new MqttMessage(msg.getBytes());
                        message.setQos(MQTT_QOS_2);
                        mOpts = new MqttConnectOptions();
                        mOpts.setCleanSession(MQTT_CLEAN_SESSION);
                        mOpts.setUserName(MQTT_USER);
                        mOpts.setPassword(MQTT_PASSWORD);
                        mClient.connect(mOpts);
                        orderTopic.publish(message);
                        mClient.disconnect();
                        log("Sent MQTT Order. Topic=" + orderTopic.getName() + " length=" + JSONOrderStr.length());
                    } else {
                        mHandler.post(feedbackNotSent);
                        log("MQTT Msg not sent");
                    }
                } catch (Exception e) {
                    log("MQTT Msg not sent, e=" + e);
                    mHandler.post(feedbackNotSent);
                }
                pd.dismiss();
                feedbackSent = true;
                mHandler.post(mUpdateResults);
            }
        }).start();
    }

    /*
	// Count the number of dishes on the table tab that have not been printed
	// Set to flag to exclude drinks
	private int unprintedDishCount(boolean excludeDrinks) {
		int udc=0;
		try {
			JSONArray JSONOrderAry = new JSONArray(JSONOrderStr);
			JSONObject JSONdishObj = JSONOrderAry.getJSONObject(jsonGetter3(JSONOrderAry,"dishes"));
			JSONArray jda = JSONdishObj.getJSONArray("dishes");
			int numdish = jda.length();
			//log("Number of total dishes=" + numdish);
			if (numdish > 0) {
				JSONArray JSONdishesAry = JSONdishObj.getJSONArray("dishes");
				for(int i=0; i<JSONdishesAry.length(); i++){
					JSONArray jd = JSONdishesAry.getJSONArray(i);
					// got each dish, now check the printed status for this dish
					Boolean printed = (Boolean) jsonGetter2(jd,"dishPrinted");
					// exclude drinks if needed ...
	                String dishcat = jsonGetter2(jd,"categoryName").toString();
	                if (excludeDrinks) {
		                if (!dishcat.equalsIgnoreCase("Drinks")) {
		                	if (!printed) udc++;
		                }
	                } else {
	                	if (!printed) udc++;
	                }
				}
			}
		} catch (Exception e) {
			log("json unprintedDishCount Exception=" + e);
		}
		//log("Table=" + tabid +" Unprinted dish count=" + udc);
		return udc;
	}
	*/

    // Count the number of dishes on the table tab that have not been printed which can be printed (not filtered)
    // Boolean True=exclude categories based on the filter
    // int printer number to determine which filter to use
    private int unprintedDishCount(Boolean filter, int printernumber) {
        int udc = 0;
        try {
            JSONArray JSONOrderAry = new JSONArray(JSONOrderStr);
            JSONObject JSONdishObj = JSONOrderAry.getJSONObject(jsonGetter3(JSONOrderAry, "dishes"));
            JSONArray jda = JSONdishObj.getJSONArray("dishes");
            int numdish = jda.length();
            //log("Number of total dishes=" + numdish);
            if (numdish > 0) {
                JSONArray JSONdishesAry = JSONdishObj.getJSONArray("dishes");
                for (int i = 0; i < JSONdishesAry.length(); i++) {
                    JSONArray jd = JSONdishesAry.getJSONArray(i);
                    // got each dish, now check the printed status for this dish
                    Boolean printed = (Boolean) jsonGetter2(jd, "dishPrinted");
                    // Dont P2 if counterOnly override
                    Boolean counterOnly = (Boolean) jsonGetter2(jd, "counterOnly");
                    // exclude item if needed based on category filter ...
                    Integer dishcatid = (Integer) jsonGetter2(jd, "categoryId");
                    //log("i=" + i + " dishcatid=" + dishcatid);
                    if (filter) {
                        if (printernumber == 2) {
                            if (P2Filter.get(dishcatid)) {
                                if (!printed) {
                                    if (!counterOnly) udc++;
                                }
                            }
                        } else if (printernumber == 3) {
                            if ((P3Filter.get(dishcatid)) || (counterOnly)) {
                                if (!printed) {
                                    udc++;
                                }
                            }
                        } else {
                            if (!printed) udc++;
                        }
                    } else {
                        if (!printed) udc++;
                    }
                }
            }
        } catch (Exception e) {
            log("json unprintedDishCount Table Exception=" + e);
        }
        //log("Table=" + tabid +" Unprinted dish count=" + udc);
        return udc;
    }

    private void getPassword(String pw, int returnID) {
        final Dialog dialogPW;
        dialogPW = new Dialog(MenuActivity.this);
        dialogPW.setContentView(R.layout.password);
        dialogPW.setCancelable(true);
        dialogPW.setCanceledOnTouchOutside(true);

        LinearLayout llpp = (LinearLayout) dialogPW.findViewById(R.id.LLpinpad);
        //llpp.setBackgroundColor(Global.BackColor);

        etPassword1 = (EditText) dialogPW.findViewById(R.id.etPassword1);
        etPassword1.setRawInputType(Configuration.KEYBOARD_12KEY);
        etPassword2 = (EditText) dialogPW.findViewById(R.id.etPassword2);
        etPassword2.setRawInputType(Configuration.KEYBOARD_12KEY);
        etPassword3 = (EditText) dialogPW.findViewById(R.id.etPassword3);
        etPassword3.setRawInputType(Configuration.KEYBOARD_12KEY);
        etPassword4 = (EditText) dialogPW.findViewById(R.id.etPassword4);
        etPassword4.setRawInputType(Configuration.KEYBOARD_12KEY);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        // set the starting selected to et1
        etPassword1.requestFocus();

        // setup the text watchers
        watcher1 = new GenericTextWatcher(etPassword1, pw, returnID, dialogPW);
        etPassword1.addTextChangedListener(watcher1);
        watcher2 = new GenericTextWatcher(etPassword2, pw, returnID, dialogPW);
        etPassword2.addTextChangedListener(watcher2);
        watcher3 = new GenericTextWatcher(etPassword3, pw, returnID, dialogPW);
        etPassword3.addTextChangedListener(watcher3);
        watcher4 = new GenericTextWatcher(etPassword4, pw, returnID, dialogPW);
        etPassword4.addTextChangedListener(watcher4);

        // setup the title
        String tit = getString(R.string.msg_password);
        SpannableStringBuilder ssBuilser = new SpannableStringBuilder(tit);
        StyleSpan span = new StyleSpan(Typeface.NORMAL);
        ScaleXSpan span1 = new ScaleXSpan(2);
        ssBuilser.setSpan(span, 0, tit.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        ssBuilser.setSpan(span1, 0, tit.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        dialogPW.setTitle(ssBuilser);
        dialogPW.show();
    }

    private class GenericTextWatcher implements TextWatcher {
        private View view;
        private String pw;
        private int returnID;
        private Dialog dialogPW;

        private GenericTextWatcher(View view, String pw, int returnID, Dialog dialogPW) {
            this.view = view;
            this.pw = pw;
            this.returnID = returnID;
            this.dialogPW = dialogPW;
        }

        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        public void afterTextChanged(Editable editable) {
            switch (view.getId()) {
                case R.id.etPassword1:
                    etPassword2.requestFocus();
                    break;
                case R.id.etPassword2:
                    etPassword3.requestFocus();
                    break;
                case R.id.etPassword3:
                    etPassword4.requestFocus();
                    break;
                case R.id.etPassword4:
                    // turn off keyboard
                    etPassword4.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        // The next one doesnt work, but the following did.
                        // stackoverflow.com/questions/1109022/close-hide-the-android-soft-keyboard
                        //imm.toggleSoftInput(0, InputMethodManager.HIDE_IMPLICIT_ONLY);
                        imm.hideSoftInputFromWindow(etPassword4.getWindowToken(), 0);
                    }
                    dialogPW.dismiss();

                    if (pwMatch()) {
                        switch (returnID) {
                            case 1:
                                // Handle Password access for the settings
                                Intent kintent = new Intent(getApplicationContext(), SettingsActivity.class);
                                kintent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_NO_HISTORY);
                                startActivity(kintent);
                                finish();
                                break;
                        }
                    }
                    break;
            }
        }
    }

    private boolean pwMatch() {
        Boolean result = false;
        String pw = etPassword1.getText().toString();
        pw = pw + etPassword2.getText().toString();
        pw = pw + etPassword3.getText().toString();
        pw = pw + etPassword4.getText().toString();
        if (pw.equals(Global.AdminPin)) result = true;
        return (result);
    }

    /**
     * A Debounced OnClickListener
     * Rejects clicks that are too close together in time.
     * This class is safe to use as an OnClickListener for multiple views, and will debounce each one separately.
     */
    public abstract class DebouncedOnClickListener implements View.OnClickListener {

        private final long minimumInterval;
        private Map<View, Long> lastClickMap;

        /**
         * Implement this in your subclass instead of onClick
         *
         * @param v The view that was clicked
         */
        public abstract void onDebouncedClick(View v);

        /**
         * The one and only constructor
         *
         * @param minimumIntervalMsec The minimum allowed time between clicks - any click sooner than this after a previous click will be rejected
         */
        public DebouncedOnClickListener(long minimumIntervalMsec) {
            this.minimumInterval = minimumIntervalMsec;
            this.lastClickMap = new WeakHashMap<View, Long>();
        }

        @Override
        public void onClick(View clickedView) {
            Long previousClickTimestamp = lastClickMap.get(clickedView);
            long currentTimestamp = SystemClock.uptimeMillis();

            lastClickMap.put(clickedView, currentTimestamp);
            if (previousClickTimestamp == null || (currentTimestamp - previousClickTimestamp.longValue() > minimumInterval)) {
                onDebouncedClick(clickedView);
                log("Clicked.");
            } else {
                log("Click ignored. Dur=" + (currentTimestamp - previousClickTimestamp.longValue()));
            }
        }
    }
}
