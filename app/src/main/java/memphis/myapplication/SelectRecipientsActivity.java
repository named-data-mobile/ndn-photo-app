package memphis.myapplication;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import io.realm.Realm;
import memphis.myapplication.RealmObjects.PublishedContent;
import memphis.myapplication.RealmObjects.User;
import timber.log.Timber;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import net.named_data.jndn.Name;
import net.named_data.jndn.encrypt.algo.EncryptAlgorithmType;
import net.named_data.jndn.encrypt.algo.EncryptParams;
import net.named_data.jndn.encrypt.algo.RsaAlgorithm;
import net.named_data.jndn.util.Blob;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import static memphis.myapplication.Globals.producerManager;

public class SelectRecipientsActivity extends Fragment implements ListDisplayRecyclerView.ItemClickListener {

    private ArrayList<String> m_selectedFriends;
    private Button m_sendButton;
    private ListDisplayRecyclerView adapter;
    private boolean m_feedSelected;
    private View selectReceipientsView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        selectReceipientsView = inflater.inflate(R.layout.fragment_select_recipients, container, false);

        m_selectedFriends = new ArrayList<>();
        feed();
        showFriends();
        
        return selectReceipientsView;
    }

    private void feed() {
        final TextView feed = selectReceipientsView.findViewById(R.id.feed);
        final GradientDrawable drawable1 = new GradientDrawable();
        drawable1.setColor(Color.CYAN);
        drawable1.setStroke(2, Color.BLACK);
        feed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
                        m_sendButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                returnList();
                            }
                        });
                    }
                }
            }
        });

    }

    private void showFriends() {
        if (!m_feedSelected) {
            Bundle bundle = this.getArguments();
            if (bundle != null) {
                ArrayList<String> friendsList = (ArrayList<String>) bundle.getSerializable("friendsList");
                m_sendButton = selectReceipientsView.findViewById(R.id.send_button);

                RecyclerView recyclerView = selectReceipientsView.findViewById(R.id.friendList);
                recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

                adapter = new ListDisplayRecyclerView(getActivity(), friendsList);
                adapter.setClickListener(this);
                recyclerView.setAdapter(adapter);
                m_sendButton.setVisibility(View.GONE);
            }else Navigation.findNavController(selectReceipientsView).popBackStack();
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
                            encryptAndPublish(data);
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
        else {
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
                            encryptAndPublish(data);
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
     * encodes sync data, encrypts photo, and publishes filename and symmetric keys
     * @param resultData: intent with filename and recipients list
     */
    public void encryptAndPublish(Intent resultData) {
        try {
            final String path = resultData.getStringExtra("photo");
            final File photo = new File(path);
            Timber.d("File size: " + photo.length());
            final Uri uri = UriFileProvider.getUriForFile(getActivity(),
                    getActivity().getApplicationContext().getPackageName() +
                            ".UriFileProvider", photo);
            final Encrypter encrypter = new Encrypter(getActivity().getApplicationContext());


            ArrayList<String> recipients;
            try {
                recipients = resultData.getStringArrayListExtra("recipients");
                SharedPrefsManager sharedPrefsManager = SharedPrefsManager.getInstance(getActivity());

                String name = sharedPrefsManager.getNamespace() + "/data";
                final String filename = sharedPrefsManager.getNamespace() + "/file" + path;

                // Generate symmetric key
                final SecretKey secretKey = encrypter.generateKey();
                final byte[] iv = encrypter.generateIV();

                // Encode sync data
                SyncData syncData = new SyncData();
                syncData.setFilename(filename);

                final boolean feed = (recipients == null);
                if (feed) {
                    Timber.d("For feed");
                    syncData.setFeed(true);
                }
                else {
                    syncData.setFeed(false);
                    Timber.d( "For friends");
                    Realm realm = Realm.getDefaultInstance();
                    for (String friend : recipients) {
                        Blob friendKey = realm.where(User.class).equalTo("username", friend).findFirst().getCert().getPublicKey();
                        byte[] encryptedKey = RsaAlgorithm.encrypt
                                (friendKey, new Blob(secretKey.getEncoded()), new EncryptParams(EncryptAlgorithmType.RsaOaep)).getImmutableArray();
                        syncData.addFriendKey(friend, encryptedKey);
                    }
                }
                // Stringify sync data
                producerManager.setDataSeqMap(syncData.stringify());
                Timber.d("Publishing file: %s", filename);

                byte[] bytes;
                try {
                    InputStream is = getActivity().getContentResolver().openInputStream(uri);
                    bytes = IOUtils.toByteArray(is);
                    Timber.d("select file activity: %s", "file byte array size: " + bytes.length);
                } catch (IOException e) {
                    Timber.d("onItemClick: failed to byte");
                    e.printStackTrace();
                    bytes = new byte[0];
                }
                Timber.d("file selection result: %s", "file path: " + path);
                try {
                    String prefixApp = "/" + sharedPrefsManager.getNamespace();

                    final String prefix = prefixApp + "/file" + path;
                    Timber.d(prefix);
                    Realm realm = Realm.getDefaultInstance();
                    realm.beginTransaction();
                    PublishedContent contentKey = realm.createObject(PublishedContent.class, path);
                    if (!feed) {
                        Timber.d("Publishing to friend(s)");
                        contentKey.addKey(secretKey);
                        realm.commitTransaction();
                        realm.close();

                        Blob encryptedBlob = encrypter.encrypt(secretKey, iv, bytes);
                        Common.publishData(encryptedBlob, new Name(prefix));
                    }
                    else {
                        Timber.d("Publishing to feed");
                        realm.commitTransaction();
                        realm.close();
                        Blob unencryptedBlob = new Blob(bytes);
                        Common.publishData(unencryptedBlob, new Name(prefix));

                    }
                    final FileManager manager = new FileManager(getActivity().getApplicationContext());
                    Bitmap bitmap = QRExchange.makeQRCode(prefix);
                    manager.saveFileQR(bitmap, prefix);
                    getActivity().runOnUiThread(makeToast("Sending photo"));
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (NoSuchPaddingException e) {
                    e.printStackTrace();
                } catch (BadPaddingException e) {
                    e.printStackTrace();
                } catch (InvalidKeyException e) {
                    e.printStackTrace();
                } catch (IllegalBlockSizeException e) {
                    e.printStackTrace();
                } catch (InvalidAlgorithmParameterException e) {
                    e.printStackTrace();
                }
                producerManager.publishFile(name);
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            getActivity().runOnUiThread(makeToast("Something went wrong with sending photo. Try resending"));
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
        if(!m_selectedFriends.contains(adapter.getItem(position))){
            m_selectedFriends.add(adapter.getItem(position));
            view.setBackgroundColor(Color.CYAN);
            Timber.d("SelectRecipients: %s", "We selected " + adapter.getItem(position));
            Timber.d("showFriends: %s", "After add: " + m_selectedFriends.size());
        }else{
            m_selectedFriends.remove(adapter.getItem(position));
            view.setBackgroundColor(Color.TRANSPARENT);
            Timber.d("SelectRecipients: %s", "We deselected " + adapter.getItem(position));
            Timber.d("showFriends: %s", "After removed: " + m_selectedFriends.size());
        }

        if(m_selectedFriends.size() == 0) {
            // remove button since we have selected 0 friends now
            m_sendButton.setVisibility(View.GONE);
        }else{
            m_sendButton.setVisibility(View.VISIBLE);
        }

        m_sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                returnList();
            }
        });
    }
}

