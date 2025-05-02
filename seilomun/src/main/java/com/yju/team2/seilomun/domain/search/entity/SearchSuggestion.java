package com.yju.team2.seilomun.domain.search.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.CompletionField;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.core.suggest.Completion;

@Document(indexName = "search_suggestions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchSuggestion {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String keyword;

    @Field(type = FieldType.Long)
    private Long weight;

    @CompletionField(maxInputLength = 100)
    private Completion suggest;

    public static SearchSuggestion from(String keyword, Long weight) {
        return SearchSuggestion.builder()
                .keyword(keyword)
                .weight(weight)
                .suggest(new Completion(new String[]{keyword}))
                .build();
    }
}