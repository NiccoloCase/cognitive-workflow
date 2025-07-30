package org.caselli.cognitiveworkflow.operational.utils;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.time.Duration;

/**
 * Custom JSON serializer for Duration objects that converts them to milliseconds.
 * @author niccolocaselli
 */
public class DurationToMillisSerializer extends JsonSerializer<Duration> {

    @Override
    public void serialize(Duration duration, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
            throws IOException {
        if (duration != null) {
            jsonGenerator.writeNumber(duration.toMillis());
        } else {
            jsonGenerator.writeNull();
        }
    }
}