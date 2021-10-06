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

package com.securityandsafetythings.web_components.webserver;

import android.content.Context;
import android.util.Log;

import com.securityandsafetythings.webserver.RequestHandlerAlreadyRegistered;
import com.securityandsafetythings.webserver.WebServerManager;
import com.securityandsafetythings.webserver.WebServerRequestHandler;

/**
 * Utility class to register a {@link WebServerRequestHandler} with the {@link WebServerManager}.
 * Create an instance of it and then use the connect() and disconnect() methods to
 * register/unregister your {@link WebServerRequestHandler}.
 * See MainService for an example.
 */
public class WebServerConnector implements WebServerManager.WebServerListener {
    private static final String LOGTAG = WebServerConnector.class.getSimpleName();
    private final Context mContext;
    private WebServerRequestHandler mHandler;
    private WebServerManager mManager;

    /**
     * Constructor
     * @param c {@link Context}
     */
    public WebServerConnector(final Context c) {
        mContext = c;
    }

    /**
     * Connects to the Web Server and register the provided handler for your application.
     *
     * @param aHandler request handler to use {@link WebServerRequestHandler}
     */
    public void connect(final WebServerRequestHandler aHandler) {
        Log.i(LOGTAG, "Connecting to web server");
        if (mHandler == null) {
            this.mHandler = aHandler;
            try {
                mManager = WebServerManager.getConnectedInstance(mContext, this);
            } catch (final RequestHandlerAlreadyRegistered e) {
                Log.e(LOGTAG, "Error starting web server connection", e);
            }
        } else {
            Log.e(LOGTAG, "A handler is already registered.");
        }
    }

    /**
     * Disconnects the Web Server of your application. This method is triggered from the
     * MainService onDestroy() method of the MainService.
     */
    public void disconnect() {
        Log.i(LOGTAG, "Disconnecting from web server");
        if (mManager != null) {
            mManager.unregisterWebServer();
            mManager.disconnect();
            mManager = null;
            mHandler = null;
        }
    }

    /**
     * Method is called after a connection to the webserver has been established. It first does some null checking
     * to make sure the {@link WebServerManager} and {@link WebServerRequestHandler} have not been garbage collected, or
     * nullified at some point during the connection. Lastly this method registers the {@link WebServerRequestHandler} with
     * the {@link WebServerManager}.
     */
    @Override
    public void onConnected() {
        Log.i(LOGTAG, "Connected to web server");
        if (mManager == null) {
            Log.e(LOGTAG, "Manager is null!");
        } else if (mHandler == null) {
            Log.e(LOGTAG, "Handler is null!");
        } else {
            try {
                mManager.registerWebServer(mHandler);
            } catch (final Exception e) {
                Log.e(LOGTAG, "Cannot register web server", e);
            }
        }
    }

    /**
     * This method is called internally from the Security and Safety Things OS by the
     * {@link com.securityandsafetythings.webserver.WebServerManager.WebServerListener}
     * interface when the application's default service has died. In the case of this app,
     * it is called by MainService onDestroy().
     */
    @Override
    public void onDisconnected() {
        Log.i(LOGTAG, "Disconnected from web server");
    }
}
