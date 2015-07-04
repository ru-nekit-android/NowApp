/*
 * Copyright (C) 2014 Daniele Maddaluno
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.madx.updatechecker.lib;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Handler;
import android.preference.PreferenceManager;

import com.madx.updatechecker.lib.utils.versioning.DefaultArtifactVersion;

import org.jsoup.Jsoup;


/**
 * It is used to verify if a new update exists.
 * In case {@link #force} is false a dialog is shown only if the update really exists.
 * In case {@link #force} is true a dialog is shown anyway.
 *
 * @author Daniele
 */
public class UpdateChecker implements Runnable {

    /**
     * The Context from which the updater is called
     */
    private final Context context;

    /**
     * The package name of your app
     */
    private final String package_name;
    /**
     * The current version of your app
     */
    private final String current_version;

    private final UpdateCheckerListener listener;
    /**
     * Each time you enter in an Activity which for example called: <b>new UpdateChecker(this, new Handler()).start();</b>
     * this is the minimum time which has to pass between an automatic verification of an update and the next automatic verification.
     */
    private final long TIME_RETRY_TO_UPDATE;

    private static final long SEC = 1000;
    private static final long MIN = 60 * SEC;
    private static final long HOUR = 60 * MIN;
    private static final long DAY = 24 * HOUR;

    /**
     * <ul>
     * <li>true - use it when the user directly expressed the wish to verify if an update exists</li>
     * <li>false - use it for automatic verification of new updates</li>
     * </ul>
     */
    private boolean force = false;
    /**
     * Represents if a new update exists or not on the Google Play Store
     */
    private boolean update_available = false;
    /**
     * Is the theme used dark or not. It is used to specify different cloud drawable in the dialog.
     */
    private boolean light_theme = false;

    /**
     * Updater Runnable constructor
     *
     * @param context              the activity from which the updater is called
     * @param listener             the handler to manage the UI in the specified {@link #context}
     * @param time_retry_to_update time in millis which represents the time after which the runnable called with force = false have to retry to check if an update exists
     */
    public UpdateChecker(Context context, UpdateCheckerListener listener, long time_retry_to_update) {
        this.context = context;
        this.package_name = context.getPackageName();
        this.listener = listener;
        String current_version = "";
        try {
            current_version = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (NameNotFoundException ignored) {
        } finally {
            this.current_version = current_version;
        }
        this.TIME_RETRY_TO_UPDATE = time_retry_to_update;
    }

    /**
     * Updater Runnable constructor, when called in automatic way it runs once a day
     *
     * @param context  the activity from which the updater is called
     * @param listener the handler to manage the UI in the specified {@link #context}
     */
    public UpdateChecker(Context context, UpdateCheckerListener listener) {
        this(context, listener, DAY);
    }

    /**
     * If true it means that the user has explicitly asked to verify if a new update exists so the runnable will show a dialog even if
     * the app is already updated (otherwise the user couldn't have a feedback from his explicit request).
     * If false it means that verification is started automatically (the user don't asked explicitly to verify), so a dialog is shown to the user
     * only if an update really exists.
     *
     * @param force if true it forces a dialog visualization (even if already updated)
     * @return an updater
     */
    public UpdateChecker force(boolean force) {
        this.force = force;
        return this;
    }

    /**
     * @param light_theme if true uses drawables compatible with Light Themes
     * @return an updater
     */
    public UpdateChecker lightTheme(boolean light_theme) {
        this.light_theme = light_theme;
        return this;
    }

    public void start() {
        new Thread(this).start();
    }

    /**
     * Runs the asynchronous web  call and shows on the UI the Dialogs if required
     */
    @Override
    public void run() {
        update_available = update_available();
        if (update_available) {
            if(listener != null){
                listener.onNewVersion(current_version);
            }
        }
    }

    /**
     * Check if you are updated
     *
     * @return true if an update is needed false otherwise
     */
    private boolean update_available() {
        // I take the time in millis when you've checked the update for the last time
        long lastUpdateTime = getLastTimeTriedUpdate(context);
        // If force = true skip this check, otherwise check if it has already checked within a {link #TIME_RETRY_TO_UPDATE}
        if (!force && (lastUpdateTime + TIME_RETRY_TO_UPDATE) > System.currentTimeMillis()) {
            //return false;
        }
        // Sets new instant of time in which it has checked the update
        setLastTimeTriedUpdate(context);
        // Check if there is really an update on the Google Play Store
        return web_update();
    }

    /**
     * Check if the Google Play version of the app match or less the current version installed
     *
     * @return true if an update is required, false otherwise
     */
    private boolean web_update() {
        try {
            String new_version = Jsoup.connect("https://play.google.com/store/apps/details?id=" + package_name + "&hl=it")
                    .timeout(30000)
                    .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                    .referrer("http://www.google.com")
                    .get()
                    .select("div[itemprop=softwareVersion]")
                    .first()
                    .ownText();
            return newer_version_available(current_version, new_version);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Used for comparing different versions of software
     *
     * @param local_version_string  the version name of the app installed on the system
     * @param online_version_string the version name of the app released on the Google Play
     * @return true if a the online_version_string is greater than the local_version_string
     */
    private static boolean newer_version_available(String local_version_string, String online_version_string) {
        DefaultArtifactVersion local_version_mvn = new DefaultArtifactVersion(local_version_string);
        DefaultArtifactVersion online_version_mvn = new DefaultArtifactVersion(online_version_string);
        return local_version_mvn.compareTo(online_version_mvn) == -1 && !local_version_string.equals("");
    }

    /**
     * @param context
     * @return the value of preference which represents the last time you verify if an update exists
     */
    private static long getLastTimeTriedUpdate(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getLong(getLastUpdateTestKey(context), 0);
    }

    /**
     * Sets the value of preference which represents the last time you verify if an update exists = the currentTimeMillis in which that function is called
     *
     * @param context
     */
    private static void setLastTimeTriedUpdate(Context context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putLong(getLastUpdateTestKey(context), System.currentTimeMillis()).commit();
    }

    /**
     * @param context
     * @return the key String of the Last Update Preference
     */
    private static String getLastUpdateTestKey(Context context) {
        return "last_update_test_preferences_" + context.getPackageName();
    }

}

