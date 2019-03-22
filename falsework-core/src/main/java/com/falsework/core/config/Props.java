package com.falsework.core.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.util.Base64;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

/**
 * java props配置文件工具类
 * 此类应该仅用于加载配置
 * Created by LiShiwu on 05/26/2017.
 */
public final class Props extends Properties {
    private static final Logger LOGGER = LoggerFactory.getLogger(Props.class);

    Props() {
    }


    /**
     * 加载配置,从绝对路径
     *
     * @param path 绝对路径
     * @return
     */
    static Props loadFromPath(String path) throws IOException {
        LOGGER.info("load properties from :{}", path);
        try (FileReader reader = new FileReader(path)) {
            Props util = new Props();
            util.load(reader);
            return util;
        }
    }

    /**
     * 从classpath加载
     *
     * @param name 资源名称
     * @return
     * @throws IOException
     */
    static Props loadFromClassPath(String name) throws IOException {
        URL resource = Thread.currentThread().getContextClassLoader().getResource(name);
        if (resource != null) {
            LOGGER.info("load properties from :{}", resource.getPath());
            try (InputStream inputStream = resource.openStream()) {
                Props util = new Props();
                util.load(inputStream);
                return util;
            }
        } else {
            throw new IOException("resource:" + name + " not found");
        }
    }

    /**
     * Get a boolean associated with the given configuration key.
     *
     * @param key The configuration key.
     * @return The associated boolean.
     */
    public boolean getBoolean(String key) {
        String value = getProperty(key);
        if (value != null) {
            return Boolean.valueOf(value);
        } else {
            throw new RuntimeException("Invalid key:" + key);
        }
    }

    /**
     * Get a boolean associated with the given configuration key.
     * If the key doesn't map to an existing object, the default value
     * is returned.
     *
     * @param key          The configuration key.
     * @param defaultValue The default value.
     * @return The associated boolean.
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        String value = getProperty(key);
        if (value != null) {
            return Boolean.valueOf(value);
        } else {
            return defaultValue;
        }
    }

    /**
     * Get a byte associated with the given configuration key.
     *
     * @param key The configuration key.
     * @return The associated byte.
     */
    public byte getByte(String key) {
        String value = getProperty(key);
        if (value != null) {
            return Byte.valueOf(value);
        } else {
            throw new RuntimeException("Invalid key:" + key);
        }
    }

    /**
     * Get a byte associated with the given configuration key.
     * If the key doesn't map to an existing object, the default value
     * is returned.
     *
     * @param key          The configuration key.
     * @param defaultValue The default value.
     * @return The associated byte.
     */
    public byte getByte(String key, byte defaultValue) {
        String value = getProperty(key);
        if (value != null) {
            return Byte.valueOf(value);
        } else {
            return defaultValue;
        }
    }

    /**
     * Get a double associated with the given configuration key.
     *
     * @param key The configuration key.
     * @return The associated double.
     */
    public double getDouble(String key) {
        String value = getProperty(key);
        if (value != null) {
            return Double.valueOf(value);
        } else {
            throw new RuntimeException("Invalid key:" + key);
        }
    }

    /**
     * Get a double associated with the given configuration key.
     * If the key doesn't map to an existing object, the default value
     * is returned.
     *
     * @param key          The configuration key.
     * @param defaultValue The default value.
     * @return The associated double.
     */
    public double getDouble(String key, double defaultValue) {
        String value = getProperty(key);
        if (value != null) {
            return Double.valueOf(value);
        } else {
            return defaultValue;
        }
    }

    /**
     * Get a float associated with the given configuration key.
     *
     * @param key The configuration key.
     * @return The associated float.
     */
    public float getFloat(String key) {
        String value = getProperty(key);
        if (value != null) {
            return Float.valueOf(value);
        } else {
            throw new RuntimeException("Invalid key:" + key);
        }
    }

    /**
     * Get a float associated with the given configuration key.
     * If the key doesn't map to an existing object, the default value
     * is returned.
     *
     * @param key          The configuration key.
     * @param defaultValue The default value.
     * @return The associated float.
     */
    public float getFloat(String key, float defaultValue) {
        String value = getProperty(key);
        if (value != null) {
            return Float.valueOf(value);
        } else {
            throw new RuntimeException("Invalid key:" + key);
        }
    }

    /**
     * Get a int associated with the given configuration key.
     *
     * @param key The configuration key.
     * @return The associated int.
     */
    public int getInt(String key) {
        String value = getProperty(key);
        if (value != null) {
            return Integer.valueOf(value);
        } else {
            throw new RuntimeException("Invalid key:" + key);
        }
    }

    /**
     * Get a int associated with the given configuration key.
     * If the key doesn't map to an existing object, the default value
     * is returned.
     *
     * @param key          The configuration key.
     * @param defaultValue The default value.
     * @return The associated int.
     */
    public int getInt(String key, int defaultValue) {
        String value = getProperty(key);
        if (value != null) {
            return Integer.valueOf(value);
        } else {
            return defaultValue;
        }
    }

    /**
     * Get a long associated with the given configuration key.
     *
     * @param key The configuration key.
     * @return The associated long.
     */
    public long getLong(String key) {
        String value = getProperty(key);
        if (value != null) {
            return Long.valueOf(value);
        } else {
            throw new RuntimeException("Invalid key:" + key);
        }
    }

    /**
     * Get a long associated with the given configuration key.
     * If the key doesn't map to an existing object, the default value
     * is returned.
     *
     * @param key          The configuration key.
     * @param defaultValue The default value.
     * @return The associated long.
     */
    public long getLong(String key, long defaultValue) {
        String value = getProperty(key);
        if (value != null) {
            return Integer.valueOf(value);
        } else {
            return defaultValue;
        }
    }


