package memphis.myapplication.UI;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.Selection;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import memphis.myapplication.Globals;
import memphis.myapplication.R;
import memphis.myapplication.utilities.SharedPrefsManager;

public class LoginFragment extends Fragment {

    final private int MISSING_ELEMENT = 1;
    final private int IMPROPER_DOMAIN = 2;
    final private String PREFIX = "/";
    private String username, password, domain;
    private ProgressBar loginProgressBar;
    private Button loginButton;
    private View loginView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        loginView = inflater.inflate(R.layout.fragment_login, container, false);

        loginProgressBar = loginView.findViewById(R.id.login_progress_bar);
        loginProgressBar.setVisibility(View.GONE);
        loginButton = loginView.findViewById(R.id.login_button);
        setButtonWidth();
        EditText pass = loginView.findViewById(R.id.password_text);
        pass.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
                if (keyEvent.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER){
                    login(view);
                }
                return false;
            }
        });
        // setBackgroundImage();

        final EditText dom = loginView.findViewById(R.id.domain_text);
        dom.setText(PREFIX);

        dom.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if(!s.toString().startsWith(PREFIX)){
                    dom.setText(String.format("%s%s", PREFIX, s.toString()));
                    Selection.setSelection(dom.getText(), dom.getText().length());
                }
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login(v);
            }
        });

        return loginView;
    }

    private void setButtonWidth() {
        DisplayMetrics metrics = this.getResources().getDisplayMetrics();
        int width = metrics.widthPixels/3;
        loginButton.setWidth(width);
    }

    // The image is rather large, so it sometimes takes a while for it to appear since Picasso must
    // resize it first. I'm leaving this here for now in case we do want to put a photo in the
    // background. If not, just delete this function.
    /*private void setBackgroundImage() {
        try {
            ImageView backImg = loginView.findViewById(R.id.backImg);
            Picasso.get().load(R.drawable.hotel).rotate(90f).fit().centerCrop().into(backImg);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }*/

    // we are heading towards a decentralized network (no database), so this is where the user will
    // ask for the user manifest (to be added) to see if their username is unique
    private static void onSignUp() {
        // redirect to a new page
    }

    // Error checking; see comment block below as well
    private boolean isFormFilled(String[] args) {
        if(args.length == 2) {
            return true;
        }
        return false;
    }

    public void login(View view) {
        EditText dom = loginView.findViewById(R.id.domain_text);
        EditText name = loginView.findViewById(R.id.username_text);
        EditText pass = loginView.findViewById(R.id.password_text);
        domain = dom.getText().toString();
        username = name.getText().toString();
        password = pass.getText().toString();
        int attempt = loginAttempt(domain, username, password);
        if(attempt == 0) {
            loginButton.setVisibility(View.GONE);
            loginProgressBar.setVisibility(View.VISIBLE);
            new LoginTask().execute();
        }
        else if(attempt == MISSING_ELEMENT) {
            Toast.makeText(getActivity(), "Please fill out form completely", Toast.LENGTH_LONG).show();
        }
        else if(attempt == IMPROPER_DOMAIN) {
            Toast.makeText(getActivity(), "Please enter a valid domain", Toast.LENGTH_LONG).show();
        }
        else {
            Toast.makeText(getActivity(), "Something went wrong. Please try again", Toast.LENGTH_SHORT).show();
        }
    }

    private int loginAttempt(String domain, String username, String password) {
        String[] array = {username, password};
        if(!domain.startsWith(PREFIX) || domain.toLowerCase().contains("/npchat") ||
                domain.contains(" ") || domain.contains("//"))
            return IMPROPER_DOMAIN;
        if(username.isEmpty() || password.isEmpty()) {
            return MISSING_ELEMENT;
        }
        // if not in database return NOT_IN_DATABASE
        else {
            return 0;
        }
    }
    private class LoginTask extends AsyncTask<Void, Void, Void>{
        @Override
        protected Void doInBackground(Void... voids) {
            // save username and go to mainpage
            SharedPrefsManager.getInstance(getActivity()).setCredentials(username, password, domain);
            Globals.setUsername(username);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Navigation.findNavController(loginView).navigate(R.id.action_loginFragment_to_mainFragment);
        }
    }
}
