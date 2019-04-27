package com.github.astronoodles.azureskinsense;


import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BaseHttpStack;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


/**
 * A simple {@link Fragment} subclass.
 */
public class CameraFragment extends Fragment {

    public ImageView skinImage;
    public Button sendAzure;
    public ProgressBar progressBar;
    public String absoluteFilePath;

    public static final String AZURE_URL = "https://eastus.api.cognitive.microsoft.com/customvision/v3.0/Prediction/" +
            "7f7706ad-93c8-43cf-b113-2e995143eb35/classify/iterations/Iteration1/image";
    public static final String PREDICTION_KEY = "216b04cef1d34aa7beeabc9e1f1b4c76";

    public CameraFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_camera, container, false);
        skinImage = v.findViewById(R.id.camera_img);
        ImageButton cameraPic = v.findViewById(R.id.picture_button);
        sendAzure = v.findViewById(R.id.send_azure);
        progressBar = v.findViewById(R.id.progressBar);

        if(savedInstanceState != null ) {
            absoluteFilePath = savedInstanceState.getString("img_path");
            skinImage.setImageBitmap(BitmapFactory.decodeFile(absoluteFilePath));
        }

        cameraPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchPhoto();
            }
        });

        sendAzure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                TODO Use Android Volley to communicate with the keys in azure and understand the JSON response that you get
//                TODO make a message for the disease and links to treat it
//                TODO make a toast if there is no disease found
                sendToAzure();
                Toast.makeText(getActivity(), "Sending to Azure", Toast.LENGTH_LONG).show();
            }
            });


        return v;
    }

    private void sendToAzure(){
        RequestQueue queue = Volley.newRequestQueue(getActivity());
        Bitmap bitmap = BitmapFactory.decodeFile(absoluteFilePath);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);

        // String appendedUrl = String.format("%s?Body=%s", AZURE_URL, absoluteFilePath);
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.POST, AZURE_URL, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Toast.makeText(getActivity(), "Success!", Toast.LENGTH_LONG).show();
                try{
                    JSONArray predictions = response.getJSONArray("Predictions");
                } catch(JSONException e){
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
                Toast.makeText(getActivity(), "Internet error found. Do you have a connection?", Toast.LENGTH_LONG).show();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headerMap = new HashMap<>(2);
                headerMap.put("Prediction-Key", PREDICTION_KEY);
                headerMap.put("Content-Type", "application/octet-stream");
                return headerMap;
            }

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> paramMap = new HashMap<>(1);
                paramMap.put("Body", new String(baos.toByteArray()));
                return paramMap;
            }
        };

        queue.add(req);
            }

    private void dispatchPhoto(){
        Intent photoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(photoIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            File imgFile = null;
            try {
                imgFile = getFileForImage();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(imgFile != null){
                Uri contentUri =
                        FileProvider.getUriForFile(getActivity(), "com.github.astronoodles.azureskinsense", imgFile);
                photoIntent.putExtra(MediaStore.EXTRA_OUTPUT, contentUri);

                // grant the uri permissions
                List<ResolveInfo> permissionList = getActivity().getPackageManager().queryIntentActivities(photoIntent, PackageManager.MATCH_DEFAULT_ONLY);
                for(ResolveInfo info : permissionList){
                    String packageName = info.activityInfo.packageName;
                    getActivity().grantUriPermission(packageName, contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                }

                startActivityForResult(photoIntent, 1);
            }
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("current_image", absoluteFilePath);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            Bitmap imgBitmap = BitmapFactory.decodeFile(absoluteFilePath);
            skinImage.setImageBitmap(imgBitmap);
        }
    }

    private File getFileForImage() throws IOException{
        File picDir = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        String bitmapName = String.format("JPEG_%s", new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()));
        File imgFile = File.createTempFile(bitmapName, ".jpg", picDir);
        absoluteFilePath = imgFile.getAbsolutePath();
        return imgFile;
    }

}
