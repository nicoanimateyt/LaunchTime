package com.quaap.launchtime.components;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import com.quaap.launchtime.GlobState;
import com.quaap.launchtime.R;
import com.quaap.launchtime.apps.AppLauncher;
import com.quaap.launchtime.apps.LaunchApp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/*
 *  Portions of this file are dereived from KISS and is licensed under the GPL v3.
 *  https://github.com/Neamar/KISS
 *
 *  Modified by Tom Kliethermes. 2017
 */

/*
 * Inspired from http://stackoverflow.com/questions/31490630/how-to-load-icon-from-icon-pack
 */

/**
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

public class IconsHandler {

    private static final String TAG = "IconsHandler";

    private IconPack iconPack;

    private volatile String iconsPackPackageName;

    // map with available icons packs
    private Map<String, String> iconsPacks = new HashMap<>();

    private final PackageManager pm;
    private final Context ctx;

    public static final String DEFAULT_PACK = "default";

    private final Theme theme;

    public IconsHandler(Context ctx) {
        super();
        this.ctx = ctx;
        this.pm = ctx.getPackageManager();
        theme = new Theme(ctx, this);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx.getApplicationContext());
        iconsPackPackageName = prefs.getString(ctx.getString(R.string.pref_key_icons_pack), DEFAULT_PACK);
        loadAvailableIconsPacks();
        loadIconsPack();
    }


    /**
     * Load configured icons pack
     */
    private void loadIconsPack() {

        loadIconsPack(iconsPackPackageName);

    }

    /**
     * Parse icons pack metadata
     *
     * @param packageName Android package ID of the package to parse
     */
    public void loadIconsPack(String packageName) {

        theme.saveUserColors();

        iconsPackPackageName = packageName;

        cacheClear();

        boolean hasusercolors = theme.restoreUserColors();

        // inbuilt theme icons, nothing to do
        if (theme.isBuiltinTheme(iconsPackPackageName)) {
            if (!hasusercolors) theme.getBuiltinTheme(iconsPackPackageName).applyTheme();
            iconPack = null;
            return;
        }
        iconPack = new IconPack(ctx,iconsPackPackageName);
    }

