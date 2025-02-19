/*
 * Copyright 2017-2022 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.http.server.tck.tests.cors;

import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpMethod;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.cors.CrossOrigin;
import io.micronaut.http.server.tck.CorsUtils;
import io.micronaut.http.server.util.HttpHostResolver;
import io.micronaut.http.uri.UriBuilder;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;
import io.micronaut.http.tck.ServerUnderTest;
import io.micronaut.http.tck.AssertionUtils;
import io.micronaut.http.tck.HttpResponseAssertion;

import java.io.IOException;
import java.net.URI;
import java.util.function.BiConsumer;

import static io.micronaut.http.tck.TestScenario.asserts;
import static io.micronaut.http.server.tck.CorsUtils.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings({
    "java:S2259", // The tests will show if it's null
    "java:S5960", // We're allowed assertions, as these are used in tests only
    "checkstyle:MissingJavadocType",
    "checkstyle:DesignForExtension"
})
public class CrossOriginTest {

    private static final String SPECNAME = "CrossOriginTest";

    @Test
    void crossOriginAnnotationWithMatchingOrigin() throws IOException {
        asserts(SPECNAME,
            preflight(UriBuilder.of("/foo").path("bar"), "https://foo.com", HttpMethod.GET),
            (server, request) -> AssertionUtils.assertDoesNotThrow(server, request, HttpResponseAssertion.builder()
                .status(HttpStatus.OK)
                .assertResponse(response -> {
                    assertCorsHeaders(response, "https://foo.com", HttpMethod.GET);
                    assertFalse(response.getHeaders().names().contains(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS));
                })
                .build()));
    }

    @Test
    void crossOriginAnnotationWithNoMatchingOrigin() throws IOException {
        asserts(SPECNAME,
            preflight(UriBuilder.of("/foo").path("bar"), "https://bar.com", HttpMethod.GET),
            (server, request) -> AssertionUtils.assertThrows(server, request, HttpResponseAssertion.builder()
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .assertResponse(CorsUtils::assertCorsHeadersNotPresent)
                .build()));
    }

    @Test
    void verifyHttpMethodIsValidatedInACorsRequest() {
        assertAll(
            () -> asserts(SPECNAME,
                preflight(UriBuilder.of("/methods").path("getit"), "https://www.google.com", HttpMethod.GET),
                (server, request) -> AssertionUtils.assertDoesNotThrow(server, request, HttpResponseAssertion.builder()
                    .status(HttpStatus.OK)
                    .assertResponse(response -> {
                        assertCorsHeaders(response, "https://www.google.com", HttpMethod.GET);
                        assertFalse(response.getHeaders().names().contains(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS));
                    })
                    .build())),
            () -> asserts(SPECNAME,
                preflight(UriBuilder.of("/methods").path("postit").path("id"), "https://www.google.com", HttpMethod.POST),
                (server, request) -> AssertionUtils.assertDoesNotThrow(server, request, HttpResponseAssertion.builder()
                    .status(HttpStatus.OK)
                    .assertResponse(response -> {
                        assertCorsHeaders(response, "https://www.google.com", HttpMethod.POST);
                        assertFalse(response.getHeaders().names().contains(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS));
                    })
                    .build())),
            () -> asserts(SPECNAME,
                preflight(UriBuilder.of("/methods").path("deleteit").path("id"), "https://www.google.com", HttpMethod.DELETE),
                (server, request) -> AssertionUtils.assertThrows(server, request, HttpResponseAssertion.builder()
                    .status(HttpStatus.FORBIDDEN)
                    .assertResponse(CorsUtils::assertCorsHeadersNotPresent)
                    .build()))
        );
    }

    @Test
    void allowedOriginsRegexHappyPath() throws IOException {
        URI uri = UriBuilder.of("/allowedoriginsregex").path("foo").build();
        String origin = "https://foo.com";
        asserts(SPECNAME, preflight(uri, origin, HttpMethod.GET), happyPathAssertion(origin));
        origin = "http://foo.com";
        asserts(SPECNAME, preflight(uri, origin, HttpMethod.GET), happyPathAssertion(origin));
    }

    private static BiConsumer<ServerUnderTest, HttpRequest<?>> happyPathAssertion(String origin) {
        return (server, request) -> AssertionUtils.assertDoesNotThrow(server, request, HttpResponseAssertion.builder()
            .status(HttpStatus.OK)
            .assertResponse(response -> {
                assertCorsHeaders(response, origin, HttpMethod.GET);
                assertFalse(response.getHeaders().names().contains(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS));
            })
            .build());
    }

    @Test
    void allowedOriginsRegexFailure() throws IOException {
        asserts(SPECNAME,
            preflight(UriBuilder.of("/allowedoriginsregex").path("bar"), "https://foo.com", HttpMethod.GET),
            (server, request) -> AssertionUtils.assertThrows(server, request, HttpResponseAssertion.builder()
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .assertResponse(CorsUtils::assertCorsHeadersNotPresent)
                .build()));
    }

    @Test
    void allowedHeadersHappyPath() throws IOException {
        asserts(SPECNAME,
            preflight(UriBuilder.of("/allowedheaders").path("bar"), "https://foo.com", HttpMethod.GET)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, HttpHeaders.AUTHORIZATION + "," + HttpHeaders.CONTENT_TYPE),
            (server, request) -> AssertionUtils.assertDoesNotThrow(server, request, HttpResponseAssertion.builder()
                .status(HttpStatus.OK)
                .assertResponse(response -> {
                    assertCorsHeaders(response, "https://foo.com", HttpMethod.GET);
                    assertTrue(response.getHeaders().names().contains(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS));
                })
                .build()));
    }

    /**
     * <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Access-Control-Allow-Headers">Access-Control-Allow-Headers</a>
     * The Access-Control-Allow-Headers response header is used in response to a preflight request which includes the Access-Control-Request-Headers to indicate which HTTP headers can be used during the actual request.
     */
    @Test
    void allowedHeadersFailure() throws IOException {
        asserts(SPECNAME,
            preflight(UriBuilder.of("/allowedheaders").path("bar"), "https://foo.com", HttpMethod.GET)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "foo"),
            (server, request) -> AssertionUtils.assertThrows(server, request, HttpResponseAssertion.builder()
                .status(HttpStatus.FORBIDDEN)
                .assertResponse(CorsUtils::assertCorsHeadersNotPresent)
                .build()));
    }

    /**
     * The Access-Control-Expose-Headers header adds the specified headers to the allowlist that JavaScript (such as getResponseHeader()) in browsers is allowed to access.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Access-Control-Expose-Headers">Access-Control-Expose-Headers</a>
     */
    @Test
    void defaultAccessControlExposeHeaderValueIsNotSet() throws IOException {
        asserts(SPECNAME,
            preflight(UriBuilder.of("/exposedheaders").path("foo"), "https://foo.com", HttpMethod.GET),
            (server, request) -> AssertionUtils.assertDoesNotThrow(server, request, HttpResponseAssertion.builder()
                .status(HttpStatus.OK)
                .assertResponse(response -> {
                    assertCorsHeaders(response, "https://foo.com", HttpMethod.GET);
                    assertFalse(response.getHeaders().names().contains(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS));
                })
                .build()));
    }

    /**
     * The Access-Control-Expose-Headers header adds the specified headers to the allowlist that JavaScript (such as getResponseHeader()) in browsers is allowed to access.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Access-Control-Expose-Headers">Access-Control-Expose-Headers</a>
     */
    @Test
    void httHeaderValueAccessControlExposeHeaderValueCanBeSetViaCrossOriginAnnotation() throws IOException {
        asserts(SPECNAME,
            CollectionUtils.mapOf("micronaut.server.cors.single-header", StringUtils.TRUE),
            preflight(UriBuilder.of("/exposedheaders").path("bar"), "https://foo.com", HttpMethod.GET)
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "foo"),
            (server, request) -> AssertionUtils.assertDoesNotThrow(server, request, HttpResponseAssertion.builder()
                .status(HttpStatus.OK)
                .assertResponse(response -> {
                    assertCorsHeaders(response, "https://foo.com", HttpMethod.GET);
                    assertTrue(response.getHeaders().names().contains(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS));
                    assertEquals("Content-Encoding,Kuma-Revision", response.getHeaders().get(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS));
                })
                .build()));
    }

    /**
     * The Access-Control-Allow-Credentials response header tells browsers whether to expose the response to the frontend JavaScript code when the request's credentials mode (Request.credentials) is include.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Access-Control-Allow-Credentials">Access-Control-Allow-Credentials</a>
     */
    @Test
    void defaultAccessControlAllowCredentialsValueIsNotSet() throws IOException {
        asserts(SPECNAME,
            preflight(UriBuilder.of("/credentials").path("foo"), "https://foo.com", HttpMethod.GET),
            (server, request) -> AssertionUtils.assertDoesNotThrow(server, request, HttpResponseAssertion.builder()
                .status(HttpStatus.OK)
                .assertResponse(response -> {
                    assertCorsHeaders(response, "https://foo.com", HttpMethod.GET, false);
                    assertFalse(response.getHeaders().names().contains(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
                })
                .build()));
    }

    /**
     * The Access-Control-Allow-Credentials response header tells browsers whether to expose the response to the frontend JavaScript code when the request's credentials mode (Request.credentials) is include.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Access-Control-Allow-Credentials">Access-Control-Allow-Credentials</a>
     */
    @Test
    void defaultAccessControlAllowCredentialsValueIsSet() throws IOException {
        asserts(SPECNAME,
            preflight(UriBuilder.of("/credentials").path("bar"), "https://foo.com", HttpMethod.GET),
            (server, request) -> AssertionUtils.assertDoesNotThrow(server, request, HttpResponseAssertion.builder()
                .status(HttpStatus.OK)
                .assertResponse(response -> {
                    assertCorsHeaders(response, "https://foo.com", HttpMethod.GET);
                    assertTrue(response.getHeaders().names().contains(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
                    assertEquals("true", response.getHeaders().get(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
                })
                .build()));
    }

    /**
     * The Access-Control-Max-Age response header indicates how long the results of a preflight request (that is the information contained in the Access-Control-Allow-Methods and Access-Control-Allow-Headers headers) can be cached.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Access-Control-Max-Age">Access-Control-Max-Age</a>
     */
    @Test
    void defaultAccessControlMaxAgeValueIsSet() throws IOException {
        asserts(SPECNAME,
            preflight(UriBuilder.of("/maxage").path("foo"), "https://foo.com", HttpMethod.GET),
            (server, request) -> AssertionUtils.assertDoesNotThrow(server, request, HttpResponseAssertion.builder()
                .status(HttpStatus.OK)
                .assertResponse(response -> {
                    assertCorsHeaders(response, "https://foo.com", HttpMethod.GET);
                    assertTrue(response.getHeaders().names().contains(HttpHeaders.ACCESS_CONTROL_MAX_AGE));
                    assertEquals("1800", response.getHeaders().get(HttpHeaders.ACCESS_CONTROL_MAX_AGE));
                })
                .build()));
    }

    /**
     * The Access-Control-Max-Age response header indicates how long the results of a preflight request (that is the information contained in the Access-Control-Allow-Methods and Access-Control-Allow-Headers headers) can be cached.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Access-Control-Max-Age">Access-Control-Max-Age</a>
     */
    @Test
    void accessControlMaxAgeValueIsSet() throws IOException {
        asserts(SPECNAME,
            preflight(UriBuilder.of("/maxage").path("bar"), "https://foo.com", HttpMethod.GET),
            (server, request) -> AssertionUtils.assertDoesNotThrow(server, request, HttpResponseAssertion.builder()
                .status(HttpStatus.OK)
                .assertResponse(response -> {
                    assertCorsHeaders(response, "https://foo.com", HttpMethod.GET, "1000");
                    assertTrue(response.getHeaders().names().contains(HttpHeaders.ACCESS_CONTROL_MAX_AGE));
                    assertEquals("1000", response.getHeaders().get(HttpHeaders.ACCESS_CONTROL_MAX_AGE));
                })
                .build()));
    }

    private static MutableHttpRequest<?> preflight(UriBuilder uriBuilder, String originValue, HttpMethod method) {
        return preflight(uriBuilder.build(), originValue, method);
    }

    private static MutableHttpRequest<?> preflight(URI uri, String originValue, HttpMethod method) {
        return HttpRequest.OPTIONS(uri)
            .header(HttpHeaders.ACCEPT, MediaType.TEXT_PLAIN)
            .header(HttpHeaders.ORIGIN, originValue)
            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, method);
    }

    @Requires(property = "spec.name", value = SPECNAME)
    @Controller("/foo")
    static class Foo {
        @CrossOrigin("https://foo.com")
        @Produces(MediaType.TEXT_PLAIN)
        @Get("/bar")
        String index() {
            return "bar";
        }
    }

    @Requires(property = "spec.name", value = SPECNAME)
    @Controller("/allowedoriginsregex")
    static class AllowedOriginsRegex {

        @CrossOrigin(
            allowedOriginsRegex = "^http(|s):\\/\\/foo\\.com$"
        )
        @Produces(MediaType.TEXT_PLAIN)
        @Get("/foo")
        String foo() {
            return "foo";
        }

        @CrossOrigin(
            value = "^http(|s):\\/\\/foo\\.com$"
            // allowedOriginsRegex defaults to false
        )
        @Produces(MediaType.TEXT_PLAIN)
        @Get("/bar")
        String bar() {
            return "bar";
        }
    }

    @Requires(property = "spec.name", value = SPECNAME)
    @Controller("/methods")
    @CrossOrigin(
        allowedOrigins = "https://www.google.com",
        allowedMethods = { HttpMethod.GET, HttpMethod.POST }
    )
    static class AllowedMethods {
        @Produces(MediaType.TEXT_PLAIN)
        @Get("/getit")
        String canGet() {
            return "get";
        }

        @Produces(MediaType.TEXT_PLAIN)
        @Post("/postit/{id}")
        String canPost(@PathVariable String id) {
            return id;
        }

        @Delete("/deleteit/{id}")
        String cantDelete(@PathVariable String id) {
            return id;
        }
    }

    @Requires(property = "spec.name", value = SPECNAME)
    @Controller("/allowedheaders")
    @CrossOrigin(
        value = "https://foo.com",
        allowedHeaders = { HttpHeaders.CONTENT_TYPE, HttpHeaders.AUTHORIZATION }
    )
    static class AllowedHeaders {
        @Produces(MediaType.TEXT_PLAIN)
        @Get("/bar")
        String index() {
            return "bar";
        }
    }

    @Requires(property = "spec.name", value = SPECNAME)
    @Controller("/exposedheaders")
    static class ExposedHeaders {
        @CrossOrigin(
            value = "https://foo.com",
            exposedHeaders = { "Content-Encoding", "Kuma-Revision" }
        )
        @Produces(MediaType.TEXT_PLAIN)
        @Get("/bar")
        String bar() {
            return "bar";
        }

        @CrossOrigin(
            value = "https://foo.com"
        )
        @Produces(MediaType.TEXT_PLAIN)
        @Get("/foo")
        String foo() {
            return "foo";
        }
    }

    @Requires(property = "spec.name", value = SPECNAME)
    @Controller("/credentials")
    static class Credentials {
        @CrossOrigin(
            value = "https://foo.com",
            allowCredentials = false
        )
        @Produces(MediaType.TEXT_PLAIN)
        @Get("/foo")
        String foo() {
            return "foo";
        }

        @CrossOrigin(
            value = "https://foo.com"
        )
        @Produces(MediaType.TEXT_PLAIN)
        @Get("/bar")
        String bar() {
            return "bar";
        }
    }

    @Requires(property = "spec.name", value = SPECNAME)
    @Controller("/maxage")
    static class MaxAge {
        @CrossOrigin(
            value = "https://foo.com"
        )
        @Produces(MediaType.TEXT_PLAIN)
        @Get("/foo")
        String foo() {
            return "foo";
        }

        @CrossOrigin(
            value = "https://foo.com",
            maxAge = 1000L
        )
        @Produces(MediaType.TEXT_PLAIN)
        @Get("/bar")
        String bar() {
            return "bar";
        }
    }

    @Requires(property = "spec.name", value = SPECNAME)
    @Replaces(HttpHostResolver.class)
    @Singleton
    static class HttpHostResolverReplacement implements HttpHostResolver {
        @Override
        public String resolve(@Nullable HttpRequest request) {
            return "https://micronautexample.com";
        }
    }
}
