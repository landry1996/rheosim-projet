package com.rheosim.domain.reporting.model;

import java.time.Instant;

public record DublinCoreMetadata(
        String title,
        String creator,
        String subject,
        String description,
        String publisher,
        Instant date,
        String type,
        String format,
        String identifier,
        String language,
        String rights
) {
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String title;
        private String creator;
        private String subject;
        private String description;
        private String publisher = "RheoSim Enterprise";
        private Instant date = Instant.now();
        private String type = "Dataset";
        private String format = "application/pdf";
        private String identifier;
        private String language = "en";
        private String rights = "All rights reserved";

        public Builder title(String title) { this.title = title; return this; }
        public Builder creator(String creator) { this.creator = creator; return this; }
        public Builder subject(String subject) { this.subject = subject; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder publisher(String publisher) { this.publisher = publisher; return this; }
        public Builder date(Instant date) { this.date = date; return this; }
        public Builder type(String type) { this.type = type; return this; }
        public Builder format(String format) { this.format = format; return this; }
        public Builder identifier(String identifier) { this.identifier = identifier; return this; }
        public Builder language(String language) { this.language = language; return this; }
        public Builder rights(String rights) { this.rights = rights; return this; }

        public DublinCoreMetadata build() {
            return new DublinCoreMetadata(title, creator, subject, description,
                    publisher, date, type, format, identifier, language, rights);
        }
    }
}
