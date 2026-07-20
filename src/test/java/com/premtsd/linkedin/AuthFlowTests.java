package com.premtsd.linkedin;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end HTTP test of the monolith's auth: signup mints a JWT, the JWT unlocks
 * a protected endpoint, and a missing token is rejected. Exercises the real
 * Spring Security chain (JwtAuthFilter -> SecurityContext), no mocks.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowTests {

    @Autowired
    MockMvc mvc;

    @Test
    void signup_then_login_then_access_protected_endpoint() throws Exception {
        String signup = """
                {"name":"Grace","email":"grace@example.com","password":"hunter2secret"}""";

        String body = mvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON).content(signup))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        String token = JsonPath.read(body, "$.token");

        // login returns a token too
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"grace@example.com","password":"hunter2secret"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());

        // protected endpoint: rejected without a token...
        mvc.perform(get("/api/notifications"))
                .andExpect(status().is4xxClientError());

        // ...and allowed with one
        mvc.perform(get("/api/notifications").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void duplicate_signup_is_rejected() throws Exception {
        String signup = """
                {"name":"Dup","email":"dup@example.com","password":"password123"}""";

        mvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON).content(signup))
                .andExpect(status().isOk());

        mvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON).content(signup))
                .andExpect(status().is4xxClientError());
    }
}
