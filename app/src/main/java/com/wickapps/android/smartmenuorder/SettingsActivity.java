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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ScaleXSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import com.epson.EpsonCom.EpsonCom.ERROR_CODE;
import com.epson.EpsonCom.EpsonComDevice;
import com.epson.EpsonCom.EpsonComDeviceParameters;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

public class SettingsActivity extends Activity {

    private static Button bDel, bBack, bSave, bReload;
    private static File dir;
    private static ImageView img;
    private SharedPreferences prefs;
    private static Boolean downloadSuccess = false;
    private String serverMv;
    private static File textDir;
    private static File retryDir;
    Dialog dialog;

    private ConnectionLog mLog;

    //EpsonCom Objects
    public static EpsonComDevice POSDev;
    public static EpsonComDeviceParameters POSParams;
    public static ERROR_CODE err;

    private static String[] filesLocalName = new String[]{"menufile.txt", "category.txt", "kitchen.txt", "settings.txt", "options.txt", "extras.txt"};
    private static String[] filesSourceName = new String[]{"", "", "", "", "", ""};
    private static String[] filesText = new String[]{"", "", "", "", "", ""};

    private ArrayList<String> catList = new ArrayList<String>();
    protected ArrayList<CharSequence> selectedP2Cat = new ArrayList<CharSequence>();
    protected ArrayList<CharSequence> selectedP3Cat = new ArrayList<CharSequence>();
    protected Button p2CatButton, p3CatButton;

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        /*
        if (hasFocus) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                | View.INVISIBLE);
        }
        */
    }

    //	Need handler for callbacks to the UI thread
    final Handler mHandler = new Handler();

    final Runnable noConnection = new Runnable() {
        public void run() {
            failedAuth0();
        }
    };
    final Runnable exceptionConnection = new Runnable() {
        public void run() {
            failedAuth2();
        }
    };
    final Runnable exceptionReload = new Runnable() {
        public void run() {
            TextView txt = (TextView) findViewById(R.id.textLoad);
            txt.setText("Failed");
            failedAuth3();
        }
    };
    final Runnable reloaded = new Runnable() {
        public void run() {
            TextView txt = (TextView) findViewById(R.id.textLoad);
            txt.setText("Success");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.sync_layout);

        /*
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.INVISIBLE);
        }
        */

        // Set the directory to save text files
        textDir = new File(getFilesDir(), "SmartMenuFiles");
        if (!textDir.exists())
            textDir.mkdirs();

        try {
            mLog = new ConnectionLog(this);
        } catch (IOException e) {
        }

        // show the JSON settings for deugging purposes. Need to create the TextView
        /*
        try {
            String tmp="";
            for (int i=0; i<Global.Settings.length(); i++) {
                JSONObject obj = Global.Settings.getJSONObject(i);
                tmp = tmp + i + "---" + obj.getString("name") + "---" + obj.getString("value") + "\n";
            }
            TextView tv = (TextView) findViewById(R.id.jsonjson);
            tv.setText(tmp);
            tv.setVisibility(View.GONE);
        } catch (JSONException e) {
        }
        */

        updateConnectionStatus();

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final Editor prefEdit = prefs.edit();
        //Global.PublicCloud = (new Boolean(prefs.getBoolean("publiccloud", true)));
        Global.PublicCloud = (Boolean) jsonGetter(Global.Settings, "publiccloud");
        RadioButton rb1 = (RadioButton) findViewById(R.id.ctBut1);
        RadioButton rb2 = (RadioButton) findViewById(R.id.ctBut2);
        if (Global.PublicCloud) {
            rb1.setChecked(false);
            rb2.setChecked(true);
        } else {
            rb1.setChecked(true);
            rb2.setChecked(false);
        }

        Global.Printer1Type = (Boolean) jsonGetter(Global.Settings, "printer1type");
        RadioButton p1rbpt1 = (RadioButton) findViewById(R.id.p1ptBut1);
        RadioButton p1rbpt2 = (RadioButton) findViewById(R.id.p1ptBut2);
        if (Global.Printer1Type) {
            p1rbpt1.setChecked(true);
            p1rbpt2.setChecked(false);
        } else {
            p1rbpt1.setChecked(false);
            p1rbpt2.setChecked(true);
        }

        Global.Printer2Type = (Boolean) jsonGetter(Global.Settings, "printer2type");
        RadioButton p2rbpt1 = (RadioButton) findViewById(R.id.p2ptBut1);
        RadioButton p2rbpt2 = (RadioButton) findViewById(R.id.p2ptBut2);
        if (Global.Printer2Type) {
            p2rbpt1.setChecked(true);
            p2rbpt2.setChecked(false);
        } else {
            p2rbpt1.setChecked(false);
            p2rbpt2.setChecked(true);
        }

        Global.Printer3Type = (Boolean) jsonGetter(Global.Settings, "printer3type");
        RadioButton p3rbpt1 = (RadioButton) findViewById(R.id.p3ptBut1);
        RadioButton p3rbpt2 = (RadioButton) findViewById(R.id.p3ptBut2);
        if (Global.Printer3Type) {
            p3rbpt1.setChecked(true);
            p3rbpt2.setChecked(false);
        } else {
            p3rbpt1.setChecked(false);
            p3rbpt2.setChecked(true);
        }

        //Global.StartEnglish = (new Boolean(prefs.getBoolean("startenglish", false)));
        Global.StartEnglish = (Boolean) jsonGetter(Global.Settings, "startenglish");
        CheckBox cb4 = (CheckBox) findViewById(R.id.checkEng);
        if (Global.StartEnglish) {
            cb4.setChecked(true);
        } else {
            cb4.setChecked(false);
        }

        //Global.CheckWifi = (new Boolean(prefs.getBoolean("checkwifi", false)));
        Global.CheckWifi = (Boolean) jsonGetter(Global.Settings, "checkwifi");
        CheckBox cb6 = (CheckBox) findViewById(R.id.checkWifi);
        if (Global.CheckWifi) {
            cb6.setChecked(true);
        } else {
            cb6.setChecked(false);
        }

        //Global.ShowPics = (new Boolean(prefs.getBoolean("showpics", true)));
        Global.ShowPics = (Boolean) jsonGetter(Global.Settings, "showpics");
        CheckBox cb7 = (CheckBox) findViewById(R.id.showPics);
        if (Global.ShowPics) {
            cb7.setChecked(true);
        } else {
            cb7.setChecked(false);
        }

        //Global.AutoMenuReload = (new Boolean(prefs.getBoolean("automenureload", false)));
        Global.AutoMenuReload = (Boolean) jsonGetter(Global.Settings, "automenureload");
        CheckBox cb9 = (CheckBox) findViewById(R.id.autoMenuReload);
        if (Global.AutoMenuReload) {
            cb9.setChecked(true);
        } else {
            cb9.setChecked(false);
        }

        //Global.ShowRating = (new Boolean(prefs.getBoolean("showrating", false)));
        Global.ShowRating = (Boolean) jsonGetter(Global.Settings, "showrating");
        CheckBox cb10 = (CheckBox) findViewById(R.id.showRating);
        if (Global.ShowRating) {
            cb10.setChecked(true);
        } else {
            cb10.setChecked(false);
        }

        //Global.ShowBadges = (new Boolean(prefs.getBoolean("showbadges", true)));
        Global.ShowBadges = (Boolean) jsonGetter(Global.Settings, "showbadges");
        CheckBox cb11 = (CheckBox) findViewById(R.id.showBadges);
        if (Global.ShowBadges) {
            cb11.setChecked(true);
        } else {
            cb11.setChecked(false);
        }

        //Global.P2KitchenCodes = (new Boolean(prefs.getBoolean("p2kitchencodes", true)));
        Global.P2KitchenCodes = (Boolean) jsonGetter(Global.Settings, "p2kitchencodes");
        CheckBox cb23 = (CheckBox) findViewById(R.id.p2KitchenCodes);
        if (Global.P2KitchenCodes) {
            cb23.setChecked(true);
        } else {
            cb23.setChecked(false);
        }

        Global.P3KitchenCodes = (Boolean) jsonGetter(Global.Settings, "p3kitchencodes");
        CheckBox cb24 = (CheckBox) findViewById(R.id.p3KitchenCodes);
        if (Global.P3KitchenCodes) {
            cb24.setChecked(true);
        } else {
            cb24.setChecked(false);
        }

        Global.P1PrintSentTime = (Boolean) jsonGetter(Global.Settings, "p1printsenttime");
        CheckBox cbp1pst = (CheckBox) findViewById(R.id.p1PrintSentTime);
        if (Global.P1PrintSentTime) {
            cbp1pst.setChecked(true);
        } else {
            cbp1pst.setChecked(false);
        }

        Global.P2PrintSentTime = (Boolean) jsonGetter(Global.Settings, "p2printsenttime");
        CheckBox cbp2pst = (CheckBox) findViewById(R.id.p2PrintSentTime);
        if (Global.P2PrintSentTime) {
            cbp2pst.setChecked(true);
        } else {
            cbp2pst.setChecked(false);
        }

        Global.P3PrintSentTime = (Boolean) jsonGetter(Global.Settings, "p3printsenttime");
        CheckBox cbp3pst = (CheckBox) findViewById(R.id.p3PrintSentTime);
        if (Global.P3PrintSentTime) {
            cbp3pst.setChecked(true);
        } else {
            cbp3pst.setChecked(false);
        }

        Global.POS1Logo = (Boolean) jsonGetter(Global.Settings, "pos1logo");
        CheckBox cb19 = (CheckBox) findViewById(R.id.pos1logo);
        if (Global.POS1Logo) {
            cb19.setChecked(true);
        } else {
            cb19.setChecked(false);
        }

        //Global.POS1Enable = (new Boolean(prefs.getBoolean("pos1enable", false)));
        Global.POS1Enable = (Boolean) jsonGetter(Global.Settings, "pos1enable");
        CheckBox cb20 = (CheckBox) findViewById(R.id.pos1enable);
        if (Global.POS1Enable) {
            cb20.setChecked(true);
        } else {
            cb20.setChecked(false);
        }

        //Global.POS2Enable = (new Boolean(prefs.getBoolean("pos2enable", false)));
        Global.POS2Enable = (Boolean) jsonGetter(Global.Settings, "pos2enable");
        CheckBox cb21 = (CheckBox) findViewById(R.id.pos2enable);
        if (Global.POS2Enable) {
            cb21.setChecked(true);
        } else {
            cb21.setChecked(false);
        }

        //Global.POS3Enable = (new Boolean(prefs.getBoolean("pos3enable", false)));
        Global.POS3Enable = (Boolean) jsonGetter(Global.Settings, "pos3enable");
        CheckBox cb22 = (CheckBox) findViewById(R.id.pos3enable);
        if (Global.POS3Enable) {
            cb22.setChecked(true);
        } else {
            cb22.setChecked(false);
        }

        //Global.AutoOpenDrawer = (new Boolean(prefs.getBoolean("autoopendrawer", false)));
        Global.AutoOpenDrawer = (Boolean) jsonGetter(Global.Settings, "autoopendrawer");
        CheckBox cbaod = (CheckBox) findViewById(R.id.autoOpenDrawer);
        if (Global.AutoOpenDrawer) {
            cbaod.setChecked(true);
        } else {
            cbaod.setChecked(false);
        }

        //Global.DisplaySpecials = (new Boolean(prefs.getBoolean("displayspecials", true)));
        Global.DisplaySpecials = (Boolean) jsonGetter(Global.Settings, "displayspecials");
        CheckBox cbdisspec = (CheckBox) findViewById(R.id.displaySpecials);
        if (Global.DisplaySpecials) {
            cbdisspec.setChecked(true);
        } else {
            cbdisspec.setChecked(false);
        }

        //Global.ShowBigExit = (new Boolean(prefs.getBoolean("showbigexit", true)));
        Global.ShowBigExit = (Boolean) jsonGetter(Global.Settings, "showbigexit");
        CheckBox cbsbe = (CheckBox) findViewById(R.id.showBigExit);
        if (Global.ShowBigExit) {
            cbsbe.setChecked(true);
        } else {
            cbsbe.setChecked(false);
        }

        //Global.ShowDishInfo = (new Boolean(prefs.getBoolean("showdishinfo", false)));
        Global.ShowDishInfo = (Boolean) jsonGetter(Global.Settings, "showdishinfo");
        CheckBox cbsdi = (CheckBox) findViewById(R.id.showDishInfo);
        if (Global.ShowDishInfo) {
            cbsdi.setChecked(true);
        } else {
            cbsdi.setChecked(false);
        }

        //Global.ShowDishFeedback = (new Boolean(prefs.getBoolean("showdishfeedback", false)));
        Global.ShowDishFeedback = (Boolean) jsonGetter(Global.Settings, "showdishfeedback");
        CheckBox cbsdf = (CheckBox) findViewById(R.id.showDishFeedback);
        if (Global.ShowDishFeedback) {
            cbsdf.setChecked(true);
        } else {
            cbsdf.setChecked(false);
        }

        Global.ShowDishDescriptions = (Boolean) jsonGetter(Global.Settings, "showdishdescriptions");
        CheckBox cbsdd = (CheckBox) findViewById(R.id.showDishDescription);
        if (Global.ShowDishDescriptions) {
            cbsdd.setChecked(true);
        } else {
            cbsdd.setChecked(false);
        }

        Global.ChooseGuests = (Boolean) jsonGetter(Global.Settings, "chooseguests");
        CheckBox cbcgu = (CheckBox) findViewById(R.id.displayGuests);
        if (Global.ChooseGuests) {
            cbcgu.setChecked(true);
        } else {
            cbcgu.setChecked(false);
        }

        Global.ShowZoomPic = (Boolean) jsonGetter(Global.Settings, "showzoompic");
        CheckBox cbszp = (CheckBox) findViewById(R.id.showZoomPicture);
        if (Global.ShowZoomPic) {
            cbszp.setChecked(true);
        } else {
            cbszp.setChecked(false);
        }
        Global.AllowChangeTable = (Boolean) jsonGetter(Global.Settings, "allowchangetable");
        CheckBox cbact = (CheckBox) findViewById(R.id.allowChangeTable);
        if (Global.AllowChangeTable) {
            cbact.setChecked(true);
        } else {
            cbact.setChecked(false);
        }
        Global.PrintDishID = (Boolean) jsonGetter(Global.Settings, "printdishid");
        CheckBox cbprdid = (CheckBox) findViewById(R.id.printDishID);
        if (Global.PrintDishID) {
            cbprdid.setChecked(true);
        } else {
            cbprdid.setChecked(false);
        }

        //Global.ServerIP = jsonGetter(Global.Settings,"serverip").toString();
        //EditText et = (EditText) findViewById(R.id.serverIP);
        //et.setText(Global.ServerIP);
        Global.POS1Ip = jsonGetter(Global.Settings, "pos1ip").toString();
        EditText et = (EditText) findViewById(R.id.ip1);
        et.setText(Global.POS1Ip);
        Global.POS2Ip = jsonGetter(Global.Settings, "pos2ip").toString();
        et = (EditText) findViewById(R.id.ip2);
        et.setText(Global.POS2Ip);
        Global.POS3Ip = jsonGetter(Global.Settings, "pos3ip").toString();
        et = (EditText) findViewById(R.id.ip3);
        et.setText(Global.POS3Ip);
        Global.POSIp = jsonGetter(Global.Settings, "posmasterip").toString();
        et = (EditText) findViewById(R.id.ipmd);
        et.setText(Global.POSIp);
        Global.TakeOutIp = jsonGetter(Global.Settings, "takeoutip").toString();
        et = (EditText) findViewById(R.id.ipto);
        et.setText(Global.TakeOutIp);

        Global.MasterDeviceId = jsonGetter(Global.Settings, "masterdeviceid").toString();
        (et = (EditText) findViewById(R.id.masterDeviceId)).setText(Global.MasterDeviceId);
        Global.CustomerName = jsonGetter(Global.Settings, "customername").toString();
        (et = (EditText) findViewById(R.id.customerName)).setText(Global.CustomerName);
        Global.CustomerNameBrief = jsonGetter(Global.Settings, "customernamebrief").toString();
        (et = (EditText) findViewById(R.id.customerNameBrief)).setText(Global.CustomerNameBrief);

        Global.StoreAddress = jsonGetter(Global.Settings, "storeaddress").toString();
        (et = (EditText) findViewById(R.id.storeAddress)).setText(Global.StoreAddress);

        Global.FontScaleFactor = (Integer) jsonGetter(Global.Settings, "fontscalefactor");
        EditText fsf = (EditText) findViewById(R.id.fontScaleFactor);
        fsf.setText(String.valueOf(Global.FontScaleFactor));

        Global.SendOrderMode = (Integer) jsonGetter(Global.Settings, "sendordermode");
        EditText som = (EditText) findViewById(R.id.sendOrderMode);
        som.setText(String.valueOf(Global.SendOrderMode));

        Global.Printer1Copy = (Integer) jsonGetter(Global.Settings, "printer1copy");
        EditText p1c = (EditText) findViewById(R.id.etP1C);
        p1c.setText(String.valueOf(Global.Printer1Copy));

        Global.FontColor = Color.parseColor(jsonGetter(Global.Settings, "fontcolor").toString());
        Button butFC = (Button) findViewById(R.id.butFontColor);
        butFC.setBackgroundColor(Global.FontColor);

        Global.BackColor = Color.parseColor(jsonGetter(Global.Settings, "backcolor").toString());
        Button butBC = (Button) findViewById(R.id.butBackColor);
        butBC.setBackgroundColor(Global.BackColor);

        Global.HeaderColor = Color.parseColor(jsonGetter(Global.Settings, "headercolor").toString());
        Button butH = (Button) findViewById(R.id.butHeadColor);
        butH.setBackgroundColor(Global.HeaderColor);

        Global.ButFontColor = Color.parseColor(jsonGetter(Global.Settings, "butfontcolor").toString());
        Button butBFC = (Button) findViewById(R.id.butButFontColor);
        butBFC.setBackgroundColor(Global.ButFontColor);

        Global.ButColor = Color.parseColor(jsonGetter(Global.Settings, "mainbuttoncolor").toString());
        Button butMBC = (Button) findViewById(R.id.butMainButtonColor);
        butMBC.setBackgroundColor(Global.ButColor);

        Global.SelButFontColor = Color.parseColor(jsonGetter(Global.Settings, "selbutfontcolor").toString());
        Button butSBFC = (Button) findViewById(R.id.butSelButFontColor);
        butSBFC.setBackgroundColor(Global.SelButFontColor);

        Global.SelButColor = Color.parseColor(jsonGetter(Global.Settings, "selbutcolor").toString());
        Button butSBC = (Button) findViewById(R.id.butSelButColor);
        butSBC.setBackgroundColor(Global.SelButColor);

        Global.LabelFontColor = Color.parseColor(jsonGetter(Global.Settings, "labelfontcolor").toString());
        Button butLFC = (Button) findViewById(R.id.butLabelFontColor);
        butLFC.setBackgroundColor(Global.LabelFontColor);

        Global.LabelColor = Color.parseColor(jsonGetter(Global.Settings, "labelcolor").toString());
        Button butLC = (Button) findViewById(R.id.butLabelColor);
        butLC.setBackgroundColor(Global.LabelColor);

        TextView tv0 = (TextView) findViewById(R.id.AboutAppName);
        Map<String, String> map0 = new LinkedHashMap<String, String>();
        map0.put(getString(R.string.msg_about_app_name), Global.AppName);
        populateField(map0, tv0);

        TextView tv1 = (TextView) findViewById(R.id.AboutVersion);
        Map<String, String> map1 = new LinkedHashMap<String, String>();
        map1.put(getString(R.string.msg_about_version_code), getVersionName());
        populateField(map1, tv1);

        TextView tv2 = (TextView) findViewById(R.id.AboutSource);
        Map<String, String> map2 = new LinkedHashMap<String, String>();
        map2.put(getString(R.string.msg_about_filesource), Global.FileSource);
        populateField(map2, tv2);

        TextView tv3 = (TextView) findViewById(R.id.AboutServerIP);
        Map<String, String> map3 = new LinkedHashMap<String, String>();
        map3.put(getString(R.string.msg_about_serverIP), Global.ServerIP);
        populateField(map3, tv3);

        TextView tv5 = (TextView) findViewById(R.id.AboutSmartMenuID);
        Map<String, String> map5 = new LinkedHashMap<String, String>();
        map5.put(getString(R.string.msg_about_smartmenuid), Global.SMID);
        populateField(map5, tv5);

        TextView tv6 = (TextView) findViewById(R.id.AboutMenuVersion);
        Map<String, String> map6 = new LinkedHashMap<String, String>();
        map6.put(getString(R.string.msg_about_menuver), Global.MenuVersion);
        populateField(map6, tv6);

        TextView tv7 = (TextView) findViewById(R.id.AboutDeviceId);
        Map<String, String> map7 = new LinkedHashMap<String, String>();
        map7.put(getString(R.string.msg_about_deviceid), Global.DeviceId);
        populateField(map7, tv7);

        TextView tv7a = (TextView) findViewById(R.id.AboutDeviceIP);
        Map<String, String> map7a = new LinkedHashMap<String, String>();
        map7a.put(getString(R.string.msg_about_deviceip), Utils.getIpAddress(true));
        populateField(map7a, tv7a);

        // Include the actual IP addresses do they can see them with the status indicators
        TextView tvServerIP = (TextView) findViewById(R.id.label1a2);
        tvServerIP.setText(Global.ServerIP);
        TextView tvPosIP = (TextView) findViewById(R.id.labelPOS1a);
        tvPosIP.setText(Global.POSIp);
        TextView tvToIP = (TextView) findViewById(R.id.labelTOa);
        tvToIP.setText(Global.TakeOutIp);
        TextView tvPOS1IP = (TextView) findViewById(R.id.label2a);
        tvPOS1IP.setText(Global.POS1Ip);
        TextView tvPOS2IP = (TextView) findViewById(R.id.label3a);
        tvPOS2IP.setText(Global.POS2Ip);
        TextView tvPOS3IP = (TextView) findViewById(R.id.label4a);
        tvPOS3IP.setText(Global.POS3Ip);

        // Set up printer filters
        Global.P2FilterCats = jsonGetter(Global.Settings, "p2filtercats").toString();
        Button fc2 = (Button) findViewById(R.id.butP2FilterCats);
        fc2.setText(Global.P2FilterCats);

        Global.P3FilterCats = jsonGetter(Global.Settings, "p3filtercats").toString();
        Button fc3 = (Button) findViewById(R.id.butP3FilterCats);
        fc3.setText(Global.P3FilterCats);

        // set the selected filter for P2
        String[] eCat = Global.P2FilterCats.split(",");
        selectedP2Cat.clear();
        for (int i = 0; i < eCat.length; i++) {
            selectedP2Cat.add(eCat[i]);
        }
        eCat = Global.P3FilterCats.split(",");
        selectedP3Cat.clear();
        for (int i = 0; i < eCat.length; i++) {
            selectedP3Cat.add(eCat[i]);
        }

        // Buttons
        Button bReload = (Button) findViewById(R.id.butReload);
        bReload.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                TextView txt = (TextView) findViewById(R.id.textLoad);
                txt.setText("Reloading files ...");
                // reload the files from the server
                final ProgressDialog pd = ProgressDialog.show(SettingsActivity.this, "Reloading", "Loading files from the server...", true, false);
                new Thread(new Runnable() {
                    public void run() {
                        // see if we can ping the server first
                        try {
                            if ((!Global.CheckAvailability) || pingIP()) {
                                reloadTheFiles();
                                // success
                                mHandler.post(reloaded);
                            } else {
                                // failed to upload
                                mHandler.post(exceptionReload);
                            }
                        } catch (Exception e) {
                            // failed to upload
                            mHandler.post(exceptionReload);
                        }
                        pd.dismiss();
                    }
                }).start();
            }
        });

        Button bRefresh = (Button) findViewById(R.id.butRefreshIP);
        bRefresh.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                updateConnectionStatus();
            }
        });

        Button bBackBack = (Button) findViewById(R.id.butBackBack);
        bBackBack.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Intent kintent = new Intent(getApplicationContext(), MenuActivity.class);
                kintent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(kintent);
                finish();
            }
        });

        Button bUpload = (Button) findViewById(R.id.butUpload);
        bUpload.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // download the file list for the server spinner
                final ProgressDialog pd = ProgressDialog.show(SettingsActivity.this, "Uploading", "Uploading settings to the server...", true, false);
                new Thread(new Runnable() {
                    public void run() {
                        // see if we can ping the server first
                        try {
                            if ((!Global.CheckAvailability) || pingIP()) {
                                // increment the menu version
                                Integer mv = Integer.parseInt(Global.MenuVersion);
                                Integer newmv = mv + 1;
                                String formatmv = String.format("%04d", newmv);
                                Global.MenuVersion = formatmv;
                                log("Settings: Upload: New menu version=" + Global.MenuVersion);
                                // update the json
                                jsonSetter(Global.Settings, "menuversion", Global.MenuVersion);
                                // save the local cache settings.txt file
                                writeOutFile(Global.Settings.toString(4), filesLocalName[3]);
                                // upload the settings.txt file which has the menuversion
                                String fpath = Global.SMID;
                                String fname = filesLocalName[3];
                                File fbody = new File(textDir, fname);
                                int sc = Utils.Uploader(fbody, fname, fpath);
                                log("MenuEdit: Upload Status: fname=" + fname + " fpath=" + fpath + " fbody=" + fbody + " lenfbody=" + fbody.length() + " statusCode=" + sc);
                            } else {
                                // failed to upload
                                mHandler.post(exceptionConnection);
                            }
                        } catch (Exception e) {
                            // failed to upload
                            mHandler.post(exceptionConnection);
                        }
                        pd.dismiss();
                    }
                }).start();
            }
        });

        Button unSent = (Button) findViewById(R.id.butUnsent);
        unSent.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                final ArrayList<String> unsentItemList = new ArrayList<String>();
                ArrayAdapter<String> unsentAdapter;
                ListView listUnsent;

                retryDir = new File(getFilesDir(), "SmartMenuRetry");
                if (!retryDir.exists()) retryDir.mkdirs();

                final CustomDialog customDialog = new CustomDialog(SettingsActivity.this);
                LayoutInflater factory = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                final View textEntryView = factory.inflate(R.layout.queue_dialog, null);

                customDialog.setContentView(textEntryView);
                customDialog.show();
                customDialog.setCancelable(true);
                customDialog.setCanceledOnTouchOutside(true);

                File[] files = retryDir.listFiles();
                unsentItemList.clear();

                for (File f : files) unsentItemList.add(f.getName());

                listUnsent = (ListView) customDialog.findViewById(R.id.unsentItemList);
                unsentAdapter = new ArrayAdapter<String>(SettingsActivity.this, R.layout.list_item_unsent, unsentItemList);

                listUnsent.setAdapter(unsentAdapter);

                // set up a button, when they click, send all the items
                Button butSnd = (Button) customDialog.findViewById(R.id.butSndAll);
                butSnd.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        final ProgressDialog pd = ProgressDialog.show(SettingsActivity.this, "Sending", "Sending order(s) to the server...", true, false);
                        new Thread(new Runnable() {
                            public void run() {
                                if ((!Global.CheckAvailability) || pingIP()) {
                                    for (String fname : unsentItemList) {
                                        String postURL = Global.ProtocolPrefix + Global.ServerIP + Global.PosSaveOrderJsonURL;
                                        try {
                                            File readFile = new File(retryDir, fname);
                                            JSONArray JSONOrder = new JSONArray(Utils.ReadLocalFile(readFile));
                                            String orderid = jsonGetter2(JSONOrder, "orderid").toString();

                                            // update the sendtype so resend=2
                                            JSONObject obj = new JSONObject();
                                            obj.put("sendtpye", "2");
                                            JSONOrder.put(jsonGetter3(JSONOrder, "sendtype"), obj);

                                            int sc = Utils.SendMultipartJsonOrder(postURL, JSONOrder.toString(1), Global.SMID);
                                            log("Resent=" + orderid + " status code=" + sc);
                                            if (sc == 200) {
                                                if (readFile.delete()) {
                                                    log("file deleted:" + fname);
                                                } else {
                                                    log("file not deleted:" + fname);
                                                }
                                            }
                                        } catch (Exception e) {
                                            log("Resending from JSON failed");
                                        }
                                    }
                                }
                                pd.dismiss();
                                customDialog.dismiss();
                            }
                        }).start();
                    }
                });
                // set up a button, when they click, delete all the items
                Button butDel = (Button) customDialog.findViewById(R.id.butDelAll);
                butDel.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        File[] files = retryDir.listFiles();
                        if (files != null) {
                            for (int i = 0; i < files.length; i++) {
                                files[i].delete();
                            }
                        }
                        customDialog.dismiss();
                    }
                });
            }
        });

        newCatList();

        p2CatButton = (Button) findViewById(R.id.butP2FilterCats);
        p2CatButton.setText(Global.P2FilterCats);
        p2CatButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                showP2CatDialog();
            }
        });

        p3CatButton = (Button) findViewById(R.id.butP3FilterCats);
        p3CatButton.setText(Global.P3FilterCats);
        p3CatButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                showP3CatDialog();
            }
        });

        Button bSave = (Button) findViewById(R.id.butSaveBack);
        bSave.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // Update the Prefs
                RadioButton rb1 = (RadioButton) findViewById(R.id.ctBut1);
                RadioButton rb2 = (RadioButton) findViewById(R.id.ctBut2);
                if (rb1.isChecked()) {
                    prefEdit.putBoolean("publiccloud", false);
                    Global.PublicCloud = false;
                    jsonSetter(Global.Settings, "publiccloud", false);
                }
                if (rb2.isChecked()) {
                    prefEdit.putBoolean("publiccloud", true);
                    Global.PublicCloud = true;
                    jsonSetter(Global.Settings, "publiccloud", true);
                }

                RadioButton p1rbpt1 = (RadioButton) findViewById(R.id.p1ptBut1);
                RadioButton p1rbpt2 = (RadioButton) findViewById(R.id.p1ptBut2);
                if (p1rbpt1.isChecked()) {
                    Global.Printer1Type = true;
                    jsonSetter(Global.Settings, "printer1type", true);
                }
                if (p1rbpt2.isChecked()) {
                    Global.Printer1Type = false;
                    jsonSetter(Global.Settings, "printer1type", false);
                }

                RadioButton p2rbpt1 = (RadioButton) findViewById(R.id.p2ptBut1);
                RadioButton p2rbpt2 = (RadioButton) findViewById(R.id.p2ptBut2);
                if (p2rbpt1.isChecked()) {
                    Global.Printer2Type = true;
                    jsonSetter(Global.Settings, "printer2type", true);
                }
                if (p2rbpt2.isChecked()) {
                    Global.Printer1Type = false;
                    jsonSetter(Global.Settings, "printer2type", false);
                }

                RadioButton p3rbpt1 = (RadioButton) findViewById(R.id.p3ptBut1);
                RadioButton p3rbpt2 = (RadioButton) findViewById(R.id.p3ptBut2);
                if (p3rbpt1.isChecked()) {
                    Global.Printer3Type = true;
                    jsonSetter(Global.Settings, "printer3type", true);
                }
                if (p3rbpt2.isChecked()) {
                    Global.Printer3Type = false;
                    jsonSetter(Global.Settings, "printer3type", false);
                }

                CheckBox cb4 = (CheckBox) findViewById(R.id.checkEng);
                CheckBox cb6 = (CheckBox) findViewById(R.id.checkWifi);
                CheckBox cb7 = (CheckBox) findViewById(R.id.showPics);
                CheckBox cb9 = (CheckBox) findViewById(R.id.autoMenuReload);
                CheckBox cb10 = (CheckBox) findViewById(R.id.showRating);
                CheckBox cb11 = (CheckBox) findViewById(R.id.showBadges);
                CheckBox cb19 = (CheckBox) findViewById(R.id.pos1logo);
                CheckBox cb20 = (CheckBox) findViewById(R.id.pos1enable);
                CheckBox cb21 = (CheckBox) findViewById(R.id.pos2enable);
                CheckBox cb22 = (CheckBox) findViewById(R.id.pos3enable);
                CheckBox cb23 = (CheckBox) findViewById(R.id.p2KitchenCodes);
                CheckBox cb24 = (CheckBox) findViewById(R.id.p3KitchenCodes);
                CheckBox cbp1pst = (CheckBox) findViewById(R.id.p1PrintSentTime);
                CheckBox cbp2pst = (CheckBox) findViewById(R.id.p2PrintSentTime);
                CheckBox cbp3pst = (CheckBox) findViewById(R.id.p3PrintSentTime);
                CheckBox cbaod = (CheckBox) findViewById(R.id.autoOpenDrawer);
                CheckBox cbdisspec = (CheckBox) findViewById(R.id.displaySpecials);
                CheckBox cbsbe = (CheckBox) findViewById(R.id.showBigExit);
                CheckBox cbsdi = (CheckBox) findViewById(R.id.showDishInfo);
                CheckBox cbsdf = (CheckBox) findViewById(R.id.showDishFeedback);
                CheckBox cbsdd = (CheckBox) findViewById(R.id.showDishDescription);
                CheckBox cbdgu = (CheckBox) findViewById(R.id.displayGuests);
                CheckBox cbszp = (CheckBox) findViewById(R.id.showZoomPicture);
                CheckBox cbact = (CheckBox) findViewById(R.id.allowChangeTable);
                CheckBox cbprdid = (CheckBox) findViewById(R.id.printDishID);

                if (cb4.isChecked()) {
                    prefEdit.putBoolean("startenglish", true);
                    Global.StartEnglish = true;
                    jsonSetter(Global.Settings, "startenglish", true);
                } else {
                    prefEdit.putBoolean("startenglish", false);
                    Global.StartEnglish = false;
                    jsonSetter(Global.Settings, "startenglish", false);
                }
                if (cb9.isChecked()) {
                    prefEdit.putBoolean("automenureload", true);
                    Global.AutoMenuReload = true;
                    jsonSetter(Global.Settings, "automenureload", true);
                } else {
                    prefEdit.putBoolean("automenureload", false);
                    Global.AutoMenuReload = false;
                    jsonSetter(Global.Settings, "automenureload", false);
                }
                if (cb6.isChecked()) {
                    prefEdit.putBoolean("checkwifi", true);
                    Global.CheckWifi = true;
                    jsonSetter(Global.Settings, "checkwifi", true);
                } else {
                    prefEdit.putBoolean("checkwifi", false);
                    Global.CheckWifi = false;
                    jsonSetter(Global.Settings, "checkwifi", false);
                }
                if (cb7.isChecked()) {
                    Global.ShowPics = true;
                    jsonSetter(Global.Settings, "showpics", true);
                } else {
                    Global.ShowPics = false;
                    jsonSetter(Global.Settings, "showpics", false);
                }
                if (cb10.isChecked()) {
                    Global.ShowRating = true;
                    jsonSetter(Global.Settings, "showrating", true);
                } else {
                    Global.ShowRating = false;
                    jsonSetter(Global.Settings, "showrating", false);
                }
                if (cb11.isChecked()) {
                    Global.ShowBadges = true;
                    jsonSetter(Global.Settings, "showbadges", true);
                } else {
                    Global.ShowBadges = false;
                    jsonSetter(Global.Settings, "showbadges", false);
                }
                if (cbp1pst.isChecked()) {
                    Global.P1PrintSentTime = true;
                    jsonSetter(Global.Settings, "p1printsenttime", true);
                } else {
                    Global.P1PrintSentTime = false;
                    jsonSetter(Global.Settings, "p1printsenttime", false);
                }
                if (cbp2pst.isChecked()) {
                    Global.P2PrintSentTime = true;
                    jsonSetter(Global.Settings, "p2printsenttime", true);
                } else {
                    Global.P2PrintSentTime = false;
                    jsonSetter(Global.Settings, "p2printsenttime", false);
                }
                if (cbp3pst.isChecked()) {
                    Global.P3PrintSentTime = true;
                    jsonSetter(Global.Settings, "p3printsenttime", true);
                } else {
                    Global.P3PrintSentTime = false;
                    jsonSetter(Global.Settings, "p3printsenttime", false);
                }
                if (cb19.isChecked()) {
                    Global.POS1Logo = true;
                    jsonSetter(Global.Settings, "pos1logo", true);
                } else {
                    Global.POS1Logo = false;
                    jsonSetter(Global.Settings, "pos1logo", false);
                }
                if (cb20.isChecked()) {
                    Global.POS1Enable = true;
                    jsonSetter(Global.Settings, "pos1enable", true);
                } else {
                    Global.POS1Enable = false;
                    jsonSetter(Global.Settings, "pos1enable", false);
                }
                if (cb21.isChecked()) {
                    Global.POS2Enable = true;
                    jsonSetter(Global.Settings, "pos2enable", true);
                } else {
                    Global.POS2Enable = false;
                    jsonSetter(Global.Settings, "pos2enable", false);
                }
                if (cb22.isChecked()) {
                    Global.POS3Enable = true;
                    jsonSetter(Global.Settings, "pos3enable", true);
                } else {
                    Global.POS3Enable = false;
                    jsonSetter(Global.Settings, "pos3enable", false);
                }
                if (cb23.isChecked()) {
                    Global.P2KitchenCodes = true;
                    jsonSetter(Global.Settings, "p2kitchencodes", true);
                } else {
                    Global.P2KitchenCodes = false;
                    jsonSetter(Global.Settings, "p2kitchencodes", false);
                }
                if (cb24.isChecked()) {
                    Global.P3KitchenCodes = true;
                    jsonSetter(Global.Settings, "p3kitchencodes", true);
                } else {
                    Global.P3KitchenCodes = false;
                    jsonSetter(Global.Settings, "p3kitchencodes", false);
                }
                if (cbaod.isChecked()) {
                    Global.AutoOpenDrawer = true;
                    jsonSetter(Global.Settings, "autoopendrawer", true);
                } else {
                    Global.AutoOpenDrawer = false;
                    jsonSetter(Global.Settings, "autoopendrawer", false);
                }
                if (cbdisspec.isChecked()) {
                    Global.DisplaySpecials = true;
                    jsonSetter(Global.Settings, "displayspecials", true);
                } else {
                    Global.DisplaySpecials = false;
                    jsonSetter(Global.Settings, "displayspecials", false);
                }
                if (cbsbe.isChecked()) {
                    Global.ShowBigExit = true;
                    jsonSetter(Global.Settings, "showbigexit", true);
                } else {
                    Global.ShowBigExit = false;
                    jsonSetter(Global.Settings, "showbigexit", false);
                }
                if (cbsdi.isChecked()) {
                    Global.ShowDishInfo = true;
                    jsonSetter(Global.Settings, "showdishinfo", true);
                } else {
                    Global.ShowDishInfo = false;
                    jsonSetter(Global.Settings, "showdishinfo", false);
                }
                if (cbsdf.isChecked()) {
                    Global.ShowDishFeedback = true;
                    jsonSetter(Global.Settings, "showdishfeedback", true);
                } else {
                    Global.ShowDishFeedback = false;
                    jsonSetter(Global.Settings, "showdishfeedback", false);
                }
                if (cbsdd.isChecked()) {
                    Global.ShowDishDescriptions = true;
                    jsonSetter(Global.Settings, "showdishdescriptions", true);
                } else {
                    Global.ShowDishDescriptions = false;
                    jsonSetter(Global.Settings, "showdishdescriptions", false);
                }
                if (cbdgu.isChecked()) {
                    Global.ChooseGuests = true;
                    jsonSetter(Global.Settings, "chooseguests", true);
                } else {
                    Global.ChooseGuests = false;
                    jsonSetter(Global.Settings, "chooseguests", false);
                }
                if (cbszp.isChecked()) {
                    Global.ShowZoomPic = true;
                    jsonSetter(Global.Settings, "showzoompic", true);
                } else {
                    Global.ShowZoomPic = false;
                    jsonSetter(Global.Settings, "showzoompic", false);
                }
                if (cbact.isChecked()) {
                    Global.AllowChangeTable = true;
                    jsonSetter(Global.Settings, "allowchangetable", true);
                } else {
                    Global.AllowChangeTable = false;
                    jsonSetter(Global.Settings, "allowchangetable", false);
                }
                if (cbprdid.isChecked()) {
                    Global.PrintDishID = true;
                    jsonSetter(Global.Settings, "printdishid", true);
                } else {
                    Global.PrintDishID = false;
                    jsonSetter(Global.Settings, "printdishid", false);
                }

                Button fc2 = (Button) findViewById(R.id.butP2FilterCats);
                Global.P2FilterCats = fc2.getText().toString();
                jsonSetter(Global.Settings, "p2filtercats", Global.P2FilterCats);

                Button fc3 = (Button) findViewById(R.id.butP3FilterCats);
                Global.P3FilterCats = fc3.getText().toString();
                jsonSetter(Global.Settings, "p3filtercats", Global.P3FilterCats);

                EditText et = (EditText) findViewById(R.id.ip1);
                Global.POS1Ip = et.getText().toString();
                jsonSetter(Global.Settings, "pos1ip", Global.POS1Ip);

                et = (EditText) findViewById(R.id.ip2);
                Global.POS2Ip = et.getText().toString();
                jsonSetter(Global.Settings, "pos2ip", Global.POS2Ip);

                et = (EditText) findViewById(R.id.ip3);
                Global.POS3Ip = et.getText().toString();
                jsonSetter(Global.Settings, "pos3ip", Global.POS3Ip);

                et = (EditText) findViewById(R.id.ipmd);
                Global.POSIp = et.getText().toString();
                jsonSetter(Global.Settings, "posmasterip", Global.POSIp);

                et = (EditText) findViewById(R.id.ipto);
                Global.TakeOutIp = et.getText().toString();
                jsonSetter(Global.Settings, "takeoutip", Global.TakeOutIp);

                //et = (EditText) findViewById(R.id.serverIP);
                //Global.ServerIP = et.getText().toString();
                //jsonSetter(Global.Settings, "serverip", Global.ServerIP);

                Global.MasterDeviceId = (et = (EditText) findViewById(R.id.masterDeviceId)).getText().toString();
                jsonSetter(Global.Settings, "masterdeviceid", Global.MasterDeviceId);
                Global.CustomerName = (et = (EditText) findViewById(R.id.customerName)).getText().toString();
                jsonSetter(Global.Settings, "customername", Global.CustomerName);
                Global.CustomerNameBrief = (et = (EditText) findViewById(R.id.customerNameBrief)).getText().toString();
                jsonSetter(Global.Settings, "customernamebrief", Global.CustomerNameBrief);

                Global.StoreAddress = (et = (EditText) findViewById(R.id.storeAddress)).getText().toString();
                jsonSetter(Global.Settings, "storeaddress", Global.StoreAddress);

                et = (EditText) findViewById(R.id.fontScaleFactor);
                int tmp = Integer.parseInt(et.getText().toString());
                if (tmp < 0) tmp = 0;
                if (tmp > 10) tmp = 10;
                Global.FontScaleFactor = tmp;
                jsonSetter(Global.Settings, "fontscalefactor", Global.FontScaleFactor);

                et = (EditText) findViewById(R.id.sendOrderMode);
                tmp = Integer.parseInt(et.getText().toString());
                if (tmp < 1) tmp = 1;
                if (tmp > 3) tmp = 3;
                Global.SendOrderMode = tmp;
                jsonSetter(Global.Settings, "sendordermode", Global.SendOrderMode);

                et = (EditText) findViewById(R.id.etP1C);
                tmp = Integer.parseInt(et.getText().toString());
                if (tmp < 1) tmp = 1;
                if (tmp > 3) tmp = 3;
                Global.Printer1Copy = tmp;
                jsonSetter(Global.Settings, "printer1copy", Global.Printer1Copy);

                prefEdit.commit();

                // update the local settings file (json)
                try {
                    writeOutFile(Global.Settings.toString(4), filesLocalName[3]);
                } catch (Exception e) {
                }
            }

        });

        Button butFontColor = (Button) findViewById(R.id.butFontColor);
        butFontColor.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                //dialog = new CustomDialog(SyncActivity.this);
                dialog = new Dialog(SettingsActivity.this);
                dialog.setContentView(R.layout.color_pick_grid);
                String tit = "Font Color";
                SpannableStringBuilder ssBuilser = new SpannableStringBuilder(tit);
                StyleSpan span = new StyleSpan(Typeface.BOLD);
                ScaleXSpan span1 = new ScaleXSpan(2);
                ssBuilser.setSpan(span, 0, tit.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                ssBuilser.setSpan(span1, 0, tit.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                dialog.setTitle(ssBuilser);
                dialog.setCancelable(true);
                dialog.setCanceledOnTouchOutside(true);
                dialog.show();
                // loop through all the color patch buttons and set up the listeners
                Button[] buttons = new Button[]{};
                buttons = new Button[24];
                for (int i = 0; i < 24; i++) {
                    String buttonID = "butcp" + (i + 1);
                    int resID = getResources().getIdentifier(buttonID, "id", getPackageName());
                    buttons[i] = ((Button) dialog.findViewById(resID));
                    buttons[i].setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            colorButtonFontClick(v);
                        }
                    });
                }
            }
        });

        Button butBackColor = (Button) findViewById(R.id.butBackColor);
        butBackColor.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                dialog = new Dialog(SettingsActivity.this);
                dialog.setContentView(R.layout.color_pick_grid);
                String tit = "Background Color";
                SpannableStringBuilder ssBuilser = new SpannableStringBuilder(tit);
                StyleSpan span = new StyleSpan(Typeface.BOLD);
                ScaleXSpan span1 = new ScaleXSpan(2);
                ssBuilser.setSpan(span, 0, tit.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                ssBuilser.setSpan(span1, 0, tit.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                dialog.setTitle(ssBuilser);
                dialog.setCancelable(true);
                dialog.setCanceledOnTouchOutside(true);
                dialog.show();
                // loop through all the color patch buttons and set up the listeners
                Button[] buttons = new Button[]{};
                buttons = new Button[24];
                for (int i = 0; i < 24; i++) {
                    String buttonID = "butcp" + (i + 1);
                    int resID = getResources().getIdentifier(buttonID, "id", getPackageName());
                    buttons[i] = ((Button) dialog.findViewById(resID));
                    buttons[i].setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            colorButtonBackClick(v);
                        }
                    });
                }
            }
        });

        Button butHeadColor = (Button) findViewById(R.id.butHeadColor);
        butHeadColor.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                dialog = new Dialog(SettingsActivity.this);
                dialog.setContentView(R.layout.color_pick_grid);
                String tit = "Header Color";
                SpannableStringBuilder ssBuilser = new SpannableStringBuilder(tit);
                StyleSpan span = new StyleSpan(Typeface.BOLD);
                ScaleXSpan span1 = new ScaleXSpan(2);
                ssBuilser.setSpan(span, 0, tit.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                ssBuilser.setSpan(span1, 0, tit.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                dialog.setTitle(ssBuilser);
                dialog.setCancelable(true);
                dialog.setCanceledOnTouchOutside(true);
                dialog.show();
                // loop through all the color patch buttons and set up the listeners
                Button[] buttons = new Button[]{};
                buttons = new Button[24];
                for (int i = 0; i < 24; i++) {
                    String buttonID = "butcp" + (i + 1);
                    int resID = getResources().getIdentifier(buttonID, "id", getPackageName());
                    buttons[i] = ((Button) dialog.findViewById(resID));
                    buttons[i].setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            colorButtonHeadClick(v);
                        }
                    });
                }
            }
        });

        Button butButFontColor = (Button) findViewById(R.id.butButFontColor);
        butButFontColor.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                dialog = new Dialog(SettingsActivity.this);
                dialog.setContentView(R.layout.color_pick_grid);
                String tit = "Button Font Color";
                SpannableStringBuilder ssBuilser = new SpannableStringBuilder(tit);
                StyleSpan span = new StyleSpan(Typeface.BOLD);
                ScaleXSpan span1 = new ScaleXSpan(2);
                ssBuilser.setSpan(span, 0, tit.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                ssBuilser.setSpan(span1, 0, tit.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                dialog.setTitle(ssBuilser);
                dialog.setCancelable(true);
                dialog.setCanceledOnTouchOutside(true);
                dialog.show();
                // loop through all the color patch buttons and set up the listeners
                Button[] buttons = new Button[]{};
                buttons = new Button[24];
                for (int i = 0; i < 24; i++) {
                    String buttonID = "butcp" + (i + 1);
                    int resID = getResources().getIdentifier(buttonID, "id", getPackageName());
                    buttons[i] = ((Button) dialog.findViewById(resID));
                    buttons[i].setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            colorButtonButFontClick(v);
                        }
                    });
                }
            }
        });

        Button butMainButtonColor = (Button) findViewById(R.id.butMainButtonColor);
        butMainButtonColor.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                dialog = new Dialog(SettingsActivity.this);
                dialog.setContentView(R.layout.color_pick_grid);
                String tit = "Main Button Color";
                SpannableStringBuilder ssBuilser = new SpannableStringBuilder(tit);
                StyleSpan span = new StyleSpan(Typeface.BOLD);
                ScaleXSpan span1 = new ScaleXSpan(2);
                ssBuilser.setSpan(span, 0, tit.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                ssBuilser.setSpan(span1, 0, tit.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                dialog.setTitle(ssBuilser);
                dialog.setCancelable(true);
                dialog.setCanceledOnTouchOutside(true);
                dialog.show();
                // loop through all the color patch buttons and set up the listeners
                Button[] buttons = new Button[]{};
                buttons = new Button[24];
                for (int i = 0; i < 24; i++) {
                    String buttonID = "butcp" + (i + 1);
                    int resID = getResources().getIdentifier(buttonID, "id", getPackageName());
                    buttons[i] = ((Button) dialog.findViewById(resID));
                    buttons[i].setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            colorMainButtonColorClick(v);
                        }
                    });
                }
            }
        });

        butSBC = (Button) findViewById(R.id.butSelButColor);
        butSBC.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                dialog = new Dialog(SettingsActivity.this);
                dialog.setContentView(R.layout.color_pick_grid);
                String tit = "Selected Button";
                SpannableStringBuilder ssBuilser = new SpannableStringBuilder(tit);
                StyleSpan span = new StyleSpan(Typeface.BOLD);
                ScaleXSpan span1 = new ScaleXSpan(2);
                ssBuilser.setSpan(span, 0, tit.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                ssBuilser.setSpan(span1, 0, tit.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                dialog.setTitle(ssBuilser);
                dialog.setCancelable(true);
                dialog.setCanceledOnTouchOutside(true);
                dialog.show();
                // loop through all the color patch buttons and set up the listeners
                Button[] buttons = new Button[]{};
                buttons = new Button[24];
                for (int i = 0; i < 24; i++) {
                    String buttonID = "butcp" + (i + 1);
                    int resID = getResources().getIdentifier(buttonID, "id", getPackageName());
                    buttons[i] = ((Button) dialog.findViewById(resID));
                    buttons[i].setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            colorSelButClick(v);
                        }
                    });
                }
            }
        });

        butSBFC = (Button) findViewById(R.id.butSelButFontColor);
        butSBFC.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                dialog = new Dialog(SettingsActivity.this);
                dialog.setContentView(R.layout.color_pick_grid);
                String tit = "Selected Button Font";
                SpannableStringBuilder ssBuilser = new SpannableStringBuilder(tit);
                StyleSpan span = new StyleSpan(Typeface.BOLD);
                ScaleXSpan span1 = new ScaleXSpan(2);
                ssBuilser.setSpan(span, 0, tit.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                ssBuilser.setSpan(span1, 0, tit.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                dialog.setTitle(ssBuilser);
                dialog.setCancelable(true);
                dialog.setCanceledOnTouchOutside(true);
                dialog.show();
                // loop through all the color patch buttons and set up the listeners
                Button[] buttons = new Button[]{};
                buttons = new Button[24];
                for (int i = 0; i < 24; i++) {
                    String buttonID = "butcp" + (i + 1);
                    int resID = getResources().getIdentifier(buttonID, "id", getPackageName());
                    buttons[i] = ((Button) dialog.findViewById(resID));
                    buttons[i].setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            colorSelButFontClick(v);
                        }
                    });
                }
            }
        });

        butLC = (Button) findViewById(R.id.butLabelColor);
        butLC.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                dialog = new Dialog(SettingsActivity.this);
                dialog.setContentView(R.layout.color_pick_grid);
                String tit = "Label Color";
                SpannableStringBuilder ssBuilser = new SpannableStringBuilder(tit);
                StyleSpan span = new StyleSpan(Typeface.BOLD);
                ScaleXSpan span1 = new ScaleXSpan(2);
                ssBuilser.setSpan(span, 0, tit.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                ssBuilser.setSpan(span1, 0, tit.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                dialog.setTitle(ssBuilser);
                dialog.setCancelable(true);
                dialog.setCanceledOnTouchOutside(true);
                dialog.show();
                // loop through all the color patch buttons and set up the listeners
                Button[] buttons = new Button[]{};
                buttons = new Button[24];
                for (int i = 0; i < 24; i++) {
                    String buttonID = "butcp" + (i + 1);
                    int resID = getResources().getIdentifier(buttonID, "id", getPackageName());
                    buttons[i] = ((Button) dialog.findViewById(resID));
                    buttons[i].setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            colorLabelClick(v);
                        }
                    });
                }
            }
        });

        butLFC = (Button) findViewById(R.id.butLabelFontColor);
        butLFC.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                dialog = new Dialog(SettingsActivity.this);
                dialog.setContentView(R.layout.color_pick_grid);
                String tit = "Label Font Color";
                SpannableStringBuilder ssBuilser = new SpannableStringBuilder(tit);
                StyleSpan span = new StyleSpan(Typeface.BOLD);
                ScaleXSpan span1 = new ScaleXSpan(2);
                ssBuilser.setSpan(span, 0, tit.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                ssBuilser.setSpan(span1, 0, tit.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                dialog.setTitle(ssBuilser);
                dialog.setCancelable(true);
                dialog.setCanceledOnTouchOutside(true);
                dialog.show();
                // loop through all the color patch buttons and set up the listeners
                Button[] buttons = new Button[]{};
                buttons = new Button[24];
                for (int i = 0; i < 24; i++) {
                    String buttonID = "butcp" + (i + 1);
                    int resID = getResources().getIdentifier(buttonID, "id", getPackageName());
                    buttons[i] = ((Button) dialog.findViewById(resID));
                    buttons[i].setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            colorLabelFontClick(v);
                        }
                    });
                }
            }
        });
    }

    public void colorButtonFontClick(View v) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final Editor prefEdit = prefs.edit();
        dialog.cancel();
        String colorhash = v.getTag().toString();
        Integer color = Color.parseColor(colorhash);
        Global.FontColor = color;
        Button butFC = (Button) findViewById(R.id.butFontColor);
        butFC.setBackgroundColor(Global.FontColor);
        prefEdit.putInt("fontcolor", color);
        jsonSetter(Global.Settings, "fontcolor", colorhash);
        prefEdit.commit();
    }

    public void colorButtonBackClick(View v) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final Editor prefEdit = prefs.edit();
        dialog.cancel();
        String colorhash = v.getTag().toString();
        Integer color = Color.parseColor(colorhash);
        Global.BackColor = color;
        Button butBC = (Button) findViewById(R.id.butBackColor);
        butBC.setBackgroundColor(Global.BackColor);
        prefEdit.putInt("backcolor", color);
        jsonSetter(Global.Settings, "backcolor", colorhash);
        prefEdit.commit();
    }

    public void colorButtonHeadClick(View v) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final Editor prefEdit = prefs.edit();
        dialog.cancel();
        String colorhash = v.getTag().toString();
        Integer color = Color.parseColor(colorhash);
        Global.HeaderColor = color;
        Button butHC = (Button) findViewById(R.id.butHeadColor);
        butHC.setBackgroundColor(Global.HeaderColor);
        prefEdit.putInt("headercolor", color);
        jsonSetter(Global.Settings, "headercolor", colorhash);
        prefEdit.commit();
    }

    public void colorButtonButFontClick(View v) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final Editor prefEdit = prefs.edit();
        dialog.cancel();
        String colorhash = v.getTag().toString();
        Integer color = Color.parseColor(colorhash);
        Global.ButFontColor = color;
        Button butBFC = (Button) findViewById(R.id.butButFontColor);
        butBFC.setBackgroundColor(Global.ButFontColor);
        prefEdit.putInt("butfontcolor", color);
        jsonSetter(Global.Settings, "butfontcolor", colorhash);
        prefEdit.commit();
    }

    public void colorMainButtonColorClick(View v) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final Editor prefEdit = prefs.edit();
        dialog.cancel();
        String colorhash = v.getTag().toString();
        Integer color = Color.parseColor(colorhash);
        Global.ButColor = color;
        Button butMBC = (Button) findViewById(R.id.butMainButtonColor);
        butMBC.setBackgroundColor(Global.ButColor);
        prefEdit.putInt("mainbuttoncolor", color);
        jsonSetter(Global.Settings, "mainbuttoncolor", colorhash);
        prefEdit.commit();
    }

    public void colorSelButClick(View v) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final Editor prefEdit = prefs.edit();
        dialog.cancel();
        String colorhash = v.getTag().toString();
        Integer color = Color.parseColor(colorhash);
        Global.SelButColor = color;
        Button butSBC = (Button) findViewById(R.id.butSelButColor);
        butSBC.setBackgroundColor(Global.SelButColor);
        prefEdit.putInt("selbutcolor", color);
        jsonSetter(Global.Settings, "selbutcolor", colorhash);
        prefEdit.commit();
    }

    public void colorSelButFontClick(View v) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final Editor prefEdit = prefs.edit();
        dialog.cancel();
        String colorhash = v.getTag().toString();
        Integer color = Color.parseColor(colorhash);
        Global.SelButFontColor = color;
        Button butSBFC = (Button) findViewById(R.id.butSelButFontColor);
        butSBFC.setBackgroundColor(Global.SelButFontColor);
        prefEdit.putInt("selbutfontcolor", color);
        jsonSetter(Global.Settings, "selbutfontcolor", colorhash);
        prefEdit.commit();
    }

    public void colorLabelClick(View v) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final Editor prefEdit = prefs.edit();
        dialog.cancel();
        String colorhash = v.getTag().toString();
        Integer color = Color.parseColor(colorhash);
        Global.LabelColor = color;
        Button butLC = (Button) findViewById(R.id.butLabelColor);
        butLC.setBackgroundColor(Global.LabelColor);
        prefEdit.putInt("labelcolor", color);
        jsonSetter(Global.Settings, "labelcolor", colorhash);
        prefEdit.commit();
    }

    public void colorLabelFontClick(View v) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final Editor prefEdit = prefs.edit();
        dialog.cancel();
        String colorhash = v.getTag().toString();
        Integer color = Color.parseColor(colorhash);
        Global.LabelFontColor = color;
        Button butLFC = (Button) findViewById(R.id.butLabelFontColor);
        butLFC.setBackgroundColor(Global.LabelFontColor);
        prefEdit.putInt("labelfontcolor", color);
        jsonSetter(Global.Settings, "labelfontcolor", colorhash);
        prefEdit.commit();
    }

    private void updateConnectionStatus() {
        // update the wi-fi status
        img = (ImageView) findViewById(R.id.lit0a);
        img.setBackgroundResource(R.drawable.presence_invisible);
        if (checkInternetConnection()) {
            img.setBackgroundResource(R.drawable.presence_online);
        } else {
            img.setBackgroundResource(R.drawable.presence_busy);
        }

        // update the SERVER connection status
        img = (ImageView) findViewById(R.id.lit1aServer);
        img.setBackgroundResource(R.drawable.presence_invisible);
        if (Global.CheckAvailability) {
            new ping204().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
        // update the Printer1 status
        if (Global.POS1Enable) {
            img = (ImageView) findViewById(R.id.lit2a);
            img.setBackgroundResource(R.drawable.presence_invisible);
            new pingFetch(Global.POS1Ip, img).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        // update the Printer2 status
        if (Global.POS2Enable) {
            img = (ImageView) findViewById(R.id.lit3a);
            img.setBackgroundResource(R.drawable.presence_invisible);
            new pingFetch(Global.POS2Ip, img).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        // update the Printer3 status
        if (Global.POS3Enable) {
            img = (ImageView) findViewById(R.id.lit4a);
            img.setBackgroundResource(R.drawable.presence_invisible);
            new pingFetch(Global.POS3Ip, img).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        // update the POS1 status
        img = (ImageView) findViewById(R.id.litPOS1);
        img.setBackgroundResource(R.drawable.presence_invisible);
        new pingFetch(Global.POSIp, img).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        // update the TakeOut status
        img = (ImageView) findViewById(R.id.litTO);
        img.setBackgroundResource(R.drawable.presence_invisible);
        new pingFetch(Global.TakeOutIp, img).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private boolean checkInternetConnection() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        // test for connection
        if (cm.getActiveNetworkInfo() != null
                && cm.getActiveNetworkInfo().isAvailable()
                && cm.getActiveNetworkInfo().isConnected()) {
            return true;
        } else {
            return false;
        }
    }

    private void populateField(Map<String, String> values, TextView view) {
        StringBuilder sb = new StringBuilder();
        for (Entry<String, String> entry : values.entrySet()) {
            String fieldName = entry.getKey();
            String fieldValue = entry.getValue();
            sb.append(fieldName)
                    .append(": ")
                    .append("<b>").append(fieldValue).append("</b>");
        }
        view.setText(Html.fromHtml(sb.toString()));
    }

    private String getVersionName() {
        String version = "";
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pi.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            version = "Package name not found";
        }
        return version;
    }

    private int getVersionCode() {
        int version = -1;
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pi.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
        }
        return version;
    }

    // check for connectivity using Java isReachable
    public class pingFetch extends AsyncTask<Void, String, Integer> {
        private String ip1;
        private Integer code;
        private InetAddress in2;
        private ImageView img;

        public pingFetch(String ip, ImageView imgv) {
            ip1 = ip;
            in2 = null;
            img = imgv;
            code = 0;
        }

        protected void onPreExecute(Void... params) {
        }

        protected Integer doInBackground(Void... params) {
            try {
                in2 = InetAddress.getByName(ip1);
            } catch (Exception e) {
                e.printStackTrace();
                code = 2;
            }
            try {
                if (in2.isReachable(Global.ConnectTimeout)) {
                    code = 1;
                } else {
                    code = 2;
                }
            } catch (Exception e) {
                e.printStackTrace();
                code = 2;
            }
            return 1;
        }

        protected void onProgressUpdate(String msg) {
        }

        protected void onPostExecute(Integer result) {
            if (code == 1) {
                img.setBackgroundResource(R.drawable.presence_online);
            }
            if (code == 2) {
                img.setBackgroundResource(R.drawable.presence_busy);
            }
        }
    }

    // Check for connectivity hitting the 204 script and update the UI
    public class ping204 extends AsyncTask<Void, String, Integer> {
        private Boolean code;

        public ping204() {
            code = false;
        }

        protected void onPreExecute(Void... params) {
        }

        protected Integer doInBackground(Void... params) {
            try {
                String ip1 = Global.ProtocolPrefix + Global.ServerIP + Global.ServerReturn204;
                int status = -1;
                code = false;

                try {
                    InputStream in = null;
                    URL url = new URL(ip1);

                    OkHttpClient.Builder b = new OkHttpClient.Builder();
                    b.readTimeout(Global.ReadTimeout, TimeUnit.MILLISECONDS);
                    b.writeTimeout(Global.ReadTimeout, TimeUnit.MILLISECONDS);
                    b.connectTimeout(Global.ConnectTimeout, TimeUnit.MILLISECONDS);
                    final OkHttpClient client = b.build();
                    Request request = new Request.Builder()
                            .url(url)
                            .build();
                    Response response = client.newCall(request).execute();
                    status = response.code();
                    if (status == 204) code = true;
                } catch (Exception e) {
                    code = false;
                }
            } catch (Exception e) {
                code = false;
            }
            return 1;
        }

        protected void onProgressUpdate(String msg) {
        }

        protected void onPostExecute(Integer result) {
            if (code == true) {
                img = (ImageView) findViewById(R.id.lit1aServer);
                img.setBackgroundResource(R.drawable.presence_online);
            } else {
                img = (ImageView) findViewById(R.id.lit1aServer);
                img.setBackgroundResource(R.drawable.presence_busy);
            }
        }
    }

    // check for ping connectivity
    // This one is good if you already are on a thread and dont need UI updated
    private boolean pingIP() {
        String ip1 = Global.ProtocolPrefix + Global.ServerIP + Global.ServerReturn204;
        int status = -1;
        downloadSuccess = false;
        try {
            URL url = new URL(ip1);

            OkHttpClient.Builder b = new OkHttpClient.Builder();
            b.readTimeout(Global.ReadTimeout, TimeUnit.MILLISECONDS);
            b.writeTimeout(Global.ReadTimeout, TimeUnit.MILLISECONDS);
            b.connectTimeout(Global.ConnectTimeout, TimeUnit.MILLISECONDS);
            final OkHttpClient client = b.build();
            Request request = new Request.Builder()
                    .url(url)
                    .build();
            Response response = client.newCall(request).execute();
            status = response.code();
            if (status == 204) {
                // reachable server
                downloadSuccess = true;
            } else {
                downloadSuccess = false;
            }
        } catch (Exception e) {
            downloadSuccess = false;
        }
        return downloadSuccess;
    }

    private void reloadTheFiles() {
        if ((!Global.CheckAvailability) || pingIP()) {
            // blow away the existing local files
            textDir = new File(getFilesDir(), "SmartMenuFiles");
            Utils.deleteDirectory(textDir);
            textDir.mkdirs();

            //setup file source path+name
            filesSourceName[0] = Global.ProtocolPrefix + Global.ServerIP + "/" + Global.SMID + "/" + "menufile.txt";
            filesSourceName[1] = Global.ProtocolPrefix + Global.ServerIP + "/" + Global.SMID + "/" + "category.txt";
            filesSourceName[2] = Global.ProtocolPrefix + Global.ServerIP + "/" + Global.SMID + "/" + "kitchen.txt";
            filesSourceName[3] = Global.ProtocolPrefix + Global.ServerIP + "/" + Global.SMID + "/" + "settings.txt";
            filesSourceName[4] = Global.ProtocolPrefix + Global.ServerIP + "/" + Global.SMID + "/" + "options.txt";
            filesSourceName[5] = Global.ProtocolPrefix + Global.ServerIP + "/" + Global.SMID + "/" + "extras.txt";

            HttpDownload2();
        } else {
            TextView txt = (TextView) findViewById(R.id.textLoad);
            txt.setText("Reload Failed.");
        }
    }

    private void HttpDownload2() {
        try {
            Global.FileSource = "Server";
            //Loop through the downloads here
            for (int i = 0; i < filesSourceName.length; i++) {
                filesText[i] = Utils.DownloadText(filesSourceName[i]);
                if (filesText[i].length() > 0) {
                    filesText[i] = Utils.removeBOMchar(filesText[i]);
                    filesText[i] = Utils.removeCommentLines(filesText[i]);
                    if (i == 0) {
                        filesText[i] = Utils.removeUnAvailable(filesText[i]);
                    }
                    writeOutFile(filesText[i], filesLocalName[i]);
                } else {
                    log("Settings: httpsdownload fail");
                    mHandler.post(exceptionReload);
                    break;
                }
            }
            log("Settings: httpsdownload success");
            Global.MENUTXT = filesText[0];
            Global.CATEGORYTXT = filesText[1];
            Global.KITCHENTXT = filesText[2];
            Global.SETTINGSTXT = filesText[3];
            Global.Settings = new JSONArray(Global.SETTINGSTXT);
            Global.OPTIONSTXT = filesText[4];
            Global.EXTRASTXT = filesText[5];

            Global.MenuVersion = jsonGetter(Global.Settings, "menuversion").toString();
            serverMv = jsonGetter(Global.Settings, "menuversion").toString();
            prefs = PreferenceManager.getDefaultSharedPreferences(this);
            Editor prefEdit = prefs.edit();
            prefEdit.putString("menuversion", serverMv);
            prefEdit.commit();

            // Build the Welcome ArrayList
            Global.welcome.clear();
            try {
                String tmp = jsonGetter(Global.Settings, "welcome").toString();
                Global.welcome.add(tmp);
            } catch (Exception e) {
                log("loadJSONsettings3 Exception=" + e);
            }

            // Build the Tablenames ArrayList
            Global.tablenames.clear();
            try {
                String tmp = jsonGetter(Global.Settings, "tablenames").toString();
                JSONArray JSONtabn = new JSONArray(tmp);
                // Loop through each modifier and add it to the modifier string array
                for (int i = 0; i < JSONtabn.length(); i++) {
                    String tabname = JSONtabn.getString(i);
                    Global.tablenames.add(i, tabname);
                }
            } catch (Exception e) {
                log("loadJSONsettings4 Exception=" + e);
            }
        } catch (Exception e) {
            log("httpDownload2 Outer Exception=" + e);
            mHandler.post(exceptionReload);
        }
    }

    // below is the over ride that will disable the back button
    @Override
    public void onBackPressed() {
    }

    public void failedAuth0() {
        AlertDialog alertDialog = new AlertDialog.Builder(SettingsActivity.this).create();
        alertDialog.setTitle("Connection");
        alertDialog.setIcon(android.R.drawable.stat_sys_warning);
        alertDialog.setMessage("Data connection not available. Files cannot be reloaded.");
        alertDialog.setCancelable(false);
        alertDialog.setButton("Back", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        alertDialog.show();
    }

    private void failedAuth2() {
        AlertDialog alertDialog = new AlertDialog.Builder(SettingsActivity.this).create();
        alertDialog.setTitle("Uploading");
        alertDialog.setIcon(android.R.drawable.stat_sys_warning);
        alertDialog.setMessage("Uploading not successful. Please try again.");
        alertDialog.setCancelable(false);
        alertDialog.setButton("Back", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        alertDialog.show();
    }

    private void failedAuth3() {
        AlertDialog alertDialog = new AlertDialog.Builder(SettingsActivity.this).create();
        alertDialog.setTitle("Reloading");
        alertDialog.setIcon(android.R.drawable.stat_sys_warning);
        alertDialog.setMessage("Reload not successful. Please try again.");
        alertDialog.setCancelable(false);
        alertDialog.setButton("Back", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        alertDialog.show();
    }

    private void jsonSetter(JSONArray array, String key, Object replace) {
        for (int i = 0; i < array.length(); i++) {
            try {
                JSONObject obj = array.getJSONObject(i);
                String value = obj.getString("name");
                if (value.equalsIgnoreCase(key)) {
                    obj.putOpt("value", replace);
                }
            } catch (JSONException e) {
                log("jsonSetter exception");
            }
        }
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
                log("jsonGetter Exception");
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
                log("jsonGetter2 Exception");
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

    private void writeOutFile(String fcontent, String fname) {
        File writeFile = new File(textDir, fname);
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(writeFile, false), "UTF-8"));
            writer.write(fcontent);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            log("writeOutFile exception");
        }
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

    protected void showP2CatDialog() {
        boolean[] checkedP2Cat = new boolean[catList.size()];
        int count = catList.size();
        for (int i = 0; i < count; i++) {
            checkedP2Cat[i] = selectedP2Cat.contains(catList.get(i));
        }
        DialogInterface.OnMultiChoiceClickListener p2DialogListener = new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                if (isChecked) selectedP2Cat.add(catList.get(which));
                else selectedP2Cat.remove(catList.get(which));
                StringBuilder stringBuilder = new StringBuilder();
                for (CharSequence ext : selectedP2Cat) {
                    // There is a '0 length' item at the beginning of the arraylist and I dont know why, so don't let it through...
                    if (ext.length() > 0) {
                        stringBuilder.append(ext.toString() + ",");
                    }
                }
                // Kill the last comma...
                String ss = stringBuilder.toString();
                if (ss.length() > 0) ss = ss.substring(0, ss.length() - 1);
                p2CatButton.setText(ss);
            }
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Print Categories");
        // need the opts in a string [] to pass in multi
        String[] tmpArr = new String[catList.size()];
        for (int i = 0; i < catList.size(); i++) {
            tmpArr[i] = catList.get(i);
        }
        builder.setMultiChoiceItems(tmpArr, checkedP2Cat, p2DialogListener);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    protected void showP3CatDialog() {
        boolean[] checkedP3Cat = new boolean[catList.size()];
        int count = catList.size();
        for (int i = 0; i < count; i++)
            checkedP3Cat[i] = selectedP3Cat.contains(catList.get(i));
        DialogInterface.OnMultiChoiceClickListener p3DialogListener = new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                if (isChecked) selectedP3Cat.add(catList.get(which));
                else selectedP3Cat.remove(catList.get(which));
                StringBuilder stringBuilder = new StringBuilder();
                for (CharSequence ext : selectedP3Cat) {
                    // There is a '0 length' item at the beginning of the arraylist and I dont know why, so don't let it through...
                    if (ext.length() > 0) {
                        stringBuilder.append(ext.toString() + ",");
                    }
                }
                // remove last ","
                String ss = stringBuilder.toString();
                if (ss.length() > 0) ss = ss.substring(0, ss.length() - 1);
                p3CatButton.setText(ss);
            }
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Print Categories");
        // need the opts in a string [] to pass in multi
        String[] tmpArr = new String[catList.size()];
        for (int i = 0; i < catList.size(); i++) {
            tmpArr[i] = catList.get(i);
        }
        builder.setMultiChoiceItems(tmpArr, checkedP3Cat, p3DialogListener);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void newCatList() {
        // get the extra list for the button
        catList.clear();
        String[] lines = Global.CATEGORYTXT.split("\\n");
        for (int i = 0; i < lines.length; i++) {
            int start = 0;
            int end = lines[i].indexOf("|");
            catList.add(lines[i].substring(start, end));
        }
    }

}