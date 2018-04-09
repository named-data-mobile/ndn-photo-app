/*
source: http://custom-android-dn.blogspot.com/2013/01/create-simple-file-explore-in-android.html
 */

package memphis.myapplication;

import java.io.File;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.text.DateFormat;
import android.os.Bundle;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

public class FileSelectActivity extends ListActivity {

    private File currentDir;
    private FileArrayAdapter adapter;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //currentDir = new File("/sdcard/");
        currentDir = new File(Environment.getExternalStorageDirectory().toString());
        fill(currentDir);
    }
    private void fill(File file)
    {
        File[]dirs = file.listFiles();
                 this.setTitle("Current Dir: " + file.getName());
                 List<FileItem> dir = new ArrayList<FileItem>();
                 List<FileItem> fls = new ArrayList<FileItem>();
                 try{
                         for(File ff: dirs)
                         {
                                Date lastModDate = new Date(ff.lastModified());
                                DateFormat formater = DateFormat.getDateTimeInstance();
                                String date_modify = formater.format(lastModDate);
                                if(ff.isDirectory()){
                                    File[] fbuf = ff.listFiles();
                                    int buf = 0;
                                    if(fbuf != null){
                                        buf = fbuf.length;
                                    }
                                    else {
                                        buf = 0;
                                    }
                                    String num_item = String.valueOf(buf);
                                    if(buf == 0) {
                                        num_item = num_item + " item";
                                    }
                                    else {
                                        num_item = num_item + " items";
                                    }

                                    dir.add(new FileItem(ff.getName(), num_item, date_modify,
                                            ff.getAbsolutePath(),"folder"));
                                }
                                else
                                {
                                    fls.add(new FileItem(ff.getName(),ff.length() + " Byte",
                                            date_modify, ff.getAbsolutePath(),"text_file"));
                                }
                         }
                 }
                 catch(Exception e) {
                     // catches if a directory is empty and therefore has no files that it can use
                     // to fill itself; therefore, don't do anything about it.
                 }
                 Collections.sort(dir);
                 Collections.sort(fls);
                 dir.addAll(fls);
                 if(!file.getName().equalsIgnoreCase("sdcard")) {
                     dir.add(0, new FileItem("..", "", "", file.getParent(), "parent_folder"));
                 }
                 adapter = new FileArrayAdapter(FileSelectActivity.this, R.layout.activity_file_select, dir);
                 this.setListAdapter(adapter);
    }
    @Override
        protected void onListItemClick(ListView l, View v, int position, long id) {
                super.onListItemClick(l, v, position, id);
                FileItem fItem = adapter.getItem(position);

                if(fItem.getImage().equalsIgnoreCase("folder") ||
                        fItem.getImage().equalsIgnoreCase("parent_folder")){
                    currentDir = new File(fItem.getPath());
                    fill(currentDir);
                }
                else
                {
                    onFileClick(fItem);
                }
        }
    private void onFileClick(FileItem fileItem)
    {
        // replace their implementation with your NDN additions
        Toast.makeText(this, "Folder Clicked: "+ currentDir, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent();
        intent.putExtra("GetPath",currentDir.toString());
        intent.putExtra("GetFileName",fileItem.getName());
        setResult(RESULT_OK, intent);
        finish();
    }
}
