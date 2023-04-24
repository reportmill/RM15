/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.graphics;
import com.wintertree.ssce.*;
import snap.text.SpellCheck;
import java.io.InputStream;
import java.util.*;

/**
 * This class provides generic spell check API ideal for RM's needs. The abstraction should let us
 * plug in any spell check technology.
 */
public class RMSpellCheck extends SpellCheck {

    // An object that can pull a word from a string
    private StringWordParser _parser;

    // An object that can find the next misspelled word from a parser
    private SpellingSession _speller;

    // I can't figure this out, but needed for WinterTree SpellingSession check()
    private StringBuffer _otherWord;

    /**
     * Constructor.
     */
    public RMSpellCheck()
    {
        super();
        _parser = new StringWordParser(true);
        _otherWord = new StringBuffer();
    }

    /**
     * Returns the first misspelled word in a given string starting at the given index (or null or no misspelled words).
     */
    @Override
    public Word getNextMisspelledWord(String aString, int anIndex)
    {
        // If given index is already beyond string bounds, just return null
        if (anIndex >= aString.length()) return null;

        // Configure word parser for given string and index
        _parser.setText(aString);
        _parser.setCursor(anIndex);

        // Get Speller and declare variable for check result
        SpellingSession speller = getSpeller();
        int checkResult;

        // Iterate over words
        while (((checkResult = speller.check(_parser, _otherWord)) & SpellingSession.END_OF_TEXT_RSLT) == 0) {

            // If misspelled, configure word and return
            if ((checkResult & SpellingSession.MISSPELLED_WORD_RSLT) != 0) {
                int wordStart = _parser.getCursor();
                String wordString = _parser.nextWord();
                return new Word(wordString, wordStart);
            }

            // Find next word
            _parser.nextWord();
        }

        // Return no misspelled words
        return null;
    }

    /**
     * Returns a list of suggestions of corrections for given word.
     */
    @Override
    protected List<String> getSuggestionsForWord(snap.text.SpellCheck.Word aWord)
    {
        // Get speller, create suggestion set and fill with suggestions
        SpellingSession speller = getSpeller();
        String wordStr = aWord.getString();
        SuggestionSet set = new SuggestionSet(8);
        speller.suggest(wordStr, SpellingSession.MAX_SUGGEST_DEPTH, new EnglishPhoneticComparator(), set);

        // Create list and fill from suggestion set
        List<String> suggestions = new ArrayList<>();
        for (Enumeration<String> e = set.words(); e.hasMoreElements(); )
            suggestions.add(e.nextElement());

        // Return
        return suggestions;
    }

    /**
     * Creates and returns a speller.
     */
    private SpellingSession getSpeller()
    {
        // If already set, just return
        if (_speller != null) return _speller;

        // Create
        try {

            // Set license key
            LicenseKey.setKey(0x80CD5259);

            // Get lexicons
            InputStream is1 = RMSpellCheck.class.getResourceAsStream("/com/wintertree/ssceam.tlx");
            StreamTextLexicon lex1 = new StreamTextLexicon(is1);
            InputStream is2 = RMSpellCheck.class.getResourceAsStream("/com/wintertree/ssceam2.clx");
            CompressedLexicon lex2 = new CompressedLexicon(is2);
            InputStream is3 = RMSpellCheck.class.getResourceAsStream("/com/wintertree/tech.tlx");
            StreamTextLexicon lex3 = new StreamTextLexicon(is3);

            // Get speller
            _speller = new SpellingSession();

            // Set lexicons
            _speller.setLexicons(new Lexicon[]{lex1, lex2, lex3});
        }

        // Catch exceptions
        catch (Exception e) { e.printStackTrace(); }

        // Return
        return _speller;
    }
}