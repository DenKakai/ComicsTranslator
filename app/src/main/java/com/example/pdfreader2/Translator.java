package com.example.pdfreader2;

import android.util.Log;

import java.io.IOException;
import java.net.URLEncoder;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class Translator {
    private final OkHttpClient client = new OkHttpClient();
    String langTo = "pl";
    String langFrom = "en";

    public void run(String text) throws Exception {
        Request request = new Request.Builder()
                .url("https://script.google.com/macros/s/AKfycbyE0UgofkOwPvNWLpUw3Fe0kcAo2-bU66-sqGe2bstr_5zRmXaxkG6TJpVPt0_g8_ca/exec" +
                        "?q=" + URLEncoder.encode(text, "UTF-8") +
                        "&target=" + langTo +
                        "&source=" + langFrom)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

                    Headers responseHeaders = response.headers();
                    for (int i = 0, size = responseHeaders.size(); i < size; i++) {
                        System.out.println(responseHeaders.name(i) + ": " + responseHeaders.value(i));
                    }

                    Log.d("tl", responseBody.string());
                }
            }
        });
    }
}
