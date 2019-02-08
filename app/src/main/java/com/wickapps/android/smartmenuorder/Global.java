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

import org.json.JSONArray;

import java.util.ArrayList;

public class Global {
    // SERVER SETTINGS ----------------------------------------------------------------------------------------
    public static String PosSaveOrderJsonURL = "/phpcommon/saveorder1118.php";
    public static String ServerReturn204 = "/phpcommon/return204.php";
    public static String UPLOADER = "/phpcommon/uploadfile0413.php";

    public static String ProtocolPrefix = "http://";  // Default to non SSL
    public static String ServerIP = "";
    public static String ServerIPHint = "order.lilysbeijing.com";
    public static String SMID = "";
    public static Boolean CheckAvailability = false;

    public static String PICBASE200 = "fetch200/"; // Path to small pics
    public static String PICBASE800 = "fetch800/"; // Path to large pics

    public static String AppName = "SmartMenu_Order";

    // MQTT settings ------------------------------------------------------------------------------------------
    public static String BrokerOrderTopic = "";
    public static String BrokerMsgTopic = "";
    public static int BrokerPort = 1883;
    public static String BrokerURLFormat = "tcp://%s:%d";

    // CUSTOMER SPECIFC SETTINGS ------------------------------------------------------------------------------
    public static String CustomerName = "";
    public static String CustomerNameBrief = "";
    public static String StoreAddress = "";
    public static String LogoName = "logotop.png";

    // MORE SETTINGS ------------------------------------------------------------------------------------------
    public static JSONArray Settings = null;

    public static Boolean PublicCloud = true;    // true = public, no support for private cloud
    public static Boolean MenuStarted = false;    // true when up and running
    public static String MenuVersion = "";        // menu version
    public static String FileSource = "";        // Info string for source files: PUBLIC CLOUD or PRIVATE CLOUD or LOCAL

    public static Boolean StartEnglish = null;    // loaded from pref
    public static Boolean CheckWifi = null;    // loaded from pref
    public static Boolean ShowPics = null;    // loaded from pref
    public static Boolean AutoMenuReload = null;    // loaded from pref
    public static Boolean ShowRating = null;    // loaded from pref
    public static Boolean ShowBadges = null;    // loaded from pref
    public static Boolean AutoOpenDrawer = null;    // loaded from pref
    public static Boolean DisplaySpecials = null;    // loaded from pref
    public static Boolean ShowBigExit = null;    // loaded from pref
    public static Boolean ShowDishInfo = null;    // loaded from pref
    public static Boolean ShowDishFeedback = null;    // loaded from pref
    public static Boolean ShowDishDescriptions = null;    // loaded from pref
    public static Boolean ChooseGuests = null;    // loaded from pref
    public static Boolean ShowZoomPic = null;    // loaded from pref
    public static Boolean AllowChangeTable = null;    // loaded from pref

    public static Integer FontColor = null;    // loaded from json
    public static Integer BackColor = null;    // loaded from json
    public static Integer HeaderColor = null;    // loaded from json
    public static Integer ButFontColor = null;    // loaded from json
    public static Integer ButColor = null;    // loaded from json
    public static Integer SelButFontColor = null;    // loaded from json
    public static Integer SelButColor = null;    // loaded from json
    public static Integer LabelFontColor = null;    // loaded from json
    public static Integer LabelColor = null;    // loaded from json

    public static int FontScaleFactor = 5;
    public static int Printer1Copy = 1;
    public static int SendOrderMode = 1;        // 1=direct print 2=POS 3=MMQT
    public static String DeviceId = null;

    public static Boolean PrintDishID = null;

    public static Boolean Printer1Type = null;    // loaded from pref, true = epson, false = GPrinter
    public static Boolean Printer2Type = null;
    public static Boolean Printer3Type = null;

    public static Boolean POS1Logo = null;    // Print .bmp logo on ticket?
    public static Boolean POS1Enable = null;    // Do we have POS printers
    public static Boolean POS2Enable = null;
    public static Boolean POS3Enable = null;

    public static String P2FilterCats = null;
    public static String P3FilterCats = null;

    public static Boolean P1PrintSentTime = null;    // loaded from pref
    public static Boolean P2PrintSentTime = null;    // loaded from pref
    public static Boolean P3PrintSentTime = null;    // loaded from pref

    public static Boolean P2KitchenCodes = null;
    public static Boolean P3KitchenCodes = null;

    public static String POS1Ip = null;    // printer IP Addresses
    public static String POS2Ip = null;
    public static String POS3Ip = null;

    public static String MasterDeviceId = "";

    public static String POSIp = "192.168.1.71";
    public static int POSSocket = 8080;

    public static String TakeOutIp = "192.168.1.70";
    public static int TakeOutSocket = 8080;

    public static int ConnectTimeout = 15000;
    public static int ReadTimeout = 15000;
    public static int MaxBuffer = 15000;
    public static int SocketRetry = 1;
    public static int SocketRetrySleep = 1000;

    public static String AdminPin = "";

    public static int TicketCharWidth = 42;
    public static int KitcTicketCharWidth = 21;

    public static Boolean EnglishLang = true;    // keeps track of current language state

    public static String MENUTXT = "menu text will download into here";
    public static String CATEGORYTXT = "category text will download into here";
    public static String KITCHENTXT = "kitchen codes will download into here";
    public static String SETTINGSTXT = "settings will download into here";
    public static String OPTIONSTXT = "dish options will download into here";
    public static String EXTRASTXT = "dish extras will download into here";

    public static String TodayDate = "";

    public static int TotalRMB = 0;

    public static int NumSpecials = 0;        // This will hold the number of specials
    public static int MenuMaxItems = 0;        // This will hold the number of menu items
    public static int NumCategory = 0;        // This will hold the number of cats

    public static ArrayList<String> fetchURL200 = new ArrayList<String>(); // strings for lazy loading of the small portrait images
    public static ArrayList<String> fetchURL800 = new ArrayList<String>(); // strings for lazy loading of the small portrait images

    public static ArrayList<String> welcome = new ArrayList<String>();
    public static ArrayList<String> tablenames = new ArrayList<String>();

    public static String OrderId = "";
    public static String TableName = "";
    public static int TableID = 0;
    public static String TableTime = "";
    public static String SendTime = "";
    public static String Guests = "0";

    public static String SaleType = "0";    // 0=cash  1=credit  2=something  3=another

    public static int PicHeight = 0;        // Device dependent pic height
}
