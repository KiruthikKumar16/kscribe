package com.kscribe.nlp;

import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.lemmatizer.DictionaryLemmatizer;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class OpenNlpService implements NlpService {
    private TokenizerME tokenizer;
    private SentenceDetectorME sentenceDetector;
    private POSTaggerME posTagger;
    private DictionaryLemmatizer lemmatizer;

    public OpenNlpService() {
        try {
            InputStream tokenModelIn = getClass().getResourceAsStream("/nlp-models/en-token.bin");
            InputStream sentenceModelIn = getClass().getResourceAsStream("/nlp-models/en-sent.bin");
            InputStream posModelIn = getClass().getResourceAsStream("/nlp-models/en-pos-maxent.bin");
            InputStream lemmaModelIn = getClass().getResourceAsStream("/nlp-models/en-lemmatizer.bin");
            if (tokenModelIn != null && sentenceModelIn != null) {
                tokenizer = new TokenizerME(new TokenizerModel(tokenModelIn));
                sentenceDetector = new SentenceDetectorME(new SentenceModel(sentenceModelIn));
            }
            if (posModelIn != null) {
                posTagger = new POSTaggerME(new POSModel(posModelIn));
            }
            if (lemmaModelIn != null) {
                lemmatizer = new DictionaryLemmatizer(lemmaModelIn);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<String> tokenize(String text) {
        if (tokenizer == null) return Collections.emptyList();
        return Arrays.asList(tokenizer.tokenize(text));
    }

    @Override
    public List<String> posTag(String text) {
        if (posTagger == null || tokenizer == null) return tokenize(text);
        String[] tokens = tokenizer.tokenize(text);
        String[] tags = posTagger.tag(tokens);
        List<String> result = new java.util.ArrayList<>();
        for (int i = 0; i < tokens.length; i++) {
            result.add(tokens[i] + "/" + tags[i]);
        }
        return result;
    }

    @Override
    public List<String> namedEntities(String text) {
        // Placeholder: Implement NER
        return Collections.emptyList();
    }

    @Override
    public List<String> sentences(String text) {
        if (sentenceDetector == null) return Collections.singletonList(text);
        return Arrays.asList(sentenceDetector.sentDetect(text));
    }

    @Override
    public List<String> lemmatize(String text) {
        if (lemmatizer == null || tokenizer == null || posTagger == null) return tokenize(text);
        String[] tokens = tokenizer.tokenize(text);
        String[] tags = posTagger.tag(tokens);
        String[] lemmas = lemmatizer.lemmatize(tokens, tags);
        List<String> result = new java.util.ArrayList<>();
        for (int i = 0; i < tokens.length; i++) {
            result.add(tokens[i] + ":" + lemmas[i]);
        }
        return result;
    }
} 