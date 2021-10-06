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

package com.securityandsafetythings.web_components.webserver.utilities;

import android.util.Log;
import com.google.common.io.Files;
import com.securityandsafetythings.web_components.webserver.RestHandler;
import com.securityandsafetythings.webserver.FormDataPart;
import com.securityandsafetythings.webserver.InvalidCharsetException;
import com.securityandsafetythings.webserver.WebServerMethod;
import com.securityandsafetythings.webserver.WebServerRequest;
import com.securityandsafetythings.webserver.WebServerResponse;

import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.POST;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Wrapper class for rest methods
 */
public final class RestMethodWrapper implements HasPathNode {
    private static final String LOGTAG = RestMethodWrapper.class.getSimpleName();
    private static final String SLASH = "/";
    private static final String TEXT = "text/";
    private static final String REQUEST_BODY_CONTENT_KEY = "content";
    private static final String REQUEST_POST_DATA_KEY = "post_data";
    private final Object mRestService;
    private final Method mMethod;
    private final List<Parameter> mParameters;
    private final Map<Parameter, String> mQueryParameterNameMap;
    private final Map<Parameter, Integer> mPathParameterIndexMap;
    private final Parameter mRequestParameter;
    private final String mRestPath;
    private final String[] mRestPathElements;
    private final String mConsumeMediaType;
    private final String mProduceMediaType;
    private final Map<String, String> mHeaders;
    private final File mCacheDir;
    private final SharedMemoryFactory mSharedMemoryFactory;

    /**
     * Builds a {@link RestMethodWrapper}
     *
     * @param service       the rest service
     * @param m             the method
     * @param path          the web path the service will hosted
     * @param cache         the cache location
     * @param memoryFactory a shared memory factory
     */
    @SuppressWarnings("MagicNumber")
    public RestMethodWrapper(
        final Object service, final Method m, final String path, final File cache,
        final SharedMemoryFactory memoryFactory) {

        mRestService = service;
        mMethod = m;
        mParameters = Arrays.asList(m.getParameters());
        mRestPath = path.startsWith(SLASH) ? path.substring(1) : path;
        mSharedMemoryFactory = memoryFactory;
        mRestPathElements = mRestPath.split(SLASH);
        mQueryParameterNameMap = mParameters.stream()
            .filter(p -> isAnnotationPresent(p, QueryParam.class))
            .collect(Collectors.toMap(Function.identity(), p -> getAnnotation(p, QueryParam.class).value()));
        mPathParameterIndexMap = mParameters.stream()
            .filter(p -> isAnnotationPresent(p, PathParam.class))
            .collect(Collectors.toMap(Function.identity(), p -> getPathParameterIndex(getAnnotation(p, PathParam.class))));
        if (mParameters.stream()
            .anyMatch(p -> isAnnotationPresent(p, HeaderParam.class) || isAnnotationPresent(p, CookieParam.class))) {
            throw new UnsupportedOperationException("Header and Cookie parameters are not yet supported");
        }
        mRequestParameter = mParameters.stream()
            .filter(this::hasNoParamAnnotations)
            .findAny()
            .orElse(null);
        if (mParameters.stream()
            .filter(this::hasNoParamAnnotations)
            .limit(2)
            .count() > 1) {
            throw new UnsupportedOperationException("Only one not annotated parameter is allowed.");
        }
        mConsumeMediaType = computeConsumeMediaType();
        mProduceMediaType = computeProduceMediaType();
        mHeaders = computeHeaders();
        mCacheDir = cache;
    }

    private String computeConsumeMediaType() {
        final Consumes classConsumes = mMethod.getDeclaringClass()
            .getAnnotation(Consumes.class);
        final Consumes methodConsumes = mMethod.getDeclaredAnnotation(Consumes.class);
        final String[] values;
        if (methodConsumes != null) {
            values = methodConsumes.value();
        } else if (classConsumes != null) {
            values = classConsumes.value();
        } else {
            values = new String[]{MediaType.APPLICATION_JSON};
        }
        if (values.length > 1) {
            throw new UnsupportedOperationException("Only exactly one consumed media type per method is supported.");
        }
        final String mediaType = values[0];
        if (!mediaType.equals(MediaType.APPLICATION_JSON)
            && !mediaType.equals(MediaType.MULTIPART_FORM_DATA)
            && !mediaType.startsWith(TEXT)) {
            throw new UnsupportedOperationException("Currently, only text, JSON and multipart form are supported as consumed media type.");
        }
        if (mediaType.equals(MediaType.MULTIPART_FORM_DATA)) {
            checkMultipartFormDataMethod();
        } else if (mediaType.startsWith(TEXT)) {
            checkConsumesPlainTextMethod();
        }
        return mediaType;
    }

