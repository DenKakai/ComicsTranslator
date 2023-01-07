package wut.mini.comicstranslator;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory;
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject;
import com.tom_roush.pdfbox.util.PDFBoxResourceLoader;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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
                    if(Arrays.asList("pdf","cbz").contains(FilenameUtils.getExtension(selectedFile.getPath()))) {
                        if(Arrays.asList("pdf").contains(FilenameUtils.getExtension(selectedFile.getPath()))) {
                            Intent intent = new Intent(context, PdfViewerActivity.class);
                            String path = selectedFile.getPath();
                            intent.putExtra("path", path);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(intent);
                        }
                        else if(Arrays.asList("cbz").contains(FilenameUtils.getExtension(selectedFile.getPath()))) {
                            //TODO: trzeba dac komunikat np. z procentami otwirania cbzki bo to zajmuje jakies ~7sekund na tym ktory testowalem
                            //TODO: trzeba posortowac strony od pierwszej do ostatniej bo sa losowo
                            PDFBoxResourceLoader.init(context);

                            PDDocument document = new PDDocument();

                            try {
                                ZipFile zipFile = new ZipFile(selectedFile.getPath());
                                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                                Log.d("xd", "Entries:");

                                // iterate through all the entries
                                while (entries.hasMoreElements()) {
                                    // get the zip entry
                                    ZipEntry entry = entries.nextElement();
                                    InputStream is = zipFile.getInputStream(entry);

                                    PDImageXObject img = JPEGFactory.createFromStream(document, is);
                                    float width = img.getWidth();
                                    float height = img.getHeight();
                                    PDPage page = new PDPage(new PDRectangle(width, height));
                                    document.addPage(page);
                                    PDPageContentStream contentStream = new PDPageContentStream(document, page);
                                    contentStream.drawImage(img, 0, 0);
                                    contentStream.close();
                                    is.close();

                                    // display the entry
                                    Log.d("xd", entry.getName());
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            try {
                                String path = context.getFilesDir().getAbsolutePath();
                                Log.d("xd", path);
                                document.save(path + "/chosenComic.pdf");
                                document.close();

                                /*File directory = new File(path);
                                File[] files = directory.listFiles();
                                Log.d("Files", "Size: "+ files.length);
                                for (int i = 0; i < files.length; i++)
                                {
                                    Log.d("Files", "FileName:" + files[i].getName());
                                }*/

                                Intent intent = new Intent(context, PdfViewerActivity.class);
                                intent.putExtra("path", path + "/chosenComic.pdf");
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                context.startActivity(intent);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    else {
                        Toast.makeText(context,"Wybierz plik .pdf albo .cbz", Toast.LENGTH_SHORT).show();
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
