package com.example.webview;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.ResultSet;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ListView articlesList;
    ArrayAdapter<String> articlesAdapter;
    DownloadArticles download;
    ArrayList<String> articleTitles;
    SQLiteDatabase sqLiteDatabase;
    Cursor titles;

    private final String bestStoriesURL = "https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty";
    private final String getStoryURL = "https://hacker-news.firebaseio.com/v0/item/%d.json?print=pretty";

    private class DownloadArticles extends AsyncTask<String, Void, StringBuilder>{

        URL url;
        HttpURLConnection connection;
        InputStream in;
        InputStreamReader reader;

        @Override
        public StringBuilder doInBackground(String... urls){
            StringBuilder result = new StringBuilder("");
            try{
                url = new URL(urls[0]);
                connection = (HttpURLConnection) url.openConnection();
                in = connection.getInputStream();
                reader = new InputStreamReader(in);

                int data = reader.read();
                while(data != -1){
                    result.append((char) data);
                    data = reader.read();
                }
            }
            catch(Exception e){
                e.printStackTrace();
            }
            return result;
        }

        @Override
        protected void onPostExecute(StringBuilder result){
            try{
                JSONArray jsonArray = new JSONArray(result.toString());
                for(int i = 0; i < jsonArray.length(); ++i){
                    String story = String.format(getStoryURL, Integer.parseInt(jsonArray.get(i).toString()));
                    Log.i("ID", story);
                    new DownloadStoryInfo().execute(story);
                }

            }
            catch (Exception e){
                e.printStackTrace();
            }
        }

    }

    private class DownloadStoryInfo extends AsyncTask<String, Void, StringBuilder>{

        URL url;
        HttpURLConnection connection;
        InputStream in;
        InputStreamReader reader;


        @Override
        public StringBuilder doInBackground(String... urls){
            StringBuilder result = new StringBuilder("");

            try{
                url = new URL(urls[0]);
                connection = (HttpURLConnection) url.openConnection();
                in = connection.getInputStream();
                reader = new InputStreamReader(in);

                int data = reader.read();
                while(data != -1){
                    result.append((char) data);
                    data = reader.read();
                }
            }
            catch(Exception e){
                e.printStackTrace();
            }

            return result;
        }

        @Override
        protected void onPostExecute(StringBuilder result){
            String title = "", link = "", id = "";
            JSONObject jsonObject = null;
            try{
                jsonObject = new JSONObject(result.toString());
                title = jsonObject.getString("title");
                link = jsonObject.getString("url");
                id = jsonObject.getString("id");

                String query = String.format("INSERT INTO news (title, url, id) VALUES (\"%s\", \"%s\", %s)", title, link, id);
                sqLiteDatabase.execSQL(query);

                articleTitles.add(title);
                articlesAdapter.notifyDataSetChanged();
            }
            catch(Exception e){
                Log.i("DATA RETURNED", jsonObject.toString());
                e.printStackTrace();
            }
        }

    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sqLiteDatabase = this.openOrCreateDatabase("Articles", MODE_PRIVATE, null);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS news");
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS news(url VARCHAR, title VARCHAR, id INT(4) PRIMARY KEY)");
        articleTitles = new ArrayList<>();
        articlesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, articleTitles);
        articlesList = (ListView) findViewById(R.id.articlesList);
        articlesList.setAdapter(articlesAdapter);
        articlesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String articleTitle = articleTitles.get(position);
                String query = "SELECT url FROM news WHERE title = \"%s\"";
                Cursor c = sqLiteDatabase.rawQuery(String.format(query, articleTitle), null);
                if(c != null){
                    if(c.moveToFirst()){
                        String url = c.getString(c.getColumnIndex("url"));
                        Intent secondActivity = new Intent(getApplicationContext(), SecondActivity.class);
                        secondActivity.putExtra("url", url);
                        startActivity(secondActivity);
                    }
                    else {
                        Log.i("MOVE TO FIRST", "FAILED!!");
                    }
                }
                else {
                    Log.i("DUDE", "ITS NOT WORKING");
                }

            }
        });
        download = new DownloadArticles();
        download.execute(bestStoriesURL);




    }
}
