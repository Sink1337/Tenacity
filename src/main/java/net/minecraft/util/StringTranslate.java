package net.minecraft.util;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.IllegalFormatException;
import java.util.Map;
import java.util.regex.Pattern;

public class StringTranslate {
    /**
     * Pattern that matches numeric variable placeholders in a resource string, such as "%d", "%3$d", "%.2f"
     */
    private static final Pattern numericVariablePattern = Pattern.compile("%(\\d+\\$)?[\\d\\.]*[df]");

    /**
     * A Splitter that splits a string on the first "=". For example, "a=b=c" would split into ["a", "b=c"].
     */
    private static final Splitter equalSignSplitter = Splitter.on('=').limit(2);

    /**
     * Is the private singleton instance of StringTranslate.
     */
    private static StringTranslate instance = new StringTranslate();
    private final Map<String, String> languageList = Maps.<String, String>newHashMap();

    /**
     * The time, in milliseconds since epoch, that this instance was last updated
     */
    private long lastUpdateTimeInMilliseconds;

    public StringTranslate() {
        InputStream inputstream = null;
        try {
            inputstream = StringTranslate.class.getResourceAsStream("/assets/minecraft/lang/en_US.lang");

            if (inputstream == null) {
                throw new IOException("Default language file (en_US.lang) not found in resources.");
            }

            for (String s : IOUtils.readLines(inputstream, Charsets.UTF_8)) {
                if (!s.isEmpty() && s.charAt(0) != 35) {
                    String[] astring = Iterables.toArray(equalSignSplitter.split(s), String.class);

                    if (astring != null && astring.length == 2) {
                        String s1 = astring[0];
                        String s2 = numericVariablePattern.matcher(astring[1]).replaceAll("%$1s");
                        this.languageList.put(s1, s2);
                    }
                }
            }

            this.lastUpdateTimeInMilliseconds = System.currentTimeMillis();
        } catch (IOException var7) {
            System.err.println("Error loading default language file:");
            var7.printStackTrace();
        } finally {
            IOUtils.closeQuietly(inputstream);
        }
    }

    /**
     * Return the StringTranslate singleton instance
     */
    static StringTranslate getInstance() {
        return instance;
    }

    /**
     * Replaces all the current instance's translations with the ones that are passed in.
     */
    public static synchronized void replaceWith(Map<String, String> p_135063_0_) {
        instance.languageList.clear();
        instance.languageList.putAll(p_135063_0_);
        instance.lastUpdateTimeInMilliseconds = System.currentTimeMillis();
    }

    /**
     * Translate a key to current language.
     */
    public synchronized String translateKey(String key) {
        return this.tryTranslateKey(key);
    }

    /**
     * Translate a key to current language applying String.format()
     */
    public synchronized String translateKeyFormat(String key, Object... format) {
        String s = this.tryTranslateKey(key);

        try {
            return String.format(s, format);
        } catch (IllegalFormatException var5) {
            return "Format error: " + s;
        }
    }

    /**
     * Tries to look up a translation for the given key; spits back the key if no result was found.
     */
    private String tryTranslateKey(String key) {
        String s = (String) this.languageList.get(key);
        return s == null ? key : s;
    }

    /**
     * Returns true if the passed key is in the translation table.
     */
    public synchronized boolean isKeyTranslated(String key) {
        return this.languageList.containsKey(key);
    }

    /**
     * Gets the time, in milliseconds since epoch, that this instance was last updated
     */
    public long getLastUpdateTimeInMilliseconds() {
        return this.lastUpdateTimeInMilliseconds;
    }
}