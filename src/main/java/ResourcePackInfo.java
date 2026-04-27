package net.clusters_prj.replugin;

import java.nio.ByteBuffer;
import java.util.Base64;

public class ResourcePackInfo {

    private final String name;
    private final String url;
    private final String sha1Hash;
    private final byte[] hashBytes;

    public ResourcePackInfo(String name, String url, String sha1Hash) {
        this.name = name;
        this.url = url;
        this.sha1Hash = sha1Hash;
        this.hashBytes = sha1ToBytes(sha1Hash);
    }

    /**
     * SHA1ハッシュ文字列をバイト配列に変換
     */
    private static byte[] sha1ToBytes(String sha1Hash) {
        byte[] bytes = new byte[20];
        for (int i = 0; i < 20; i++) {
            bytes[i] = (byte) Integer.parseInt(sha1Hash.substring(i * 2, i * 2 + 2), 16);
        }
        return bytes;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public String getHash() {
        // Minecraft API形式: SHA1を16進数文字列で返す
        return sha1Hash;
    }

    public byte[] getHashBytes() {
        return hashBytes;
    }

    @Override
    public String toString() {
        return String.format("ResourcePackInfo{name='%s', url='%s', hash='%s'}", name, url, sha1Hash);
    }
}
