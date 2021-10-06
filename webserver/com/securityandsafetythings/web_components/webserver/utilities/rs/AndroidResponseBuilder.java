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

package com.securityandsafetythings.web_components.webserver.utilities.rs;

import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

class AndroidResponseBuilder extends Response.ResponseBuilder {
    private Response.StatusType mStatusType;

    @Override
    public Response build() {
        return new AndroidResponse(mStatusType);
    }

    //CHECKSTYLE OFF: SuperCloneCheck
    @Override
    public Response.ResponseBuilder clone() {
        return this;
    }

    @Override
    public Response.ResponseBuilder status(final int status) {
        return status(status, "");
    }

    @Override
    public Response.ResponseBuilder status(final int status, final String reasonPhrase) {
        mStatusType = new Response.StatusType() {
            @Override
            public int getStatusCode() {
                return status;
            }

            @Override
            public Response.Status.Family getFamily() {
                return Response.Status.Family.familyOf(status);
            }

            @Override
            public String getReasonPhrase() {
                return reasonPhrase;
            }
        };
        return this;
    }

    @Override
    public Response.ResponseBuilder entity(final Object entity) {
        return this;
    }

    @Override
    public Response.ResponseBuilder entity(final Object entity, final Annotation[] annotations) {
        return this;
    }

    @Override
    public Response.ResponseBuilder allow(final String... methods) {
        return this;
    }

    @Override
    public Response.ResponseBuilder allow(final Set<String> methods) {
        return this;
    }

    @Override
    public Response.ResponseBuilder cacheControl(final CacheControl cacheControl) {
        return this;
    }

    @Override
    public Response.ResponseBuilder encoding(final String encoding) {
        return this;
    }

    @Override
    public Response.ResponseBuilder header(final String name, final Object value) {
        return this;
    }

    @Override
    public Response.ResponseBuilder replaceAll(final MultivaluedMap<String, Object> headers) {
        return this;
    }

    @Override
    public Response.ResponseBuilder language(final String language) {
        return this;
    }

    @Override
    public Response.ResponseBuilder language(final Locale language) {
        return this;
    }

    @Override
    public Response.ResponseBuilder type(final MediaType type) {
        return this;
    }

    @Override
    public Response.ResponseBuilder type(final String type) {
        return this;
    }

    @Override
    public Response.ResponseBuilder variant(final Variant variant) {
        return this;
    }

    @Override
    public Response.ResponseBuilder contentLocation(final URI location) {
        return this;
    }

    @Override
    public Response.ResponseBuilder cookie(final NewCookie... cookies) {
        return this;
    }

    @Override
    public Response.ResponseBuilder expires(final Date expires) {
        return this;
    }

    @Override
    public Response.ResponseBuilder lastModified(final Date lastModified) {
        return this;
    }

    @Override
    public Response.ResponseBuilder location(final URI location) {
        return this;
    }

    @Override
    public Response.ResponseBuilder tag(final EntityTag tag) {
        return this;
    }

    @Override
    public Response.ResponseBuilder tag(final String tag) {
        return this;
    }

    @Override
    public Response.ResponseBuilder variants(final Variant... variants) {
        return this;
    }

    @Override
    public Response.ResponseBuilder variants(final List<Variant> variants) {
        return this;
    }

    @Override
    public Response.ResponseBuilder links(final Link... links) {
        return this;
    }

    @Override
    public Response.ResponseBuilder link(final URI uri, final String rel) {
        return this;
    }

    @Override
    public Response.ResponseBuilder link(final String uri, final String rel) {
        return this;
    }
}
