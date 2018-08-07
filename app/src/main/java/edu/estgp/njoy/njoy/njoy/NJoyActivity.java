package edu.estgp.njoy.njoy.njoy;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.estgp.njoy.njoy.R;
import edu.estgp.njoy.njoy.app.Option;
import edu.estgp.njoy.njoy.app.Screen;
import edu.estgp.njoy.njoy.ble.BluetoothConnectionState;
import edu.estgp.njoy.njoy.ble.BluetoothLeScanActivity;
import edu.estgp.njoy.njoy.ble.BlunoActivity;
import edu.estgp.njoy.njoy.contact.NJoyContact;
import edu.estgp.njoy.njoy.pilight.DeviceStatusInterface;
import edu.estgp.njoy.njoy.pilight.Pilight;
import edu.estgp.njoy.njoy.sms.NJoySMS;
import edu.estgp.njoy.njoy.sms.SMS;


public class NJoyActivity extends BlunoActivity implements DeviceStatusInterface {
    private ListView listView;
    private GridView gridView;
    private GridAdapter gridViewAdapter;
    private TextView textViewTitle;
    private ImageButton imageButtonState;
    private TextView textViewState;
    int contador = 0;

    private static int currentScreen = 0;
    private static Stack<Integer> stackOfScreens = new Stack<Integer>();

    private static final int JOYSTICK_MODE_SCAN_NEXT = 0;
    private static final int JOYSTICK_MODE_SCAN_PREVIOUS = 1;
    private static final int JOYSTICK_MODE_LEFT_RIGHT = 2;
    private static final int JOYSTICK_MODE_FULL_DIRECTIONS = 3;

    private static final int TIMEOUT_MOVE = 2000;

    private static final int BUTTON_A_MODE = 0;
    private static final int BUTTON_B_MODE = 1;
    private static final int TIMEOUT_BUTTONS = 3000;

    private int joystickMode = JOYSTICK_MODE_LEFT_RIGHT;
    private int timeoutMove = TIMEOUT_MOVE;

    int buttonA = BUTTON_A_MODE;
    int buttonB = BUTTON_B_MODE;
    private int timeoutButtons = TIMEOUT_BUTTONS;


    private boolean connecting = true;

    private Handler mHandler = new Handler();

    private String dataBuffer = "";
    private Pattern pattern = Pattern.compile("(\\d{6})\\r\\n");
    private long previousJoystickMovement = 0;
    private long previousButtonClick = 0;

    private String TAG = NJoyActivity.class.getSimpleName();
    private ProgressDialog pDialog;


   ArrayList<Screen> screens = new ArrayList<>();





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        new GetOptions().execute();
        getOptionsFromJson();
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences("njoy", Context.MODE_PRIVATE);
        joystickMode = sharedPref.getInt("joystick_mode", JOYSTICK_MODE_SCAN_NEXT);
        timeoutMove = sharedPref.getInt("timeout_move", TIMEOUT_MOVE);
        buttonA = sharedPref.getInt("button_A_mode", BUTTON_A_MODE);
        buttonB = sharedPref.getInt("button_B_mode", BUTTON_B_MODE);
        timeoutButtons = sharedPref.getInt("timeout_buttons", TIMEOUT_BUTTONS);

        View rootView = findViewById(android.R.id.content);
        rootView.setKeepScreenOn(true);

