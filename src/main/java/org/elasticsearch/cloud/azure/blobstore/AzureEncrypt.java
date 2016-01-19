package org.elasticsearch.cloud.azure.blobstore;

import com.google.common.io.ByteStreams;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.IvParameterSpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public class AzureEncrypt {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int IV_LENGTH = 16;
    private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String HASH_ALGORITHM = "PBKDF2WithHmacSHA1";
    private static final String KEY_ALGORITHM = "AES";
    private static final int ITERATIONS = 10000;
    private static final int LENGTH = 128;

    private final String secret;
    private final String salt;
    private SecretKey key;

    public AzureEncrypt(String secret, String salt) {
        this.secret = secret;
        this.salt = salt;
    }

    protected byte[] generateIV() {
        final byte[] iv = new byte[IV_LENGTH];
        RANDOM.nextBytes(iv);
        return iv;
    }

    private Cipher getCipher(int mode, byte[] iv) throws IOException {
        try {
            final SecretKey key=getKey();
            final Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            final AlgorithmParameterSpec ivSpec = new IvParameterSpec(iv);

            cipher.init(mode, key, ivSpec);
            return cipher;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public OutputStream output(OutputStream out) throws IOException {
        final byte[] iv = generateIV();
        final Cipher cipher = getCipher(Cipher.ENCRYPT_MODE, iv);

        out.write(iv);
        return new CipherOutputStream(out, cipher);
    }

    public InputStream input(InputStream in) throws IOException {
        final byte[] iv = new byte[IV_LENGTH];
        ByteStreams.readFully(in, iv);

        final Cipher cipher = getCipher(Cipher.DECRYPT_MODE, iv);
        return new CipherInputStream(in, cipher);
    }

    protected SecretKey getKey() throws NoSuchAlgorithmException, InvalidKeySpecException, UnsupportedEncodingException {
        if(key == null) {
            final SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(HASH_ALGORITHM);
            final SecretKey intermediateKey = keyFactory.generateSecret(new PBEKeySpec(secret.toCharArray(), salt.getBytes("UTF-8"), ITERATIONS, LENGTH));
            key = new SecretKeySpec(intermediateKey.getEncoded(), KEY_ALGORITHM);
        }
        return key;
    }
}
