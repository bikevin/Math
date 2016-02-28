package com.kevin.math;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.myscript.atk.maw.MathWidgetApi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.*;
import org.xml.sax.InputSource;
import org.w3c.dom.*;
import java.io.*;

public class MainActivity extends AppCompatActivity implements
        MathWidgetApi.OnConfigureListener,
        MathWidgetApi.OnRecognitionListener,
        MathWidgetApi.OnGestureListener,
        MathWidgetApi.OnWritingListener,
        MathWidgetApi.OnTimeoutListener,
        MathWidgetApi.OnSolvingListener,
        MathWidgetApi.OnUndoRedoListener {

    String currentUrl = "";
    String currentOutput = "";
    String currentImgUrl = "";
    String requestResult = "";
    String bestGuess = "";
    private static final String key = "79XT2W-3WXQVGTJ48";
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "EZMath";
    /** Notify the user that a MSB resource is not found or invalid. */
    public static final int DIALOG_ERROR_RESSOURCE = 0;
    /** Notify the user that a MSB certificate is missing or invalid. */
    public static final int DIALOG_ERROR_CERTIFICATE = 1;
    /** Notify the user that maximum number of items has been reached. */
    public static final int DIALOG_ERROR_RECOTIMEOUT = 2;
    /** One error dialog at a time. */
    private boolean mErrorDlgDisplayed = false;

    private MathWidgetApi mWidget;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mWidget = (MathWidgetApi) findViewById(R.id.myscript_maw);
        mWidget.setOnConfigureListener(this);
        mWidget.setOnRecognitionListener(this);
        mWidget.setOnGestureListener(this);
        mWidget.setOnWritingListener(this);
        mWidget.setOnTimeoutListener(this);

        configure();


    }

    private void configure()
    {
        // Equation resource
        final String[] resources = new String[]{"math-ak.res", "math-grm-maw.res"};

        // Prepare resources
        final String subfolder = "math";
        String resourcePath = new String(getFilesDir().getPath() + java.io.File.separator + subfolder);

        SimpleResourceHelper
                .copyResourcesFromAssets(getAssets(), subfolder /* from */, resourcePath /* to */, resources /* resource names */);

        // Configure math widget
        mWidget.setResourcesPath(resourcePath);
        mWidget.configure(this, resources, MyCertificate.getBytes(), MathWidgetApi.AdditionalGestures.DefaultGestures);
    }

    @Override
    public void onConfigurationBegin()
    {
        if (DBG)
            Log.d(TAG, "Equation configuration begins");
    }

    @Override
    public void onConfigurationEnd(final boolean success)
    {
        if (DBG)
        {
            if (success)
                Log.d(TAG, "Equation configuration succeeded");
            else
                Log.d(TAG, "Equation configuration failed (" + mWidget.getErrorString() + ")");
        }

        if (DBG)
        {
            if (success)
                Log.d(TAG, "Equation configuration loaded successfully");
            else
                Log.d(
                        TAG,
                        "Equation configuration error - did you copy the equation resources to your SD card? ("
                                + mWidget.getErrorString() + ")");
        }

        // Notify user using dialog box
        if (!success)
            showErrorDlg(DIALOG_ERROR_RESSOURCE);
    }

    // ----------------------------------------------------------------------
    // Math Widget styleable library - equation recognition process

    //sends the math request to Wolfram

    @Override
    public void onRecognitionBegin()
    {
        if (DBG)
            Log.d(TAG, "Equation recognition begins");
    }

    @Override
    public void onRecognitionEnd()
    {
        if (DBG)
            Log.d(TAG, "Equation recognition end");
        Log.d("recog", mWidget.getResultAsLaTeX());
        new APIRequest().execute(mWidget.getResultAsLaTeX());
        if(requestResult.equals("false")){
            Log.e("result", requestResult);
            new APIRequest().execute(bestGuess);
        }
    }

    @Override
    public void onUsingAngleUnitChanged(final boolean used)
    {
        if (DBG)
            Log.d(TAG, "An angle unit usage has changed in the current computation and is currently " + used);
    }

    // ----------------------------------------------------------------------
    // Math Widget styleable library - equation recognition gestures

    @Override
    public void onEraseGesture(final boolean partial)
    {
        if (DBG)
            Log.d(TAG, "Erase gesture handled by current equation and is partial " + partial);
    }

    // ----------------------------------------------------------------------
    // Math Widget styleable library - ink edition

    @Override
    public void onWritingBegin()
    {
        if (DBG)
            Log.d(TAG, "Start writing");
    }

    @Override
    public void onWritingEnd()
    {
        if (DBG)
            Log.d(TAG, "End writing");
    }

    @Override
    public void onRecognitionTimeout()
    {
        showErrorDlg(DIALOG_ERROR_RECOTIMEOUT);
    }

    // ----------------------------------------------------------------------
    // Math Widget styleable library - Undo / Redo

    @Override
    public void onUndoRedoStateChanged()
    {
        if (DBG)
            Log.d(TAG, "End writing");
    }

    // ----------------------------------------------------------------------
    // Math Widget styleable library - Errors

    // showDialog is deprecated but still used to simplify the example.
    @SuppressWarnings("deprecation")
    private void showErrorDlg(final int id)
    {
        if (DBG)
            Log.i(TAG, "Show error dialog");
        if (!mErrorDlgDisplayed)
        {
            mErrorDlgDisplayed = true;
            showDialog(id);
        }
    }
    public static String getCharacterDataFromElement(Element e) {
        Node child = e.getFirstChild();
        if (child instanceof CharacterData) {
            CharacterData cd = (CharacterData) child;
            return cd.getData();
        }
        return "?";
    }
    @Override
    public Dialog onCreateDialog(final int id)
    {
        final AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setCancelable(false);
        switch (id)
        {
            // Language pack update missing resource
            case DIALOG_ERROR_RESSOURCE :
                alertBuilder.setTitle(R.string.langpack_parsing_error_title);
                alertBuilder.setMessage(R.string.langpack_parsing_error_msg);
                alertBuilder.setPositiveButton(android.R.string.ok, abortListener);
                break;
            // Certificate error
            case DIALOG_ERROR_CERTIFICATE :
                alertBuilder.setTitle(R.string.certificate_error_title);
                alertBuilder.setMessage(R.string.certificate_error_msg);
                alertBuilder.setPositiveButton(android.R.string.ok, abortListener);
                break;
            // Maximum item count error
            case DIALOG_ERROR_RECOTIMEOUT :
                alertBuilder.setTitle(R.string.recotimeout_error_title);
                alertBuilder.setMessage(R.string.recotimeout_error_msg);
                alertBuilder.setPositiveButton(android.R.string.ok, closeListener);
                break;
        }
        final AlertDialog alert = alertBuilder.create();
        return alert;
    }

    private final DialogInterface.OnClickListener closeListener = new DialogInterface.OnClickListener()
    {
        @Override
        public void onClick(final DialogInterface di, final int position)
        {
            mErrorDlgDisplayed = false;
        }
    };

    private final DialogInterface.OnClickListener abortListener = new DialogInterface.OnClickListener()
    {
        @Override
        public void onClick(final DialogInterface di, final int position)
        {
            mErrorDlgDisplayed = false;
            finish();
        }
    };

    private class APIRequest extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {
            String info = params[0];
            String url = "http://api.wolframalpha.com/v2/query?appid=" + key + "&input=";
            for (int i = 0; i < info.length(); i++) {
                if (!(info.charAt(i) >= 97 && info.charAt(i) <= 122)) {
                    url += '%' + Integer.toHexString(info.charAt(i) | 0x10000).substring(3).toUpperCase();
                } else {
                    url += info.charAt(i);
                }
            }
            url += "&format=image,plaintext";

            HttpURLConnection c = null;
            try {
                URL u = new URL(url);
                c = (HttpURLConnection) u.openConnection();
                c.setRequestMethod("GET");
                c.setRequestProperty("Content-length", "0");
                c.setUseCaches(false);
                c.setAllowUserInteraction(false);
                c.setConnectTimeout(5000);
                c.setReadTimeout(5000);
                c.connect();
                int status = c.getResponseCode();
                Log.e("status", String.valueOf(status));
                switch (status) {
                    case 201:
                    case 200:
                        BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) {
                            sb.append(line + "\n");
                        }
                        br.close();
                        currentOutput = sb.toString();
                        Log.d("aloha", currentOutput);
                }

            } catch (MalformedURLException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            } finally {
                if (c != null) {
                    try {
                        c.disconnect();
                    } catch (Exception ex) {
                        Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }

            try {
                DocumentBuilderFactory dbf =
                        DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
//                Log.d("plsplsppls",currentOutput);
                InputSource is = new InputSource(new StringReader(currentOutput));

                Document doc = db.parse(is);
//                Log.d("kevinbiiii", currentOutput);

                NodeList nodes = doc.getElementsByTagName("pod");


                // iterate the employeesm

                Element element = (Element) nodes.item(0);

                if(element != null) {
                    NodeList name = element.getElementsByTagName("subpod");

                    Element line = (Element) name.item(0);
                    NodeList text = line.getElementsByTagName("plaintext");
                    NodeList link = line.getElementsByTagName("img");
                    Node node = text.item(0);
                    Element nodeLink = (Element) link.item(0);
//                Log.d("jerry", node.getTextContent());
                    currentImgUrl = nodeLink.getAttribute("src");
//                Log.d("meng", currentImgUrl);



                }
                requestResult = ((Element) doc.getElementsByTagName("queryresult").item(0)).getAttribute("success");
                if(requestResult.equals("false")){
                    if(doc.getElementsByTagName("tips").item(0) == null) {
                        bestGuess = ((Element) doc.getElementsByTagName("didyoumeans").item(0)).getElementsByTagName("didyoumean").item(0).getTextContent();
                    } else {
                        bestGuess = "";
                    }
                }
                Log.e("reuslt", requestResult);
                Log.e("bestguess", bestGuess);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }



        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            if(requestResult.equals("false") && !bestGuess.equals("")){
                Log.e("result", requestResult);
                new APIRequest().execute(bestGuess);
            }
        }
    }

}
