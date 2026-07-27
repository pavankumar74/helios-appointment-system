package com.hellodoctor.helios;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class AuthAppointmentFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String json(Map<String, ?> body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    private JsonNode register(String name, String email, String role) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("email", email);
        body.put("password", "Password123");
        body.put("role", role);
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    @Test
    void patientCanBookAndDoctorCanSeeAppointment() throws Exception {
        JsonNode patient = register("Pat Ient", "patient1@example.com", "PATIENT");
        JsonNode doctor = register("Doc Tor", "doctor1@example.com", "DOCTOR");

        String patientToken = patient.get("token").asText();
        String doctorToken = doctor.get("token").asText();
        long doctorId = doctor.get("userId").asLong();

        Map<String, Object> booking = new LinkedHashMap<>();
        booking.put("doctorId", doctorId);
        booking.put("scheduledAt", LocalDateTime.now().plusDays(1).withNano(0).toString());
        booking.put("notes", "Routine check-up");

        mockMvc.perform(post("/api/appointments")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(booking)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.doctorId", is((int) doctorId)));

        // Doctor should see exactly one appointment in their list.
        mockMvc.perform(get("/api/appointments")
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].patientName", is("Pat Ient")));
    }

    @Test
    void doctorCannotBookAppointment() throws Exception {
        JsonNode doctor = register("Doc Two", "doctor2@example.com", "DOCTOR");
        String doctorToken = doctor.get("token").asText();

        Map<String, Object> booking = new LinkedHashMap<>();
        booking.put("doctorId", doctor.get("userId").asLong());
        booking.put("scheduledAt", LocalDateTime.now().plusDays(1).withNano(0).toString());

        mockMvc.perform(post("/api/appointments")
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(booking)))
                .andExpect(status().isForbidden());
    }

    @Test
    void patientCannotAccessAdminUserList() throws Exception {
        JsonNode patient = register("Pat Two", "patient2@example.com", "PATIENT");
        String token = patient.get("token").asText();

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/appointments"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void duplicateEmailRegistrationIsRejected() throws Exception {
        register("First User", "dupe@example.com", "PATIENT");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "Second User");
        body.put("email", "dupe@example.com");
        body.put("password", "Password123");
        body.put("role", "PATIENT");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isConflict());
    }
}
