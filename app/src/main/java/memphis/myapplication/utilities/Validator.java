package memphis.myapplication.utilities;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.ValidatorConfig;
import net.named_data.jndn.security.v2.CertificateV2;
import net.named_data.jndn.security.v2.DataValidationFailureCallback;
import net.named_data.jndn.security.v2.DataValidationSuccessCallback;
import net.named_data.jndn.security.v2.InterestValidationFailureCallback;
import net.named_data.jndn.security.v2.InterestValidationSuccessCallback;
import net.named_data.jndn.security.v2.ValidationError;

import memphis.myapplication.Globals;
import memphis.myapplication.data.RealmRepository;
import timber.log.Timber;

public class Validator {
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

        public Validator(Data data, String mutual_friend, Interest interest)
        {
            face = Globals.face;
            valConfig = new ValidatorConfig(face);
            rules = "validator\n" +
                    "\n" +
                    "{\n" +
                    "\n" +
                    "  rule\n" +
                    "\n" +
                    "  {\n" +
                    "\n" +
                    "    id \"Verify potential friend's certificate\n" +
                    "\n" +
                    "        (data) signed by key which has\n" +
                    "\n" +
                    "        mutual friend's certificate\"\n" +
                    "\n" +
                    "    for data\n" +
                    "\n" +
                    "    filter\n" +
                    "\n" +
                    "    {\n" +
                    "\n" +
                    "      type name\n" +
                    "\n" +
                    "      regex ^([^<npChat>]+)<npChat><><KEY><><><>$\n" +
                    "\n" +
                    "      ; Let's not accept anything that starts with npChat or KEY and also use +\n" +
                    "\n" +
                    "    }\n" +
                    "\n" +
                    "    checker\n" +
                    "\n" +
                    "    {\n" +
                    "\n" +
                    "      type customized\n" +
                    "\n" +
                    "      sig-type rsa-sha256\n" +
                    "\n" +
                    "      key-locator\n" +
                    "\n" +
                    "      {\n" +
                    "\n" +
                    "        type name\n" +
                    "\n" +
                    "        regex ^([^<npChat>]+)<npChat><><KEY><>$\n" +
                    "\n" +
                    "      }\n" +
                    "\n" +
                    "    }\n" +
                    "\n" +
                    "  }\n" +
                    "\n" +
                    "  ; no trust schema here because we will load it in code\n" +
                    "\n" +
                    "\n" +
                    "  rule\n" +
                    "\n" +
                    "  {\n" +
                    "\n" +
                    "    id \"Verify friend request\n" +
                    "\n" +
                    "        from potential friend\n" +
                    "\n" +
                    "        via mutual friend cert\"\n" +
                    "\n" +
                    "    for interest\n" +
                    "\n" +
                    "    filter\n" +
                    "\n" +
                    "    {\n" +
                    "\n" +
                    "      type name\n" +
                    "\n" +
                    "      ; * means zero or more times, + means 1 or more, should we allow the name to be started from /npChat\n" +
                    "\n" +
                    "      ; Do not start with npChat and have atleast 1 component before npChat\n" +
                    "\n" +
                    "      regex ^([^<npChat>]+)<npChat><><friend-request><mutual-friend><>+<npChat><><KEY><><><>$\n" +
                    "\n" +
                    "    }\n" +
                    "\n" +
                    "    checker\n" +
                    "\n" +
                    "    {\n" +
                    "\n" +
                    "      type customized\n" +
                    "\n" +
                    "      sig-type rsa-sha256\n" +
                    "\n" +
                    "      key-locator\n" +
                    "\n" +
                    "      {\n" +
                    "\n" +
                    "        type name\n" +
                    "\n" +
                    "        regex ^([^<npChat>]+)<npChat><><KEY><>$\n" +
                    "\n" +
                    "      }\n" +
                    "\n" +
                    "    }\n" +
                    "\n" +
                    "  }\n" +
                    "\n" +
                    "  rule\n" +
                    "\n" +
                    "  {\n" +
                    "\n" +
                    "    id \"Verify friend request\n" +
                    "\n" +
                    "        from potential friend\n" +
                    "\n" +
                    "        via testbed cert\"\n" +
                    "\n" +
                    "    for interest\n" +
                    "\n" +
                    "    filter\n" +
                    "\n" +
                    "    {\n" +
                    "\n" +
                    "      type name\n" +
                    "\n" +
                    "      regex ^([^<npChat>]+)<npChat><><friend-request><testbed><>+<npChat><><KEY><><><>$\n" +
                    "\n" +
                    "    }\n" +
                    "\n" +
                    "    checker\n" +
                    "\n" +
                    "    {\n" +
                    "\n" +
                    "      type hierarchical\n" +
                    "\n" +
                    "      sig-type rsa-sha256\n" +
                    "\n" +
                    "    }\n" +
                    "\n" +
                    "  }\n" +
                    "\n" +
                    "}";


            try {
                keyChain = Globals.keyChain;
                // We already trust this friend (friend is self-signed in this example)
                // In real app, we would load the certificate from C into valConfig upon verification
//                valConfig.loadAnchor("test", SharedPrefsManager.getInstance(context).getFriendCert("self_" + mutual_friend));
                RealmRepository realmRepository = RealmRepository.getInstanceForNonUI();
                valConfig.loadAnchor("test", realmRepository.getFriend(mutual_friend).getCert());
                realmRepository.close();
                valConfig.load(rules, "simple");
                ValidationCallbacks callbacks = new ValidationCallbacks(valConfig);
                Timber.d("Pending friend cert: %s", data.toString());

                valConfig.validate(data, callbacks, callbacks);

                valConfig.loadAnchor("test", new CertificateV2(data));

                valConfig.validate(interest, callbacks, callbacks);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    public Validator(Data data, String mutual_friend)
    {
        face = Globals.face;
        valConfig = new ValidatorConfig(face);
        rules = "validator\n" +
                "\n" +
                "{\n" +
                "\n" +
                "  rule\n" +
                "\n" +
                "  {\n" +
                "\n" +
                "    id \"Verify potential friend's certificate\n" +
                "\n" +
                "        (data) signed by key which has\n" +
                "\n" +
                "        mutual friend's certificate\"\n" +
                "\n" +
                "    for data\n" +
                "\n" +
                "    filter\n" +
                "\n" +
                "    {\n" +
                "\n" +
                "      type name\n" +
                "\n" +
                "      regex ^([^<npChat>]+)<npChat><><KEY><><><>$\n" +
                "\n" +
                "      ; Let's not accept anything that starts with npChat or KEY and also use +\n" +
                "\n" +
                "    }\n" +
                "\n" +
                "    checker\n" +
                "\n" +
                "    {\n" +
                "\n" +
                "      type customized\n" +
                "\n" +
                "      sig-type rsa-sha256\n" +
                "\n" +
                "      key-locator\n" +
                "\n" +
                "      {\n" +
                "\n" +
                "        type name\n" +
                "\n" +
                "        regex ^([^<npChat>]+)<npChat><><KEY><>$\n" +
                "\n" +
                "      }\n" +
                "\n" +
                "    }\n" +
                "\n" +
                "  }\n" +
                "\n" +
                "  ; no trust schema here because we will load it in code\n" +
                "\n" +
                "\n" +
                "  rule\n" +
                "\n" +
                "  {\n" +
                "\n" +
                "    id \"Verify friend request\n" +
                "\n" +
                "        from potential friend\n" +
                "\n" +
                "        via mutual friend cert\"\n" +
                "\n" +
                "    for interest\n" +
                "\n" +
                "    filter\n" +
                "\n" +
                "    {\n" +
                "\n" +
                "      type name\n" +
                "\n" +
                "      ; * means zero or more times, + means 1 or more, should we allow the name to be started from /npChat\n" +
                "\n" +
                "      ; Do not start with npChat and have atleast 1 component before npChat\n" +
                "\n" +
                "      regex ^([^<npChat>]+)<npChat><><friend-request><mutual-friend><>+<npChat><><KEY><><><>$\n" +
                "\n" +
                "    }\n" +
                "\n" +
                "    checker\n" +
                "\n" +
                "    {\n" +
                "\n" +
                "      type customized\n" +
                "\n" +
                "      sig-type rsa-sha256\n" +
                "\n" +
                "      key-locator\n" +
                "\n" +
                "      {\n" +
                "\n" +
                "        type name\n" +
                "\n" +
                "        regex ^([^<npChat>]+)<npChat><><KEY><>$\n" +
                "\n" +
                "      }\n" +
                "\n" +
                "    }\n" +
                "\n" +
                "  }\n" +
                "\n" +
                "  rule\n" +
                "\n" +
                "  {\n" +
                "\n" +
                "    id \"Verify friend request\n" +
                "\n" +
                "        from potential friend\n" +
                "\n" +
                "        via testbed cert\"\n" +
                "\n" +
                "    for interest\n" +
                "\n" +
                "    filter\n" +
                "\n" +
                "    {\n" +
                "\n" +
                "      type name\n" +
                "\n" +
                "      regex ^([^<npChat>]+)<npChat><><friend-request><testbed><>+<npChat><><KEY><><><>$\n" +
                "\n" +
                "    }\n" +
                "\n" +
                "    checker\n" +
                "\n" +
                "    {\n" +
                "\n" +
                "      type hierarchical\n" +
                "\n" +
                "      sig-type rsa-sha256\n" +
                "\n" +
                "    }\n" +
                "\n" +
                "  }\n" +
                "\n" +
                "}";

        try {
            keyChain = Globals.keyChain;
            RealmRepository realmRepository = RealmRepository.getInstanceForNonUI();
            valConfig.loadAnchor("test", realmRepository.getFriend(mutual_friend).getCert());
            realmRepository.close();
            valConfig.load(rules, "simple");
            ValidationCallbacks callbacks = new ValidationCallbacks(valConfig);
            Timber.d("Pending friend cert: %s", data.toString());

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