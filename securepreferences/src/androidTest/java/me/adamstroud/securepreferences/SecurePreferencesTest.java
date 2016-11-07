package me.adamstroud.securepreferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.security.KeyStore;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Test cases for {@link SecurePreferences}.
 *
 * @author Adam Stroud &#60;<a href="mailto:adam.stroud@gmail.com">adam.stroud@gmail.com</a>&#62;
 */
@RunWith(AndroidJUnit4.class)
public class SecurePreferencesTest {
    private static final String ALIAS = "testKey";

    private KeyStore keyStore;
    private SharedPreferences sharedPreferences;
    private SecurePreferences securePreferences;

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

        assertEquals(0, keyStore.size());
        assertNull(keyStore.getEntry(ALIAS, null));
        assertTrue(sharedPreferences.getAll().isEmpty());
        securePreferences = new SecurePreferences(sharedPreferences, appContext);
    }

    @After
    public void tearDown() throws Exception {
        keyStore.deleteEntry(ALIAS);
        assertTrue(sharedPreferences.edit().clear().commit());
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
            assertTrue(values.add("String" + (i + 1)));
        }

        securePreferences.edit().putStringSet(key, values).commit();

        assertThat(securePreferences.getStringSet(key, null), is(equalTo(values)));

        Set<String> actual = sharedPreferences.getStringSet(key, null);
        assertEquals(values.size(), actual.size());

        for (String value : values) {
            assertThat(actual, not(contains(value)));
        }
    }
}
