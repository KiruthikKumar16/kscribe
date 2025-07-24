package com.kscribe.nlp;

import java.util.List;

public interface NlpService {
    List<String> tokenize(String text);
    List<String> posTag(String text);
    List<String> namedEntities(String text);
    List<String> sentences(String text);
    List<String> lemmatize(String text);
} 