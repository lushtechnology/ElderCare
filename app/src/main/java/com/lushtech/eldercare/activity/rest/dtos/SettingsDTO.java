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

package com.lushtech.eldercare.activity.rest.dtos;

import com.google.gson.annotations.SerializedName;

/**
 * Data transfer object for application settings
 */
public class SettingsDTO {

    // Confidence threshold for detections
    @SerializedName("confidence")
    private float mConfidence;

    /**
     * Getter for confidence threshold
     * @return float representing the confidence threshold
     */
    public float getConfidence() {
        return mConfidence;
    }

    /**
     * Setter for confidence threshold
     * @param confidence float containing new value for confidence threshold
     */
    public void setConfidence(final float confidence) {
        this.mConfidence = confidence;
    }
}