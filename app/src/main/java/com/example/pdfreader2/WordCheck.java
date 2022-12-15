package com.example.pdfreader2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
            if(i.length()==1 & !i.toUpperCase().equals("I") & !i.toUpperCase().equals("A") & !i.toUpperCase().equals("O")) {
                continue;
            } words_list.add(i);
        }
        StringBuilder builder = new StringBuilder();
        for (String ch : words_list) {
            builder.append(ch);
            builder.append(" ");
        }
        String str = builder.toString();
        return str;
    }

    public static String words_position(String result, String boxes) {
        String[] words_list = result.split(" ");
        List<String> boxes_list = Arrays.asList(boxes.split("\n"));
        int it =0;
        String words_position = "";
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

            String word = cur_word + " " + first_char_coordinates[1] + " " + first_char_coordinates[2]
                    + " " + last_char_coordinates[3] +  " " + last_char_coordinates[4];
            word += "\n";
            words_position += word;


            it+=cur_word_length;
        }
        return words_position;
    }

}