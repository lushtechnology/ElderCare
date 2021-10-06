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
import android.content.res.AssetFileDescriptor;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Pair;
import android.webkit.MimeTypeMap;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.securityandsafetythings.web_components.webserver.utilities.InstantSerializer;
import com.securityandsafetythings.web_components.webserver.utilities.RestMethodWrapper;
import com.securityandsafetythings.web_components.webserver.utilities.RestPath;
import com.securityandsafetythings.web_components.webserver.utilities.SharedMemoryFactory;
import com.securityandsafetythings.webserver.WebServerMethod;
import com.securityandsafetythings.webserver.WebServerRequest;
import com.securityandsafetythings.webserver.WebServerRequestHandler;
import com.securityandsafetythings.webserver.WebServerResponse;
import com.securityandsafetythings.webserver.WebSocketUpgradeException;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.securityandsafetythings.webserver.WebServerResponse.ResponseStatus.REDIRECT_SEE_OTHER;

/**
 * Implements a web request handler that you can feed with JAX-RS style annotated methods.
 * You can create an instance of it, use the {@link #register(Object)} method to add JAX-RS style handlers,
 * (for example RestEndPoint) and then register this instance as a web server using
 * the Web Server Manager (or WebServerConnector in this example).
 * See MainService for an example.
 */
public class RestHandler implements WebServerRequestHandler {
    private static final String LOGTAG = RestHandler.class.getSimpleName();
    private static final Gson GSON = new GsonBuilder().registerTypeAdapter(Instant.class, new InstantSerializer())
        .create();
    private static final String REST_PATH = "/rest";
    private static final String SLASH = "/";
    private static final String TEXT_PLAIN = "text/plain";
    private static final String APP = "app";
    @SuppressWarnings("MagicNumber")
    private static final int HTTP_REDIRECT_CODE_MIN = 300;
    @SuppressWarnings("MagicNumber")
    private static final int HTTP_REDIRECT_CODE_MAX = 308;
    private static final String EMPTY_PATH = "";
    private static final String JAVASCRIPT_EXT = "js";
    private static final List<String> FONTS_EXT = Arrays.asList("woff", "woff2", "ttf", "otf");

    private final RestPath mGetRoutes = new RestPath("GET");
    private final RestPath mPutRoutes = new RestPath("PUT");
    private final RestPath mPostRoutes = new RestPath("POST");
    private final RestPath mDeleteRoutes = new RestPath("DELETE");
    private final Map<String, Pair<String, WebServerResponse.ResponseStatus>> mRedirections = new HashMap<>();
    private final Set<Object> mRestServices = new HashSet<>();
    private final SharedMemoryFactory mSharedMemoryFactory = new SharedMemoryFactory();
    private final Context mContext;
    private final String mBasePath;
    private final String mWebsiteAssetPath;
    private WebSocketManager mWebSocketManager;

    /**
     * Constructor accepts {@link Context} parameter and uses it to create the basePath
     *
     * @param c context
     * @param websiteAssetPath path to store website assets = website
     */
    public RestHandler(final Context c, final String websiteAssetPath) {
        this(c, websiteAssetPath, null);
    }

    /**
     * Constructor accepts {@link Context} parameter and uses it to create the basePath
     *
     * @param c context
     * @param websiteAssetPath path to store website assets = website
     * @param webSocketManager the {@link WebSocketManager} object in case we need WebSocket in the app
     */
    public RestHandler(final Context c, final String websiteAssetPath, final WebSocketManager webSocketManager) {
        mContext = c;
        mBasePath = File.separator + APP + File.separator + c.getPackageName();
        mWebsiteAssetPath = websiteAssetPath;
        mWebSocketManager = webSocketManager;
    }

    /**
     * Converts from json to T
     *
     * @param json input Json format string.
     * @param type the target type to convert to.
     * @param <T>  the expected type
     * @return the converted object
     */
    public static <T> T fromJson(final String json, final Type type) {
        return GSON.fromJson(json, type);
    }

    /**
     * Convert from an object to Json format string.
     *
     * @param object the object to convert.
     * @return Json format string.
     */
    public static String toJson(final Object object) {
        return GSON.toJson(object);
    }

    /**
     * Register a new object to handle requests. See RestEndPoint for an example of
     * a suitable object.
     *
     * @param restService The instance to register. Must be filled with methods annotated
     *                    with JAX-RS annotations.
     * @param <T>         The type of the service
     */
    @SuppressWarnings("unchecked")
    public <T> void register(@NonNull final T restService) {
        register(restService, (Class<T>)restService.getClass());
    }

