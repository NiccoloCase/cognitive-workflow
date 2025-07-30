package org.caselli.cognitiveworkflow.operational.observability;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Data;
import org.caselli.cognitiveworkflow.operational.utils.DurationToMillisSerializer;
import java.time.Instant;
import java.time.Duration;

/**
 * Abstract base class for Observability Tracing
 * @author niccolocaselli
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public abstract class ObservabilityReport {

    protected boolean success;
    protected String errorMessage;
    protected Throwable exception;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    protected final Instant startTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    protected Instant endTime;

    @JsonSerialize(using = DurationToMillisSerializer.class)
    protected Duration totalExecutionTime;

    public ObservabilityReport() {
        this.startTime = Instant.now();
        this.success = false;
    }

    /**
     * Convert the class to a JSON string
     * @return A JSON String
     */
    public String toJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            mapper.disable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS);
            mapper.enable(SerializationFeature.INDENT_OUTPUT);

            return mapper.writeValueAsString(this);
        } catch (Exception e) {
            return "Error serializing to JSON: " + e.getMessage();
        }
    }

    /**
     * Mark the execution as completed
     * @param success If the execution completed with success
     * @param errorMessage An error message
     * @param exception An exception
     */
    public void markCompleted(boolean success, String errorMessage, Throwable exception) {
        this.endTime = Instant.now();
        this.totalExecutionTime = Duration.between(startTime, endTime);
        this.success = success;
        this.errorMessage = errorMessage;
        this.exception = exception;
    }
}