    private String computeProduceMediaType() {
        final Produces classConsumes = mMethod.getDeclaringClass()
            .getAnnotation(Produces.class);
        final Produces methodConsumes = mMethod.getDeclaredAnnotation(Produces.class);
        final String[] values;
        if (methodConsumes != null) {
            values = methodConsumes.value();
        } else if (classConsumes != null) {
            values = classConsumes.value();
        } else {
            values = new String[]{MediaType.APPLICATION_JSON};
        }
        if (values.length > 1) {
            throw new UnsupportedOperationException("Only exactly one produced media type per method is supported.");
        }
        final String mediaType = values[0];
        final String appOctetStream = "application/octet-stream";
        final String image = "image/";
        if (!mediaType.equals(MediaType.APPLICATION_JSON) && !mediaType.startsWith(image)
            && !appOctetStream.equals(mediaType)) {
            throw new UnsupportedOperationException(
                "Currently, only text, JSON, image types and octet stream are supported as produced media type.");
        }
        if (mediaType.startsWith(image) || appOctetStream.equals(mediaType)) {
            checkRawByteMethod();
        } else if (mediaType.startsWith(TEXT)) {
            checkProducesPlainTextMethod();
        }
        return mediaType;
    }

    @SuppressWarnings("MagicNumber")
    private Map<String, String> computeHeaders() {
        final ProducesHeader classProducesHeader = mMethod.getDeclaringClass()
            .getAnnotation(ProducesHeader.class);
        final ProducesHeader methodProducesHeader = mMethod.getDeclaredAnnotation(ProducesHeader.class);
        final String[] values;
        if (methodProducesHeader != null) {
            values = methodProducesHeader.value();
        } else if (classProducesHeader != null) {
            values = classProducesHeader.value();
        } else {
            return Collections.emptyMap();
        }
        final String delimiter = ":";
        return Arrays.stream(values)
            .map(v -> v.split(delimiter))
            .collect(Collectors.toMap(a -> a[0], a -> {
                if (a.length >= 2) {
                    return String.join(delimiter, Arrays.copyOfRange(a, 1, a.length));
                } else {
                    return "";
                }
            }));
    }

    /*
     * There seems to be a bug somewhere in the implementation of isAnnotationPresent(), getAnnotation() & co.
     * This bug causes the crash (SIGSEV) of the process when used on methods with multiple parameters whose some
     * are not annotated.
     * Avoiding to call the functions when a parameter has no annotation seems to work as a workaround, that's
     * what the functions below are for.
     */