    private <T> void register(final T restService, final Class<? super T> clazz) {
        if (mRestServices.contains(restService)) {
            return;
        }
        mRestServices.add(restService);
        final Path classPath = clazz.getAnnotation(Path.class);
        for (final Method method : clazz.getMethods()) {
            final Path methodPath = method.getAnnotation(Path.class);
            final String route = REST_PATH + pathToRoute(classPath) + pathToRoute(methodPath);
            if (method.isAnnotationPresent(GET.class)) {
                Log.v(LOGTAG, String.format("Registering %s#%s for route GET %s", clazz.getSimpleName(), method.getName(), route));
                mGetRoutes.addMethod(route, new RestMethodWrapper(
                    restService, method, route, mContext.getCacheDir(), mSharedMemoryFactory));
            } else if (method.isAnnotationPresent(PUT.class)) {
                Log.v(LOGTAG, String.format("Registering %s#%s for route PUT %s", clazz.getSimpleName(), method.getName(), route));
                mPutRoutes.addMethod(route, new RestMethodWrapper(
                    restService, method, route, mContext.getCacheDir(), mSharedMemoryFactory));
            } else if (method.isAnnotationPresent(POST.class)) {
                Log.v(LOGTAG, String.format("Registering %s#%s for route POST %s", clazz.getSimpleName(), method.getName(), route));
                mPostRoutes.addMethod(route, new RestMethodWrapper(
                    restService, method, route, mContext.getCacheDir(), mSharedMemoryFactory));
            } else if (method.isAnnotationPresent(DELETE.class)) {
                Log.v(LOGTAG, String.format("Registering %s#%s for route DELETE %s", clazz.getSimpleName(), method.getName(), route));
                mDeleteRoutes.addMethod(route, new RestMethodWrapper(restService, method, route, mContext.getCacheDir(),
                    mSharedMemoryFactory));
            }
        }
    }

    /**
     * Register a redirection (303 SEE OTHER) from a specific path to another one.
     * See {@link #registerRedirect(String, String, WebServerResponse.ResponseStatus)}
     *
     * @param from the path to redirect.
     * @param to   the path that should be redirected to.
     */
    public void registerRedirect(final String from, final String to) {
        registerRedirect(from, to, REDIRECT_SEE_OTHER);
    }

    /**
     * Register a redirection from a specific path to another one.
     *
     * @param from          The path to redirect from. Is always a local path, can be prefixed with "/" or not.
     * @param to            The redirection target. Can be an absolute path ("https://www.google.com"),
     *                      a device path ("/app/com.sast.test/something"), or a local path ("rest/example/info").
     * @param redirectType: The type of redirect to perform (300 - 308).
     */
    private void registerRedirect(final String from, final String to, final WebServerResponse.ResponseStatus redirectType) {
        if (redirectType.getRequestStatus() < HTTP_REDIRECT_CODE_MIN || redirectType.getRequestStatus() > HTTP_REDIRECT_CODE_MAX) {
            throw new IllegalArgumentException("Invalid redirect response: " + redirectType);
        }
        if (from.startsWith(SLASH)) {
            mRedirections.put(from, new Pair<>(to, redirectType));
        } else {
            mRedirections.put(SLASH + from, new Pair<>(to, redirectType));
        }
    }

    /**
     * Directs a an incoming WebServerRequest to the appropriate handler function
     * based on the request type.
     *
     * @param webServerRequest The incoming web request

     **/
    @Override
    public WebServerResponse handleRequest(final WebServerRequest webServerRequest) {
        if (webServerRequest.isWebsocketUpgradeRequest()) {
            if (mWebSocketManager != null) {
                try {
                    return WebServerResponse.createWebSocketUpgradeResponse(webServerRequest, mWebSocketManager);
                } catch (WebSocketUpgradeException e) {
                    Log.e(LOGTAG, "Could not instantiate WebSocketSession after trying to upgrade response from request.", e);
                }
            } else {
                Log.e(LOGTAG, "WebSocket upgrade request received, but RestHandler was not instantiated with a valid WebSocketManager");
            }
        }
        final int requestType = webServerRequest.getMethod();
        WebServerResponse response = null;
        switch(requestType) {
        case WebServerMethod.GET:
            response = onGet(webServerRequest);
            break;
        case WebServerMethod.PUT:
            response = onPut(webServerRequest);
            break;
        case WebServerMethod.POST:
            response = onPost(webServerRequest);
            break;
        case WebServerMethod.DELETE:
            response = onDelete(webServerRequest);
            break;
        default:
        }
        return response;
    }

