package com.agenticplayer.safety.recall;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CpscRecall(
        @JsonProperty("RecallID") Long recallId,
        @JsonProperty("RecallNumber") String recallNumber,
        @JsonProperty("RecallDate") LocalDateTime recallDate,
        @JsonProperty("Title") String title,
        @JsonProperty("Description") String description,
        @JsonProperty("URL") String url,
        @JsonProperty("ConsumerContact") String consumerContact,
        @JsonProperty("Products") List<NamedValue> products,
        @JsonProperty("Images") List<RecallImage> images,
        @JsonProperty("Injuries") List<NamedValue> injuries,
        @JsonProperty("Manufacturers") List<NamedValue> manufacturers,
        @JsonProperty("Retailers") List<NamedValue> retailers,
        @JsonProperty("Hazards") List<NamedValue> hazards,
        @JsonProperty("Remedies") List<NamedValue> remedies,
        @JsonProperty("RemedyOptions") List<RemedyOption> remedyOptions,
        @JsonProperty("ProductUPCs") List<ProductUpc> productUpcs) {

    public CpscRecall {
        products = safe(products);
        images = safe(images);
        injuries = safe(injuries);
        manufacturers = safe(manufacturers);
        retailers = safe(retailers);
        hazards = safe(hazards);
        remedies = safe(remedies);
        remedyOptions = safe(remedyOptions);
        productUpcs = safe(productUpcs);
    }

    private static <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record NamedValue(
            @JsonProperty("Name") String name,
            @JsonProperty("Model") String model,
            @JsonProperty("NumberOfUnits") String numberOfUnits) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RecallImage(
            @JsonProperty("URL") String url,
            @JsonProperty("Caption") String caption) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RemedyOption(@JsonProperty("Option") String option) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProductUpc(@JsonProperty("UPC") String upc) {
    }
}
