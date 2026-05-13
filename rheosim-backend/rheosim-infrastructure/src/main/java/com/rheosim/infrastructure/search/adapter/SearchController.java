package com.rheosim.infrastructure.search.adapter;

import com.rheosim.infrastructure.search.dto.SearchResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

    private final ElasticsearchIndexer indexer;

    public SearchController(ElasticsearchIndexer indexer) {
        this.indexer = indexer;
    }

    @GetMapping
    public ResponseEntity<SearchResult> search(
            @RequestParam String q,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        SearchResult result = indexer.search(q, type, page, size);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/suggest")
    public ResponseEntity<List<String>> suggest(
            @RequestParam String q,
            @RequestParam(defaultValue = "5") int limit) {
        List<String> suggestions = indexer.suggest(q, limit);
        return ResponseEntity.ok(suggestions);
    }

    @PostMapping("/reindex")
    public ResponseEntity<Void> reindex() {
        indexer.reindexAll();
        return ResponseEntity.accepted().build();
    }
}
