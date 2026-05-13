-- V2: Marketplace plugin tables

CREATE TABLE marketplace_plugins (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    slug VARCHAR(200) UNIQUE NOT NULL,
    description TEXT,
    author_id UUID NOT NULL REFERENCES users(id),
    license VARCHAR(50),
    tags TEXT[],
    downloads_count INTEGER DEFAULT 0,
    rating_average DECIMAL(3,2) DEFAULT 0,
    rating_count INTEGER DEFAULT 0,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE plugin_versions (
    id UUID PRIMARY KEY,
    plugin_id UUID NOT NULL REFERENCES marketplace_plugins(id) ON DELETE CASCADE,
    version VARCHAR(50) NOT NULL,
    artifact_url VARCHAR(1000) NOT NULL,
    artifact_hash VARCHAR(128) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    changelog TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(plugin_id, version)
);

CREATE TABLE plugin_reviews (
    id UUID PRIMARY KEY,
    plugin_id UUID NOT NULL REFERENCES marketplace_plugins(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id),
    rating INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(plugin_id, user_id)
);

-- Indexes
CREATE INDEX idx_plugins_author ON marketplace_plugins(author_id);
CREATE INDEX idx_plugins_slug ON marketplace_plugins(slug);
CREATE INDEX idx_plugins_status ON marketplace_plugins(status);
CREATE INDEX idx_plugins_tags ON marketplace_plugins USING GIN(tags);
CREATE INDEX idx_plugin_versions_plugin ON plugin_versions(plugin_id);
CREATE INDEX idx_plugin_versions_status ON plugin_versions(status);
CREATE INDEX idx_plugin_reviews_plugin ON plugin_reviews(plugin_id);
CREATE INDEX idx_plugin_reviews_user ON plugin_reviews(user_id);

-- Full-text search index
CREATE INDEX idx_plugins_search ON marketplace_plugins
    USING GIN(to_tsvector('english', name || ' ' || COALESCE(description, '')));
