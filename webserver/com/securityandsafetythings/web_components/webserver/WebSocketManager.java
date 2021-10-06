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

import android.support.annotation.NonNull;

import com.securityandsafetythings.webserver.WebSocketSession;

import java.util.ArrayList;
import java.util.List;

/**
 * Class that will handle websocket functionalities
 */
public class WebSocketManager implements WebSocketSession.WebSocketListener {
    private List<WebSocketSession> mSessions = new ArrayList<>();

    /**
     * Class constructor
     */
    public WebSocketManager() { }

    /**
     * Sends a message as byte[] to whoever is listening
     *
     * @param message byte[] with message content to be sent
     */
    public void sendByteMessage(final byte[] message) {
        broadcastMessage(message);
    }

    /**
     * Sends a message as String to whoever is listening
     *
     * @param message string with message content to be sent
     */
    public void sendStringMessage(final String message) {
        broadcastMessage(message);
    }

    /**
     * Receives a string message and broadcast it to whoever is registered in the session
     * @param message String containing message to be sent
     */
    private void broadcastMessage(final String message) {
        for (WebSocketSession s : mSessions) {
            s.send(message);
        }
    }

    /**
     * Receives a byte[] message and broadcast it to whoever is registered in the session
     * @param message byte[] containing the message to be sent
     */
    private void broadcastMessage(final byte[] message) {
        for (WebSocketSession s : mSessions) {
            s.send(message);
        }
    }

    /**
     * Client requested to connect to the websocket
     * @param session object for websocket connection
     */
    @Override
    public void onOpen(@NonNull final WebSocketSession session) {
        mSessions.add(session);
    }

    /**
     * Client requested to close the session with the websocket
     * @param session the client connected to the websocket
     * @param code from the client described in the RFC6455 or an internal error code
     * @param reason string from the client
     */
    @Override
    public void onClose(@NonNull final WebSocketSession session, final int code, final String reason) {
        mSessions.remove(session);
    }
}