//    public boolean isIconTintable() {
//        return theme.isBuiltinThemeIconTintable(iconsPackPackageName);
//    }


    public IconPack getIconPack() {
        return iconPack;
    }

    public boolean isIconTintable(String packageName) {
        return theme.isBuiltinThemeIconTintable(packageName);
    }

    public boolean isIconTintable() {
        return theme.isBuiltinThemeIconTintable(iconsPackPackageName);
    }


    public Drawable getCustomIcon(ComponentName componentName) {
        Drawable app_icon = null;
        Bitmap custombitmap = SpecialIconStore.loadBitmap(ctx, componentName, SpecialIconStore.IconType.Custom);
        if (custombitmap != null) {
            app_icon = new BitmapDrawable(ctx.getResources(), custombitmap);
        }
        return  app_icon;
    }

    public Drawable getDefaultAppDrawable(AppLauncher app) {
        return getDefaultAppDrawable(app, false);
    }

    private Drawable getDefaultAppDrawable(AppLauncher app, boolean nodefault) {

        Drawable app_icon = null;

        try {
            app_icon = getCustomIcon(app.getComponentName());
            if (app_icon!=null) return app_icon;

            ComponentName baseComponentName = app.getBaseComponentName();

            try {
                if (!app.isOreoShortcut()) {
                    Intent intent = LaunchApp.getAppIntent(app);
                    app_icon = pm.getActivityIcon(intent);
                }
            } catch (Exception | OutOfMemoryError e) {
                Log.e("IconLookup", "Couldn't get icon for " + baseComponentName.getClassName(), e);
            }

            if (app_icon == null) {
                try {
                    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        LauncherApps launcher = (LauncherApps) ctx.getSystemService(Context.LAUNCHER_APPS_SERVICE);
                        if (launcher!=null) {
                            LauncherActivityInfo info = launcher.getActivityList(baseComponentName.getPackageName(), android.os.Process.myUserHandle()).get(0);
                            app_icon = info.getBadgedIcon(0);
                        }
                    } else {
                        app_icon = pm.getActivityIcon(baseComponentName);
                    }
                } catch (NameNotFoundException | IndexOutOfBoundsException | OutOfMemoryError e) {
                    Log.e(TAG, "Unable to find component " + baseComponentName.toString() + e);
                    return null;
                }
            }

            if (app_icon == null && !nodefault) {
                app_icon = pm.getDefaultActivityIcon();
            }

            Bitmap bitmap = SpecialIconStore.loadBitmap(ctx, app.getComponentName(), SpecialIconStore.IconType.Shortcut);

            if (bitmap != null && app_icon!=null) {
                //Log.d(TAG, "Got special icon for " + app.getComponentName().getClassName());
                try {
                    Bitmap newbm = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
                    Canvas canvas = new Canvas(newbm);
                    canvas.drawBitmap(bitmap, 0, 0, null);
                    app_icon.setBounds(canvas.getWidth() / 2, 0, canvas.getWidth(), canvas.getHeight() / 2);
                    app_icon.draw(canvas);
                    app_icon = new BitmapDrawable(ctx.getResources(), newbm);
                    //Log.d("loadAppIconAsync", " yo");
                } catch (Exception | OutOfMemoryError e) {
                    Log.e("loadAppIconAsync", "couldn't make special icon", e);
                }
            }
        } catch (Exception | OutOfMemoryError e) {
            Log.e(TAG, "Exception getting app icon for " + app.getComponentName() , e);
        }
        if (app_icon == null && !nodefault)  {
            app_icon = pm.getDefaultActivityIcon();
        }

        return app_icon;
    }


    /**
     * Get or generate icon for an app
     */
    public Drawable getDrawableIconForPackage(AppLauncher app) {

        for (String key: theme.getBuiltinIconThemes().keySet()) {

            if (iconsPackPackageName.equalsIgnoreCase(key)) {
                return theme.getBuiltinTheme(key).getDrawable(app);
            }
        }

        ComponentName componentName = app.getComponentName();

        Drawable icon = getCustomIcon(componentName);

        if (icon!=null) return  icon;

        icon = iconPack.get(componentName);
        if (icon!=null) return  icon;

        //search first in cache
        Drawable systemIcon = cacheGetDrawable(componentName.toString());
        if (systemIcon != null)
            return systemIcon;

        systemIcon = this.getDefaultAppDrawable(app);
        if (systemIcon instanceof BitmapDrawable) {
            Drawable generated = iconPack.generateBitmap(systemIcon);
            cacheStoreDrawable(componentName.toString(), generated);
            return generated;
        }
        return systemIcon;
    }


    public String getIconsPackPackageName() {
        return iconsPackPackageName;
    }


    public Theme getTheme() {
        return theme;
    }


    /**
     * Scan for installed icons packs
     */
    public void loadAvailableIconsPacks() {

        iconsPacks = IconPack.listAvailableIconsPacks(ctx);

    }



    private Map<String, String> getIconsPacks() {
        return iconsPacks;
    }

    public Map<String, String> getAllIconsThemes() {
        Map<String, String> iconsThemes = new LinkedHashMap<>();

        iconsThemes.put(DEFAULT_PACK, theme.getBuiltinTheme(DEFAULT_PACK).getPackName());

        iconsThemes.putAll(getIconsPacks());

        for (Theme.BuiltinTheme ic: theme.getBuiltinIconThemes().values()) {
            if ( !ic.getPackKey().equals(DEFAULT_PACK)) {
                iconsThemes.put(ic.getPackKey(), ic.getPackName() + " (sys)");
            }
        }

        return iconsThemes;
    }

    private boolean isDrawableInCache(String key) {
        File drawableFile = cacheGetFileName(key);
        return drawableFile.isFile();
    }

    private void cacheStoreDrawable(String key, Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            File drawableFile = cacheGetFileName(key);
            FileOutputStream fos;
            try {
                fos = new FileOutputStream(drawableFile);
                ((BitmapDrawable) drawable).getBitmap().compress(CompressFormat.PNG, 100, fos);
                fos.flush();
                fos.close();
            } catch (Exception e) {
                Log.e(TAG, "Unable to store drawable in cache " + e);
            }
        }
    }

    private Drawable cacheGetDrawable(String key) {

        if (!isDrawableInCache(key)) {
            return null;
        }

        FileInputStream fis=null;
        try {
            fis = new FileInputStream(cacheGetFileName(key));

            return new BitmapDrawable(this.ctx.getResources(), BitmapFactory.decodeStream(fis));
        } catch (Exception e) {
            Log.e(TAG, "Unable to get drawable from cache " + e);
        } finally {
            if (fis!=null) try {
                fis.close();
            } catch (IOException e) {
                Log.e(TAG, "Unable close fh " + e);
            }
        }

        return null;
    }

    /**
     * create path for icons cache like this
     * {cacheDir}/icons/{icons_pack_package_name}_{key_hash}.png
     */
    private File cacheGetFileName(String key) {
        return new File(getIconsCacheDir() + iconsPackPackageName + "_" + key.hashCode() + ".png");
    }

    private File getIconsCacheDir() {
        return new File(this.ctx.getCacheDir().getPath() + "/icons/");
    }

    /**
     * Clear cache
     */
    private void cacheClear() {
        File cacheDir = this.getIconsCacheDir();

        if (!cacheDir.isDirectory())
            return;

        for (File item : cacheDir.listFiles()) {
            if (!item.delete()) {
                Log.w(TAG, "Failed to delete file: " + item.getAbsolutePath());
            }
        }
    }

    public static Bitmap drawableToBitmap (Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable)drawable).getBitmap();
        }

        int width = drawable.getIntrinsicWidth();
        width = width > 0 ? width : 1;
        int height = drawable.getIntrinsicHeight();
        height = height > 0 ? height : 1;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }




    private static Drawable currentLinkSymbol;
    private static int currentIconTint;


    public static Drawable drawLinkSymbol(Drawable app_icon, Context context) {
        try {
            Bitmap newbm = Bitmap.createBitmap(app_icon.getIntrinsicWidth(), app_icon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(newbm);
            app_icon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            app_icon.draw(canvas);

            int tint = GlobState.getStyle(context).getIconTint();
            if (tint!=currentIconTint || currentLinkSymbol==null) {
                Drawable linkSymbol;
                if (Build.VERSION.SDK_INT >= 21) {
                    linkSymbol = context.getResources().getDrawable(R.drawable.link, context.getTheme());
                } else {
                    linkSymbol = context.getResources().getDrawable(R.drawable.link);
                }

                if (linkSymbol.getConstantState() == null) {
                    currentLinkSymbol = linkSymbol.mutate();
                } else {
                    currentLinkSymbol = linkSymbol.getConstantState().newDrawable().mutate();
                }

                applyIconTint(currentLinkSymbol, tint);

                currentIconTint = tint;

            }
            currentLinkSymbol.setBounds(canvas.getWidth() * 3 / 4, canvas.getHeight() * 3 / 4, canvas.getWidth(), canvas.getHeight());
            currentLinkSymbol.draw(canvas);

            app_icon = new BitmapDrawable(context.getResources(), newbm);
            //Log.d("loadAppIconAsync", " yo");
        } catch (Exception | OutOfMemoryError e) {
            Log.e("loadAppIconAsync", "couldn't make link icon", e);
        }
        return app_icon;
    }


    public static void applyIconTint(final Drawable app_icon, int mask_color) {
        if (Color.alpha(mask_color) > 10) {

            app_icon.mutate();

            //int avg = (Color.red(mask_color) + Color.green(mask_color) + Color.blue(mask_color) ) / 3;

            if (Color.red(mask_color)>5 && Color.red(mask_color) == Color.green(mask_color) && Color.red(mask_color) == Color.blue(mask_color) ) {
                setSaturation(app_icon, (255f-Color.red(mask_color))/255f, Color.alpha(mask_color)/255f);
            } else {
                PorterDuff.Mode mode = PorterDuff.Mode.MULTIPLY;
                app_icon.setColorFilter(mask_color, mode);
            }
        }
    }

    private static ColorMatrixColorFilter colorMatrixColorFilter;
    private static float filterSaturation;
    private static float filterAlpha;

    public static void setSaturation(final Drawable drawable, float saturation, float alpha) {

        if (colorMatrixColorFilter==null || saturation!=filterSaturation || filterAlpha!=alpha) {
            filterSaturation = saturation;
            filterAlpha = alpha;

            ColorMatrix matrixA = new ColorMatrix();
            matrixA.setSaturation(filterSaturation);

            float[] matrixBItems =
                    new float[] {
                            1, 0, 0, 0, 0,
                            0, 1, 0, 0, 0,
                            0, 0, 1, 0, 0,
                            0, 0, 0, alpha,0};

            ColorMatrix matrixB = new ColorMatrix(matrixBItems);

            matrixA.setConcat(matrixB, matrixA);

            colorMatrixColorFilter = new ColorMatrixColorFilter(matrixA);

        }

//        ColorMatrix matrix = new ColorMatrix();
//        matrix.setSaturation(saturation);
//
//        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);

        drawable.setColorFilter(colorMatrixColorFilter);

    }

}