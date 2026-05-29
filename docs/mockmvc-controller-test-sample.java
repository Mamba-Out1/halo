package run.halo.app.content;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * MockMvc sample for report/demo purpose.
 * Note: Halo runtime is WebFlux, so production tests should prefer WebTestClient.
 */
@SpringBootTest
@AutoConfigureMockMvc
class MockMvcControllerTestSample {

    @Autowired
    MockMvc mockMvc;

    @Test
    void loginApi() throws Exception {
        mockMvc.perform(get("/login/public-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.base64Format").isNotEmpty());
    }

    @Test
    void draftPostApi() throws Exception {
        var body = """
                {
                  "post": {
                    "spec": {
                      "title": "mockmvc-post",
                      "slug": "mockmvc-post",
                      "visible": "PUBLIC",
                      "publish": true,
                      "deleted": false
                    },
                    "apiVersion": "content.halo.run/v1alpha1",
                    "kind": "Post",
                    "metadata": {
                      "generateName": "post-"
                    }
                  },
                  "content": {
                    "raw": "<p>hello world</p>",
                    "content": "<p>hello world</p>",
                    "rawType": "HTML"
                  }
                }
                """;
        mockMvc.perform(post("/apis/api.console.halo.run/v1alpha1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata.name").isNotEmpty());
    }

    @Test
    void deleteApi() throws Exception {
        mockMvc.perform(delete("/apis/api.console.halo.run/v1alpha1/posts/{name}/content", "post-xxx")
                        .queryParam("snapshotName", "snapshot-xxx"))
                .andExpect(status().isOk());
    }
}
