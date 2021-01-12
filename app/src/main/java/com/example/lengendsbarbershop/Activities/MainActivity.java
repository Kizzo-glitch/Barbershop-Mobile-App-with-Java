package com.example.lengendsbarbershop.Activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.lengendsbarbershop.R;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private Button customerButton, barberButton;
    CallbackManager callbackManager;
    SharedPreferences sharedPref;
    private Button buttonLogin;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        customerButton = (Button) findViewById(R.id.button_customer);
        barberButton = (Button) findViewById(R.id.button_barber);

        // Handle Customer Button
        customerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                customerButton.setBackgroundColor(getResources().getColor(R.color.colorAccent));
                customerButton.setTextColor(getResources().getColor(R.color.gmm_white));

                barberButton.setBackgroundColor(getResources().getColor(R.color.gmm_white));
                barberButton.setTextColor(getResources().getColor(R.color.colorAccent));
            }
        });

        // Handle Barber Button
        barberButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                barberButton.setBackgroundColor(getResources().getColor(R.color.colorAccent));
                barberButton.setTextColor(getResources().getColor(R.color.gmm_white));

                customerButton.setBackgroundColor(getResources().getColor(R.color.gmm_white));
                customerButton.setTextColor(getResources().getColor(R.color.colorAccent));
            }
        });

        findViewById(R.id.fb_login_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (AccessToken.getCurrentAccessToken() == null){
                    LoginManager.getInstance().logInWithReadPermissions(MainActivity.this, Arrays.asList("public_profile", "email"));
                }else {
                    ColorDrawable customerbuttonColor = (ColorDrawable) customerButton.getBackground();
                    if (customerbuttonColor.getColor() == getResources().getColor(R.color.colorAccent)) {
                        Intent intent = new Intent(getApplicationContext(), CustomerMainActivity2.class);
                        startActivity(intent);

                    } else {
                        Intent intent = new Intent(getApplicationContext(), BarberMainActivity2.class);
                        startActivity(intent);
                    }

                }

            }
        });


        callbackManager = CallbackManager.Factory.create();
        sharedPref = getSharedPreferences("MY_KEY", Context.MODE_PRIVATE);

        LoginManager.getInstance().registerCallback(callbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        // App code
                        Log.d("FACEBOOK TOKEN", loginResult.getAccessToken().getToken());
                        GraphRequest request = GraphRequest.newMeRequest(
                                loginResult.getAccessToken(),
                                new GraphRequest.GraphJSONObjectCallback() {
                                    @Override
                                    public void onCompleted(
                                            JSONObject object,
                                            GraphResponse response) {
                                        // Application code
                                        Log.d("FACEBOOK DETAILS", object.toString());

                                        SharedPreferences.Editor editor = sharedPref.edit();

                                        try{
                                            editor.putString("name", object.getString("name"));
                                            editor.putString("email", object.getString("email"));
                                            editor.putString("avatar", object.getJSONObject("picture").getJSONObject("data").getString("url"));
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                        editor.commit();
                                    }
                                });
                        Bundle parameters = new Bundle();
                        parameters.putString("fields", "id,name,email, picture");
                        request.setParameters(parameters);
                        request.executeAsync();

                        ColorDrawable customerbuttonColor = (ColorDrawable) customerButton.getBackground();

                        if (customerbuttonColor.getColor() == getResources().getColor(R.color.colorAccent)) {
                            loginToServer(AccessToken.getCurrentAccessToken().getToken(), "customer");
                        } else {
                            loginToServer(AccessToken.getCurrentAccessToken().getToken(), "barber");
                        }
                    }

                    @Override
                    public void onCancel() {
                        // App code
                    }

                    @Override
                    public void onError(FacebookException exception) {
                        // App code
                    }
                });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void loginToServer(String facebookAccessToken, final String userType) {
        //Button buttonLogin = findViewById(R.id.fb_login_button);
        buttonLogin.setText("LOADING...");
        buttonLogin.setClickable(false);
        buttonLogin.setBackgroundColor(getResources().getColor(R.color.colorLightGray));

        String url = getString(R.string.API_URL) + "/social/revoke-token";

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("grant_type", "convert_token");
            jsonBody.put("client_id", getString(R.string.CLIENT_ID));
            jsonBody.put("client_secret", getString(R.string.CLIENT_SECRET));
            jsonBody.put("backend", "facebook");
            jsonBody.put("token", facebookAccessToken);
            jsonBody.put("user_type", userType);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.POST, url, jsonBody, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        // Execute code
                        Log.d("LOGIN TO SERVER", response.toString());

                        // Shave server token to local database
                        SharedPreferences.Editor editor = sharedPref.edit();

                        try {
                            editor.putString("token", response.getString("access_token"));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        editor.commit();

                        // Start main activity
                        if (userType.equals("customer")) {
                            Intent intent = new Intent(getApplicationContext(), CustomerMainActivity2.class);
                            startActivity(intent);
                        } else {
                            Intent intent = new Intent(getApplicationContext(), BarberMainActivity2.class);
                            startActivity(intent);
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO: Handle error

                    }
                });

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(jsonObjectRequest);
    }

}