        ImageView imageViewLogo = (ImageView)findViewById(R.id.imageViewLogo);
        imageViewLogo.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Intent intent = new Intent(NJoyActivity.this, SettingsActivity.class);
                startActivity(intent);
                return false;
            }
        });

        textViewTitle = (TextView) findViewById(R.id.textViewTitle);
        textViewState = (TextView) findViewById(R.id.textViewState);
        imageButtonState = (ImageButton) findViewById(R.id.imageButtonState);
        imageButtonState.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(NJoyActivity.this, BluetoothLeScanActivity.class);
                startActivity(intent);
            }
        });

        gridView = (GridView)findViewById(R.id.gridview);
        setScreen(currentScreen);

        listView = (ListView)findViewById(R.id.listviewSMS);

        // connect to pilight
        Pilight.connect(this);
    }

    @Override
    public void onBackPressed() {
        if (stackOfScreens.isEmpty())
            ; //finish();
        else {
            if (listView.getVisibility() == View.VISIBLE) {
                listView.setVisibility(View.GONE);
                gridView.setVisibility(View.VISIBLE);
                Screen screen = screens.get(currentScreen);

                textViewTitle.setText(screen.getTitle());
            }
            else {
                int idx = stackOfScreens.pop();

                for (Screen screen : screens) {


                }
                Screen screen = screens.get(idx);
                textViewTitle.setText(screen.getTitle());
                gridViewAdapter = new GridAdapter(this, screen);
                gridView.setAdapter(gridViewAdapter);
                gridViewAdapter.setSelection(0);
                TTS.speak(this, screen.getTitle());

                currentScreen = idx;
            }
        }
    }

    /* pilight domotic events */
    @Override
    public void onInitialDeviceStatus(final String device, final boolean state) {
        Log.d("NJOY", "onInitialDeviceStatus " + device + ", " + state);
    }

    @Override
    public void onDeviceStatusChanged(final String device, final boolean state) {
        Log.d("NJOY", "onDeviceStatusChanged " + device + ", " + state);

        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                String tts = "";
                switch (device) {
                    case "tv":
                        tts = "Televisão " + (state ? "ligada" : "desligada");
                        break;
                    case "hvac":
                        tts = "Rádio " + (state ? "ligado" : "desligado");
                        break;
                    case "light":
                        tts = "Luz " + (state ? "ligada" : "desligada");
                        break;
                    case "xxx":
                        tts = "Ar condicionado " + (state ? "ligado" : "desligado");
                        break;
                }

                TTS.speak(NJoyActivity.this, tts);

                setDeviceText(gridViewAdapter.getViewSelected(), state);
            }
        });
    }

    private void setDeviceText(View view, Boolean state) {
        if (view != null) {
            TextView textView = (TextView)view.findViewById(R.id.textViewText);

            if (textView != null) {
                String text = textView.getText().toString();
                text = text.replace("Desligar\n", "");
                text = text.replace("Ligar\n", "");

                textView.setText((state ? "Desligar\n" : "Ligar\n") + text);
            }
        }
    }


    // joystick events
    @Override
    public void onBluetoothPermissionsResult(boolean allGranted) {
        if (!allGranted) textViewState.setText("Ligue o GPS");
    }

    @Override
    public void onConectionStateChange(BluetoothConnectionState connectionState) {
        switch (connectionState) {
            case CONNECTED:
                Log.d("NJOY", "Ligado ao joystick NJOY");
                imageButtonState.setImageResource(R.drawable.leddarkblue);
                textViewState.setText("Ligado");

                connecting = false;
                break;
            case CONNECTING:
                Log.d("NJOY", "A ligar ao joystick NJOY...");
                imageButtonState.setImageResource(R.drawable.ledlightblue);
                textViewState.setText("A ligar ao joystick...");

                connecting = true;
                break;
            case SCAN:
                Log.d("NJOY", "Procurar joystick NJOY");
                imageButtonState.setImageResource(R.drawable.ledlightorange);
                textViewState.setText("Procurar joystick");

                connecting = true;
                break;
            case SCANNING:
                Log.d("NJOY", "A procurar o joystick NJOY...");
                imageButtonState.setImageResource(R.drawable.ledorange);
                textViewState.setText("A procurar joystick...");

                connecting = true;
                break;
            case DISCONNECTING:
                Log.d("NJOY", "A desligar do joystick NJOY");
                imageButtonState.setImageResource(R.drawable.ledgray);
                textViewState.setText("Desligado");
                break;
            default:
                break;
        }
    }

    @Override
    public void onSerialReceived(String theString) {
        Log.d("NJOY", "onSerialReceived: " + theString);

        dataBuffer += theString;
        Matcher matcher = pattern.matcher(dataBuffer);

        while (matcher.find()) {
            String found = matcher.group(1);

            Log.d("NJOY", "onSerialReceived process: " + found);
            byte data[] = found.getBytes();

            boolean joystick = data[0] == '1' || data[1] == '1' || data[2] == '1' || data[3] == '1';
            boolean bothButtons = data[4] == '1' && data[5] == '1';

            if (!TTS.isSpeaking()) {
                if (joystick && (System.currentTimeMillis() - previousJoystickMovement) > timeoutMove) {

                    if (joystickMode == JOYSTICK_MODE_SCAN_NEXT) {
                        next();
                    } else if (joystickMode == JOYSTICK_MODE_SCAN_PREVIOUS) {
                        previous();
                    } else if (joystickMode == JOYSTICK_MODE_LEFT_RIGHT) {
                        if (data[0] == '1' || data[1] == '1') {
                            next();
                        } else {
                            previous();
                        }
                    } else { // if (joystickMode == JOYSTICK_MODE_FULL_DIRECTIONS)
                        next();
                    }
                }
                else if (!bothButtons && (System.currentTimeMillis() - previousButtonClick) > timeoutButtons) {
                    if (data[5] == '1') {   // button A
                        Log.d("NJOY", "Button A");
                        if (buttonA == 0 && listView.getVisibility() == View.GONE)
                            gridViewAdapter.executeSelectedOption();
                        else
                            onBackPressed();
                    }
                    if (data[4] == '1') {   // button B
                        Log.d("NJOY", "Button B");
                        if (buttonB == 0 && listView.getVisibility() == View.GONE)
                            gridViewAdapter.executeSelectedOption();
                        else
                            onBackPressed();
                    }

                    previousButtonClick = System.currentTimeMillis();
                }
            }

            if (dataBuffer.length() > 8) {
                dataBuffer = dataBuffer.substring(8, dataBuffer.length());
            }
            else
                dataBuffer = "";
        }
    }

    public void setScreen(int idx) {

        for (Screen screen : screens) {
            if(idx == screen.getId()){
                textViewTitle.setText(screen.getTitle());
                gridViewAdapter = new GridAdapter(this, screen);
                gridView.setAdapter(gridViewAdapter);
                gridViewAdapter.setSelection(0);

                if (idx != 0 && idx != currentScreen) stackOfScreens.push(currentScreen);
                currentScreen = idx;

                TTS.speak(this, screen.getTitle());

            }

        }


    }

    private ArrayList<String> valuesSMS = new ArrayList<String>();
    public void readSms() {
        List<SMS> list = NJoySMS.getUnreadMessages(this);

        valuesSMS.clear();
        for (SMS sms: list) {
            valuesSMS.add(String.format(getString(R.string.sms_received),
                    NJoyContact.findContactByPhone(this, sms.getAddress()), sms.getMsg()));
        }

        if (valuesSMS.size() == 0) {
            TTS.speak(this, getString(R.string.no_sms));
            return;
        }
        gridView.setVisibility(View.GONE);
        listView.setVisibility(View.VISIBLE);

        textViewTitle.setText(R.string.sms_new);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_activated_1, android.R.id.text1, valuesSMS);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TTS.speak(NJoyActivity.this, valuesSMS.get(listView.getCheckedItemPosition()));
            }
        });

        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        listView.setItemChecked(-1, true);
    }

    private void next() {
        if (listView.getVisibility() == View.VISIBLE) {
            int size = listView.getCount();
            int position = listView.getCheckedItemPosition() + 1;
            if (position >= size) position = 0;

            listView.setItemChecked(position, true);
            TTS.speak(this, valuesSMS.get(position));

            // set message read
            NJoySMS.markMessageReadByPosition(this, position);
        }
        else {
            int size = gridViewAdapter.getCount();
            int position = gridViewAdapter.getSelection() + 1;
            if (position >= size) position = 0;

            gridViewAdapter.setSelection(position);
        }

        previousJoystickMovement = System.currentTimeMillis();
    }

    private void previous() {
        if (listView.getVisibility() == View.VISIBLE) {
            int size = listView.getCount();
            int position = listView.getCheckedItemPosition() - 1;
            if (position < 0) position = size - 1;

            listView.setItemChecked(position, true);
            TTS.speak(this, valuesSMS.get(position));

            // set message read
            NJoySMS.markMessageReadByPosition(this, position);
        }
        else {
            int size = gridViewAdapter.getCount();
            int position = gridViewAdapter.getSelection() - 1;
            if (position < 0) position = size - 1;

            gridViewAdapter.setSelection(position);
        }
        previousJoystickMovement = System.currentTimeMillis();
    }



    private void getOptionsFromJson (){

        String jsonStr = loadJSONFromAsset(this);

        if (jsonStr != null) {
            try {
                JSONObject jsonObj = new JSONObject(jsonStr);

                // Getting JSON Array node
                JSONArray screensArray = jsonObj.getJSONArray("screens");

                // looping through All Contacts
                for (int i = 0; i < screensArray.length(); i++) {


                    JSONObject s = screensArray.getJSONObject(i);

                    String id = s.getString("id");
                    String name = s.getString("name");

                    JSONArray options = s.getJSONArray("options");


                    ArrayList<Option> screenOptions = new ArrayList<>();

                    for(int j =0 ; j < options.length();j++){

                        JSONObject o = options.getJSONObject(j);

                        String optName = o.getString("name");
                        String optAction = o.getString("action");
                        String resourceName = o.getString("resourceName");

                        int resID = getResources().getIdentifier(resourceName, "drawable", getPackageName());

                        screenOptions.add(new Option(resID,optName,optAction));
                    }



                    Screen newScreen = new Screen(name,screenOptions, Integer.parseInt(id));
                    screens.add(newScreen);


                }
            } catch (final JSONException e) {
                Log.e(TAG, "Json parsing error: " + e.getMessage());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),
                                "Json parsing error: " + e.getMessage(),
                                Toast.LENGTH_LONG)
                                .show();
                    }
                });

            }
        } else {
            Log.e(TAG, "Couldn't get json from server.");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(),
                            "Couldn't get json from server. Check LogCat for possible errors!",
                            Toast.LENGTH_LONG)
                            .show();
                }
            });

        }

    }


    /**
     * Async task class to get json by making HTTP call
     */
    private class GetOptions extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Showing progress dialog
            pDialog = new ProgressDialog(NJoyActivity.this);
            pDialog.setMessage("Please wait...");
            pDialog.setCancelable(false);
            pDialog.show();

        }

        @Override
        protected Void doInBackground(Void... arg0) {

            // Making a request to url and getting response
            String jsonStr = loadJSONFromAsset(getApplicationContext());

            if (jsonStr != null) {
                try {
                    JSONObject jsonObj = new JSONObject(jsonStr);

                    // Getting JSON Array node
                    JSONArray screensArray = jsonObj.getJSONArray("screens");

                    // looping through All Contacts
                    for (int i = 0; i < screensArray.length(); i++) {


                        JSONObject s = screensArray.getJSONObject(i);

                        String id = s.getString("id");
                        String name = s.getString("name");

                        JSONArray options = s.getJSONArray("options");


                        ArrayList<Option> screenOptions = new ArrayList<>();

                        for(int j =0 ; j < options.length();j++){

                            JSONObject o = options.getJSONObject(j);

                            String optName = o.getString("name");
                            String optAction = o.getString("action");
                            String resourceName = o.getString("resourceName");

                            
                            int resID = getResources().getIdentifier(resourceName, "drawable", getPackageName());

                            screenOptions.add(new Option(resID,optName,optAction));
                        }



                        Screen newScreen = new Screen(name,screenOptions, Integer.parseInt(id));
                        screens.add(newScreen);
                        // tmp hash map for single contact

                    }
                } catch (final JSONException e) {
                    Log.e(TAG, "Json parsing error: " + e.getMessage());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),
                                    "Json parsing error: " + e.getMessage(),
                                    Toast.LENGTH_LONG)
                                    .show();
                        }
                    });

                }
            } else {
                Log.e(TAG, "Couldn't get json from server.");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),
                                "Couldn't get json from server. Check LogCat for possible errors!",
                                Toast.LENGTH_LONG)
                                .show();
                    }
                });

            }

            Log.e("READJSON", "Response from url: " + jsonStr);



            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            // Dismiss the progress dialog
            if (pDialog.isShowing())
                pDialog.dismiss();
        }

    }

    public String loadJSONFromAsset(Context context) {
        String json = null;


        try {

            AssetManager a = getAssets();

            InputStream is = a.open("screen.json");

            int size = is.available();

            byte[] buffer = new byte[size];

            is.read(buffer);

            is.close();

            json = new String(buffer, "UTF-8");


        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;

    }
}
