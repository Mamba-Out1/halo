package run.halo.app.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import run.halo.app.core.extension.Role;
import run.halo.app.core.extension.content.Post;
import run.halo.app.core.user.service.RoleService;
import run.halo.app.extension.Metadata;
import run.halo.app.infra.utils.JsonUtils;

@SpringBootTest
@AutoConfigureWebTestClient
@WithMockUser(username = "fake-user", password = "fake-password", roles = "fake-super-role")
class ControllerApiIntegrationTests {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    RoleService roleService;

    @BeforeEach
    void setUp() {
        var rule = new Role.PolicyRule.Builder()
                .apiGroups("*")
                .resources("*")
                .verbs("*")
                .build();
        var role = new Role();
        role.setMetadata(new Metadata());
        role.getMetadata().setName("super-role");
        role.setRules(List.of(rule));
        when(roleService.listDependenciesFlux(anySet())).thenReturn(Flux.just(role));
        webTestClient = webTestClient.mutateWith(csrf());
    }

    @Test
    void loginPublicKeyApiShouldReturnOk() {
        webTestClient
                .get()
                .uri("/login")
                .exchange()
                .expectStatus()
                .isFound()
                .expectHeader()
                .valueEquals("Location", "/uc");
    }

    @Test
    void draftPostApiShouldReturnCreatedDraftPost() {
        webTestClient
                .post()
                .uri("/apis/api.console.halo.run/v1alpha1/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(postDraftRequest())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(Post.class)
                .value(post -> {
                    assertThat(post.getMetadata().getName()).startsWith("post-");
                    assertThat(post.getSpec().getOwner()).isEqualTo("fake-user");
                    assertThat(post.getSpec().getHeadSnapshot()).isNotBlank();
                });
    }

    @Test
    void deletePostContentApiShouldReturnDeletedSnapshot() {
        var createdPost = webTestClient
                .post()
                .uri("/apis/api.console.halo.run/v1alpha1/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(postDraftRequest())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(Post.class)
                .returnResult()
                .getResponseBody();

        assertThat(createdPost).isNotNull();
        assertThat(createdPost.getSpec().getHeadSnapshot()).isNotBlank();

        var updatedPost = webTestClient
                .put()
                .uri("/apis/api.console.halo.run/v1alpha1/posts/{name}/content", createdPost.getMetadata().getName())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "raw": "<p>hello halo</p>",
                          "content": "<p>hello halo</p>",
                          "rawType": "HTML"
                        }
                        """)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(Post.class)
                .returnResult()
                .getResponseBody();

        assertThat(updatedPost).isNotNull();
        assertThat(updatedPost.getSpec().getHeadSnapshot()).isNotBlank();

        webTestClient
                .delete()
                .uri(uriBuilder -> uriBuilder
                        .path("/apis/api.console.halo.run/v1alpha1/posts/{name}/content")
                        .queryParam("snapshotName", updatedPost.getSpec().getHeadSnapshot())
                        .build(updatedPost.getMetadata().getName()))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.raw")
                .value(raw -> assertThat((String) raw).contains("hello halo"));
    }

    private PostRequest postDraftRequest() {
        var payload = """
            {
                "post": {
                    "spec": {
                        "title": "controller-api-test-post",
                        "slug": "controller-api-test-post",
                        "template": "",
                        "cover": "",
                        "deleted": false,
                        "publish": true,
                        "publishTime": "",
                        "pinned": false,
                        "allowComment": true,
                        "visible": "PUBLIC",
                        "version": 1,
                        "priority": 0,
                        "excerpt": {
                            "autoGenerate": true,
                            "raw": ""
                        },
                        "categories": [],
                        "tags": [],
                        "htmlMetas": []
                    },
                    "apiVersion": "content.halo.run/v1alpha1",
                    "kind": "Post",
                    "metadata": {
                        "name": "",
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
        return JsonUtils.jsonToObject(payload, PostRequest.class);
    }
}
