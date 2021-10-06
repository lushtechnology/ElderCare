/*
 * Copyright 2019-2020 by Security and Safety Things GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lushtech.eldercare.movinet.utilities;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.lushtech.eldercare.movinet.ElderCareApplication;
import com.lushtech.eldercare.movinet.rest.dtos.SettingsDTO;

/**
 * Base Shared Preference common definition
 */
public final class EasySharedPreference {

    // Singleton instance for this class
    private static EasySharedPreference sInstance = null;

    // String key for accessing confidence in EasySharedPreference
    private static final String PREF_KEY_MIN_CONFIDENCE = "pref_key_min_confidence";

    // Key value store for persisting applications preferences
    private final SharedPreferences mSharedPrefs;

    // Default confidence threshold
    @SuppressWarnings("magicNumber")
    private final float mDefaultConfidence = 0.5f;

    /**
     * Private constructor for Singleton. Uses the application context to
     * retrieve a SharedPreferences object for this class.
     */
    private EasySharedPreference() {
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(ElderCareApplication.getAppContext());
    }

    /**
     * Method for returning a Singleton instance of this class
     * @return The Singleton instance of this class
     */
    public static synchronized EasySharedPreference getInstance() {

        // If no instance exists create one. Else return existing instance
        if (sInstance == null) {
            sInstance = new EasySharedPreference();
        }

        return sInstance;
    }

    /**
     * Gets the current settings configuration of the application
     * @return A SettingsDTO object that represents current settings
     */
    public SettingsDTO getApplicationSettings() {
        final SettingsDTO settings = new SettingsDTO();
        settings.setConfidence(getMinConfidenceLevel());
        return settings;
    }

    /**
     * Sets a new settings configuration for the application
     * @param settings A SettingsDTO object that holds the new settings configuration
     */
    public void setApplicationSettings(final SettingsDTO settings) {
        setMinConfidenceLevel(settings.getConfidence());
    }

    /**
     * Get the minimum confidence level for the detector
     *
     * @return Minimum confidence level for detections to be considered viable
     */
    public float getMinConfidenceLevel() {
        return mSharedPrefs.getFloat(PREF_KEY_MIN_CONFIDENCE, mDefaultConfidence);
    }

    /**
     * Set the minimum confidence level for the detector
     *
     * @param confidence Minimum confidence level for detections to be considered viable
     */
    public void setMinConfidenceLevel(final float confidence) {
        mSharedPrefs.edit().putFloat(PREF_KEY_MIN_CONFIDENCE, confidence).apply();
    }
}
