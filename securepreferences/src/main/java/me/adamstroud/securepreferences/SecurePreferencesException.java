package me.adamstroud.securepreferences;

/**
 * Indicates a problem either or writing encrypted preferences.
 *
 * @author Adam Stroud &#60;<a href="mailto:adam.stroud@gmail.com">adam.stroud@gmail.com</a>&#62;
 */
public class SecurePreferencesException extends RuntimeException {
    public SecurePreferencesException(String message, Throwable cause) {
        super(message, cause);
    }
}
