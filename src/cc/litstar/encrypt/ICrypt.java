package cc.litstar.encrypt;

import java.io.ByteArrayOutputStream;

/**
 * Interface of crypt
 */
public interface ICrypt {
    byte[] encrypt(byte[] data);
    byte[] decrypt(byte[] data);
    void encrypt(byte[] data, ByteArrayOutputStream stream);
    void encrypt(byte[] data, int length, ByteArrayOutputStream stream);
    void decrypt(byte[] data, ByteArrayOutputStream stream);
    void decrypt(byte[] data, int length, ByteArrayOutputStream stream);
    int getIVLength();
    int getKeyLength();
}