    private static boolean isAnnotationPresent(final AnnotatedElement element, final Class<? extends Annotation> annotationClass) {
        return Arrays.stream(element.getAnnotations())
            .anyMatch(annotationClass::isInstance);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Annotation> T getAnnotation(final AnnotatedElement element, final Class<T> annotationClass) {
        return (T)Arrays.stream(element.getAnnotations())
            .filter(annotationClass::isInstance)
            .findFirst()
            .orElse(null);
    }

    private void checkMultipartFormDataMethod() {
        if (!mMethod.isAnnotationPresent(POST.class)) {
            throw new UnsupportedOperationException("Multi-part FORM data only supported for POST endpoints.");
        }
        Class<?> type = mRequestParameter != null ? mRequestParameter.getType() : null;
        if (type != null && type.isAssignableFrom(List.class)) {
            final ParameterizedType parameterizedType = (ParameterizedType)mRequestParameter.getParameterizedType();
            type = parameterizedType.getActualTypeArguments()[0].getClass();
        }
        if (type == null || !type.isAssignableFrom(File.class)) {
            throw new UnsupportedOperationException("Multi-part FORM data expects a File or a list of Files as parameter.");
        }
    }

    private void checkConsumesPlainTextMethod() {
        final Class<?> type = mRequestParameter != null ? mRequestParameter.getType() : null;
        if (type == null || !type.isAssignableFrom(String.class)) {
            throw new UnsupportedOperationException("Plain text methods expect a String as parameter.");
        }
    }

    private void checkRawByteMethod() {
        if (!byte[].class.isAssignableFrom(mMethod.getReturnType())) {
            throw new UnsupportedOperationException("Raw methods must return a byte array.");
        }
    }

    private void checkProducesPlainTextMethod() {
        if (!String.class.isAssignableFrom(mMethod.getReturnType())) {
            throw new UnsupportedOperationException("Plain text methods must return a String.");
        }
    }

    @Override
    public String getPathNode() {
        return mRestPathElements[mRestPathElements.length - 1];
    }

    String getRestPath() {
        return mRestPath;
    }

    /**
     * Invokes a request
     *
     * @param webServerRequest the request to invoke
     * @param currentRestPath  the request path
     * @return {@link WebServerResponse}
     */
    public WebServerResponse invoke(final WebServerRequest webServerRequest, final String currentRestPath) {
        final String[] currentRestPathElements = currentRestPath.split(SLASH);
        final Object[] realParameters;
        try {
            realParameters = mParameters.stream()
                .map(p -> {
                    try {
                        return getRequestParameters(p, webServerRequest, currentRestPathElements);
                    } catch (InvalidCharsetException e) {
                        e.printStackTrace();
                        return webApplicationExceptionToResponse(e);
                    }
                })
                .toArray();
        } catch (final Exception e) {
            return webApplicationExceptionToResponse(e);
        }
        try {
            final Object result = mMethod.invoke(mRestService, realParameters);
            if (result == null) {
                return WebServerResponse.createStringResponse("",
                    WebServerResponse.ResponseStatus.NO_CONTENT,
                    MediaType.TEXT_PLAIN,
                    mHeaders);
            }
            if (mProduceMediaType.equals(MediaType.APPLICATION_JSON)) {
                return WebServerResponse
                    .createSharedMemoryResponse(mSharedMemoryFactory.createSharedMemoryForString(RestHandler.toJson(result)),
                        WebServerResponse.ResponseStatus.OK,
                        MediaType.APPLICATION_JSON + "; charset=UTF-8",
                        mHeaders);
            } else if (mProduceMediaType.startsWith(TEXT)) {
                return WebServerResponse
                    .createSharedMemoryResponse(mSharedMemoryFactory.createSharedMemoryForString((String)result),
                        WebServerResponse.ResponseStatus.OK,
                        mProduceMediaType,
                        mHeaders);
            } else {
                return WebServerResponse
                    .createSharedMemoryResponse(mSharedMemoryFactory.createSharedMemoryForBytes((byte[])result),
                        WebServerResponse.ResponseStatus.OK,
                        mProduceMediaType,
                        mHeaders);
            }
        } catch (final Exception e) {
            final Throwable cause = e.getCause();
            return webApplicationExceptionToResponse(cause == null ? e : cause);
        }
    }

    private WebServerResponse webApplicationExceptionToResponse(final Throwable ex) {
        if (ex instanceof WebApplicationException) {
            final WebApplicationException e = (WebApplicationException)ex;
            final Response response = e.getResponse();
            return WebServerResponse.createStringResponse("Error " + response.getStatus() + ": " + e.getMessage(),
                toResponseStatus(response.getStatusInfo()),
                MediaType.TEXT_PLAIN,
                Collections.emptyMap());
        } else {
            Log.e(LOGTAG, "500 Error received", ex);
            return WebServerResponse.createStringResponse("Error 500: " + ex.getMessage(),
                WebServerResponse.ResponseStatus.INTERNAL_ERROR,
                MediaType.TEXT_PLAIN,
                Collections.emptyMap());
        }
    }

    private Object getRequestParameters(
        final Parameter parameter,
        final WebServerRequest webServerRequest,
        final String[] currentRestPathElements) throws InvalidCharsetException {

        if (mQueryParameterNameMap.containsKey(parameter)) {
            final List<String> strings = webServerRequest.getParameters()
                .get(mQueryParameterNameMap.get(parameter));
            if (strings == null) {
                return null;
            }
            if (parameter.getType()
                .isAssignableFrom(List.class)) {
                final Type elementType = (((ParameterizedType)parameter.getParameterizedType()).getActualTypeArguments())[0];
                return strings.stream()
                    .map(v -> toRequestParameter(v, elementType))
                    .collect(Collectors.toList());
            }
            return toRequestParameter(strings.get(0), parameter.getParameterizedType());
        } else if (mPathParameterIndexMap.containsKey(parameter)) {
            final Integer index = mPathParameterIndexMap.get(parameter);
            return index != null
                ? toRequestParameter(currentRestPathElements[index], parameter.getParameterizedType()) : null;
        } else if (parameter.equals(mRequestParameter)) {
            if (mConsumeMediaType.equals(MediaType.MULTIPART_FORM_DATA)) {
                final List<FormDataPart> dataParts = webServerRequest.getFileParts();
                if (parameter.getType()
                    .isAssignableFrom(List.class)) {
                    return dataParts.stream()
                        .map(this::formDataPartToFile)
                        .collect(Collectors.toList());
                } else {
                    return dataParts.isEmpty() ? null : formDataPartToFile(dataParts.get(0));
                }
            } else if (mConsumeMediaType.startsWith(TEXT)) {
                return getBodyJson(webServerRequest);
            } else {
                return toRequestParameter(getBodyJson(webServerRequest), mRequestParameter.getParameterizedType());
            }
        }
        throw new RuntimeException("Unexpected parameter " + parameter.getName());
    }

    @SuppressWarnings("MagicNumber")
    private File formDataPartToFile(final FormDataPart dataPart) {
        if (dataPart == null) {
            return null;
        }

        String filename = dataPart.getFilename();
        if (filename == null) {
            filename = dataPart.getName();
        }
        if (filename == null) {
            filename = UUID.randomUUID()
                .toString();
        }

        final File file = new File(mCacheDir, filename);
        try (
            FileInputStream inputStream = new FileInputStream(dataPart.getParcelFileDescriptor()
                .getFileDescriptor());
            OutputStream output = new FileOutputStream(file)
        ) {
            final byte[] buffer = new byte[1 << 12];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            output.flush();
            return file;
        } catch (IOException e) {
            throw new RuntimeException("Unable to save received file", e);
        }
    }

    private Object toRequestParameter(final String value, final Type type) {
        return value != null ? RestHandler.fromJson(value, type) : null;
    }

    private WebServerResponse.ResponseStatus toResponseStatus(final Response.StatusType status) {
        return Arrays.stream(WebServerResponse.ResponseStatus.values())
            .filter(s -> s.getRequestStatus() == status.getStatusCode())
            .findAny()
            .orElse(WebServerResponse.ResponseStatus.INTERNAL_ERROR);
    }

    private boolean hasNoParamAnnotations(final Parameter parameter) {
        return !isAnnotationPresent(parameter, PathParam.class) && !isAnnotationPresent(parameter, HeaderParam.class)
            && !isAnnotationPresent(parameter, CookieParam.class) && !isAnnotationPresent(parameter, QueryParam.class);
    }

    private int getPathParameterIndex(final PathParam pathParam) {
        final String parameterName = "{" + pathParam.value() + "}";
        for (int i = 0; i < mRestPathElements.length; ++i) {
            if (parameterName.equals(mRestPathElements[i])) {
                return i;
            }
        }
        throw new IllegalArgumentException("Parameter name " + pathParam.value() + " not found in route " + mRestPath);
    }

    private String getBodyJson(final WebServerRequest webServerRequest) throws InvalidCharsetException {
        final Map<String, String> body = createRequestBodyMap(webServerRequest);
        final String postData = REQUEST_POST_DATA_KEY;
        final String content = REQUEST_BODY_CONTENT_KEY;
        if (body.containsKey(postData)) {
            return body.get(postData);
        } else if (body.containsKey(content)) {
            try {
                return Files.asCharSource(new File(Objects.requireNonNull(body.get(content))), getBodyCharset(webServerRequest))
                    .read();
            } catch (final IOException e) {
                throw new InternalServerErrorException("Unable to load cached file.", e);
            }
        } else {
            return null;
        }
    }

    private Map<String, String> createRequestBodyMap(final WebServerRequest webServerRequest) {
        final Map<String, String> bodyMap = new HashMap<String, String>();
        try {
            if (webServerRequest.getMethod() == WebServerMethod.POST) {
                bodyMap.put(REQUEST_POST_DATA_KEY, webServerRequest.getBodyAsString());
            } else {
                bodyMap.put(REQUEST_BODY_CONTENT_KEY, webServerRequest.getBodyAsString());
            }
        } catch (final InvalidCharsetException e) {
            return new HashMap<String, String>();
        }
        return bodyMap;
    }

    private Charset getBodyCharset(final WebServerRequest webServerRequest) {
        final String contentType = webServerRequest.getHeaders()
            .get("Content-Type");
        final String filterString = "charset=";
        Charset charset;
        if (contentType != null && contentType.contains(filterString)) {
            final String[] split = contentType.split(filterString);
            try {
                charset = Charset.forName(split[1]);
            } catch (final UnsupportedCharsetException e) {
                charset = StandardCharsets.UTF_8;
            }
        } else {
            charset = StandardCharsets.UTF_8;
        }
        return charset;
    }
}
