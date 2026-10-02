package com.diwan.common.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    @RestController
    static class Probe {
        @GetMapping("/header")
        String header(@RequestHeader("X-User-Id") Long id) { return "ok"; }

        @GetMapping("/boom")
        String boom() { throw new IllegalStateException("kaboom"); }

        @GetMapping("/forbidden")
        String forbidden() { throw new ResponseStatusException(HttpStatus.FORBIDDEN, "nope"); }
    }

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new Probe()).setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void missingHeaderStays400() throws Exception {
        mvc.perform(get("/header")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void unknownPathStays404AndWrongMethodStays405() throws Exception {
        mvc.perform(get("/nope")).andExpect(status().isNotFound());
        mvc.perform(post("/header")).andExpect(status().isMethodNotAllowed());
    }

    @Test
    void responseStatusExceptionKeepsItsStatusAndReason() throws Exception {
        mvc.perform(get("/forbidden")).andExpect(status().isForbidden()).andExpect(jsonPath("$.message").value("nope"));
    }

    @Test
    void unexpectedExceptionIs500WithMessage() throws Exception {
        mvc.perform(get("/boom")).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false)).andExpect(jsonPath("$.message").value("kaboom"));
    }
}
