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

package com.vrem.wifianalyzer;

import android.content.Context;
import android.content.res.Resources;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.MenuInflater;

import com.vrem.wifianalyzer.settings.Repository;
import com.vrem.wifianalyzer.settings.Settings;
import com.vrem.wifianalyzer.vendor.model.VendorService;
import com.vrem.wifianalyzer.vendor.model.VendorServiceFactory;
import com.vrem.wifianalyzer.wifi.filter.adapter.FilterAdapter;
import com.vrem.wifianalyzer.wifi.scanner.ScannerService;
import com.vrem.wifianalyzer.wifi.scanner.ScannerServiceFactory;

public enum MainContext {
    INSTANCE;

    private Settings settings;
    private MainActivity mainActivity;
    private ScannerService scannerService;
    private VendorService vendorService;
    private Configuration configuration;
    private FilterAdapter filterAdapter;

    public Settings getSettings() {
        return settings;
    }

    void setSettings(Settings settings) {
        this.settings = settings;
    }

    public VendorService getVendorService() {
        return vendorService;
    }

    void setVendorService(VendorService vendorService) {
        this.vendorService = vendorService;
    }

    //获取扫描服务
    public ScannerService getScannerService() {
        return scannerService;
    }

    //获取网络数据
    public ScannerService getNetData() {
        return scannerService;
    }

    void setScannerService(ScannerService scannerService) {
        this.scannerService = scannerService;
    }

    public MainActivity getMainActivity() {
        return mainActivity;
    }
    void setMainActivity(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    public Context getContext() {
        return mainActivity.getApplicationContext();
    }

    public Resources getResources() {
        return getContext().getResources();
    }

    public LayoutInflater getLayoutInflater() {
        return mainActivity.getLayoutInflater();
    }

    public MenuInflater getOptionMenu(){
        return mainActivity.getMenuInflater();
    }

    //获取配置
    public Configuration getConfiguration() {
        return configuration;
    }

    void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public FilterAdapter getFilterAdapter() {
        return filterAdapter;
    }

    void setFilterAdapter(FilterAdapter filterAdapter) {
        this.filterAdapter = filterAdapter;
    }


    public void initialize(@NonNull MainActivity mainActivity, boolean largeScreen) {
        Context applicationContext = mainActivity.getApplicationContext();//获取Context对象
        WifiManager wifiManager = (WifiManager) applicationContext.getSystemService(Context.WIFI_SERVICE);//获取wifi服务
        Handler handler = new Handler();
        Settings currentSettings = new Settings(new Repository(applicationContext));
        Configuration currentConfiguration = new Configuration(largeScreen);

        setMainActivity(mainActivity);
        setConfiguration(currentConfiguration);
        setSettings(currentSettings);
        setVendorService(VendorServiceFactory.makeVendorService(mainActivity.getResources()));
//        setScannerService(ScannerServiceFactory.makeScannerService(wifiManager, handler, currentSettings));
        setScannerService(ScannerServiceFactory.makeScannerService(wifiManager, handler, currentSettings,applicationContext));
        setFilterAdapter(new FilterAdapter(currentSettings));
    }

}
