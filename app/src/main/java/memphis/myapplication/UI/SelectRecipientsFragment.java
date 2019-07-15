package memphis.myapplication.UI;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import memphis.myapplication.R;
import memphis.myapplication.data.Common;
import timber.log.Timber;

import static android.content.Context.LOCATION_SERVICE;

public class SelectRecipientsFragment extends Fragment implements ListDisplayRecyclerView.ItemClickListener, LocationListener {

    private ArrayList<String> m_selectedFriends;
    private Button m_sendButton;
    private ListDisplayRecyclerView adapter;
    private boolean m_feedSelected;
    private View selectReceipientsView;
    private CheckBox locationCheckView;
    private TextView currentLocation;
    private boolean locationAdded;
    private TextView feed;
    private TextView friends;
    private GradientDrawable drawable1;

    private LocationManager locationManager;
    private Location location = null;
    private double latitude;
    private double longitude;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        selectReceipientsView = inflater.inflate(R.layout.fragment_select_recipients, container, false);

        m_selectedFriends = new ArrayList<>();
        m_sendButton = selectReceipientsView.findViewById(R.id.send_button);
        feed();
        showFriends();

        m_sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                returnList();
            }
        });
        return selectReceipientsView;
    }

    private void feed() {
        feed = selectReceipientsView.findViewById(R.id.feed);
        drawable1 = new GradientDrawable();
        drawable1.setColor(Color.CYAN);
        drawable1.setStroke(2, Color.BLACK);
        feed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                friends.setBackground(null);
                friends.setSelected(false);
                if (m_selectedFriends.size() <= 0) {
                    if (feed.isSelected()) {
                        m_feedSelected = false;
                        feed.setSelected(false);
                        feed.setBackground(null);
                        m_sendButton.setVisibility(View.GONE);
                    } else {
                        m_feedSelected = true;
                        feed.setSelected(true);
                        feed.setBackground(drawable1);
                        m_sendButton.setVisibility(View.VISIBLE);
                    }
                }
            }
        });

    }

    private void showFriends() {
        friends = selectReceipientsView.findViewById(R.id.friends);
        friends.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                feed.setSelected(false);
                feed.setBackground(null);
                m_feedSelected = false;

                if (friends.isSelected()) {
                    friends.setSelected(false);
                    friends.setBackground(null);
                    m_sendButton.setVisibility(View.GONE);
                } else {
                    friends.setBackground(drawable1);
                    friends.setSelected(true);
                    m_sendButton.setVisibility(View.VISIBLE);
                }
            }
        });
        locationCheckView = selectReceipientsView.findViewById(R.id.location);
        currentLocation = selectReceipientsView.findViewById(R.id.current_location);

        if (!m_feedSelected) {
            Bundle bundle = this.getArguments();
            if (bundle != null) {
                ArrayList<String> friendsList = (ArrayList<String>) bundle.getSerializable("friendsList");
                if (friendsList.size() == 0) {
                    friends.setVisibility(View.GONE);
                    locationCheckView.setVisibility(View.GONE);
                    currentLocation.setVisibility(View.GONE);
                } else {
                    locationCheckView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            if (isChecked) {
                                locationAdded = true;
                                work();
                            } else {
                                latitude = 0;
                                longitude = 0;
                                currentLocation.setText("Removed");
                                locationAdded = false;
                            }
                        }
                    });
                    RecyclerView recyclerView = selectReceipientsView.findViewById(R.id.friendList);
                    recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

                    adapter = new ListDisplayRecyclerView(getActivity(), friendsList);
                    adapter.setClickListener(this);
                    recyclerView.setAdapter(adapter);
                }
                m_sendButton.setVisibility(View.GONE);
            } else Navigation.findNavController(selectReceipientsView).popBackStack();
        }
    }

    private void returnList() {
        // first ask for confirmation; do they want to send the photo to (show list of selected)
        // friends
        if (m_feedSelected) {
            AlertDialog.Builder question = new AlertDialog.Builder(getActivity());
            question.setTitle("Publish photo to your feed?");
            question.setCancelable(false);

            question.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    try {
                        Bundle bundle = getArguments();
                        if (bundle != null) {
                            String path = bundle.getString("photo");
                            Intent data = new Intent();
                            data.putExtra("photo", path);
                            Common.encryptAndPublish(data, getActivity());
                            Navigation.findNavController(selectReceipientsView).popBackStack();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Intent data = new Intent();
                        data.putStringArrayListExtra("recipients", m_selectedFriends);
                        getActivity().runOnUiThread(makeToast("Something went wrong with sending photo. Try resending"));
                        Navigation.findNavController(selectReceipientsView).popBackStack();
                    }
                }
            });

            question.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    // do nothing; we'll just go back to selecting friends
                }
            });

            question.show();

        } else {
            AlertDialog.Builder question = new AlertDialog.Builder(getActivity());
            question.setTitle("Send photo to these friends?");
            StringBuilder sb = new StringBuilder();
            for (String friend : m_selectedFriends) {
                sb.append(friend);
                sb.append(", ");
            }
            sb.delete(sb.lastIndexOf(","), sb.length());
            question.setMessage(sb.toString());
            question.setCancelable(false);

            question.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                    try {
                        Bundle bundle = getArguments();
                        if (bundle != null) {
                            String path = bundle.getString("photo");
                            Intent data = new Intent();
                            data.putStringArrayListExtra("recipients", m_selectedFriends);
                            data.putExtra("photo", path);
                            if (locationAdded) {
                                data.putExtra("location", true);
                                Bundle params = new Bundle();
                                params.putString("latitude", String.format("%.4f", latitude));
                                params.putString("longitude", String.format("%.4f", longitude));
                                data.putExtras(params);
                            }
                            data.putExtra("photo", path);
                            Common.encryptAndPublish(data, getActivity());
                            Navigation.findNavController(selectReceipientsView).popBackStack();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Intent data = new Intent();
                        data.putStringArrayListExtra("recipients", m_selectedFriends);
                        getActivity().runOnUiThread(makeToast("Something went wrong with sending photo. Try resending"));
                        Navigation.findNavController(selectReceipientsView).popBackStack();
                    }
                }
            });

            question.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    // do nothing; we'll just go back to selecting friends
                }
            });

            question.show();
        }
    }

    /**
     * Android is very particular about UI processes running on a separate thread. This function
     * creates and returns a Runnable thread object that will display a Toast message.
     */
    public Runnable makeToast(final String s) {
        return new Runnable() {
            public void run() {
                Toast.makeText(getActivity().getApplicationContext(), s, Toast.LENGTH_LONG).show();
            }
        };
    }

    @Override
    public void onItemClick(View view, int position) {
        // Toast.makeText(getActivity(), "Selected Friend Name: " + adapter.getItem(position),Toast.LENGTH_SHORT).show();
        // Assumes Each Friend Name will be unique.
        // This condition fails if duplicates exist in friendList
        if (!m_selectedFriends.contains(adapter.getItem(position))) {
            m_selectedFriends.add(adapter.getItem(position));
            view.setBackgroundColor(Color.CYAN);
            Timber.d("SelectRecipients: %s", "We selected " + adapter.getItem(position));
            Timber.d("showFriends: %s", "After add: " + m_selectedFriends.size());
        } else {
            m_selectedFriends.remove(adapter.getItem(position));
            view.setBackgroundColor(Color.TRANSPARENT);
            Timber.d("SelectRecipients: %s", "We deselected " + adapter.getItem(position));
            Timber.d("showFriends: %s", "After removed: " + m_selectedFriends.size());
        }

        if (m_selectedFriends.size() == 0) {
            // remove button since we have selected 0 friends now
            feed.setSelected(false);
            feed.setBackground(null);
            friends.setSelected(false);
            friends.setBackground(null);
            m_sendButton.setVisibility(View.GONE);
        } else {
            feed.setSelected(false);
            feed.setBackground(null);
            friends.setSelected(true);
            friends.setBackground(drawable1);
            m_feedSelected = false;
            m_sendButton.setVisibility(View.VISIBLE);
        }
    }

    private void work() {
        try {
            locationManager = (LocationManager) getActivity()
                    .getSystemService(LOCATION_SERVICE);

            // Getting GPS status
            boolean isGPSEnabled = locationManager
                    .isProviderEnabled(LocationManager.GPS_PROVIDER);

            // Getting network status
            boolean isNetworkEnabled = locationManager
                    .isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            Timber.i(isGPSEnabled + " : " + isNetworkEnabled);
            if (!isGPSEnabled && !isNetworkEnabled) {
                Toast.makeText(getActivity(), "Turn on the location", Toast.LENGTH_SHORT).show();
                locationCheckView.setChecked(false);
                currentLocation.setText("location off");
            } else {
                if (isNetworkEnabled) {
                    if (getActivity().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) !=
                            PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                1);
                        return;
                    }
                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            (long) 500,
                            (float) 50, this);
                    location = locationManager
                            .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    Timber.d("Network: " + location);
                    updateLocation(location);
                }
                // If GPS enabled, get latitude/longitude using GPS Services
                if (isGPSEnabled) {
                    if (location == null) {
                        Timber.i("here");
                        locationManager.requestLocationUpdates(
                                LocationManager.GPS_PROVIDER,
                                500,
                                50, this);
                        location = locationManager
                                .getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        Timber.d("GPS Enabled: " + location);
                        updateLocation(location);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void updateLocation(Location location) {
        if (location != null) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
            Timber.d("latitude: " + latitude + " longitude: " + longitude);
            currentLocation.setText(String.format("%.2f", latitude) + " : " + String.format("%.2f", longitude));
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Timber.d("changed: " + location);
        updateLocation(location);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    work();
                } else {
                    locationCheckView.setChecked(false);
                    currentLocation.setText("Denied");
                    Toast.makeText(getActivity(), "Please allow location permission", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        locationManager.removeUpdates(this);
    }
}

