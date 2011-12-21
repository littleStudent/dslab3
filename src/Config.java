import java.util.ResourceBundle;

/**
 * The helper class for reading configuration from .properties file.
 */
public class Config {

    private final ResourceBundle bundle;

    /**
     * Creates instance of Config which reads configuration data form
     * .properties file with given name found in classpath.
     * 
     * @param name the name of the .properties file
     */
    public Config(final String name) {
        this.bundle = ResourceBundle.getBundle(name);
    }

    /**
     * Returns the value as String for the given key.
     * 
     * @param key the property's key
     * @return String value of the property
     */
    public String getString(final String key) {
        return this.bundle.getString(key);
    }

    /**
     * Returns the value as int for the given key.
     * 
     * @param key the property's key
     * @return int value of the property
     */
    public int getInt(final String key) {
        return Integer.parseInt(getString(key));
    }
}
