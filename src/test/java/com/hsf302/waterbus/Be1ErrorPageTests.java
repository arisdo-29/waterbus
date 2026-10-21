package com.hsf302.waterbus;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(Be1ErrorPageTests.FailureFixture.class)
class Be1ErrorPageTests {
    @LocalServerPort int port;

    @Test
    void bootErrorDispatchRendersVietnamese404And500WithoutLeakingException() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        for (int code : new int[]{404, 500}) {
            String url = code == 404 ? "/__test/does-not-exist" : "/__test/be1-failure";
            HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + url))
                    .header("Accept", "text/html").GET().build(), HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(code);
            assertThat(response.headers().firstValue("Content-Type").orElse("")).contains("text/html", "charset=UTF-8");
            assertThat(response.body()).contains("Lỗi " + code, "<footer", "src=\"/js/app.js\"", "Về trang chủ")
                    .doesNotContain("secret-stack-trace", "Whitelabel Error Page");
            Files.createDirectories(Path.of("target/be1-admin-preview"));
            Files.writeString(Path.of("target/be1-admin-preview/error" + code + ".html"), response.body());
        }
    }

    @Controller
    public static class FailureFixture {
        @GetMapping("/__test/be1-failure")
        public String fail() {
            throw new IllegalStateException("secret-stack-trace");
        }
    }
}
