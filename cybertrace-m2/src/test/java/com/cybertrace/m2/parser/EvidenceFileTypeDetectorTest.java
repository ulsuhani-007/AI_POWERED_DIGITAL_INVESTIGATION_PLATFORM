package com.cybertrace.m2.parser;

import com.cybertrace.m2.exception.InvalidEvidenceFileException;
import com.cybertrace.m2.model.EvidenceFileType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EvidenceFileTypeDetectorTest {

    private EvidenceFileTypeDetector detector;

    @BeforeEach
    void setUp() {
        List<EvidenceParser> parsers = List.of(
                new LoginEvidenceParser(),
                new BrowserActivityEvidenceParser(),
                new FileAccessEvidenceParser(),
                new NetworkEventsEvidenceParser()
        );
        detector = new EvidenceFileTypeDetector(parsers);
    }

    @Test
    @DisplayName("Should detect LOGIN file type from login headers")
    void testDetectLogin() {
        Set<String> headers = detector.extractHeadersFromLine("timestamp,user,ipAddress,device,status");
        EvidenceFileType type = detector.detectType(headers);
        assertThat(type).isEqualTo(EvidenceFileType.LOGIN);
    }

    @Test
    @DisplayName("Should detect BROWSER file type from browser activity headers")
    void testDetectBrowser() {
        Set<String> headers = detector.extractHeadersFromLine("timestamp,user,url,device");
        EvidenceFileType type = detector.detectType(headers);
        assertThat(type).isEqualTo(EvidenceFileType.BROWSER);
    }

    @Test
    @DisplayName("Should detect FILE_ACCESS file type from file access headers")
    void testDetectFileAccess() {
        Set<String> headers = detector.extractHeadersFromLine("timestamp,user,fileName,action,device");
        EvidenceFileType type = detector.detectType(headers);
        assertThat(type).isEqualTo(EvidenceFileType.FILE_ACCESS);
    }

    @Test
    @DisplayName("Should detect NETWORK file type from network headers")
    void testDetectNetwork() {
        Set<String> headers = detector.extractHeadersFromLine("timestamp,sourceIp,destinationIp,port,bytes,device");
        EvidenceFileType type = detector.detectType(headers);
        assertThat(type).isEqualTo(EvidenceFileType.NETWORK);
    }

    @Test
    @DisplayName("Should throw InvalidEvidenceFileException on unknown headers")
    void testUnknownHeaders() {
        Set<String> headers = detector.extractHeadersFromLine("foo,bar,baz");
        assertThatThrownBy(() -> detector.detectType(headers))
                .isInstanceOf(InvalidEvidenceFileException.class)
                .hasMessageContaining("Unable to auto-detect evidence file type");
    }

    @Test
    @DisplayName("Should throw InvalidEvidenceFileException on blank header line")
    void testBlankHeaderLine() {
        assertThatThrownBy(() -> detector.extractHeadersFromLine("   "))
                .isInstanceOf(InvalidEvidenceFileException.class)
                .hasMessageContaining("Evidence file is empty or missing header line");
    }
}
