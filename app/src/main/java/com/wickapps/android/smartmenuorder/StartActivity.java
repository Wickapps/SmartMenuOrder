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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings.Secure;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class StartActivity extends Activity {

    Boolean downloadSuccess = false;
    int returnCode = 0;

    private File textDir;
    private String serverMv;
    private Locale lc;

    private SharedPreferences prefs;

    private ConnectionLog mLog;

    private static String[] filesLocalName = new String[]{"menufile.txt", "category.txt", "kitchen.txt", "settings.txt", "options.txt", "extras.txt"};
    private static String[] filesSourceName = new String[]{"", "", "", "", "", ""};
    private static String[] filesText = new String[]{"", "", "", "", "", ""};

    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    //	Need handler for callbacks to the UI thread
    final Handler mHandler = new Handler();

    final Runnable noConnection = new Runnable() {
        public void run() {
            // Force a initial setup next time because a setting must be incorrect
            Editor prefEdit = prefs.edit();
            prefEdit.putString("smid", "");
            prefEdit.putString("serverip", "");
            prefEdit.commit();

            if (returnCode == 1) {
                failedAuth1();
            } else if (returnCode == 2) {
                failedAuth2();
            } else if (returnCode == 3) {
                failedAuth3();
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            mLog = new ConnectionLog(this);
        } catch (Exception e) {
        }

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

        if (Global.MenuStarted) {
            Intent kintent = new Intent(getApplicationContext(), MenuActivity.class);
            kintent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(kintent);
        } else {
            this.setContentView(R.layout.splash);

            Global.MenuStarted = false;

            String curTime = Utils.FancyDate();
            Global.TodayDate = curTime;
            Global.PicHeight = Utils.getPicHeight(StartActivity.this);

            // Use the PREF to bootstrap load, other settings from json settings.txt
            prefs = PreferenceManager.getDefaultSharedPreferences(this);

            Global.DeviceId = Secure.getString(this.getContentResolver(), Secure.ANDROID_ID);
            // save the unique id so we can see it in the crashlog if there is a crash
            Editor prefEdit = prefs.edit();
            prefEdit.putString("deviceid", Global.DeviceId);
            prefEdit.commit();

            Global.BrokerOrderTopic = Global.SMID + "ORDER";
            Global.BrokerMsgTopic = Global.SMID + "MSG";

            // Stage 1: Check if we have the required Boot settings, if not prompt for first time setup
            Global.ServerIP = (new String(prefs.getString("serverip", "")));
            Global.SMID = (new String(prefs.getString("smid", "")));
            Global.ProtocolPrefix = (new String(prefs.getString("protocolprefix", "http://")));
            Global.AdminPin = (new String(prefs.getString("adminpin", "")));
            Global.CheckAvailability = (new Boolean(prefs.getBoolean("checkavailability", false)));

            Global.PublicCloud = (new Boolean(prefs.getBoolean("publiccloud", true)));
            Global.MenuVersion = (new String(prefs.getString("menuversion", "0000")));
            Global.StartEnglish = (new Boolean(prefs.getBoolean("startenglish", false)));
            Global.AutoMenuReload = (new Boolean(prefs.getBoolean("automenureload", true)));

            if (Global.StartEnglish) {
                Configuration config = getBaseContext().getResources().getConfiguration();
                lc = new Locale("en");
                Locale.setDefault(lc);
                config.locale = lc;
                getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
                Global.EnglishLang = true;
            }

            // If any of these are blank, prompt for initial setting
            if ((Global.ServerIP.length() == 0) ||
                    (Global.SMID.length() == 0) ||
                    (Global.ProtocolPrefix.length() == 0)) {
                textDir = new File(getFilesDir(), "SmartMenuFiles");
                Utils.deleteDirectory(textDir);
                getBootSettings();
            } else {
                stage2();
            }
        }
    }

    // Dialog for Initial Boot Settings
    private void getBootSettings() {
        LayoutInflater factory = LayoutInflater.from(this);
        final View textEntryView = factory.inflate(R.layout.boot_settings, null);

        final CustomDialog customDialog = new CustomDialog(this);
        customDialog.setContentView(textEntryView);
        customDialog.show();
        customDialog.setCancelable(false);
        customDialog.setCanceledOnTouchOutside(false);

        TextView tx1 = (TextView) customDialog.findViewById(R.id.textBootTitle);
        tx1.setText(getString(R.string.boot_title));
        tx1 = (TextView) customDialog.findViewById(R.id.textBootIntro);
        tx1.setText(getString(R.string.boot_intro));

        EditText et1 = (EditText) customDialog.findViewById(R.id.serverIP);
        if (Global.ServerIP.length() == 0) {
            et1.setText(Global.ServerIPHint);
        } else {
            et1.setText(Global.ServerIP);
        }

        EditText et2 = (EditText) customDialog.findViewById(R.id.SMID);
        et2.setText(Global.SMID);

        Button but1 = (Button) customDialog.findViewById(R.id.butContinue);
        but1.setText(getString(R.string.boot_continue));
        but1.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // Grab the inputs and see how they look
                Editor prefEdit = prefs.edit();

                EditText et1 = (EditText) customDialog.findViewById(R.id.serverIP);
                Global.ServerIP = et1.getText().toString();

                EditText et2 = (EditText) customDialog.findViewById(R.id.SMID);
                Global.SMID = et2.getText().toString();

                EditText et3 = (EditText) customDialog.findViewById(R.id.AdminPin);
                Global.AdminPin = et3.getText().toString();

                // Check for SSL Protocol
                CheckBox cbSSL = (CheckBox) customDialog.findViewById(R.id.SSLprotocol);
                if (cbSSL.isChecked()) prefEdit.putString("protocolprefix", "https://");
                else prefEdit.putString("protocolprefix", "http://");
                prefEdit.commit();
                Global.ProtocolPrefix = (new String(prefs.getString("protocolprefix", "http://")));

                // Check for Server Availability 204 Check
                CheckBox cb204 = (CheckBox) customDialog.findViewById(R.id.Check204);
                if (cb204.isChecked()) prefEdit.putBoolean("checkavailability", true);
                else prefEdit.putBoolean("checkavailability", false);
                prefEdit.commit();
                Global.CheckAvailability = (new Boolean(prefs.getBoolean("checkavailability", false)));

                if ((Global.ServerIP.length() > 0) &&
                        (Global.SMID.length() > 0)) {
                    prefEdit.putString("serverip", Global.ServerIP);
                    prefEdit.putString("smid", Global.SMID);
                    prefEdit.putString("adminpin", Global.AdminPin);
                    prefEdit.commit();
                    // Carry on ...
                    customDialog.dismiss();
                    stage2();
                }
            }
        });
    }

    // Continue loading preferences
    private void stage2() {
        // Now we are ready to load

        // Set the directory to save text files
        textDir = new File(getFilesDir(), "SmartMenuFiles");
        if (!textDir.exists())
            textDir.mkdirs();

        // get the initial menu version from device
        serverMv = Global.MenuVersion;

        // do the loading operations on a background thread
        new Thread(new Runnable() {
            public void run() {
                if (!Global.AutoMenuReload) {
                    log("No AutoMenuReload");
                    if (allLocalFilesExist()) {
                        LocalFileRead2();
                    } else if (pingIP()) {
                        HttpDownload2();
                    } else {
                        returnCode = 1;
                        mHandler.post(noConnection);
                    }
                } else {
                    log("AutoMenuReload");
                    if (pingIP()) {
                        if (serverMv.equalsIgnoreCase(Global.MenuVersion)) {
                            // no new menu so load from local
                            //log("same menu version");
                            if (allLocalFilesExist()) {
                                LocalFileRead2();
                            } else {
                                HttpDownload2();
                            }
                        } else {
                            log("different menu version");
                            // new menu so get files from server
                            Editor prefEdit = prefs.edit();
                            prefEdit.putString("menuversion", serverMv);
                            prefEdit.commit();
                            //jsonSetter(Global.Settings, "menuversion", serverMv);
                            Global.MenuVersion = serverMv;
                            HttpDownload2();
                        }
                    } else if (allLocalFilesExist()) {
                        LocalFileRead2();
                    } else {
                        returnCode = 1;
                        mHandler.post(noConnection);
                    }
                }
            }
        }).start();
    }

    // check for ping connectivity and get the current server menu version
    private boolean pingIP() {
        log("entering PingIP");
        String ip1 = Global.ProtocolPrefix + Global.ServerIP + Global.ServerReturn204;
        filesSourceName[3] = Global.ProtocolPrefix + Global.ServerIP + "/" + Global.SMID + "/" + "settings.txt";
        String ip2 = filesSourceName[3]; // settings file
        int status = -1;

        try {
            if (Global.CheckAvailability) {
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
                    // reachable so now get the menuversion in the settings.txt json file
                    String tmpSettings = Utils.DownloadText(ip2);
                    downloadSuccess = true;
                    // create the json settings files
                    JSONArray tmpJson = new JSONArray(tmpSettings);
                    // load the menuversion
                    serverMv = jsonGetter(tmpJson, "menuversion").toString();
                } else {
                    downloadSuccess = false;
                    log("PingIP Fail SC=" + status + " expecting 204");
                }
            } else {
                String tmpSettings = Utils.DownloadText(ip2);
                downloadSuccess = true;
                JSONArray tmpJson = new JSONArray(tmpSettings);
                serverMv = jsonGetter(tmpJson, "menuversion").toString();
            }
        } catch (Exception e) {
            downloadSuccess = false;
            log("PingIP Exception SC=" + status + " ex=" + e);
        }
        return downloadSuccess;
    }

    private void HttpDownload2() {
        try {
            Global.FileSource = "Server";
            //Loop through the downloads here
            //setup file source path+name
            filesSourceName[0] = Global.ProtocolPrefix + Global.ServerIP + "/" + Global.SMID + "/" + "menufile.txt";
            filesSourceName[1] = Global.ProtocolPrefix + Global.ServerIP + "/" + Global.SMID + "/" + "category.txt";
            filesSourceName[2] = Global.ProtocolPrefix + Global.ServerIP + "/" + Global.SMID + "/" + "kitchen.txt";
            filesSourceName[3] = Global.ProtocolPrefix + Global.ServerIP + "/" + Global.SMID + "/" + "settings.txt";
            filesSourceName[4] = Global.ProtocolPrefix + Global.ServerIP + "/" + Global.SMID + "/" + "options.txt";
            filesSourceName[5] = Global.ProtocolPrefix + Global.ServerIP + "/" + Global.SMID + "/" + "extras.txt";
            for (int i = 0; i < filesSourceName.length; i++) {
                filesText[i] = Utils.DownloadText(filesSourceName[i]);
                if (filesText[i].length() > 0) {
                    filesText[i] = Utils.removeBOMchar(filesText[i]);
                    filesText[i] = Utils.removeCommentLines(filesText[i]);
                    if (i == 0) {
                        filesText[i] = Utils.removeUnAvailable(filesText[i]);
                    }
                    //save it to the local cache for later use
                    writeOutFile(filesText[i], filesLocalName[i]);
                } else {
                    log("Start: httpsdownload fail");
                    returnCode = 2;
                    mHandler.post(noConnection);
                    break;
                }
            }

            Global.MENUTXT = filesText[0];
            Global.CATEGORYTXT = filesText[1];
            Global.KITCHENTXT = filesText[2];
            Global.SETTINGSTXT = filesText[3];
            Global.Settings = new JSONArray(Global.SETTINGSTXT);
            Global.OPTIONSTXT = filesText[4];
            Global.EXTRASTXT = filesText[5];

            Global.MenuVersion = jsonGetter(Global.Settings, "menuversion").toString();
            serverMv = jsonGetter(Global.Settings, "menuversion").toString();

            Editor prefEdit = prefs.edit();
            prefEdit.putString("menuversion", serverMv);
            prefEdit.commit();

            loadSettings();
            Intent kintent = new Intent(getApplicationContext(), MenuActivity.class);
            kintent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(kintent);
            finish();
        } catch (Exception e) {
            returnCode = 2;
            log("ex2=" + e);
            mHandler.post(noConnection);
        }
    }

    private void LocalFileRead2() {
        try {
            for (int i = 0; i < filesLocalName.length; i++) {
                File readFile = new File(textDir, filesLocalName[i]);
                filesText[i] = Utils.ReadLocalFile(readFile);
            }
            // process and start it up
            //log("LocalFileRead Success");
            Global.FileSource = "Local Files";
            Global.MENUTXT = filesText[0];
            Global.CATEGORYTXT = filesText[1];
            Global.KITCHENTXT = filesText[2];
            Global.SETTINGSTXT = filesText[3];
            Global.Settings = new JSONArray(Global.SETTINGSTXT);
            Global.OPTIONSTXT = filesText[4];
            Global.EXTRASTXT = filesText[5];

            Global.MenuVersion = jsonGetter(Global.Settings, "menuversion").toString();
            serverMv = jsonGetter(Global.Settings, "menuversion").toString();

            Editor prefEdit = prefs.edit();
            prefEdit.putString("menuversion", serverMv);
            prefEdit.commit();

            loadSettings();
            Intent kintent = new Intent(getApplicationContext(), MenuActivity.class);
            kintent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(kintent);
            finish();
        } catch (Exception e) {
            returnCode = 3;
            log("ex3=" + e);
            mHandler.post(noConnection);
        }
    }

    private Boolean allLocalFilesExist() {
        Boolean gotAllFiles = true;
        for (int i = 0; i < filesLocalName.length; i++) {
            File testExist = new File(textDir, filesLocalName[i]);
            if ((!testExist.exists()) | (testExist.length() == 0)) {
                gotAllFiles = false;
            }
        }
        return gotAllFiles;
    }

    public boolean checkInternetConnection() {
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

    public void failedAuth1() {
        log("No Ping No Local");
        AlertDialog alertDialog = new AlertDialog.Builder(StartActivity.this).create();
        alertDialog.setTitle("Connection 1");
        alertDialog.setIcon(android.R.drawable.stat_sys_warning);
        alertDialog.setMessage("SmartMenu could not be started. Please check your settings.");
        alertDialog.setCancelable(false);
        alertDialog.setButton("Exit", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        alertDialog.show();
    }

    public void failedAuth2() {
        log("Local files error");
        AlertDialog alertDialog = new AlertDialog.Builder(StartActivity.this).create();
        alertDialog.setTitle("Connection 2");
        alertDialog.setIcon(android.R.drawable.stat_sys_warning);
        alertDialog.setMessage("SmartMenu could not be started. Please check your settings.");
        alertDialog.setCancelable(false);
        alertDialog.setButton("Exit", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        alertDialog.show();
    }

    public void failedAuth3() {
        log("Files download error");
        AlertDialog alertDialog = new AlertDialog.Builder(StartActivity.this).create();
        alertDialog.setTitle("Connection 3");
        alertDialog.setIcon(android.R.drawable.stat_sys_warning);
        alertDialog.setMessage("SmartMenu could not be started. Please check your settings.");
        alertDialog.setCancelable(false);
        alertDialog.setButton("Exit", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        alertDialog.show();
    }

    public void loadSettings() {
        // load the rest from prefs or JSON settings

        Global.Printer1Type = (Boolean) jsonGetter(Global.Settings, "printer1type");
        Global.Printer2Type = (Boolean) jsonGetter(Global.Settings, "printer1type");
        Global.Printer3Type = (Boolean) jsonGetter(Global.Settings, "printer1type");

        Global.CheckWifi = (Boolean) jsonGetter(Global.Settings, "checkwifi");
        Global.ShowPics = (Boolean) jsonGetter(Global.Settings, "showpics");
        Global.ShowRating = (Boolean) jsonGetter(Global.Settings, "showrating");
        Global.ShowBadges = (Boolean) jsonGetter(Global.Settings, "showbadges");
        Global.P1PrintSentTime = (Boolean) jsonGetter(Global.Settings, "p1printsenttime");
        Global.P2PrintSentTime = (Boolean) jsonGetter(Global.Settings, "p1printsenttime");
        Global.P3PrintSentTime = (Boolean) jsonGetter(Global.Settings, "p1printsenttime");

        Global.FontColor = Color.parseColor(jsonGetter(Global.Settings, "fontcolor").toString());
        Global.BackColor = Color.parseColor(jsonGetter(Global.Settings, "backcolor").toString());
        Global.HeaderColor = Color.parseColor(jsonGetter(Global.Settings, "headercolor").toString());
        Global.ButFontColor = Color.parseColor(jsonGetter(Global.Settings, "butfontcolor").toString());
        Global.ButColor = Color.parseColor(jsonGetter(Global.Settings, "mainbuttoncolor").toString());
        Global.SelButFontColor = Color.parseColor(jsonGetter(Global.Settings, "selbutfontcolor").toString());
        Global.SelButColor = Color.parseColor(jsonGetter(Global.Settings, "selbutcolor").toString());
        Global.LabelFontColor = Color.parseColor(jsonGetter(Global.Settings, "labelfontcolor").toString());
        Global.LabelColor = Color.parseColor(jsonGetter(Global.Settings, "labelcolor").toString());

        Global.FontScaleFactor = (Integer) jsonGetter(Global.Settings, "fontscalefactor");
        Global.Printer1Copy = (Integer) jsonGetter(Global.Settings, "printer1copy");
        Global.SendOrderMode = (Integer) jsonGetter(Global.Settings, "sendordermode");

        Global.POS1Ip = jsonGetter(Global.Settings, "pos1ip").toString();
        Global.POS2Ip = jsonGetter(Global.Settings, "pos2ip").toString();
        Global.POS3Ip = jsonGetter(Global.Settings, "pos3ip").toString();

        Global.POS1Logo = (Boolean) jsonGetter(Global.Settings, "pos1logo");
        Global.POS1Enable = (Boolean) jsonGetter(Global.Settings, "pos1enable");
        Global.POS2Enable = (Boolean) jsonGetter(Global.Settings, "pos2enable");
        Global.POS3Enable = (Boolean) jsonGetter(Global.Settings, "pos3enable");

        Global.P2KitchenCodes = (Boolean) jsonGetter(Global.Settings, "p2kitchencodes");
        Global.P3KitchenCodes = (Boolean) jsonGetter(Global.Settings, "p3kitchencodes");

        Global.P2FilterCats = jsonGetter(Global.Settings, "p2filtercats").toString();
        Global.P3FilterCats = jsonGetter(Global.Settings, "p3filtercats").toString();

        Global.AutoOpenDrawer = (Boolean) jsonGetter(Global.Settings, "autoopendrawer");
        Global.DisplaySpecials = (Boolean) jsonGetter(Global.Settings, "displayspecials");
        Global.ShowBigExit = (Boolean) jsonGetter(Global.Settings, "showbigexit");
        Global.ShowDishInfo = (Boolean) jsonGetter(Global.Settings, "showdishinfo");
        Global.ShowDishFeedback = (Boolean) jsonGetter(Global.Settings, "showdishfeedback");
        Global.ShowDishDescriptions = (Boolean) jsonGetter(Global.Settings, "showdishdescriptions");
        Global.ChooseGuests = (Boolean) jsonGetter(Global.Settings, "chooseguests");
        Global.ShowZoomPic = (Boolean) jsonGetter(Global.Settings, "showzoompic");
        Global.AllowChangeTable = (Boolean) jsonGetter(Global.Settings, "allowchangetable");
        Global.PrintDishID = (Boolean) jsonGetter(Global.Settings, "printdishid");

        Global.MasterDeviceId = jsonGetter(Global.Settings, "masterdeviceid").toString();
        Global.POSIp = jsonGetter(Global.Settings, "posmasterip").toString();
        Global.TakeOutIp = jsonGetter(Global.Settings, "takeoutip").toString();
        Global.CustomerName = jsonGetter(Global.Settings, "customername").toString();
        Global.CustomerNameBrief = jsonGetter(Global.Settings, "customernamebrief").toString();
        Global.StoreAddress = jsonGetter(Global.Settings, "storeaddress").toString();

        if (Global.AdminPin.length() == 0) {
            Global.AdminPin = jsonGetter(Global.Settings, "adminpin").toString();
        }

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
                log("exj0=" + e);
            }
        }
        return value;
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
                log("exj1=" + e);
            }
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

    private void writeOutFile(String fcontent, String fname) {
        File writeFile = new File(textDir, fname);
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(writeFile, false), "UTF-8"));
            writer.write(fcontent);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            log("exw=" + e);
        }
    }
}