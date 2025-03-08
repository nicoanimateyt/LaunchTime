package com.quaap.launchtime.apps;

import android.content.ComponentName;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.os.AsyncTask;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import com.quaap.launchtime.GlobState;
import com.quaap.launchtime.MainActivity;
import com.quaap.launchtime.R;
import com.quaap.launchtime.db.DB;
import com.quaap.launchtime.ui.StaticListView;

import java.lang.ref.WeakReference;

/**
 * Copyright (C) 2017   Tom Kliethermes
 *
 * This file is part of LaunchTime and is is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */
public class AppCursorAdapter extends ResourceCursorAdapter implements StaticListView.OnItemClickListener {
    private final MainActivity mMain;

    private final EditText mTextHolder;


    //private DB mDB;

    public AppCursorAdapter(final MainActivity main, EditText textHolder, int layout, int flags) {
        super(main, layout, null, flags);
        mMain = main;

        mTextHolder = textHolder;

//        mTextHolder.setOnEditorActionListener(new TextView.OnEditorActionListener() {
//            @Override
//            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
//                if (actionId==EditorInfo.IME_ACTION_SEARCH) {
//                    mTextHolder.postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            refreshCursor();
//                        }
//                    },10);
//                }
//                return false;
//            }
//        });

        mTextHolder.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                mTextHolder.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        refreshCursor();
                    }
                },10);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });


//        mTextHolder.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                refreshCursor();
//            }
//        },10);

    }

    private DB db() {
        return GlobState.getGlobState(mMain).getDB();
    }

    public void refreshCursor() {
        String text = mTextHolder.getText().toString().trim();
        if (text.length()==0) {
            text = "XXXXXXXXXXXX";
        } else {

            text = text.replace(".", "_");
            text = "%" + text + "%";
        }

       // Log.d("refreshCursor", text);

        getFilter().filter(text);

    }

    @Override
    public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
       // Log.d("BackThread", constraint.toString());
        return db().getAppCursor(constraint.toString());
    }

    static class ViewHolder {
        ViewGroup appholder;
        TextView labelView;
        TextView catagoryView;
    }

    // The bindView method is used to bind all data to a given view
    // such as setting the text on a TextView.
    @Override
    public void bindView(View view, final Context context, Cursor cursor) {

        final String activityName;
        final String pkgName;
        String origlabel;
        String customlabel;
        final String category;
        final String label;
        try {
            // "select " + ACTVNAME + " _id, " + PKGNAME + " pkg,  app." + LABEL + " label, tab." + LABEL + " category " + ", app." + CUSTOMLABEL + " customlabel" +
            activityName = cursor.getString(cursor.getColumnIndex("_id"));
            pkgName = cursor.getString(cursor.getColumnIndex("pkg"));
            origlabel = cursor.getString(cursor.getColumnIndex("label"));
            category = cursor.getString(cursor.getColumnIndex("category"));
            customlabel = cursor.getString(cursor.getColumnIndex("customlabel"));
            if (customlabel==null) {
                label = origlabel;
            } else {
                label = customlabel + " (" + origlabel + ")";
            }

        } catch (CursorIndexOutOfBoundsException e) {
            Log.e("LaunchTime", "Bad cursor");
            return;
        } catch (Exception e) {
            Log.e("LaunchTime", "Bad cursor", e);
            return;
        }

        ViewHolder holder = (ViewHolder)view.getTag();
        if (holder==null) {
            holder = new ViewHolder();

            holder.appholder = view.findViewById(R.id.icontarget);
            holder.labelView = view.findViewById(R.id.label);
            holder.catagoryView = view.findViewById(R.id.catagory);
            view.setTag(holder);
        }


        holder.appholder.removeAllViews();
        holder.labelView.setText(label);
        holder.catagoryView.setText(category);

        new ListLoaderTask(this, holder, context).execute(pkgName, activityName);

    }

    public void close() {
        try {
            changeCursor(null);
        } catch(Exception e)  {
            Log.d("Appcursor", "Exception on 'close()'", e);
        }
    }

    @Override
    public void onItemClick(Object item, View itemView, int position, long id) {
        Cursor cursor = (Cursor) item;
        String activityName = cursor.getString(cursor.getColumnIndex("_id"));
        String pkgName = cursor.getString(cursor.getColumnIndex("pkg"));
        //String label = cursor.getString(1);

       // mTextHolder.setText(label);

        mMain.getAppLauncher().launchApp(activityName, pkgName);

    }


    private static class ListLoaderTask extends  AsyncTask<String,Void,AppLauncher> {
        final ViewHolder viewholder;
        final WeakReference<AppCursorAdapter> appadaptref;
        final WeakReference<Context> contextref;

        ListLoaderTask(AppCursorAdapter appadapt, ViewHolder viewholder, Context context) {
            appadaptref = new WeakReference<>(appadapt);
            this.contextref = new WeakReference<>(context);
            this.viewholder = viewholder;
        }

        @Override
        protected AppLauncher doInBackground(String... names) {

            return appadaptref.get().db().getApp(new ComponentName(names[0],names[1]));
        }

        @Override
        protected void onPostExecute(AppLauncher app) {
            if (app != null) {
                //Context context = contextref.get();
                AppCursorAdapter appadapt = appadaptref.get();
                Context context = contextref.get();
                if (appadapt==null || context==null) return;
                app.loadAppIconAsync(context);
                View v = appadapt.mMain.getLauncherView(app, false, false);

                if (v!=null) {
                    viewholder.appholder.addView(v);
                } else {
                    viewholder.appholder.addView(new TextView(context));
                }
            }

        }
    }

}
