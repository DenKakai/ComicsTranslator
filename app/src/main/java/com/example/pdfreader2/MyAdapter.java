package com.example.pdfreader2;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.Arrays;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder>{

    Context context;
    File[] filesAndFolders;
    public MyAdapter(Context context, File[] filesAndFolders) {
        this.context = context;
        this.filesAndFolders = filesAndFolders;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(context).inflate(R.layout.recycler_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MyAdapter.ViewHolder holder, int position) {

        File selectedFile = filesAndFolders[position];

        holder.textView.setText(selectedFile.getName());
        Log.d("TOJESTTEST", FilenameUtils.getExtension(selectedFile.getPath()));

        if(selectedFile.isDirectory()){
            holder.imageView.setImageResource(R.drawable.ic_baseline_folder_24);
        }else {
            holder.imageView.setImageResource(R.drawable.ic_baseline_insert_drive_file_24);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(selectedFile.isDirectory()) {
                    Intent intent = new Intent(context, FileListActivity.class);
                    String path = selectedFile.getPath();
                    intent.putExtra("path", path);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }else {
                    //co ma sie stac po wybraniu pliku
                    if(Arrays.asList("pdf","cbz","cbv").contains(FilenameUtils.getExtension(selectedFile.getPath()))) {
                        //TODO: dodac rozne odpalenie pdf, cbz, cbv ifami
                        Intent intent = new Intent(context, PdfViewerActivity.class);
                        String path = selectedFile.getPath();
                        intent.putExtra("path", path);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                    }
                    else {
                        Toast.makeText(context,"Wybierz plik .pdf, .cbz albo .cbv", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return filesAndFolders.length;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        ImageView imageView;

        public ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.file_name_textview);
            imageView = itemView.findViewById(R.id.icon_view);

        }
    }
}
