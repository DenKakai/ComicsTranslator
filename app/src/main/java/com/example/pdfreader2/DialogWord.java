package com.example.pdfreader2;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;

import org.w3c.dom.Text;

import java.util.Objects;

public class DialogWord extends AppCompatDialogFragment {

    private final String word;
    private final String translation;
    private TextView helloTextView;

    public DialogWord(String word, String translation) {
        this.word = word;
        this.translation = translation;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_word, null);
        helloTextView = view.findViewById(R.id.translation);
        helloTextView.setText(word + " - " + translation);

        builder.setView(view)
                .setTitle("Tłumaczenie")
                .setPositiveButton("ok", (dialogInterface, i) -> {

                });

        return builder.create();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }
}
