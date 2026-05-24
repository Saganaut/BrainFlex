/**
 * Slice test for {@link GlobalExceptionHandler}. A tiny inline @RestController
 * throws one exception per branch; standalone MockMvc wires it to the advice so
 * we can assert the RFC 9457 ProblemDetail contract end-to-end: status,
 * content-type, code/title/detail/instance/traceId, the validation errors[]
 * array, and — critically — that 5xx responses never leak the cause's message
 * or class name.
 */
package cephadex.brainflex.exception;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void notFoundException_ProducesProblemDetail() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail").value("Deck not found"))
                .andExpect(jsonPath("$.code").value("DECK_NOT_FOUND"))
                .andExpect(jsonPath("$.instance").value("/test/not-found"))
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void forbiddenException_ProducesProblemDetail() throws Exception {
        mockMvc.perform(get("/test/forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Forbidden"))
                .andExpect(jsonPath("$.code").value("DECK_EDIT_FORBIDDEN"))
                .andExpect(jsonPath("$.detail").value("You do not have edit access to this deck"));
    }

    @Test
    void legacyResponseStatusException_IsNormalized() throws Exception {
        // The 130+ unmigrated throw-sites still work: code is derived from status.
        mockMvc.perform(get("/test/legacy-rse"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.detail").value("legacy not found"))
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void accessDeniedException_MapsTo403() throws Exception {
        mockMvc.perform(get("/test/access-denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void validationFailure_ProducesFieldErrors() throws Exception {
        mockMvc.perform(post("/test/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].message").exists());
    }

    @Test
    void serverError_IsMaskedAndDoesNotLeak() throws Exception {
        mockMvc.perform(get("/test/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.detail").value("Something went wrong, please try again."))
                .andExpect(jsonPath("$.traceId").exists())
                // The cause's message and class name must never reach the client.
                .andExpect(content().string(not(containsString("super secret"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))));
    }

    @Test
    void responseStatusException5xx_IsAlsoMasked() throws Exception {
        mockMvc.perform(get("/test/rse-5xx"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.detail").value("Something went wrong, please try again."))
                .andExpect(content().string(not(containsString("upstream secret"))));
    }

    // ── inline fixture ────────────────────────────────────────────────────

    @RestController
    static class TestController {

        @GetMapping("/test/not-found")
        String notFound() {
            throw new NotFoundException("DECK_NOT_FOUND", "Deck not found");
        }

        @GetMapping("/test/forbidden")
        String forbidden() {
            throw new ForbiddenException("DECK_EDIT_FORBIDDEN", "You do not have edit access to this deck");
        }

        @GetMapping("/test/legacy-rse")
        String legacyRse() {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "legacy not found");
        }

        @GetMapping("/test/rse-5xx")
        String rse5xx() {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "upstream secret detail");
        }

        @GetMapping("/test/access-denied")
        String accessDenied() {
            throw new AccessDeniedException("nope");
        }

        @PostMapping("/test/validate")
        String validate(@Valid @RequestBody SampleBody body) {
            return "ok";
        }

        @GetMapping("/test/boom")
        String boom() {
            throw new IllegalStateException("super secret internal detail");
        }
    }

    record SampleBody(@NotBlank String name) {
    }
}