    /**
     * Get a short associated with the given configuration key.
     *
     * @param key The configuration key.
     * @return The associated short.
     */
    public short getShort(String key) {
        String value = getProperty(key);
        if (value != null) {
            return Short.valueOf(value);
        } else {
            throw new RuntimeException("Invalid key:" + key);
        }
    }

    /**
     * Get a short associated with the given configuration key.
     *
     * @param key          The configuration key.
     * @param defaultValue The default value.
     * @return The associated short.
     */
    public short getShort(String key, short defaultValue) {
        String value = getProperty(key);
        if (value != null) {
            return Short.valueOf(value);
        } else {
            throw new RuntimeException("Invalid key:" + key);
        }
    }

    /**
     * Get a {@link BigDecimal} associated with the given configuration key.
     *
     * @param key The configuration key.
     * @return The associated BigDecimal if key is found and has valid format
     */
    public BigDecimal getBigDecimal(String key) {
        String value = getProperty(key);
        if (value != null) {
            return new BigDecimal(value);
        } else {
            throw new RuntimeException("Invalid key:" + key);
        }
    }

    /**
     * Get a {@link BigDecimal} associated with the given configuration key.
     * If the key doesn't map to an existing object, the default value
     * is returned.
     *
     * @param key          The configuration key.
     * @param defaultValue The default value.
     * @return The associated BigDecimal if key is found and has valid
     * format, default value otherwise.
     */
    public BigDecimal getBigDecimal(String key, BigDecimal defaultValue) {
        String value = getProperty(key);
        if (value != null) {
            return new BigDecimal(value);
        } else {
            return defaultValue;
        }
    }

    /**
     * Get a {@link BigInteger} associated with the given configuration key.
     *
     * @param key The configuration key.
     * @return The associated BigInteger if key is found and has valid format
     */
    public BigInteger getBigInteger(String key) {
        String value = getProperty(key);
        if (value != null) {
            return new BigInteger(value);
        } else {
            throw new RuntimeException("Invalid key:" + key);
        }
    }

    /**
     * Get a {@link BigInteger} associated with the given configuration key.
     * If the key doesn't map to an existing object, the default value
     * is returned.
     *
     * @param key          The configuration key.
     * @param defaultValue The default value.
     * @return The associated BigInteger if key is found and has valid
     * format, default value otherwise.
     */
    public BigInteger getBigInteger(String key, BigInteger defaultValue) {
        String value = getProperty(key);
        if (value != null) {
            return new BigInteger(value);
        } else {
            return defaultValue;
        }
    }

    /**
     * Get an array of strings associated with the given configuration key.
     *
     * @param key The configuration key.
     * @return The associated string array if key is found.
     */
    public String[] getStringArray(String key) {
        String value = getProperty(key);
        if (value != null) {
            return value.split(",");
        } else {
            throw new RuntimeException("Invalid key:" + key);
        }
    }

    /**
     * Get an array of strings associated with the given configuration key.
     * If the key doesn't map to an existing object, the default value
     * is returned.
     *
     * @param key The configuration key.
     * @return The associated string array if key is found.
     */
    public String[] getStringArray(String key, String[] defaultValue) {
        String value = getProperty(key);
        if (value != null) {
            return value.split(",");
        } else {
            return defaultValue;
        }
    }

    /**
     * Get an array of bytes associated with the given configuration key.
     *
     * @param key The configuration key.
     * @return The associated string array if key is found.
     */

    public byte[] getByteArrayBase64(String key) {
        String value = getProperty(key);
        if (value != null) {
            return Base64.getDecoder().decode(value);
        } else {
            throw new RuntimeException("Invalid key:" + key);
        }
    }

    /**
     * Get an array of bytes associated with the given configuration key.
     * If the key doesn't map to an existing object, the default value
     * is returned.
     *
     * @param key The configuration key.
     * @return The associated string array if key is found.
     */
    public byte[] getByteArrayBase64(String key, byte[] defaultValue) {
        String value = getProperty(key);
        if (value != null) {
            return Base64.getDecoder().decode(value);
        } else {
            throw new RuntimeException("Invalid key:" + key);
        }
    }

    /**
     * Get an Props of bytes associated with the given configuration prefix.
     */
    public Props subProps(String prefix) {
        int prefixSlicePos = prefix.length() + 1;
        Props subProps = new Props();
        for (String key : stringPropertyNames()) {
            if (key.startsWith(prefix)) {
                subProps.put(key.substring(prefixSlicePos), getProperty(key));
            }
        }
        return subProps;
    }

    /**
     * sub props exits
     *
     * @param prefix
     * @return
     */
    public boolean existSubProps(String prefix) {
        prefix = prefix + ".";
        for (String key : stringPropertyNames()) {
            if (key.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get named Props of bytes associated with the given configuration prefix.
     */
    public Map<String, Props> subNamedProps(String prefix) {
        int prefixSlicePos = prefix.length();
        Map<String, Props> utilHashtable = new Hashtable<>();
        for (String key : stringPropertyNames()) {
            if (key.startsWith(prefix)) {
                int subKeySlicePos = key.indexOf(".", prefixSlicePos + 1);
                String namedKey = key.substring(prefixSlicePos + 1, subKeySlicePos);
                utilHashtable.computeIfAbsent(namedKey, k -> new Props())
                        .put(key.substring(subKeySlicePos + 1), getProperty(key));
            }
        }
        return utilHashtable;
    }

    @Override
    public synchronized String toString() {
        return "masked";
    }
}
