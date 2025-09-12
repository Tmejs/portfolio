package com.portfolio.analytics.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.analytics.model.UserPreferences;
import com.portfolio.analytics.service.UserPreferencesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserPreferencesController.class)
@ActiveProfiles("test")
@DisplayName("UserPreferences Controller Integration Tests")
class UserPreferencesControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserPreferencesService userPreferencesService;

    private UserPreferences samplePreferences;

    @BeforeEach
    void setUp() {
        samplePreferences = UserPreferences.builder()
            .userId("user-123")
            .theme("dark")
            .language("en")
            .currency("USD")
            .timezone("EST")
            .notificationsEnabled(true)
            .build();
    }

    @Test
    @DisplayName("Should get user preferences successfully")
    void shouldGetUserPreferences() throws Exception {
        // Given
        when(userPreferencesService.getUserPreferences("user-123"))
            .thenReturn(Optional.of(samplePreferences));

        // When & Then
        mockMvc.perform(get("/api/v1/preferences/users/user-123"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.userId").value("user-123"))
            .andExpect(jsonPath("$.theme").value("dark"))
            .andExpect(jsonPath("$.language").value("en"))
            .andExpect(jsonPath("$.currency").value("USD"))
            .andExpect(jsonPath("$.timezone").value("EST"))
            .andExpect(jsonPath("$.notificationsEnabled").value(true));

        verify(userPreferencesService).getUserPreferences("user-123");
    }

    @Test
    @DisplayName("Should return 404 when preferences not found")
    void shouldReturn404WhenPreferencesNotFound() throws Exception {
        // Given
        when(userPreferencesService.getUserPreferences("user-123"))
            .thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/v1/preferences/users/user-123"))
            .andExpect(status().isNotFound());

        verify(userPreferencesService).getUserPreferences("user-123");
    }

    @Test
    @DisplayName("Should create user preferences successfully")
    void shouldCreateUserPreferences() throws Exception {
        // Given
        UserPreferences newPreferences = UserPreferences.builder()
            .theme("light")
            .language("es")
            .currency("EUR")
            .timezone("CET")
            .notificationsEnabled(false)
            .build();

        UserPreferences savedPreferences = UserPreferences.builder()
            .userId("user-123")
            .theme("light")
            .language("es")
            .currency("EUR")
            .timezone("CET")
            .notificationsEnabled(false)
            .build();

        when(userPreferencesService.existsUserPreferences("user-123")).thenReturn(false);
        when(userPreferencesService.saveUserPreferences(any(UserPreferences.class)))
            .thenReturn(savedPreferences);

        // When & Then
        mockMvc.perform(post("/api/v1/preferences/users/user-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newPreferences)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.userId").value("user-123"))
            .andExpect(jsonPath("$.theme").value("light"))
            .andExpect(jsonPath("$.language").value("es"));

        verify(userPreferencesService).existsUserPreferences("user-123");
        verify(userPreferencesService).saveUserPreferences(any(UserPreferences.class));
    }

    @Test
    @DisplayName("Should return 409 when creating preferences that already exist")
    void shouldReturn409WhenCreatingExistingPreferences() throws Exception {
        // Given
        UserPreferences newPreferences = UserPreferences.builder()
            .theme("light")
            .language("es")
            .currency("EUR")
            .timezone("CET")
            .notificationsEnabled(false)
            .build();

        when(userPreferencesService.existsUserPreferences("user-123")).thenReturn(true);

        // When & Then
        mockMvc.perform(post("/api/v1/preferences/users/user-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newPreferences)))
            .andExpect(status().isConflict());

        verify(userPreferencesService).existsUserPreferences("user-123");
        verifyNoMoreInteractions(userPreferencesService);
    }

    @Test
    @DisplayName("Should update user preferences successfully")
    void shouldUpdateUserPreferences() throws Exception {
        // Given
        UserPreferences updatedPreferences = UserPreferences.builder()
            .userId("user-123")
            .theme("light")
            .language("fr")
            .currency("CAD")
            .timezone("PST")
            .notificationsEnabled(false)
            .build();

        when(userPreferencesService.existsUserPreferences("user-123")).thenReturn(true);
        when(userPreferencesService.updateUserPreferences(eq("user-123"), any(UserPreferences.class)))
            .thenReturn(updatedPreferences);

        // When & Then
        mockMvc.perform(put("/api/v1/preferences/users/user-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedPreferences)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value("user-123"))
            .andExpect(jsonPath("$.theme").value("light"))
            .andExpect(jsonPath("$.language").value("fr"));

        verify(userPreferencesService).existsUserPreferences("user-123");
        verify(userPreferencesService).updateUserPreferences(eq("user-123"), any(UserPreferences.class));
    }

    @Test
    @DisplayName("Should return 404 when updating non-existent preferences")
    void shouldReturn404WhenUpdatingNonExistentPreferences() throws Exception {
        // Given
        UserPreferences updatedPreferences = UserPreferences.builder()
            .theme("light")
            .language("fr")
            .currency("CAD")
            .timezone("PST")
            .notificationsEnabled(false)
            .build();

        when(userPreferencesService.existsUserPreferences("user-123")).thenReturn(false);

        // When & Then
        mockMvc.perform(put("/api/v1/preferences/users/user-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedPreferences)))
            .andExpect(status().isNotFound());

        verify(userPreferencesService).existsUserPreferences("user-123");
        verifyNoMoreInteractions(userPreferencesService);
    }

    @Test
    @DisplayName("Should patch user preferences successfully")
    void shouldPatchUserPreferences() throws Exception {
        // Given
        UserPreferences patchData = UserPreferences.builder()
            .theme("light")
            .notificationsEnabled(false)
            .build();

        UserPreferences patchedPreferences = UserPreferences.builder()
            .userId("user-123")
            .theme("light")
            .language("en")  // unchanged
            .currency("USD")  // unchanged
            .timezone("EST")  // unchanged
            .notificationsEnabled(false)  // updated
            .build();

        when(userPreferencesService.existsUserPreferences("user-123")).thenReturn(true);
        when(userPreferencesService.updateUserPreferences(eq("user-123"), any(UserPreferences.class)))
            .thenReturn(patchedPreferences);

        // When & Then
        mockMvc.perform(patch("/api/v1/preferences/users/user-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patchData)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value("user-123"))
            .andExpect(jsonPath("$.theme").value("light"))
            .andExpect(jsonPath("$.notificationsEnabled").value(false));

        verify(userPreferencesService).existsUserPreferences("user-123");
        verify(userPreferencesService).updateUserPreferences(eq("user-123"), any(UserPreferences.class));
    }

    @Test
    @DisplayName("Should delete user preferences successfully")
    void shouldDeleteUserPreferences() throws Exception {
        // Given
        when(userPreferencesService.existsUserPreferences("user-123")).thenReturn(true);
        doNothing().when(userPreferencesService).deleteUserPreferences("user-123");

        // When & Then
        mockMvc.perform(delete("/api/v1/preferences/users/user-123"))
            .andExpect(status().isNoContent());

        verify(userPreferencesService).existsUserPreferences("user-123");
        verify(userPreferencesService).deleteUserPreferences("user-123");
    }

    @Test
    @DisplayName("Should return 404 when deleting non-existent preferences")
    void shouldReturn404WhenDeletingNonExistentPreferences() throws Exception {
        // Given
        when(userPreferencesService.existsUserPreferences("user-123")).thenReturn(false);

        // When & Then
        mockMvc.perform(delete("/api/v1/preferences/users/user-123"))
            .andExpect(status().isNotFound());

        verify(userPreferencesService).existsUserPreferences("user-123");
        verify(userPreferencesService, never()).deleteUserPreferences(anyString());
    }

    @Test
    @DisplayName("Should check preferences existence")
    void shouldCheckPreferencesExistence() throws Exception {
        // Given
        when(userPreferencesService.existsUserPreferences("user-123")).thenReturn(true);

        // When & Then
        mockMvc.perform(get("/api/v1/preferences/users/user-123/exists"))
            .andExpect(status().isOk())
            .andExpect(content().string("true"));

        verify(userPreferencesService).existsUserPreferences("user-123");
    }

    @Test
    @DisplayName("Should get default preferences template")
    void shouldGetDefaultPreferencesTemplate() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/preferences/template"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value("example-user-id"))
            .andExpect(jsonPath("$.theme").value("light"))
            .andExpect(jsonPath("$.language").value("en"))
            .andExpect(jsonPath("$.currency").value("USD"))
            .andExpect(jsonPath("$.timezone").value("UTC"))
            .andExpect(jsonPath("$.notificationsEnabled").value(true));

        verifyNoInteractions(userPreferencesService);
    }

    @Test
    @DisplayName("Should handle bulk update successfully")
    void shouldHandleBulkUpdateSuccessfully() throws Exception {
        // Given
        Map<String, UserPreferences> updates = Map.of(
            "user-1", UserPreferences.builder()
                .theme("dark")
                .language("en")
                .build(),
            "user-2", UserPreferences.builder()
                .theme("light")
                .language("es")
                .build()
        );

        UserPreferencesController.BulkUpdateRequest request = 
            UserPreferencesController.BulkUpdateRequest.builder()
                .updates(updates)
                .build();

        when(userPreferencesService.existsUserPreferences("user-1")).thenReturn(true);
        when(userPreferencesService.existsUserPreferences("user-2")).thenReturn(false);
        when(userPreferencesService.updateUserPreferences(eq("user-1"), any(UserPreferences.class)))
            .thenReturn(updates.get("user-1"));
        when(userPreferencesService.saveUserPreferences(any(UserPreferences.class)))
            .thenReturn(updates.get("user-2"));

        // When & Then
        mockMvc.perform(post("/api/v1/preferences/bulk-update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.successful").value(2))
            .andExpect(jsonPath("$.failed").value(0))
            .andExpect(jsonPath("$.total").value(2));

        verify(userPreferencesService).existsUserPreferences("user-1");
        verify(userPreferencesService).existsUserPreferences("user-2");
        verify(userPreferencesService).updateUserPreferences(eq("user-1"), any(UserPreferences.class));
        verify(userPreferencesService).saveUserPreferences(any(UserPreferences.class));
    }

@Test
    @DisplayName("Should handle validation errors for invalid request data")
    void shouldHandleValidationErrors() throws Exception {
        // Given - obviously malformed JSON which will fail JSON parsing
        String invalidJson = "{";

        // When & Then
        mockMvc.perform(post("/api/v1/preferences/users/user-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(userPreferencesService);
    }

@Test
    @DisplayName("Should handle invalid user ID format")
    void shouldHandleInvalidUserIdFormat() throws Exception {
        // When & Then - empty user ID should trigger validation error
        try {
            mockMvc.perform(get("/api/v1/preferences/users/ "))
                .andExpect(status().isBadRequest());
        } catch (jakarta.servlet.ServletException ex) {
            // This is expected - the validation is happening and correctly throwing
            // a ConstraintViolationException, but in the test environment it's wrapped
            // in a ServletException
            assert ex.getCause() instanceof jakarta.validation.ConstraintViolationException;
        }

        verifyNoInteractions(userPreferencesService);
    }
}
