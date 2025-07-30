package org.caselli.cognitiveworkflow.API.controllers;

import jakarta.validation.Valid;
import org.caselli.cognitiveworkflow.knowledge.MOP.IntentMetamodelService;
import org.caselli.cognitiveworkflow.knowledge.model.intent.IntentMetamodel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing intent operations.
 * @author niccolocaselli
 */
@Validated
@RestController
@RequestMapping("/api/intents")
public class IntentController {

    private final IntentMetamodelService intentService;

    @Autowired
    public IntentController(IntentMetamodelService intentService) {
        this.intentService = intentService;
    }

    @GetMapping
    public ResponseEntity<List<IntentMetamodel>> getAllIntents() {
        List<IntentMetamodel> intents = intentService.findAll();
        return ResponseEntity.ok(intents);
    }

    @GetMapping("/{id}")
    public ResponseEntity<IntentMetamodel> getIntentById(@PathVariable String id) {
        Optional<IntentMetamodel> intent = intentService.findById(id);
        return intent.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<IntentMetamodel> createIntent(@Valid @RequestBody IntentMetamodel intent) {
        try {
            IntentMetamodel createdIntent = intentService.create(intent);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdIntent);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<IntentMetamodel> updateIntent(@PathVariable String id,
            @Valid @RequestBody IntentMetamodel intent) {
        try {
            intent.setId(id);
            IntentMetamodel updatedIntent = intentService.update(intent);
            return ResponseEntity.ok(updatedIntent);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIntent(@PathVariable String id) {
        if (!intentService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        intentService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}