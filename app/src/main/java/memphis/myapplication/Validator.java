package memphis.myapplication;

import android.content.Context;
import android.util.Log;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnInterestCallback;
import net.named_data.jndn.OnRegisterFailed;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.ValidatorConfig;
import net.named_data.jndn.security.ValidatorConfigError;
import net.named_data.jndn.security.v2.CertificateV2;
import net.named_data.jndn.security.v2.DataValidationFailureCallback;
import net.named_data.jndn.security.v2.DataValidationSuccessCallback;
import net.named_data.jndn.security.v2.InterestValidationFailureCallback;
import net.named_data.jndn.security.v2.InterestValidationSuccessCallback;
import net.named_data.jndn.security.v2.ValidationError;

import java.io.IOException;

import io.realm.Realm;
import memphis.myapplication.RealmObjects.User;

public class Validator {
        private static Context context;
        private String rules;
        private boolean validData;
        private boolean validInterest;

        private class ValidationCallbacks
                implements DataValidationSuccessCallback, DataValidationFailureCallback, InterestValidationSuccessCallback,  InterestValidationFailureCallback {

            public ValidationCallbacks(ValidatorConfig val) {
                valConfig = val;
            }
            private ValidatorConfig valConfig;

            public void
            successCallback(Data interest)
            {
                System.out.println("Data Signature verification: VERIFIED");
                validData = true;
            }

            public void
            failureCallback(Data interest, ValidationError error)
            {
                System.out.println
                        ("Data Signature verification: FAILED. Reason: " + error.getInfo());
            }

            public void
            successCallback(Interest interest) {
                System.out.println("Interest Signature verification: VERIFIED");
                validInterest = true;

            }

            public void
            failureCallback(Interest interest, ValidationError error) {
                System.out.println
                        ("Interest Signature verification: FAILED. Reason: " + error.getInfo());
            }

        }

