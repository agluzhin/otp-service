package ru.agluzhin.model;

public class OtpConfig {

    private int codeLength;
    private int ttlSeconds;

    public OtpConfig() {}

    public OtpConfig(int codeLength, int ttlSeconds) {
        this.codeLength = codeLength;
        this.ttlSeconds = ttlSeconds;
    }

    public int getCodeLength() { return codeLength; }
    public void setCodeLength(int codeLength) { this.codeLength = codeLength; }

    public int getTtlSeconds() { return ttlSeconds; }
    public void setTtlSeconds(int ttlSeconds) { this.ttlSeconds = ttlSeconds; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OtpConfig other)) return false;
        return codeLength == other.codeLength && ttlSeconds == other.ttlSeconds;
    }

    @Override
    public int hashCode() {
        return 31 * codeLength + ttlSeconds;
    }

}
