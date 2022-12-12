package com.example.pdfreader2;

import static android.content.Context.TEXT_SERVICES_MANAGER_SERVICE;

import android.content.Context;
import android.view.textservice.SentenceSuggestionsInfo;
import android.view.textservice.SpellCheckerSession;
import android.view.textservice.SuggestionsInfo;
import android.view.textservice.TextInfo;
import android.view.textservice.TextServicesManager;
import android.widget.TextView;

import java.util.Locale;

public class SpellingsClient implements SpellCheckerSession.SpellCheckerSessionListener {

    private TextView suggestions;



    @Override
    public void onGetSuggestions(SuggestionsInfo[] results) {
    }

    @Override
    public void onGetSentenceSuggestions(SentenceSuggestionsInfo[] sentenceSuggestionsInfos) {

    }


    public void fetchSuggestionsFor(String input, Context context){
        TextServicesManager tsm =
                (TextServicesManager) context.getSystemService(TEXT_SERVICES_MANAGER_SERVICE);

        SpellCheckerSession session =
                tsm.newSpellCheckerSession(null, Locale.ENGLISH, this, true);

        session.getSentenceSuggestions(
                new TextInfo[]{ new TextInfo(input) }, 5);



    }
}
