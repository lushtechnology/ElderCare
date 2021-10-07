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

package com.lushtech.eldercare.activity.rest;

import android.graphics.Bitmap;
import android.media.Image;

import com.lushtech.eldercare.activity.rest.dtos.SettingsDTO;
import com.lushtech.eldercare.activity.utilities.BitmapUtilities;
import com.lushtech.eldercare.activity.utilities.EasySharedPreference;
import com.securityandsafetythings.web_components.webserver.utilities.ProducesHeader;

import javax.ws.rs.Consumes;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;


/**
 * Class responsible to receive API calls from the front end, process it, and return the result.
 */
@Path("example")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class RestEndPoint {
    private byte[] mBitmapBytes;

    /**
     * Default empty constructor to obtain a class instance.
     */
    public RestEndPoint() {

    }

    /**
     * Simple endpoint used to test the connection on the front end.
     *
     * @return String declaring a connection was made
     */
    @GET
    @Path("test")
    @Produces(MediaType.APPLICATION_JSON)
    public String testConnection() {
        return "Test Successful";
    }

    /**
     * Sets the most recently received {@link Image} from the Video pipeline.
     *
     * @param bitmap the {@link Image} retrieved from the video pipeline
     */
    public synchronized void setImage(final Bitmap bitmap) {
        mBitmapBytes = BitmapUtilities.compressBitmap(bitmap);
    }

    /**
     * Returns the most recent video pipeline {@link Image} as a byte[].
     *
     * @return byte[] the converted byte[] of the {@link Bitmap}
     */
    @SuppressWarnings("MagicNumber")
    @GET
    @Path("live")
    @Produces("image/jpeg")
    @ProducesHeader("Cache-Control: max-age=5")
    public synchronized byte[] getImage() {
        if (mBitmapBytes == null) {
            throw new NotFoundException();
        }
        return mBitmapBytes;
    }

    /**
     * Returns the currently selected settings
     *
     * @return Currently active settings of the application
     */
    @GET
    @Path("settings")
    @Produces(MediaType.APPLICATION_JSON)
    public SettingsDTO getSettings() {
        return EasySharedPreference.getInstance().getApplicationSettings();
    }

    /**
     * Sets the currently active settings
     *
     * @param settings The new user selected settings from the front end
     */
    @POST
    @Path("settings")
    public void updateSettings(final SettingsDTO settings) {
        EasySharedPreference.getInstance().setApplicationSettings(settings);
    }

}
