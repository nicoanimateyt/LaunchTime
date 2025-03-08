package com.quaap.launchtime.widgets;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;

/**
 * Created by tom on 1/14/17.
 * <p>
 * Copyright (C) 2017  tom
 * <p>
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
class LaunchAppWidgetHost extends AppWidgetHost {

    //private List<Integer> mAppIds = new ArrayList<>();

    LaunchAppWidgetHost(Context context, int hostId) {
        super(context, hostId);
    }

    @Override
    protected AppWidgetHostView onCreateView(Context context, int appWidgetId, AppWidgetProviderInfo appWidget) {
        return new LaunchAppWidgetHostView(context);
    }

    @Override
    public void stopListening() {
        super.stopListening();
        clearViews();
    }

    @Override
    public int allocateAppWidgetId() {
        int appid = super.allocateAppWidgetId();
        //mAppIds.add(appid);
        return appid;
    }

    @Override
    public void deleteAppWidgetId(int appWidgetId) {
        //clearViews();
        //mAppIds.remove((Object)appWidgetId);
        super.deleteAppWidgetId(appWidgetId);
    }

//    @Override
//    protected void onProviderChanged(int appWidgetId, AppWidgetProviderInfo appWidget) {
//        super.onProviderChanged(appWidgetId, appWidget);
//    }
}
