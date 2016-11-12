package me.adamstroud.securepreferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.security.Provider;
import java.security.Security;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Test cases for {@link SecurePreferences}.
 *
 * @author Adam Stroud &#60;<a href="mailto:adam.stroud@gmail.com">adam.stroud@gmail.com</a>&#62;
 */
@RunWith(AndroidJUnit4.class)
public class SecurePreferencesTest {
    private static final String TAG = SecurePreferencesTest.class.getSimpleName();
    private static final String ALIAS = "testKey";

    private KeyStore keyStore;
    private SharedPreferences sharedPreferences;
    private SecurePreferences securePreferences;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @BeforeClass
    public static void beforeClass() {
        for (Provider provider : Security.getProviders()) {
            Log.d(TAG, "Provider: " + provider.getName());

            for (Provider.Service service : provider.getServices()) {
                Log.d(TAG, "\t" + service.getAlgorithm());
            }
        }
    }

    @Before
    public void setUp() throws Exception {
        Context appContext = InstrumentationRegistry.getTargetContext();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext);
        keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);

        Enumeration<String> aliases = keyStore.aliases();

        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            keyStore.deleteEntry(alias);
        }

        assertThat(keyStore.size(), is(equalTo(0)));
        assertThat(keyStore.getEntry(ALIAS, null), is(nullValue(KeyStore.Entry.class)));
        assertThat(sharedPreferences.edit().clear().commit(), is(true));
        assertThat(sharedPreferences.getAll().isEmpty(), is(true));
        securePreferences = new SecurePreferences(sharedPreferences, appContext);
    }

    @After
    public void tearDown() throws Exception {
        keyStore.deleteEntry(ALIAS);
        assertThat(sharedPreferences.edit().clear().commit(), is(true));
    }

    @Test
    public void testString() throws Exception {
        final String key = "stringKey";
        final String value = "value";

        securePreferences.edit().putString(key, value).commit();

        assertThat(securePreferences.getString(key, null), is(equalTo(value)));
        assertThat(sharedPreferences.getString(key, null), is(not(equalTo(value))));
    }

    @Test
    public void testInt() throws Exception {
        final String key = "intKey";
        final int value = Integer.MAX_VALUE;

        securePreferences.edit().putInt(key, value).commit();

        assertThat(securePreferences.getInt(key, -1), is(equalTo(value)));
        assertThat(sharedPreferences.getString(key, null), is(not(equalTo(String.valueOf(value)))));
    }

    @Test
    public void testLong() throws Exception {
        final String key = "longKey";
        final long value = Long.MAX_VALUE;

        securePreferences.edit().putLong(key, value).commit();

        assertThat(securePreferences.getLong(key, -1), is(equalTo(value)));
        assertThat(sharedPreferences.getString(key, null), is(not(equalTo(String.valueOf(value)))));
    }

    @Test
    public void testFloat() throws Exception {
        final String key = "floatKey";
        final float value = Float.MAX_VALUE;

        securePreferences.edit().putFloat(key, value).commit();

        assertThat(securePreferences.getFloat(key, -1), is(equalTo(value)));
        assertThat(sharedPreferences.getString(key, null), is(not(equalTo(String.valueOf(value)))));
    }

    @Test
    public void testBoolean_true() throws Exception {
        final String key = "booleanKey";
        final boolean value = true;

        securePreferences.edit().putBoolean(key, value).commit();

        assertThat(securePreferences.getBoolean(key, false), is(equalTo(value)));
        assertThat(sharedPreferences.getString(key, null), is(not(equalTo(String.valueOf(value)))));
    }

    @Test
    public void testBoolean_false() throws Exception {
        final String key = "booleanKey";
        final boolean value = false;

        securePreferences.edit().putBoolean(key, value).commit();

        assertThat(securePreferences.getBoolean(key, true), is(equalTo(value)));
        assertThat(sharedPreferences.getString(key, null), is(not(equalTo(String.valueOf(value)))));
    }

    @Test
    public void testStringSet() throws Exception {
        final String key = "stringSetKey";
        final Set<String> values = new HashSet<>();

        for (int i = 0; i < 10; i++) {
            assertThat(values.add("String" + (i + 1)), is(true));
        }

        securePreferences.edit().putStringSet(key, values).commit();

        assertThat(securePreferences.getStringSet(key, null), is(equalTo(values)));

        Set<String> actual = sharedPreferences.getStringSet(key, null);

        assertThat(values.size(), is(equalTo(actual.size())));

        for (String value : values) {
            assertThat(actual, not(hasItem(value)));
        }
    }

    @Test
    public void testStringSet_edit() throws Exception {
        thrown.expect(UnsupportedOperationException.class);

        final String key = "stringSetKey";
        final Set<String> values = new HashSet<>();

        for (int i = 0; i < 10; i++) {
            assertThat(values.add("String" + (i + 1)), is(true));
        }

        securePreferences.edit().putStringSet(key, values).commit();
        securePreferences.getStringSet(key, null).clear();
    }

    @Test
    public void testStringSet_putNull() throws Exception {
        final String key = "stringSetKey";
        final Set<String> values = new HashSet<>();

        for (int i = 0; i < 10; i++) {
            assertThat(values.add("String" + (i + 1)), is(true));
        }

        securePreferences.edit().putStringSet(key, values).commit();
        assertThat(sharedPreferences.contains(key), is(true));
        securePreferences.edit().putStringSet(key, null).commit();
        assertThat(sharedPreferences.contains(key), is(false));
    }

    @Test
    public void testGetAll() throws Exception {
        final String stringKey = "stringKey";
        final String stringValue = "stringValue";

        final String booleanKey = "booleanKey";
        final boolean booleanValue = true;

        final String intKey = "intKey";
        final int intValue = Integer.MIN_VALUE;

        final String floatKey = "floatKey";
        final float floatValue = Float.MIN_VALUE;

        final String longKey = "longKey";
        final long longValue = Long.MIN_VALUE;

        securePreferences.edit()
                .putString(stringKey, stringValue)
                .putBoolean(booleanKey, booleanValue)
                .putInt(intKey, intValue)
                .putFloat(floatKey, floatValue)
                .putLong(longKey, longValue)
                .commit();

        Map<String, byte[]> decryptedValues = securePreferences.getAll();

        assertThat(ByteBuffer.allocate(Integer.BYTES).put(decryptedValues.get(intKey)).getInt(0), is(equalTo(intValue)));
        assertThat(ByteBuffer.allocate(Float.BYTES).put(decryptedValues.get(floatKey)).getFloat(0), is(equalTo(floatValue)));
        assertThat(ByteBuffer.allocate(Long.BYTES).put(decryptedValues.get(longKey)).getLong(0), is(equalTo(longValue)));
        assertThat(new String(decryptedValues.get(stringKey)), is(equalTo(stringValue)));
        assertThat(decryptedValues.get(booleanKey)[0] == 1, is(equalTo(booleanValue)));
    }

    @Test
    public void testGetAll_edit() throws Exception {
        thrown.expect(UnsupportedOperationException.class);
        securePreferences.edit().putLong("key", Long.MAX_VALUE).commit();
        securePreferences.getAll().clear();
    }

    @Test
    public void testApply() throws Exception {
        final Lock lock = new ReentrantLock();
        final Condition callback = lock.newCondition();
        final String key = "key";
        final String value = "value";
        final SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
                assertThat(Thread.currentThread(), is(equalTo(Looper.getMainLooper().getThread())));
                assertThat(sharedPreferences.getString(key, null), is(not(equalTo(value))));
                assertThat(securePreferences.getString(key, null), is(equalTo(value)));

                lock.lock();
                try {
                    callback.signalAll();
                } finally {
                    lock.unlock();
                }
            }
        };

        lock.lock();

        try {
            sharedPreferences.registerOnSharedPreferenceChangeListener(listener);
            securePreferences.edit().putString(key, value).apply();

            assertThat(callback.await(30, TimeUnit.SECONDS), is(true));
        } finally {
            lock.unlock();
        }

        sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener);
    }
}
