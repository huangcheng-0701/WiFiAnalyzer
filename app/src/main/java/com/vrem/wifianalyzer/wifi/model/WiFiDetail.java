/*
 * WiFiAnalyzer
 * Copyright (C) 2018  VREM Software Development <VREMSoftwareDevelopment@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.vrem.wifianalyzer.wifi.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.vrem.AppUtil;
import com.vrem.wifianalyzer.MainContext;
import com.vrem.wifianalyzer.R;
import com.vrem.wifianalyzer.settings.Settings;
import com.vrem.wifianalyzer.wifi.adapter.APDialogListAdapter;
import com.vrem.wifianalyzer.wifi.band.WiFiWidth;
import com.vrem.wifianalyzer.wifi.common.BaseUtils;
import com.vrem.wifianalyzer.wifi.common.FrequencyTransformTools;
import com.vrem.wifianalyzer.wifi.common.MacSsidDBUtils;
import com.vrem.wifianalyzer.wifi.common.PrefSingleton;
import com.vrem.wifianalyzer.wifi.common.VolleySingleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static android.content.ContentValues.TAG;

//单条wifi详细信息
public class WiFiDetail implements Comparable<WiFiDetail> {
    public static final WiFiDetail EMPTY = new WiFiDetail(StringUtils.EMPTY,
            StringUtils.EMPTY, StringUtils.EMPTY, WiFiSignal.EMPTY, "",
            "","",0,"","","");
    private static final String SSID_EMPTY = "***";//用于返回空SSID的wifiSSID信息

    private final List<WiFiDetail> children; //单项wifi信息的集合
    private final String SSID;  //wifiSSID
    private final String BSSID; //mac地址
    private final String capabilities; //加密方式
    private final WiFiSignal wiFiSignal; //wifi信号
    private final WiFiAdditional wiFiAdditional; //wifi中心频率，主频率，水平，信道、MHz差、信道

    private final String client; //客户端mac
    private final String cipher; //算法
    private final String wps;
    private final double rate;
    private final String beacons; //信号强度
    private final String last_time; // 最后扫描时间
    private final String count; // last_time对比叠加次数

    private static APDialogListAdapter apDialogListAdapter;

    public WiFiDetail(@NonNull String SSID, @NonNull String BSSID, @NonNull String capabilities,
                      @NonNull WiFiSignal wiFiSignal, @NonNull WiFiAdditional wiFiAdditional,
                      String client,String cipher,String wps,double rate,String beacons,String last_time,String count) {
        if (SSID.equals("")){
            this.SSID = "隐藏SSID";
        }else {
            this.SSID = SSID;
        }
        this.BSSID = BSSID;
        this.capabilities = capabilities;
        this.wiFiSignal = wiFiSignal;
        this.wiFiAdditional = wiFiAdditional;
        this.children = new ArrayList<>();

        this.client = client;
        this.cipher = cipher;
        this.wps = wps;
        this.rate = rate;
        this.beacons = beacons;
        this.last_time = last_time;
        this.count = count;
    }

    //接收下面WiFiDetail构造方法回传的数据
    public WiFiDetail(@NonNull String SSID, @NonNull String BSSID, @NonNull String capabilities, @NonNull WiFiSignal wiFiSignal,
                      String client, String cipher,String wps,double rate,String beacons,String last_time,String count) {
        this(SSID, BSSID, capabilities, wiFiSignal, WiFiAdditional.EMPTY,client,cipher,wps,rate,beacons,last_time,count);
    }

    //定义构造方法，回传给上面的WiFiDetail构造方法
    public WiFiDetail(@NonNull WiFiDetail wiFiDetail, @NonNull WiFiAdditional wiFiAdditional,String client,
                      String cipher,String wps,double rate,String beacons,String last_time,String count) {
        this(wiFiDetail.SSID, wiFiDetail.BSSID, wiFiDetail.getCapabilities(), wiFiDetail.getWiFiSignal(),
                wiFiAdditional,client,cipher,wps,rate,beacons,last_time,count);
    }

    public double getRate() {
        return rate;
    }
    //返回wifi加密方式
    public Security getSecurity() {
        return Security.findOne(capabilities);
    }

    public String getSSID() {
        return isHidden() ? SSID_EMPTY : SSID; //为true则返回SSID_EMPTY 反之
    }

    public String getLast_time() { return last_time; }

    public String getCount() { return count; }

    boolean isHidden() {
        return StringUtils.isBlank(SSID); //判断SSID是否为空，为空返回true 反之
    }

    public String getWps(){
        return wps;
    }

    public String getBSSID() {
        return BSSID;
    }
    public String getClient(){
        return client;
    }
    public String getCipher(){
        return cipher;
    }

    public String getCapabilities() {
        return capabilities;
    }

    public WiFiSignal getWiFiSignal() {
        return wiFiSignal;
    }

    public WiFiAdditional getWiFiAdditional() {
        return wiFiAdditional;
    }

    public List<WiFiDetail> getChildren() {
        return children;
    }

    public boolean noChildren() {
        return !getChildren().isEmpty();
    }

    //返回SSID BSSID
    public String getTitle() {
        return String.format("%s (%s)", getSSID(), BSSID);
    }

    //添加单条wifi数据到list集合中
    public void addChild(@NonNull WiFiDetail wiFiDetail) {
        children.add(wiFiDetail);
    }

    public String getBeacons() {
        return beacons;
    }

    /**
     * 重写object的equals、hashCode、compareTo、toString方法 对数据属性做相应的封装
     * */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        WiFiDetail that = (WiFiDetail) o;

        return new EqualsBuilder()
                .append(getSSID(), that.getSSID())
                .append(getBSSID(), that.getBSSID())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(getSSID())
                .append(getBSSID())
                .toHashCode();
    }

    @Override
    public int compareTo(@NonNull WiFiDetail another) {
        return new CompareToBuilder()
                .append(getSSID(), another.getSSID())
                .append(getBSSID(), another.getBSSID())
                .toComparison();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public static void setAPInfo(final Context context,
                                 final ListView listview, String devId, final int tag,
                                 final ProgressBar progressBar, final int mainPage, final int sort,
                                 final TextView refresh, final TextView noData,
                                 final boolean isDialog) throws JSONException {
        final List<WiFiDetail> apData = new ArrayList<>();

//        if (mainPage == 1) {
//            ScanActivity.flag = 0;
//            ((Activity) context).invalidateOptionsMenu();
//        }
        progressBar.setVisibility(View.VISIBLE);
        listview.setVisibility(View.GONE);
        SharedPreferences sharedPreferences = context.getSharedPreferences("user_info", 0);
        String token = sharedPreferences.getString("token", "");
        String username = sharedPreferences.getString("username", "");
        String ip = sharedPreferences.getString("ip", "");
        JSONObject obj = new JSONObject();
        obj.put("username", username);
        obj.put("token", token);
        obj.put("devid", devId);
        String url = "http://" + ip + "/mobi_api/v1/deviceinfo";
        JsonObjectRequest getRequest = new JsonObjectRequest(
                Request.Method.POST, url, obj,
                new Response.Listener<JSONObject>() {
                    public void onResponse(JSONObject response_tmp) {
                        // display response
                        JSONObject jo = null;
                        try {
                            JSONObject response = new JSONObject("{\"action\":\"action\",\"status\":0,\"data\":{\"aps\":{\"5a:fb:84:b4:9a:8d\":{\"wpa_cipher\":[],\"sta_bssids\":[\"10:2a:b3:dd:3d:98\"],\"bssid\":\"5a:fb:84:b4:9a:8d\",\"enc\":[\"WPA2\"],\"channel\":1,\"last_time\":832.841157,\"wps\":\"configured\",\"essid\":\"Lab13\",\"first_time\":832.221925,\"basic_rates\":[24,12,6],\"rx_datas\":0,\"wpa_auth\":[],\"extra_rates\":[48,9,18,36,54],\"wpa2_auth\":[\"PSK\"],\"wpa2_cipher\":[\"CCMP\"],\"tx_datas\":0,\"power\":-60,\"beacons\":7},\"f0:b4:29:6a:ca:d9\":{\"wpa_cipher\":[],\"sta_bssids\":[\"10:2a:b3:dd:3d:98\"],\"bssid\":\"f0:b4:29:6a:ca:d9\",\"enc\":[\"WPA2\"],\"channel\":1,\"last_time\":832.853479,\"wps\":\"configured\",\"essid\":\"Lab_BCMI_233\",\"first_time\":832.322223,\"basic_rates\":[1,2,11,5.5],\"rx_datas\":0,\"wpa_auth\":[],\"extra_rates\":[36,6,9,12,48,18,54,24],\"wpa2_auth\":[\"PSK\"],\"wpa2_cipher\":[\"TKIP\",\"CCMP\"],\"tx_datas\":0,\"power\":-78,\"beacons\":6},\"a8:bd:27:24:e4:a4\":{\"wpa_cipher\":[],\"sta_bssids\":[],\"bssid\":\"a8:bd:27:24:e4:a4\",\"enc\":[\"WPA2\"],\"channel\":1,\"last_time\":832.800368,\"wps\":null,\"essid\":\"eduroam\",\"first_time\":832.288364,\"basic_rates\":[24,12,6],\"rx_datas\":0,\"wpa_auth\":[],\"extra_rates\":[48,9,18,36,54],\"wpa2_auth\":[\"MGT\"],\"wpa2_cipher\":[\"CCMP\"],\"tx_datas\":0,\"power\":-80,\"beacons\":4},\"7c:dd:90:e0:00:e6\":{\"wpa_cipher\":[],\"sta_bssids\":[\"10:2a:b3:dd:3d:98\"],\"bssid\":\"7c:dd:90:e0:00:e6\",\"enc\":[\"WPA2\"],\"channel\":1,\"last_time\":832.891639,\"wps\":null,\"essid\":\"default_e0_00_e6\",\"first_time\":832.195933,\"basic_rates\":[1,2,11,5.5],\"rx_datas\":0,\"wpa_auth\":[],\"extra_rates\":[36,6,9,12,48,18,54,24],\"wpa2_auth\":[\"PSK\"],\"wpa2_cipher\":[\"CCMP\"],\"tx_datas\":0,\"power\":-28,\"beacons\":8},\"a8:bd:27:24:e4:a0\":{\"wpa_cipher\":[],\"sta_bssids\":[],\"bssid\":\"a8:bd:27:24:e4:a0\",\"enc\":[\"OPEN\"],\"channel\":1,\"last_time\":832.79931,\"wps\":null,\"essid\":\"SJTU-Web\",\"first_time\":832.184792,\"basic_rates\":[24,12,6],\"rx_datas\":0,\"wpa_auth\":[],\"extra_rates\":[48,9,18,36,54],\"wpa2_auth\":[],\"wpa2_cipher\":[],\"tx_datas\":0,\"power\":-80,\"beacons\":7},\"a8:bd:27:24:e4:a5\":{\"wpa_cipher\":[],\"sta_bssids\":[\"70:56:81:a0:e5:21\",\"f0:b4:29:9e:68:88\"],\"bssid\":\"a8:bd:27:24:e4:a5\",\"enc\":[\"WEP\"],\"channel\":1,\"last_time\":832.894687,\"wps\":null,\"essid\":\"SJTU\",\"first_time\":832.186203,\"basic_rates\":[24,12,6],\"rx_datas\":1,\"wpa_auth\":[],\"extra_rates\":[48,9,18,36,54],\"wpa2_auth\":[\"PSK\"],\"wpa2_cipher\":[\"CCMP\"],\"tx_datas\":1,\"power\":-78,\"beacons\":4},\"a8:bd:27:24:e4:a3\":{\"wpa_cipher\":[],\"sta_bssids\":[\"10:2a:b3:dd:3d:98\"],\"bssid\":\"a8:bd:27:24:e4:a3\",\"enc\":[\"OPEN\"],\"channel\":1,\"last_time\":832.841411,\"wps\":null,\"essid\":\"ChinaUnicom\",\"first_time\":832.185583,\"basic_rates\":[24,12,6],\"rx_datas\":0,\"wpa_auth\":[],\"extra_rates\":[48,9,18,36,54],\"wpa2_auth\":[],\"wpa2_cipher\":[],\"tx_datas\":0,\"power\":-80,\"beacons\":4},\"a8:bd:27:24:e4:a1\":{\"wpa_cipher\":[],\"sta_bssids\":[],\"bssid\":\"a8:bd:27:24:e4:a1\",\"enc\":[\"OPEN\"],\"channel\":1,\"last_time\":832.799516,\"wps\":null,\"essid\":\"CMCC\",\"first_time\":832.287529,\"basic_rates\":[24,12,6],\"rx_datas\":0,\"wpa_auth\":[],\"extra_rates\":[48,9,18,36,54],\"wpa2_auth\":[],\"wpa2_cipher\":[],\"tx_datas\":0,\"power\":-78,\"beacons\":5},\"a8:bd:27:24:e4:a2\":{\"wpa_cipher\":[],\"sta_bssids\":[],\"bssid\":\"a8:bd:27:24:e4:a2\",\"enc\":[\"OPEN\"],\"channel\":1,\"last_time\":832.799963,\"wps\":null,\"essid\":\"CMCC-EDU\",\"first_time\":832.799963,\"basic_rates\":[24,12,6],\"rx_datas\":0,\"wpa_auth\":[],\"extra_rates\":[48,9,18,36,54],\"wpa2_auth\":[],\"wpa2_cipher\":[],\"tx_datas\":0,\"power\":-74,\"beacons\":1}},\"stas\":{\"70:56:81:a0:e5:21\":{\"ap_bssid\":\"a8:bd:27:24:e4:a5\",\"rate_from\":null,\"probes\":[],\"first_time\":832.701825,\"rx_datas\":0,\"last_time\":832.701825,\"channel\":1,\"bssid\":\"70:56:81:a0:e5:21\",\"tx_datas\":1,\"rate_to\":6,\"power\":-78},\"f0:b4:29:9e:68:88\":{\"ap_bssid\":null,\"rate_from\":6,\"probes\":[],\"first_time\":832.894061,\"rx_datas\":0,\"last_time\":832.894687,\"channel\":1,\"bssid\":\"f0:b4:29:9e:68:88\",\"tx_datas\":0,\"rate_to\":6,\"power\":-80},\"10:2a:b3:dd:3d:98\":{\"ap_bssid\":null,\"rate_from\":1,\"probes\":[],\"first_time\":832.816341,\"rx_datas\":0,\"last_time\":832.853479,\"channel\":1,\"bssid\":\"10:2a:b3:dd:3d:98\",\"tx_datas\":0,\"rate_to\":1,\"power\":-66}}},\"id\":1}");
                            jo = new JSONObject(BaseUtils.JSONTokener(response.getString("data")));
                        } catch (JSONException e1) {
                            e1.printStackTrace();
                        }
                        try {
                            JSONObject jsonObjectAps = jo.getJSONObject("aps");
                            Iterator<?> keys = jsonObjectAps.keys();
                            String essid = null,enc = null,wps = null, bssid = null,channel = null,client = null,beacons = null;
                            String last_time = null;
                            double cRate = 0;
                            int power = 0;
                            while (keys.hasNext()) {
                                String key = (String)keys.next();
                                if (key.equals("")) continue;
                                JSONObject jsonObjectAp = jsonObjectAps.getJSONObject(key);
                                if (tag == 1 && jsonObjectAp.get("wps") == null) continue;
                                WiFiDetail apInfo = null;
                                essid = jsonObjectAp.getString("essid");
                                power = jsonObjectAp.getInt("power");
                                enc = jsonObjectAp.getJSONArray("enc").getString(0);
                                wps = jsonObjectAp.get("wps") == null ? "false" : "true";
                                bssid = key;
                                String cipher = "";
                                beacons = String.valueOf(jsonObjectAp.getInt("beacons"));
                                last_time = String.valueOf(jsonObjectAp.getLong("last_time"));
                                JSONArray wpa_cipher = jsonObjectAp.getJSONArray("wpa_cipher");
                                JSONArray wpa2_cipher = jsonObjectAp.getJSONArray("wpa2_cipher");
                                for (int i = 0; i < wpa_cipher.length(); i++) {
                                    if (cipher.equals("")) {
                                        cipher = cipher + wpa_cipher.getString(i);
                                    } else {
                                        cipher = cipher + " " + wpa_cipher.getString(i);
                                    }
                                }
                                for (int i = 0; i < wpa2_cipher.length(); i++) {
                                    if (cipher.equals("")) {
                                        cipher = cipher + wpa2_cipher.getString(i);
                                    } else {
                                        cipher = cipher + " " + wpa2_cipher.getString(i);
                                    }
                                }
//                                apInfo.setCipher(cipher);
//                                apInfo.setClient("[]");
                                client = "[]";
//                                apInfo.setChannel(jsonObjectAp.getInt("channel"));
                                channel = String.valueOf(jsonObjectAp.getInt("channel"));

                                JSONArray basic_rates = jsonObjectAp.getJSONArray("basic_rates"); //获取基础速率组
                                JSONArray extra_rates = jsonObjectAp.getJSONArray("extra_rates"); //获取额外速率组
                                double minRate = -1;
                                for (int i = 0; i < basic_rates.length(); i++) {
                                    if (basic_rates.getDouble(i) < minRate || minRate < 0) {
                                        minRate = basic_rates.getDouble(i);
                                    }
                                }
                                if (basic_rates.length() == 0) {
                                    for (int i = 0; i < extra_rates.length(); i++) {
                                        if (extra_rates.getDouble(i) < minRate || minRate < 0) {
                                            minRate = extra_rates.getDouble(i);
                                        }
                                    }
                                }
                                cRate = minRate;

                                WiFiWidth wiFiWidth =WiFiWidth.MHZ_40;//模拟wifi宽度
                                WiFiSignal addWiFiSignal = new WiFiSignal(1,2,wiFiWidth,power,channel);//模拟wifi信号 13：power dbm,
                                WiFiDetail addWiFiDetail = new WiFiDetail(essid,bssid,enc,addWiFiSignal,client,cipher,wps,cRate,beacons,last_time,"");//模拟单条wifi的基本信息 1111:essid  fds:bssid  fdss:enc
                                apData.add(addWiFiDetail);
                            }
//                            switch (sort) {
//                                case 0:
//                                    break;
//                                case 1:
//                                    ChannelComparator channelComparator = new ChannelComparator();
//                                    Collections.sort(apData, channelComparator);
//                                    break;
//                                case 2:
//                                    SignalComparator signaleComparator = new SignalComparator();
//                                    Collections.sort(apData, signaleComparator);
//                                    break;
//                                case 3:
//                                    WpsComparator wpsComparator = new WpsComparator();
//                                    Collections.sort(apData, wpsComparator);
//                                    break;
//                                case 4:
//                                    PrivacyComparator privacyComparator = new PrivacyComparator();
//                                    Collections.sort(apData, privacyComparator);
//                                    break;
//                                case 5:
//                                    ClientComparator clientComparator = new ClientComparator();
//                                    Collections.sort(apData, clientComparator);
//                                    break;
//                                default:
//                                    break;
//                            }

                            progressBar.setVisibility(View.GONE);
                            if (apData.size() == 0) {
                                noData.setVisibility(View.VISIBLE);
                                refresh.setVisibility(View.GONE);
                            } else {
                                noData.setVisibility(View.GONE);
                                refresh.setVisibility(View.GONE);
                                listview.setVisibility(View.VISIBLE);
                                if (isDialog) {
                                    apDialogListAdapter = new APDialogListAdapter(context, apData,R.layout.scan_dialog_listitem);
                                    listview.setAdapter(apDialogListAdapter);
                                } else {
//                                    apListAdapter = new APListAdapter(context,apData, R.layout.scan_listitem);
//                                    listview.setAdapter(apListAdapter);
                                }
                            }
                        } catch (JSONException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                progressBar.setVisibility(View.GONE);
                refresh.setVisibility(View.VISIBLE);
                noData.setVisibility(View.GONE);
                Toast.makeText(context, "通讯错误，请重试", Toast.LENGTH_SHORT).show();
                return;
            }
        });
        getRequest.setRetryPolicy(new DefaultRetryPolicy(30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        // add it to the RequestQueue
        if (VolleySingleton.getInstance(context).getRequestQueue() != null)
            VolleySingleton.getInstance(context).getRequestQueue().add(getRequest);
    }

    public static int scanStep1(final Context context) throws JSONException {
        Log.d(TAG, "scanStep1: 123--0");
        String url = PrefSingleton.getInstance().getString("url");

        JSONObject obj = new JSONObject();
        int gId = PrefSingleton.getInstance().getInt("id");
        PrefSingleton.getInstance().putInt("id", gId + 1);
        obj.put("id", gId); // 1

        JSONArray channels = new JSONArray();
        JSONObject param = new JSONObject();
        param.put("channels", channels); // 2-1
        param.put("interval", 1.5); // 2-2
        param.put("action", "scan"); // 2-3
        obj.put("param", param); // 3

        Log.w("SCAN_STEP_1", "REQUEST: " + obj.toString());
        System.out.println("20210305==发送指令：" + obj.toString());

        RequestFuture<JSONObject> requestFuture = RequestFuture.newFuture();
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url,  obj, requestFuture, requestFuture);
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        VolleySingleton.getInstance(context.getApplicationContext()).getRequestQueue().add(jsonObjectRequest);

        try {
            JSONObject response = requestFuture.get(5 - 1, TimeUnit.SECONDS);
            System.out.println("20210305==返回结果：" + response.toString());
            int status = response.getInt("status");
            if (status == 0) {
                Log.w("成功", "RESPONSE:" + response.toString());
                return 0;
            } else {
                Log.w("失败", "UNEXPECTED RESPONSE: " + response.toString());
                return -2;
            }
        } catch (TimeoutException e) {
            Log.w("超时", "TIMEOUT");
            PrefSingleton.getInstance().putString("deviceInfo",null);
            return -1;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return -3;
    }

    public static JSONObject scanStep2(Context context) throws JSONException {
        Log.d(TAG, "scanStep1: 123--1");
        String url = PrefSingleton.getInstance().getString("url");

        final JSONObject obj = new JSONObject();
        JSONObject param = new JSONObject();
        param.put("action", "action");
        obj.put("param", param);

        Log.w("SCAN_STEP_2", "REQUEST: " + obj.toString());
        System.out.println("20210305==发送指令：" + obj.toString());

        RequestFuture < JSONObject > requestFuture = RequestFuture.newFuture(); //声明同步的网络请求对象
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url,  obj, requestFuture, requestFuture); //声明接收对象
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        //设置超时和重试请求,第一个代表超时时间,第三个参数代表最大重试次数,这里设置为1.0f代表如果超时，则不重试
        VolleySingleton.getInstance(context.getApplicationContext()).getRequestQueue().add(jsonObjectRequest); //把请求加入队列,此时已产生数据

        try {
            JSONObject response = requestFuture.get(10 - 1, TimeUnit.SECONDS); //获取请求结果,包含扫描的wifi数据
            System.out.println("20210305==返回结果：" + response.toString());

            int status = response.getInt("status");
            if (status == 0) {
                Log.w("SCAN_STEP_2", "RESPONSE:" + response.toString());
                return response;
            } else {
                Log.w("SCAN_STEP_2", "UNEXPECTED RESPONSE: " + response.toString());
                return null;
            }
        } catch (TimeoutException e) {
            Log.w("超时", "TIMEOUT");
            return null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<WiFiDetail> response2ApData(JSONObject response, int tag, int sort, Context context) throws JSONException {
        Log.d(TAG, "scanStep1: 123--2");

        List<WiFiDetail> apData = new ArrayList<WiFiDetail>();
        if (response.getInt("id") == -1) { //表明没有数据
            return null;
        }
        JSONObject jo = new JSONObject(BaseUtils.JSONTokener(response.getString("data"))); //获取到data数据
        JSONObject jsonObjectAps = jo.getJSONObject("aps");
        JSONObject jsonObjectStas = jo.getJSONObject("stas");
        Iterator<?> keys = jsonObjectAps.keys();
        String chanel = null, bssid = null, enc = null, essid = null, cipher = "", clientTmp = null, wps = null, beacons = null;
        String last_time = null;
        int power = 0;
        double cRate = 0;
        while (keys.hasNext()) { //遍历所有扫描到的数据
            String key = (String)keys.next();
            if (key.equals("")) continue;
            JSONObject jsonObjectAp = jsonObjectAps.getJSONObject(key); //获取单条wifi数据的所有信息

            beacons = String.valueOf(jsonObjectAp.getInt("beacons")); //信号强度
            last_time = String.valueOf(jsonObjectAp.getLong("last_time")); // System.currentTimeMillis();


            if (tag == 1 && jsonObjectAp.get("wps") == null) continue;
            if (jsonObjectAp.get("essid") instanceof String) {
//                apInfo.setSsid(jsonObjectAp.getString("essid"));//设置wifi名称
                essid = jsonObjectAp.getString("essid");
            } else {
//                apInfo.setSsid("");
                essid = "";
            }
            essid = essid.replace("\u0000", ""); // 旧版隐藏SSID
            essid = essid.replace("\\x00", ""); // 新版隐藏SSID
            int tmp = 0;
            try {
                tmp = jsonObjectAp.getInt("power"); //获取功率
            } catch (JSONException e) {
                tmp = 0;
            }
//            apInfo.setPower(tmp);//设置功率
            power = tmp;
            try {
                JSONArray ja = jsonObjectAp.getJSONArray("enc"); //获取加密方式,可能是多种加密方式,所以为Array
                String privacy = "";
                if (ja.length() > 0) {
                    privacy = ja.getString(0);
                }
//            apInfo.setPrivacy(privacy);//设置加密方式
                enc = privacy;
            } catch (JSONException e) {
                e.printStackTrace();
            }
            //apInfo.setWps((jsonObjectAp.get("wps") == null) ? "false" : "true");
            //wps = (jsonObjectAp.get("wps") == null) ? "false" : "true";
            if (jsonObjectAp.get("wps") == null){
                wps = "false";
            } else if(jsonObjectAp.get("wps").equals("configured")) {
                wps = "true";
            } else{
                wps = "false";
            }
//            apInfo.setMac(key); //设置mac地址
            bssid = key;
            JSONArray wpa_cipher = jsonObjectAp.getJSONArray("wpa_cipher"); //获取wpa暗号
            JSONArray wpa2_cipher = jsonObjectAp.getJSONArray("wpa2_cipher"); //获取wpa2暗号
            for (int i = 0; i < wpa_cipher.length(); i++) { //遍历暗号
                if (cipher.equals("")) {
                    cipher = cipher + wpa_cipher.getString(i); //拼接暗号
                } else {
                    cipher = cipher + " " + wpa_cipher.getString(i); //拼接暗号
                }
            }

            for (int i = 0; i < wpa2_cipher.length(); i++) { //遍历暗号
                if (cipher.equals("")) {
                    cipher = cipher + wpa2_cipher.getString(i); //拼接暗号
                } else {
                    cipher = cipher + " " + wpa2_cipher.getString(i); //拼接暗号
                }
            }
//            apInfo.setCipher(cipher);//给wifi对象设置暗号

            //apInfo.setClient("[]");
            JSONArray clientBssids = jsonObjectAp.getJSONArray("sta_bssids"); //客户端MAC
            JSONArray clients = new JSONArray();
            for (int i = 0; i < clientBssids.length(); i++) {//遍历客户端wifi的mac地址
                String clientBssid = clientBssids.getString(i);
                String client_power = jsonObjectStas.getJSONObject(clientBssid).getString("power");

                JSONArray probesArr;
                String probes;
                if (jsonObjectStas.has(clientBssid) && jsonObjectStas.getJSONObject(clientBssid).has("probes")) {
                    probes = jsonObjectStas.getJSONObject(clientBssid).getJSONArray("probes").toString();
                } else {
                    probes = "";
                }
                if (probes.equals("[]")) {
                    probes = "无";
                }
                probes = probes.replace("[", "").replace("]", "").replace("\"", "");

                JSONObject client = new JSONObject();
                client.put("mac", clientBssid);
                client.put("probe", probes);
                client.put("power", client_power);
                client.put("rx_datas",jsonObjectStas.getJSONObject(clientBssid).getString("rx_datas"));
                client.put("tx_datas",jsonObjectStas.getJSONObject(clientBssid).getString("tx_datas"));
                clients.put(client);
            }
            clientTmp = clients.toString();

            chanel = String.valueOf(jsonObjectAp.getInt("channel"));

            int frequency = 1;
            if (FrequencyTransformTools.getInstance().get(chanel) != null){
                frequency = FrequencyTransformTools.getInstance().get(chanel);
            }

            JSONArray basic_rates = jsonObjectAp.getJSONArray("basic_rates"); //获取基础速率组
            JSONArray extra_rates = jsonObjectAp.getJSONArray("extra_rates"); //获取额外速率组
            double minRate = -1;
            for (int i = 0; i < basic_rates.length(); i++) {
                if (basic_rates.getDouble(i) < minRate || minRate < 0) {
                    minRate = basic_rates.getDouble(i);
                }
            }
            if (basic_rates.length() == 0) {
                for (int i = 0; i < extra_rates.length(); i++) {
                    if (extra_rates.getDouble(i) < minRate || minRate < 0) {
                        minRate = extra_rates.getDouble(i);
                    }
                }
            }
            cRate = minRate;
            WiFiWidth wiFiWidth = WiFiWidth.MHZ_40;//模拟wifi宽度

/**
* 拿到滤波器设置的信息，对获取的数据进行处理(SSID/WiFi信道/信号强度/安全)
* addWiFiSignal.getWiFiBand()/addWiFiSignal.getStrength()
* addWiFiDetail.SSID/addWiFiDetail.capabilities
**/
            Settings settings = MainContext.INSTANCE.getSettings();
            String deviceInfo = PrefSingleton.getInstance().getString("deviceInfo"); //获取存储的数据
            JSONObject jsonObject = new JSONObject(deviceInfo);
            String data = jsonObject.getString("data");
            JSONObject dataJson = new JSONObject(data);
            String device_name = dataJson.getString("device");
            //String device_name = "device";

            // last_time对比，叠加count，回传数据显示处理
            MacSsidDBUtils macSsidDBUtils = new MacSsidDBUtils(context);
            macSsidDBUtils.open();
            String Last_time = macSsidDBUtils.getLastTime(bssid);
            String count = macSsidDBUtils.getCount(bssid);
            count = count.equals("") ? String.valueOf(0) : count;
            if (!essid.equals(device_name) && !essid.equals("")){
                if (last_time.equals(Last_time)){
                    macSsidDBUtils.updataCount(bssid, String.valueOf(Integer.valueOf(count) + 1));
                    macSsidDBUtils.updataLast_time(bssid,last_time);
                } else {
                    macSsidDBUtils.updataCount(bssid, String.valueOf(0));
                    macSsidDBUtils.updataLast_time(bssid,last_time);
                    macSsidDBUtils.updataTime(bssid,String.valueOf(System.currentTimeMillis()));
                }
            }
            macSsidDBUtils.close();

            //essid(隐藏SSID(null))，bssid(00:00:00:00:00:00),信号(0),距离(0m);不添加至列表显示
            //if (!essid.equals("") && Integer.valueOf(count) < (10/scan_count + 1)) {
            if (!essid.equals("") && Integer.valueOf(count) < 6) {
                if (essid.indexOf(device_name) != -1) { //设备信息保存在界面
                    WiFiSignal addWiFiSignal = new WiFiSignal(frequency, frequency, wiFiWidth, power, chanel); //WIFI信号
                    WiFiDetail addWiFiDetail = new WiFiDetail(essid, bssid, enc, addWiFiSignal, clientTmp, cipher, wps, cRate, beacons, last_time, count); //单条WIFI的基本信息
                    PrefSingleton.getInstance().putString("device_mac", addWiFiDetail.getBSSID());
                    apData.add(addWiFiDetail);
                } else {
                    WiFiSignal addWiFiSignal = new WiFiSignal(frequency, frequency, wiFiWidth, power, chanel); //WIFI信号
                    String WifiBands = String.valueOf(addWiFiSignal.getWiFiBand()); //WIFI信道(2.4GHZ/5GHZ)
                    String EwifiBands = String.valueOf(settings.getWiFiBands());
                    String a = String.valueOf(EwifiBands).substring(1, String.valueOf(EwifiBands).length() - 1);
                    String Strengths = String.valueOf(addWiFiSignal.getStrength()); //信号强度(Zero/One/Two/Three/Four)
                    String Estrengths = String.valueOf(settings.getStrengths());
                    String d = String.valueOf(Estrengths).substring(1, String.valueOf(Estrengths).length() - 1);
                    if ((a.indexOf(WifiBands) != -1) && (d.indexOf(Strengths) != -1)) {
                        WiFiDetail addWiFiDetail = new WiFiDetail(essid, bssid, enc, addWiFiSignal, clientTmp, cipher, wps, cRate, beacons, last_time, count); //单条WIFI的基本信息
                        String SSIDs = addWiFiDetail.SSID; //SSID 名称
                        //String SSIDs = addWiFiDetail.BSSID; //BSSID
                        String Essid = String.valueOf(settings.getSSIDs());
                        String b = String.valueOf(Essid).substring(1, String.valueOf(Essid).length() - 1);
                        String SSIDs_low = SSIDs.toLowerCase();
                        String b_low = b.toLowerCase(); //忽略大小写
                        String Securities = addWiFiDetail.capabilities; //安全(OPEN/WPS/WEP/WPA/WPA2)
                        if (Securities.equals("") || Securities.equals("OPEN")) {
                            Securities = "NONE";
                        }
                        String Esecurities = String.valueOf(settings.getSecurities());
                        String c = String.valueOf(Esecurities).substring(1, String.valueOf(Esecurities).length() - 1);
                        if ((c.indexOf(Securities) != -1) && (SSIDs_low.indexOf(b_low) != -1)) {
                            apData.add(addWiFiDetail);
                        }
                    }
                }
            } else if (!essid.equals("") && Integer.valueOf(count) >= 6){
                String Bssid = bssid + count;
                if (essid.indexOf(device_name) != -1) { //设备信息保存在界面
                    WiFiSignal addWiFiSignal = new WiFiSignal(frequency, frequency, wiFiWidth, power, chanel); //WIFI信号
                    WiFiDetail addWiFiDetail = new WiFiDetail(essid, Bssid, enc, addWiFiSignal, clientTmp, cipher, wps, cRate, beacons, last_time, count); //单条WIFI的基本信息
                    PrefSingleton.getInstance().putString("device_mac", addWiFiDetail.getBSSID());
                    apData.add(addWiFiDetail);
                } else {
                    WiFiSignal addWiFiSignal = new WiFiSignal(frequency, frequency, wiFiWidth, power, chanel); //WIFI信号
                    String WifiBands = String.valueOf(addWiFiSignal.getWiFiBand()); //WIFI信道(2.4GHZ/5GHZ)
                    String EwifiBands = String.valueOf(settings.getWiFiBands());
                    String a = String.valueOf(EwifiBands).substring(1, String.valueOf(EwifiBands).length() - 1);
                    String Strengths = String.valueOf(addWiFiSignal.getStrength()); //信号强度(Zero/One/Two/Three/Four)
                    String Estrengths = String.valueOf(settings.getStrengths());
                    String d = String.valueOf(Estrengths).substring(1, String.valueOf(Estrengths).length() - 1);
                    if ((a.indexOf(WifiBands) != -1) && (d.indexOf(Strengths) != -1)) {
                        WiFiDetail addWiFiDetail = new WiFiDetail(essid, Bssid, enc, addWiFiSignal, clientTmp, cipher, wps, cRate, beacons, last_time, count); //单条WIFI的基本信息
                        String SSIDs = addWiFiDetail.SSID; //SSID 名称
                        //String SSIDs = addWiFiDetail.BSSID; //BSSID
                        String Essid = String.valueOf(settings.getSSIDs());
                        String b = String.valueOf(Essid).substring(1, String.valueOf(Essid).length() - 1);
                        String SSIDs_low = SSIDs.toLowerCase();
                        String b_low = b.toLowerCase(); //忽略大小写
                        String Securities = addWiFiDetail.capabilities; //安全(OPEN/WPS/WEP/WPA/WPA2)
                        if (Securities.equals("") || Securities.equals("OPEN")) {
                            Securities = "NONE";
                        }
                        String Esecurities = String.valueOf(settings.getSecurities());
                        String c = String.valueOf(Esecurities).substring(1, String.valueOf(Esecurities).length() - 1);
                        if ((c.indexOf(Securities) != -1) && (SSIDs_low.indexOf(b_low) != -1)) {
                            apData.add(addWiFiDetail);
                        }
                    }
                }
            }

            /*WiFiSignal addWiFiSignal = new WiFiSignal(frequency,frequency,wiFiWidth,power,chanel);//模拟wifi信号 13：power dbm,
            WiFiDetail addWiFiDetail = new WiFiDetail(essid,bssid,enc,addWiFiSignal,clientTmp,cipher,wps,cRate);//模拟单条wifi的基本信息 1111:essid  fds:bssid  fdss:enc
            apData.add(addWiFiDetail);//将单个wifi对象添加到wifi对象组list*/
        }
        /*switch (sort) {
            case 0:
                break;
            case 1:
                ChannelComparator channelComparator = new ChannelComparator();
                Collections.sort(apData, channelComparator);
                break;
            case 2:
                SignalComparator signaleComparator = new SignalComparator();
                Collections.sort(apData, signaleComparator);
                break;
            case 3:
                WpsComparator wpsComparator = new WpsComparator();
                Collections.sort(apData, wpsComparator);
                break;
            case 4:
                PrivacyComparator privacyComparator = new PrivacyComparator();
                Collections.sort(apData, privacyComparator);
                break;
            case 5:
                ClientComparator clientComparator = new ClientComparator();
                Collections.sort(apData, clientComparator);
                break;
            default:
                break;
        }*/
        return apData;
    }

    //wps设备返回结果
    public static List<WiFiDetail> responseWpsApData(JSONObject response, int tag, int sort) throws JSONException {
        List<WiFiDetail> apData = new ArrayList<WiFiDetail>();
        if (response.getInt("id") == -1) {//表明没有数据
            return null;
        }
        JSONObject jo = new JSONObject(BaseUtils.JSONTokener(response.getString("data")));//获取到data数据
        JSONObject jsonObjectAps = jo.getJSONObject("aps");
        JSONObject jsonObjectStas = jo.getJSONObject("stas");
        Iterator<?> keys = jsonObjectAps.keys();
        String chanel=null,bssid=null,enc=null,essid=null,cipher="",clientTmp=null,wps=null,beacons=null;
        int power=0;
        double cRate=0;
        String last_time = null;
        while (keys.hasNext()) {//遍历所有扫描到的数据
            String key = (String)keys.next();
            if (key.equals("")) continue;
            JSONObject jsonObjectAp = jsonObjectAps.getJSONObject(key);//获取单条wifi数据的所有信息
            beacons = String.valueOf(jsonObjectAp.getInt("beacons"));
            last_time = String.valueOf(jsonObjectAp.getLong("last_time"));

            if (tag == 1 && jsonObjectAp.get("wps") == null) continue;
            if (jsonObjectAp.get("essid") instanceof String) {
                essid = jsonObjectAp.getString("essid");
            }
            else {
                essid = "";
            }

            int tmp = 0;
            try {
                tmp = jsonObjectAp.getInt("power");//获取功率
            } catch (JSONException e) {
                tmp = 0;
            }
            power = tmp;

            JSONArray ja = jsonObjectAp.getJSONArray("enc");//获取加密方式 可能是多种加密方式，所以为Array
            String privacy = "";
            if (ja.length() > 0) {
                privacy = ja.getString(0);
            }
            enc = privacy;
            if (jsonObjectAp.get("wps") == null){
                wps = "false";
            }else if(jsonObjectAp.get("wps").equals("configured")) {
                wps = "true";
            }else{
                wps = "false";
            }
            bssid = key;
            JSONArray wpa_cipher = jsonObjectAp.getJSONArray("wpa_cipher");//获取wpa暗号
            JSONArray wpa2_cipher = jsonObjectAp.getJSONArray("wpa2_cipher");//获取wpa2暗号
            for (int i = 0; i < wpa_cipher.length(); i++) {//遍历暗号
                if (cipher.equals("")) {
                    cipher = cipher + wpa_cipher.getString(i);//拼接暗号
                }
                else {
                    cipher = cipher + " " + wpa_cipher.getString(i);//拼接暗号
                }
            }
            for (int i = 0; i < wpa2_cipher.length(); i++) {//遍历暗号
                if (cipher.equals("")) {
                    cipher = cipher + wpa2_cipher.getString(i);//拼接暗号
                }
                else {
                    cipher = cipher + " " + wpa2_cipher.getString(i);//拼接暗号
                }
            }
            JSONArray clientBssids = jsonObjectAp.getJSONArray("sta_bssids");
            JSONArray clients = new JSONArray();
            for (int i = 0; i < clientBssids.length(); i++) {//遍历客户端wifi的mac地址
                String clientBssid = clientBssids.getString(i);
                JSONArray probesArr;
                String probes;
                if (jsonObjectStas.has(clientBssid) && jsonObjectStas.getJSONObject(clientBssid).has("probes")) {
                    probes = jsonObjectStas.getJSONObject(clientBssid).getJSONArray("probes").toString();
                }
                else {
                    probes = "";
                }
                if (probes.equals("[]")) {
                    probes = "无";
                }
                probes = probes.replace("[", "").replace("]", "").replace("\"", "");
                JSONObject client = new JSONObject();
                client.put("mac", clientBssid);
                client.put("probe", probes);
                clients.put(client);
            }
            clientTmp = clients.toString();

            chanel = String.valueOf(jsonObjectAp.getInt("channel"));
            JSONArray basic_rates = jsonObjectAp.getJSONArray("basic_rates");//获取基础速率组
            JSONArray extra_rates = jsonObjectAp.getJSONArray("extra_rates");//获取额外速率组
            double minRate = -1;
            for (int i = 0; i < basic_rates.length(); i++) {
                if (basic_rates.getDouble(i) < minRate || minRate < 0) {
                    minRate = basic_rates.getDouble(i);
                }
            }
            if (basic_rates.length() == 0) {
                for (int i = 0; i < extra_rates.length(); i++) {
                    if (extra_rates.getDouble(i) < minRate || minRate < 0) {
                        minRate = extra_rates.getDouble(i);
                    }
                }
            }
            cRate = minRate;

            if (wps.equals("true")){
                WiFiWidth wiFiWidth =WiFiWidth.MHZ_40;//模拟wifi宽度
                WiFiSignal addWiFiSignal = new WiFiSignal(1,2,wiFiWidth,power,chanel);//模拟wifi信号 13：power dbm,
                WiFiDetail addWiFiDetail = new WiFiDetail(essid,bssid,enc,addWiFiSignal,clientTmp,cipher,wps,cRate,beacons,last_time,"");//模拟单条wifi的基本信息 1111:essid  fds:bssid  fdss:enc
                apData.add(addWiFiDetail);//将单个wifi对象添加到wifi对象组list
            }
        }
        return apData;
    }
}