package wut.mini.comicstranslator;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;

public class ExampleDialog extends AppCompatDialogFragment {

    private NumberPicker numberPickerPage;
    private ExampleDialogListener listener;
    private final int min;
    private final int max;
    private final int curPage;

    public ExampleDialog(int min, int max, int curPage) {
        this.min = min;
        this.max = max;
        this.curPage = curPage;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog, null);

        builder.setView(view)
                .setTitle("Choose page")
                .setNegativeButton("cancel", (dialogInterface, i) -> {

                })
                .setPositiveButton("ok", (dialogInterface, i) -> {
                    int pageNumber = numberPickerPage.getValue();
                    listener.applyTexts(pageNumber);
                });

        numberPickerPage = view.findViewById(R.id.pagePicker);
        numberPickerPage.setMinValue(min);
        numberPickerPage.setMaxValue(max);
        numberPickerPage.setValue(curPage);

        return builder.create();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        try {
            listener = (ExampleDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context +
                    "must implement ExampleDialogListener");
        }
    }

    public interface ExampleDialogListener {
        void applyTexts(int pageNumber);
    }
}
