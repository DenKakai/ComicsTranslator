package com.example.pdfreader2;

import static java.lang.Character.isDigit;
import static java.lang.Character.isLetter;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class WordCheck {

    public static String removeSingleChars(String result){
        result = result.replace("\n", " ");
        result = result.toUpperCase();
        result = result.replace("_", " ");
        result = result.replace("—", "-");
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
            Log.d("slowo", cleared_word);
            if (cleared_word.chars().filter(ch -> isLetter(ch)).count() == 1) {
                String string1 = cleared_word.chars().filter(ch -> isLetter(ch)).boxed().map(a -> String.valueOf(a)).collect(Collectors.joining());
                Log.d("pierwsyz if", string1);
                if (!string1.equals("65") & !string1.equals("73") & !string1.equals("79")) {
                    Log.d("drugi if", cleared_word);
                    continue;
                }
            }
            Log.d("slowo2", cleared_word);
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
        Log.d("result", result);
        Log.d("boxes", boxes);

        String[] words_list = result.split(" ");
        boxes = boxes.replace("|", "I");
        List<String> boxes_list = Arrays.asList(boxes.split("\n"));
        int it = 0;
        List<Rectangle> resultList = new ArrayList<>();
        for (String cur_word : words_list) {
            if (cur_word == " " | cur_word.isEmpty() | Objects.isNull(cur_word)) {continue;}

            int cur_word_length = cur_word.length();
            if (cur_word_length==1 & !cur_word.equals("I") & !cur_word.equals("A")) {
                it+=1;
                continue;
            }
            Log.d("current word", cur_word);

            while (boxes_list.get(it).toUpperCase().charAt(0) != cur_word.charAt(0)
                    | boxes_list.get(it+cur_word_length-1).toUpperCase().charAt(0) != cur_word.charAt(cur_word_length-1)) {
                it+=1;
            }
            String first_char = boxes_list.get(it);
            String[] first_char_coordinates = first_char.split(" ");
            int min_height = Integer.parseInt(first_char_coordinates[2]);
            Log.d("first character", first_char);

            String last_char = boxes_list.get(it + cur_word_length -1);
            String[] last_char_coordinates = last_char.split(" ");
            int max_height = Integer.parseInt(last_char_coordinates[4]);
            Log.d("last char", last_char);

            for (int k = it+1; k < it + cur_word_length -1; k++) {
                String cur_char = boxes_list.get(k);
                String[] cur_char_coordinates = cur_char.split(" ");
                if (Integer.parseInt(cur_char_coordinates[2]) < min_height) {
                    min_height = Integer.parseInt(cur_char_coordinates[2]);
                }
                if (Integer.parseInt(cur_char_coordinates[4]) > max_height) {
                    max_height = Integer.parseInt(cur_char_coordinates[4]);
                }
            }



            Rectangle tmp = new Rectangle(Integer.parseInt(first_char_coordinates[1]), min_height, Integer.parseInt(last_char_coordinates[3]), max_height);
            Log.d("prostokat", String.valueOf(tmp) + " | " + cur_word);
            tmp.setText(cur_word);

            resultList.add(tmp);
            it+=cur_word_length;
        }
        return resultList;
    }

}