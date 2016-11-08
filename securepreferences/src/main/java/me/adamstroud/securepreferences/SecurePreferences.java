package me.adamstroud.securepreferences;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyProperties;
import android.util.ArraySet;
import android.util.Base64;
import android.util.Log;

import com.google.PRNGFixes;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.security.auth.x500.X500Principal;

/**
 * TODO
 *
 * @author Adam Stroud &#60;<a href="mailto:adam.stroud@gmail.com">adam.stroud@gmail.com</a>&#62;
 */
public class SecurePreferences implements SharedPreferences {
    private static final String TAG = SecurePreferences.class.getSimpleName();
    private static final String KEYSTORE_PROVIDER = "AndroidKeyStore";
    private static final String CIPHER_PROVIDER = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            ? "AndroidKeyStoreBCWorkaround"
            : "AndroidOpenSSL";
    private static final String ALIAS = "securePreferenceKey";
    private static final int BASE_64_FLAGS = Base64.DEFAULT;
    private static final String CIPHER_TRANSFORMATION = "RSA/ECB/PKCS1Padding";

    private final SharedPreferences sharedPreferences;

    private static boolean prngFixed = false;

    static {
        if (!prngFixed) {
            PRNGFixes.apply();
            prngFixed = true;
        }
    }

    public SecurePreferences(SharedPreferences sharedPreferences, Context context) {
        this.sharedPreferences = sharedPreferences;

        try {
            KeyStore keystore = KeyStore.getInstance(KEYSTORE_PROVIDER);
            keystore.load(null);

            KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry) keystore.getEntry(ALIAS, null);

            if (entry == null) {
                Calendar end = Calendar.getInstance();
                end.add(Calendar.YEAR, 100);

                @SuppressLint("InlinedApi")
                KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, KEYSTORE_PROVIDER);

                KeyPairGeneratorSpec.Builder builder = new KeyPairGeneratorSpec.Builder(context)
                        .setSubject(new X500Principal("CN=" + ALIAS))
                        .setSerialNumber(BigInteger.TEN)
                        .setStartDate(new Date())
                        .setEndDate(end.getTime())
                        .setAlias(ALIAS);

                keyPairGenerator.initialize(builder.build());

                keyPairGenerator.generateKeyPair();
            }
        } catch (IOException
                | CertificateException
                | NoSuchAlgorithmException
                | UnrecoverableEntryException
                | KeyStoreException
                | NoSuchProviderException
                | InvalidAlgorithmParameterException e) {
            Log.w(TAG, "Could not init", e);
            throw new RuntimeException("Could not init Secure Preferences", e);
        }
    }

    private byte[] decrypt(String ciphertext) {
        try {
            KeyStore keystore = KeyStore.getInstance(KEYSTORE_PROVIDER);
            keystore.load(null);
            KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry) keystore.getEntry(ALIAS, null);

            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION, CIPHER_PROVIDER);
            cipher.init(Cipher.DECRYPT_MODE, entry.getPrivateKey());

            return cipher.doFinal(Base64.decode(ciphertext, BASE_64_FLAGS));
        } catch(NoSuchPaddingException
                | NoSuchAlgorithmException
                | KeyStoreException
                | UnrecoverableEntryException
                | InvalidKeyException
                | BadPaddingException
                | IllegalBlockSizeException
                | NoSuchProviderException
                | CertificateException
                | IOException e) {
            throw new SecurePreferencesException("Could not decrypt preference", e);
        }
    }

    @Override
    public Map<String, ?> getAll() {
        throw new UnsupportedOperationException("Not Yet Implemented");
    }
    
    @Override
    public String getString(String key, String defValue) throws SecurePreferencesException {
        String value = defValue;

        if (sharedPreferences.contains(key)) {
            value = new String(decrypt(sharedPreferences.getString(key, null)));
        }

        return value;
    }

    @Override
    public Set<String> getStringSet(String key, Set<String> defValues) throws SecurePreferencesException {
        Set<String> values = defValues;

        if (sharedPreferences.contains(key)) {
            Set<String> ciphertextValues = sharedPreferences.getStringSet(key, null);
            values = createSet();

            for (String ciphertextValue : ciphertextValues) {
                values.add(new String(decrypt(ciphertextValue)));
            }

            return values;
        }

        return Collections.unmodifiableSet(values);
    }

    @Override
    public int getInt(String key, int defValue) throws SecurePreferencesException {
        int value = defValue;

        if (sharedPreferences.contains(key)) {
            byte[] bytes = decrypt(sharedPreferences.getString(key, null));
            value = ByteBuffer.allocate(bytes.length).put(bytes).getInt(0);
        }

        return value;
    }

    @Override
    public long getLong(String key, long defValue) throws SecurePreferencesException {
        long value = defValue;

        if (sharedPreferences.contains(key)) {
            byte[] bytes = decrypt(sharedPreferences.getString(key, null));
            value = ByteBuffer.allocate(bytes.length).put(bytes).getLong(0);
        }

        return value;
    }

    @Override
    public float getFloat(String key, float defValue) throws SecurePreferencesException {
        float value = defValue;

        if (sharedPreferences.contains(key)) {
            byte[] bytes = decrypt(sharedPreferences.getString(key, null));
            value = ByteBuffer.allocate(bytes.length).put(bytes).getFloat(0);
        }

        return value;
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) throws SecurePreferencesException {
        boolean value = defValue;

        if (sharedPreferences.contains(key)) {
            byte[] bytes = decrypt(sharedPreferences.getString(key, null));
            value = (bytes[0] == 1);
        }

        return value;
    }

    @Override
    public boolean contains(String key) {
        return sharedPreferences.contains(key);
    }

    @Override
    public Editor edit() {
        return new SecureEditor(sharedPreferences.edit());
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener);
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener);
    }

    private Set<String> createSet() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return new ArraySet<>();
        } else {
            return new HashSet<>();
        }
    }

    public static class SecureEditor implements SharedPreferences.Editor {
        private Editor editor;

        /* default */ SecureEditor(Editor editor) {
            this.editor = editor;
        }

        private String encrypt(byte... plaintext) {
            try {
                KeyStore keystore = KeyStore.getInstance(KEYSTORE_PROVIDER);
                keystore.load(null);
                KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry) keystore.getEntry(ALIAS, null);

                Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION, CIPHER_PROVIDER);
                cipher.init(Cipher.ENCRYPT_MODE, entry.getCertificate().getPublicKey());

                return Base64.encodeToString(cipher.doFinal(plaintext), BASE_64_FLAGS);
            } catch(NoSuchPaddingException
                    | NoSuchAlgorithmException
                    | KeyStoreException
                    | UnrecoverableEntryException
                    | InvalidKeyException
                    | BadPaddingException
                    | IllegalBlockSizeException
                    | NoSuchProviderException
                    | CertificateException
                    | IOException e) {
                throw new SecurePreferencesException("Could not encrypt preference", e);
            }
        }

        private Set<String> createSet(int size) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return new ArraySet<>(size);
            } else {
                return new HashSet<>(size);
            }
        }

        /**
         * TODO
         *
         * @param key
         * @param value
         * @return
         * @throws SecurePreferencesException
         */
        @Override
        public SharedPreferences.Editor putString(String key, String value) throws SecurePreferencesException {
            String ciphertext = null;

            if (value != null) {
                ciphertext = encrypt(value.getBytes());
            }

            editor.putString(key, ciphertext);
            return this;
        }

        @Override
        public SharedPreferences.Editor putStringSet(String key, Set<String> values) throws SecurePreferencesException {
            if (values == null) {
                editor.putStringSet(key, null);
            } else {
                Set<String> encryptedValues = createSet(values.size());

                for (String value : values) {
                    encryptedValues.add(encrypt(value.getBytes()));
                }

                editor.putStringSet(key, encryptedValues);
            }

            return this;
        }

        /**
         * TODO
         *
         * @param key
         * @param value
         * @return
         * @throws SecurePreferencesException
         */
        @Override
        public SharedPreferences.Editor putInt(String key, int value) throws SecurePreferencesException {
            @SuppressLint("InlinedApi")
            final int allocationSize = Integer.BYTES;

            editor.putString(key, encrypt(ByteBuffer.allocate(allocationSize).putInt(value).array()));
            return this;
        }

        /**
         * TODO
         *
         * @param key
         * @param value
         * @return
         * @throws SecurePreferencesException
         */
        @Override
        public SharedPreferences.Editor putLong(String key, long value) throws SecurePreferencesException {
            @SuppressLint("InlinedApi")
            final int allocationSize = Long.BYTES;

            editor.putString(key, encrypt(ByteBuffer.allocate(allocationSize).putLong(value).array()));
            return this;
        }

        /**
         * TODO
         *
         * @param key
         * @param value
         * @return
         * @throws SecurePreferencesException
         */
        @Override
        public SharedPreferences.Editor putFloat(String key, float value) throws SecurePreferencesException {
            @SuppressLint("InlinedApi")
            final int allocationSize = Float.BYTES;

            editor.putString(key, encrypt(ByteBuffer.allocate(allocationSize).putFloat(value).array()));
            return this;
        }

        /**
         * TODO
         *
         * @param key
         * @param value
         * @return
         * @throws SecurePreferencesException
         */
        @Override
        public SharedPreferences.Editor putBoolean(String key, boolean value) throws SecurePreferencesException {
            editor.putString(key, encrypt((value ? (byte) 1 : (byte) 0)));
            return this;
        }

        @Override
        public SharedPreferences.Editor remove(String key) {
            editor.remove(key);
            return this;
        }

        @Override
        public SharedPreferences.Editor clear() {
            editor.clear();
            return this;
        }

        @Override
        public boolean commit() {
            return editor.commit();
        }

        @Override
        public void apply() {
            editor.apply();
        }
    }
}