    /**
     * Handles GET requests to the webserver
     *
     * @param webServerRequest The incoming GET request
     * @return WebServerResponse The appropriate response to the get request
     **/
    public WebServerResponse onGet(final WebServerRequest webServerRequest) {
        // Get the full path from the web request
        final String route = getPath(webServerRequest);
        // Check the hashmap containing all the routes for this specific path
        final Pair<String, WebServerResponse.ResponseStatus> redirectionPair = mRedirections.get(route);

        // If the path was not in the hashmap redirect the request
        if (redirectionPair != null) {
            return createRedirectResponse(redirectionPair.first, redirectionPair.second);
        }

        /* If route start with REST_PATH then we know it's a REST API request
         * Otherwise it's a request for html/css/js resources
         */
        if (route.startsWith(REST_PATH)) {
            return invokeMethod(mGetRoutes, route, webServerRequest);
        } else {
            if ("".equals(route)) {
                return createRedirectResponse(mBasePath + SLASH, REDIRECT_SEE_OTHER);
            }
            String resource = route.substring(1);
            // Get the resource type from the web request path
            String mimeType = resolveMimeType(resource);
            if (mimeType == null) {
                /*
                * If we don't find the asset, and its media type is unknown
                * (which is default if no other type matches) return index.html instead
                */
                resource = "index.html";
                mimeType = "text/html";
            }
            // Create an AssetFileDescriptor from the full resource path
            final AssetFileDescriptor fileDescriptor = getAssetFileDescriptor(mWebsiteAssetPath + SLASH + resource);
            if (fileDescriptor == null) {
                return WebServerResponse.createStatusResponse(WebServerResponse.ResponseStatus.INTERNAL_ERROR);
            }
            // Create a response with this AssetFileDescriptor
            return WebServerResponse.createAssetFileResponse(fileDescriptor,
                    WebServerResponse.ResponseStatus.OK, mimeType, Collections.emptyMap());
        }
    }

    private AssetFileDescriptor getAssetFileDescriptor(final String path) {
        try {
            return mContext.getAssets().openFd(path);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Handles PUT requests to the webserver
     *
     * @param webServerRequest The incoming PUT request
     * @return WebServerResponse The appropriate response to the get request
     **/
    public WebServerResponse onPut(final WebServerRequest webServerRequest) {
        final String route = getPath(webServerRequest);
        return invokeMethod(mPutRoutes, route, webServerRequest);
    }

    /**
     * Handles POST requests to the webserver
     *
     * @param webServerRequest The incoming Post request
     * @return WebServerResponse The appropriate response to the get request
     **/
    public WebServerResponse onPost(final WebServerRequest webServerRequest) {
        final String route = getPath(webServerRequest);
        return invokeMethod(mPostRoutes, route, webServerRequest);
    }

    /**
     * Handles DELETE requests to the webserver
     *
     * @param webServerRequest The incoming DELETE request
     * @return WebServerResponse The appropriate response to the get request
     **/
    public WebServerResponse onDelete(final WebServerRequest webServerRequest) {
        final String route = getPath(webServerRequest);
        return invokeMethod(mDeleteRoutes, route, webServerRequest);
    }

    private WebServerResponse invokeMethod(final RestPath restPath, final String route, final WebServerRequest webServerRequest) {
        Log.d(LOGTAG, String.format("%s %s", restPath.getPathNode(), route));
        final String normalizedRoute = route.startsWith(SLASH) ? route.substring(1) : route;
        return restPath.resolve(normalizedRoute)
            .map(restMethodWrapper1 -> restMethodWrapper1.invoke(webServerRequest, normalizedRoute))
            .orElse(null);
    }

    private String getPath(final WebServerRequest webServerRequest) {
        String path = SLASH;
        if (webServerRequest.getUri() != null && webServerRequest.getUri()
            .getPath() != null) {
            path = webServerRequest.getUri()
                .getPath()
                .replace(mBasePath, EMPTY_PATH);
        }
        return path;
    }

    private String pathToRoute(final Path path) {
        if (path != null) {
            return path.value()
                .startsWith(SLASH) ? path.value() : SLASH + path.value();
        }
        return EMPTY_PATH;
    }

    private String resolveMimeType(@NonNull final String route) {
        final String fileExtension = MimeTypeMap.getFileExtensionFromUrl(route);
        String mimeType = MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(fileExtension);
        if (mimeType == null && FONTS_EXT.contains(fileExtension)) {
            mimeType = "font/" + fileExtension;
        }
        if (mimeType == null && JAVASCRIPT_EXT.equals(fileExtension)) {
            mimeType = "text/javascript";
        }
        return mimeType;
    }

    private WebServerResponse createRedirectResponse(final String to, final WebServerResponse.ResponseStatus type) {
        return WebServerResponse.createStringResponse(
            EMPTY_PATH,
            type,
            TEXT_PLAIN,
            ImmutableMap.of("location", to)
        );
    }
}
