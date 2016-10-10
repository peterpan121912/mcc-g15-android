package fi.aalto.openoranges.project1.mcc;

import android.*;
import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.support.v4.app.ActivityCompat;
import android.location.Location;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private String mToken;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;

    private static final String LOG_TAG = "MainActivity";
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    OkHttpClient client = new OkHttpClient();
    private ApplicationList mAppList = null;


    private TextView mTextView;

    private String mAppsListTest = "void";
    private ArrayList<JSONObject> arrays = null;
    private List<Application> myApps = new ArrayList<Application>();

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLogoutTask mAuthTask = null;
    private View mProgressView;
    private View mListView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        mToken = getIntent().getStringExtra("token");
        // Toast.makeText(MainActivity.this, mToken, Toast.LENGTH_SHORT).show();

        mTextView = (TextView) findViewById(R.id.textView);
        // mListTextView = (TextView) findViewById(R.id.Liste);


        mAppList = new ApplicationList("test", "test");
        mAppList.execute((Void) null);


        Button logoutButton = (Button) findViewById(R.id.logout);
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogout();
            }
        });

        // populateListView();
        //mListView = findViewById(R.id.oo_AppsListView);
        mListView = findViewById(R.id.textView);
        mProgressView = findViewById(R.id.logout_progress);
    }

    private void populateListView() {
        ArrayAdapter<Application> adapter = new MyListAdapter();
        ListView list = (ListView) findViewById(R.id.oo_AppsListView);
        list.setAdapter(adapter);
    }

    private class MyListAdapter extends ArrayAdapter<Application> {
        public MyListAdapter() {
            super(MainActivity.this, R.layout.item_view, myApps);
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            //Make sure to have a view to work with
            View itemView = convertView;
            if (itemView == null)
                itemView = getLayoutInflater().inflate(R.layout.item_view, parent, false);
            //find app to work with
            Application currentApp = myApps.get(position);
            //Fill the view
            ImageView imageView = (ImageView) itemView.findViewById(R.id.mr_volume_item_icon);
            imageView.setImageBitmap(getImageBitmap(currentApp.getIcon_url()));
            return itemView;
        }
    }

    private Bitmap getImageBitmap(String url) {
        Bitmap bm = null;
        try {
            URL aURL = new URL(url);
            URLConnection conn = aURL.openConnection();
            conn.connect();
            InputStream is = conn.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            bm = BitmapFactory.decodeStream(bis);
            bis.close();
            is.close();
        } catch (IOException e) {
            Log.e("tag", "Error getting bitmap", e);
        }
        return bm;
    }

    private void populateAppList() {
        for (int j = 0; j < arrays.size(); j++) {
            String name = null;
            String id = null;
            String icon_url = null;
            String description = null;
            try {
                name = arrays.get(j).getString("name");
                id = arrays.get(j).getString("id");
                icon_url = arrays.get(j).getString("icon_url");
                description = arrays.get(j).getString("description");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            myApps.add(new Application(name, id, icon_url, description));
        }
    }

    public Response getList(String url) throws IOException {
        return get(url);
    }

    public Response get(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", mToken)
                .build();
        return client.newCall(request).execute();
    }


    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class ApplicationList extends AsyncTask<Void, Void, ArrayList<JSONObject>> {

        private final String mLatitude;
        private final String mLongitude;

        ApplicationList(String latitude, String longitude) {
            mLatitude = latitude;
            mLongitude = longitude;
        }

        @Override
        protected ArrayList<JSONObject> doInBackground(Void... params) {

            String lat = "12.124124";
            String lng = "12.344345";
            String l = "?lat=" + lat + "&lng=" + lng;

            try {
                // Simulate network access.
                Response response = getList("https://mccg15.herokuapp.com/application" + l);
                int code = response.code();
                if (code == 200) {
                    mAppsListTest = response.body().string().toString();
                    mAppsListTest = "{'apps': " + mAppsListTest + "}";
                    try {
                        JSONObject myjson = new JSONObject(mAppsListTest);
                        JSONArray the_json_array = myjson.getJSONArray("apps");
                        int size = the_json_array.length();
                        arrays = new ArrayList<JSONObject>();
                        for (int i = 0; i < size; i++) {
                            JSONObject another_json_object = the_json_array.getJSONObject(i);

                            arrays.add(another_json_object);
                        }

                        JSONObject[] jsons = new JSONObject[arrays.size()];
                        arrays.toArray(jsons);
                        return arrays;
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Code: 401", Toast.LENGTH_LONG).show();
                    return arrays;
                }
            } catch (Exception i) {
                //Toast.makeText(MainActivity.this, "FAILURE", Toast.LENGTH_LONG).show();
                i.printStackTrace();
                Toast.makeText(MainActivity.this, "hallo" + i.getMessage(), Toast.LENGTH_LONG).show();
                return null;
            }

            return arrays;
        }


        protected void onPostExecute(ArrayList<JSONObject> liste) {
            mAppList = null;
            arrays = liste;
           // populateAppList();
           // populateListView();
        }

        @Override
        protected void onCancelled() {
            mAppList = null;
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        //Connect the client
        mGoogleApiClient.connect();


    }

    @Override
    protected void onStop() {
        //Disconnect the client
        mGoogleApiClient.disconnect();
        super.onStop();

    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        //Identifying location update parameters
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(1000); // Update location every second

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(LOG_TAG, "GoogleApiClient has been suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(LOG_TAG, "GoogleApiClient has been suspended");
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i(LOG_TAG, location.toString());
        mTextView.setText(location.toString());
    }


    //Logout activity
    private void attemptLogout() {
        if (mAuthTask != null) {
            return;
        }


        boolean cancel = false;
        View focusView = null;

        showProgress(true);
        mAuthTask = new UserLogoutTask();
        mAuthTask.execute((Void) null);


    }

    public Response post(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", mToken)
                .build();
        return client.newCall(request).execute();
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mListView.setVisibility(show ? View.GONE : View.VISIBLE);
            mListView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mListView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mListView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLogoutTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {
            // TODO: attempt authentication against a network service.

            try {
                // Simulate network access.
                Response response = post("https://mccg15.herokuapp.com/users/logout");
                int code = response.code();
                if (code == 200) {

                } else {
                    return false;
                }
            } catch (Exception i) {

                return false;
            }

            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(false);

            if (success) {
                Toast.makeText(MainActivity.this, "Logout successfully!", Toast.LENGTH_SHORT).show();
                Intent i = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(i);
            } else {
                Toast.makeText(MainActivity.this, "Logout failed!", Toast.LENGTH_SHORT).show();

                //TESTING
                Intent i = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(i);
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }
}
