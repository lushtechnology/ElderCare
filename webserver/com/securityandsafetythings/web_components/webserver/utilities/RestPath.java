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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * {@link RestPath} class
 */
public class RestPath implements HasPathNode {
    private static final String SLASH = "/";
    private final String mPathNode;
    private final Map<String, RestPath> mNormalChildren = new HashMap<>();
    private final Map<String, RestMethodWrapper> mNormalLeaves = new HashMap<>();
    private RestPath mParametrizedChild;
    private RestMethodWrapper mParametrizedLeaf;

    /**
     * Constructs a {@link RestPath}
     *
     * @param path the path
     */
    public RestPath(final String path) {
        mPathNode = path;
    }

    @Override
    public String getPathNode() {
        return mPathNode;
    }

    /**
     * Resolves a path.
     *
     * @param path the path to resolve.
     * @return {@link Optional <RestMethodWrapper>}
     */
    public Optional<RestMethodWrapper> resolve(final String path) {
        final String normalized = path.startsWith(SLASH) ? path.substring(1) : path;
        final String[] pathElements = normalized.split(SLASH);
        switch (pathElements.length) {
        case 0:
            return Optional.empty();
        case 1:
            return findNode(pathElements[0], mNormalLeaves, mParametrizedLeaf);
        default:
            final String nodeName = pathElements[0];
            final Optional<RestPath> child = findNode(nodeName, mNormalChildren, mParametrizedChild);
            return child.flatMap(c -> c.resolve(getChildPath(pathElements)));
        }
    }

    /**
     * Adds a rest method
     *
     * @param path              where the method will be hosted.
     * @param restMethodWrapper the method to host
     */
    public void addMethod(final String path, final RestMethodWrapper restMethodWrapper) {
        final String normalized = path.startsWith(SLASH) ? path.substring(1) : path;
        final String[] pathElements = normalized.split(SLASH);
        final String nodeName;
        switch (pathElements.length) {
        case 0:
            throw new UnsupportedOperationException("Cannot add method at root path");
        case 1:
            nodeName = pathElements[0];
            if (findNode(nodeName, mNormalLeaves, mParametrizedLeaf).isPresent()) {
                throw new IllegalArgumentException("Trying to add path which already exists: "
                    + restMethodWrapper.getRestPath());
            }
            if (isPathParameter(nodeName)) {
                mParametrizedLeaf = restMethodWrapper;
            } else {
                mNormalLeaves.put(nodeName, restMethodWrapper);
            }
            break;
        default:
            nodeName = pathElements[0];
            RestPath child = findNode(nodeName, mNormalChildren, mParametrizedChild).orElse(null);
            if (child == null) {
                child = new RestPath(nodeName);
                if (isPathParameter(nodeName)) {
                    mParametrizedChild = child;
                } else {
                    mNormalChildren.put(nodeName, child);
                }
            }
            child.addMethod(getChildPath(pathElements), restMethodWrapper);
        }
    }

    private boolean isPathParameter(final String nodeName) {
        return nodeName.startsWith("{") && nodeName.endsWith("}");
    }

    private <T extends HasPathNode> Optional<T> findNode(final String nodeName, final Map<String, T> normalNodes,
        final T parametrizedNode) {

        final T node = normalNodes.get(nodeName);
        if (node != null) {
            return Optional.of(node);
        }
        return Optional.ofNullable(parametrizedNode);
    }

    @SuppressWarnings("MagicNumber")
    private String getChildPath(final String[] pathElements) {
        final StringBuilder sb = new StringBuilder(pathElements[1]);
        for (int i = 2; i < pathElements.length; ++i) {
            sb.append(SLASH)
                .append(pathElements[i]);
        }
        return sb.toString();
    }
}
