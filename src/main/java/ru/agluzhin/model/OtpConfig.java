package ru.agluzhin.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OtpConfig {

    private int codeLength;
    private int ttlSeconds;

    public OtpConfig() {
    }

    public OtpConfig(int codeLength, int ttlSeconds) {
        this.codeLength = codeLength;
        this.ttlSeconds = ttlSeconds;
    }

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
