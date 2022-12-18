package com.example.pdfreader2;

import static java.lang.Character.isDigit;
import static java.lang.Character.isLetter;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class WordCheck {
    String test;

    public String getTest() {
        return test;
    }

    public void setTest(String test) {
        this.test = test;
    }

    public static String removeSingleChars(String result){
        result = result.replace("\n", " ");

        String[] words_array = result.split(" ");
        List<String> words_list = new ArrayList<String>();
        for (String i : words_array) {
            StringBuilder cur_str = new StringBuilder();
            String cleared_word = "";
            for (int j = 0; j < i.length(); j++) {
                char cur_char = i.charAt(j);
                if (cur_char == '|') {cur_char = 'I';}
                if (cur_char == '’') {cur_char = '\'';}
                if (isLetter(cur_char)) {
                    cur_str.append(cur_char);
                    continue;
                }
                if (isDigit(cur_char)) {
                    cur_str.append(cur_char);
                    continue;
                }
                if (cur_char == '.' | cur_char == '\'' | cur_char == '-' | cur_char == ',' | cur_char == '?' | cur_char == '!') {
                    cur_str.append(cur_char);
                    continue;
                }
            }
            cleared_word = cur_str.toString();
            if (cleared_word.length() == 1 & !cleared_word.toUpperCase().equals("I") & !cleared_word.toUpperCase().equals("A") & !cleared_word.toUpperCase().equals("O")) {
                continue;
            }
            if (cleared_word.chars().filter(ch -> isLetter(ch)).count() == 1) {continue;}
            words_list.add(cleared_word);
        }
        StringBuilder builder = new StringBuilder();
        for (String ch : words_list) {
            builder.append(ch);
            builder.append(" ");
        }
        String str = builder.toString();
        return str;
    }

    public static List<Rectangle> words_position(String result, String boxes) {
        String[] words_list = result.split(" ");
        List<String> boxes_list = Arrays.asList(boxes.split("\n"));
        int it = 0;
        List<Rectangle> resultList = new ArrayList<>();
        for (String cur_word : words_list) {

            int cur_word_length = cur_word.length();
            if (cur_word_length==1 & !cur_word.equals("I") & !cur_word.equals("A")) {
                it+=1;
                continue;
            }
            while (boxes_list.get(it).charAt(0) != cur_word.charAt(0)) {
                it+=1;
            }

            String first_char = boxes_list.get(it);
            String[] first_char_coordinates = first_char.split(" ");
            String last_char = boxes_list.get(it + cur_word_length -1);
            String[] last_char_coordinates = last_char.split(" ");

            Rectangle tmp = new Rectangle(Integer.parseInt(first_char_coordinates[1]), Integer.parseInt(first_char_coordinates[2]), Integer.parseInt(last_char_coordinates[3]), Integer.parseInt(last_char_coordinates[4]));
            Log.d("Pawlak", String.valueOf(tmp) + " | " + cur_word);
            tmp.setText(cur_word);

            resultList.add(tmp);
            it+=cur_word_length;
        }
        return resultList;
    }

}