        public Validator(Data data, String mutual_friend, Interest interest, Context _context)
        {
            context = _context;
            face = Globals.face;
            valConfig = new ValidatorConfig(face);
            rules = "validator\n" +
                    " {\n" +
                    "   rule\n" +
                    "   {\n" +
                    "     id \"Verify potential friend's certificate\"\n" +
                    "     for data\n" +
                    "     filter\n" +
                    "     {\n" +
                    "       type name\n" +
                    "       ; /<prefix>/npChat/userA/KEY/o%85%C2k%97EC%85/self/%FD%00%00%01j%A9%13%A7%86\n" +
                    "       regex ^<npChat><><KEY><><><>$\n" +
                    "     }\n" +
                    "     ; First checker for mutual friend, assume that trust anchors of mutual friends are stored in the app\n" +
                    "     ; On app start up, load all the trust anchor into validato\n" +
                    "     checker\n" +
                    "     {\n" +
                    "       type customized\n" +
                    "       sig-type rsa-sha256\n" +
                    "       key-locator\n" +
                    "       {\n" +
                    "         type name\n" +
                    "         regex ^<npChat><><KEY><>$\n" +
                    "         relation is-strict-prefix-of\n" +
                    "       }\n" +
                    "     }\n" +
                    "     ; Second checker for testbed, assume that testbed root is burned in the app\n" +
                    "     ; On app startup, the testbed root is loaded into the validator config via load\n" +
                    "     ; Commented because we need it as a separate rule, otherwise validator tries to apply all rules at once\n" +
                    "     ;checker\n" +
                    "     ;{\n" +
                    "     ;  type hierarchical\n" +
                    "     ;  sig-type rsa-sha256\n" +
                    "     ;}\n" +
                    "   }\n" +
                    "\n" +
                    "   rule\n" +
                    "   {\n" +
                    "     id \"Verify friend request was from potential friend\"\n" +
                    "     for interest\n" +
                    "     filter\n" +
                    "     {\n" +
                    "       type name\n" +
                    "       ; userB part needs to be edited by the app once to be its own user name\n" +
                    "       ; /<prefix>/npChat/userB/friend-request/mutual-friend\n" +
                    "       name /npChat/" + SharedPrefsManager.getInstance(context).getUsername() + "/friend-request/mutual-friend\n" +
                    "       relation is-prefix-of\n" +
                    "     }\n" +
                    "     checker\n" +
                    "     {\n" +
                    "       type customized\n" +
                    "       sig-type rsa-sha256\n" +
                    "       key-locator\n" +
                    "       {\n" +
                    "         type name\n" +
                    "         regex ^<npChat><><KEY><>$\n" +
                    "         relation is-strict-prefix-of\n" +
                    "       }\n" +
                    "     }\n" +
                    "   }\n" +
                    " }";

            try {
                Realm realm = Realm.getDefaultInstance();
                keyChain = Globals.keyChain;
                // We already trust this friend (friend is self-signed in this example)
                // In real app, we would load the certificate from C into valConfig upon verification
//                valConfig.loadAnchor("test", SharedPrefsManager.getInstance(context).getFriendCert("self_" + mutual_friend));
                valConfig.loadAnchor("test", realm.where(User.class).equalTo("username", mutual_friend).findFirst().getCert());
                valConfig.load(rules, "simple");
                ValidationCallbacks callbacks = new ValidationCallbacks(valConfig);
                Log.d("Pending friend cert: ", data.toString());

                valConfig.validate(data, callbacks, callbacks);

                valConfig.loadAnchor("test", new CertificateV2(data));

                valConfig.validate(interest, callbacks, callbacks);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    public Validator(Data data, String mutual_friend, Context _context)
    {
        context = _context;
        face = Globals.face;
        valConfig = new ValidatorConfig(face);
        rules = "validator\n" +
                " {\n" +
                "   rule\n" +
                "   {\n" +
                "     id \"Verify potential friend's certificate\"\n" +
                "     for data\n" +
                "     filter\n" +
                "     {\n" +
                "       type name\n" +
                "       ; /<prefix>/npChat/userA/KEY/o%85%C2k%97EC%85/self/%FD%00%00%01j%A9%13%A7%86\n" +
                "       regex ^<npChat><><KEY><><><>$\n" +
                "     }\n" +
                "     ; First checker for mutual friend, assume that trust anchors of mutual friends are stored in the app\n" +
                "     ; On app start up, load all the trust anchor into validato\n" +
                "     checker\n" +
                "     {\n" +
                "       type customized\n" +
                "       sig-type rsa-sha256\n" +
                "       key-locator\n" +
                "       {\n" +
                "         type name\n" +
                "         regex ^<npChat><><KEY><>$\n" +
                "         relation is-strict-prefix-of\n" +
                "       }\n" +
                "     }\n" +
                "     ; Second checker for testbed, assume that testbed root is burned in the app\n" +
                "     ; On app startup, the testbed root is loaded into the validator config via load\n" +
                "     ; Commented because we need it as a separate rule, otherwise validator tries to apply all rules at once\n" +
                "     ;checker\n" +
                "     ;{\n" +
                "     ;  type hierarchical\n" +
                "     ;  sig-type rsa-sha256\n" +
                "     ;}\n" +
                "   }\n" +
                "\n" +
                "   rule\n" +
                "   {\n" +
                "     id \"Verify friend request was from potential friend\"\n" +
                "     for interest\n" +
                "     filter\n" +
                "     {\n" +
                "       type name\n" +
                "       ; userB part needs to be edited by the app once to be its own user name\n" +
                "       ; /<prefix>/npChat/userB/friend-request/mutual-friend\n" +
                "       name /npChat/" + SharedPrefsManager.getInstance(context).getUsername() + "/friend-request/mutual-friend\n" +
                "       relation is-prefix-of\n" +
                "     }\n" +
                "     checker\n" +
                "     {\n" +
                "       type customized\n" +
                "       sig-type rsa-sha256\n" +
                "       key-locator\n" +
                "       {\n" +
                "         type name\n" +
                "         regex ^<npChat><><KEY><>$\n" +
                "         relation is-strict-prefix-of\n" +
                "       }\n" +
                "     }\n" +
                "   }\n" +
                " }";

        try {
            Realm realm = Realm.getDefaultInstance();
            keyChain = Globals.keyChain;
            valConfig.loadAnchor("test", realm.where(User.class).equalTo("username", mutual_friend).findFirst().getCert());
            valConfig.load(rules, "simple");
            ValidationCallbacks callbacks = new ValidationCallbacks(valConfig);
            Log.d("Pending friend cert: ", data.toString());

            valConfig.validate(data, callbacks, callbacks);
            validInterest = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

        public boolean valid() {
            return (validData && validInterest);
        }

    public KeyChain keyChain;
    public Face face;
    public ValidatorConfig valConfig;
    private Interest friendRequest;
}