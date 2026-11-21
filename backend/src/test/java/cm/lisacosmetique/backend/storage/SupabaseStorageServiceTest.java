package cm.lisacosmetique.backend.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class SupabaseStorageServiceTest {

    private static final String STORAGE = "https://projet.supabase.co/storage/v1";

    private MockRestServiceServer server;
    private SupabaseStorageService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        service = new SupabaseStorageService(new SupabaseStorageProperties(
                "https://projet.supabase.co/", "cle-secrete", "lisa-cosmetique", Duration.ofMinutes(15)),
                builder);
    }

    @Test
    void signedUrlIsRequestedWithTtlAndReturnedAsAbsoluteUrl() {
        server.expect(requestTo(STORAGE + "/object/sign/lisa-cosmetique/products/s%C3%A9rum%20bio.jpg"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("apikey", "cle-secrete"))
                .andExpect(header("Authorization", "Bearer cle-secrete"))
                .andExpect(content().json("{\"expiresIn\":900}"))
                .andRespond(withSuccess(
                        "{\"signedURL\":\"/object/sign/lisa-cosmetique/products/x.jpg?token=abc\"}",
                        MediaType.APPLICATION_JSON));

        String url = service.presignedDownloadUrl("products/sérum bio.jpg");

        assertThat(url).isEqualTo(STORAGE + "/object/sign/lisa-cosmetique/products/x.jpg?token=abc");
        server.verify();
    }

    @Test
    void storageErrorsAreWrappedInStorageException() {
        server.expect(requestTo(STORAGE + "/object/lisa-cosmetique/products/absent.jpg"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> service.delete("products/absent.jpg"))
                .isInstanceOf(StorageException.class);
    }

    @Test
    void missingBucketIsCreatedAsPrivate() {
        server.expect(requestTo(STORAGE + "/bucket/lisa-cosmetique"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"statusCode\":\"404\",\"error\":\"Bucket not found\"}"));
        server.expect(requestTo(STORAGE + "/bucket"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"id\":\"lisa-cosmetique\",\"public\":false}"))
                .andRespond(withSuccess());

        service.ensureBucketExists();

        server.verify();
    }
}
