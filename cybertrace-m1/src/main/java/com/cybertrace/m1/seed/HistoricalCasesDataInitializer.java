package com.cybertrace.m1.seed;

import com.cybertrace.m1.client.M3Client;
import com.cybertrace.m1.dto.AnalysisDTO;
import com.cybertrace.m1.dto.FindingDTO;
import com.cybertrace.m1.dto.NormalizedEventDTO;
import com.cybertrace.m1.entity.CaseEntity;
import com.cybertrace.m1.entity.EvidenceEntity;
import com.cybertrace.m1.entity.NormalizedEventEntity;
import com.cybertrace.m1.entity.enums.CasePriority;
import com.cybertrace.m1.entity.enums.CaseStatus;
import com.cybertrace.m1.entity.enums.EvidenceStatus;
import com.cybertrace.m1.repository.AnalysisReportRepository;
import com.cybertrace.m1.repository.CaseRepository;
import com.cybertrace.m1.repository.EvidenceRepository;
import com.cybertrace.m1.repository.NormalizedEventRepository;
import com.cybertrace.m1.service.AnalysisReportService;
import com.cybertrace.m1.service.Sha256HashService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Idempotent initializer that populates the six historical investigation demonstration cases:
 * 1. Lufthansa Heist — 1978 (CASE-HIST-1978-LUFTHANSA)
 * 2. Mundhra Scandal — 1957 (CASE-HIST-1957-MUNDHRA)
 * 3. Bhopal Gas Tragedy — 1984 (CASE-HIST-1984-BHOPAL)
 * 4. Harshad Mehta Securities Scam — 1992 (CASE-HIST-1992-HARSHAD-MEHTA)
 * 5. Lindbergh Kidnapping — 1932 (CASE-HIST-1932-LINDBERGH)
 * 6. Mona Lisa Theft — 1911 (CASE-HIST-1911-MONALISA)
 *
 * Data Integrity:
 * All records are strictly identified as demonstration/reference records based on publicly
 * documented information. No fabricated official evidence, lab findings, or authentic victim
 * statements are represented.
 */
@Component
@Order(10)
public class HistoricalCasesDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(HistoricalCasesDataInitializer.class);

    private final CaseRepository caseRepository;
    private final EvidenceRepository evidenceRepository;
    private final NormalizedEventRepository normalizedEventRepository;
    private final AnalysisReportRepository analysisReportRepository;
    private final AnalysisReportService analysisReportService;
    private final Sha256HashService sha256HashService;
    private final M3Client m3Client;

    @Autowired
    public HistoricalCasesDataInitializer(
            CaseRepository caseRepository,
            EvidenceRepository evidenceRepository,
            NormalizedEventRepository normalizedEventRepository,
            AnalysisReportRepository analysisReportRepository,
            AnalysisReportService analysisReportService,
            Sha256HashService sha256HashService,
            @Autowired(required = false) M3Client m3Client
    ) {
        this.caseRepository = caseRepository;
        this.evidenceRepository = evidenceRepository;
        this.normalizedEventRepository = normalizedEventRepository;
        this.analysisReportRepository = analysisReportRepository;
        this.analysisReportService = analysisReportService;
        this.sha256HashService = sha256HashService;
        this.m3Client = m3Client;
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Checking historical investigation demonstration cases...");

        seedCase1Lufthansa();
        seedCase2Mundhra();
        seedCase3Bhopal();
        seedCase4HarshadMehta();
        seedCase5Lindbergh();
        seedCase6MonaLisa();

        log.info("Historical demonstration cases initialization completed.");
    }

    // =========================================================================
    // CASE 1: Lufthansa Heist — 1978
    // =========================================================================
    private void seedCase1Lufthansa() {
        String caseNumber = "CASE-HIST-1978-LUFTHANSA";
        String title = "Lufthansa Heist — 1978";
        String description = "Category: Major Robbery. Historical reference demonstration record based on publicly documented information regarding the armed robbery at JFK International Airport Building 261 (Lufthansa Cargo Terminal) on December 11, 1978. Approximately $5.875M ($5M cash, $875K jewelry) was stolen by armed masked individuals aided by insider knowledge of cargo manifests and vault protocols.";
        String leadInvestigator = "FBI / Port Authority Police (Historical Record)";

        CaseEntity caseEntity = getOrCreateCase(caseNumber, title, description, CasePriority.CRITICAL, CaseStatus.CLOSED, leadInvestigator);

        // Evidence 1
        EvidenceEntity ev1 = getOrCreateEvidence(
                caseEntity,
                "lufthansa_valuable_cargo_manifest.csv",
                "CSV Evidence",
                14280L,
                "JFK Air Cargo Manifest Archive (Demonstration)",
                "Historical reference demonstration record based on publicly documented shipping records for Lufthansa Flight LH493 carrying currency consignments."
        );

        // Evidence 2
        EvidenceEntity ev2 = getOrCreateEvidence(
                caseEntity,
                "lufthansa_terminal_security_logs.csv",
                "CSV Evidence",
                8920L,
                "Port Authority Building 261 Access Control (Demonstration)",
                "Historical reference demonstration record based on documented ramp gate sensor logs and vault alarm logs during the night of December 11, 1978."
        );

        // Evidence 3
        EvidenceEntity ev3 = getOrCreateEvidence(
                caseEntity,
                "lufthansa_police_dispatch_telemetry.csv",
                "CSV Evidence",
                25600L,
                "Port Authority Police Radio Dispatch (Demonstration)",
                "Historical reference demonstration record based on emergency telecommunications logs and pursuit dispatch records."
        );

        // Events
        List<NormalizedEventEntity> events = new ArrayList<>();
        events.add(createEvent(caseEntity, 1, LocalDateTime.of(1978, 12, 10, 14, 30, 0), "FILE_ACCESS",
                "Louis Werner", "TERMINAL-JFK-CARGO-01", null, null,
                "manifest_lh493_valuable_cargo.dat", "MANIFEST_LOOKUP", null, null, null, ev1.getId(),
                "[Historical Reference Demo] Lufthansa cargo agent Louis Werner accesses consignment manifest for incoming West German Deutsche Mark currency exchange. Historical reference demonstration record based on publicly documented information.", true));

        events.add(createEvent(caseEntity, 2, LocalDateTime.of(1978, 12, 10, 22, 15, 0), "NETWORK_EVENT",
                "System Dispatch", "TERMINAL-JFK-CARGO-01", "10.197.8.21", "10.197.8.50",
                null, "CUSTOMS_CLEARANCE_DISPATCH", null, 8080, 48000L, ev1.getId(),
                "[Historical Reference Demo] Automated customs and terminal routing update transmitted confirming arrival and vault transfer of currency containers. Historical reference demonstration record based on publicly documented information.", true));

        events.add(createEvent(caseEntity, 3, LocalDateTime.of(1978, 12, 11, 3, 12, 0), "LOGIN",
                "Kerry Whalen", "TERMINAL-JFK-RAMP-02", "10.197.8.12", null,
                null, "LOGIN_SUCCESS", null, null, null, ev2.getId(),
                "[Historical Reference Demo] Cargo transfer agent Kerry Whalen investigates unauthorized black Ford Econoline van outside ramp gate and is confronted by armed perpetrators. Off-hours terminal security event. Historical reference demonstration record based on publicly documented information.", true));

        events.add(createEvent(caseEntity, 4, LocalDateTime.of(1978, 12, 11, 3, 25, 0), "FILE_ACCESS",
                "John Murray", "TERMINAL-JFK-VAULT-01", null, null,
                "high_security_vault_inner_door.dat", "VAULT_DOOR_UNLOCK", null, null, null, ev2.getId(),
                "[Historical Reference Demo] Senior cargo agent John Murray forced at gunpoint to unlock high-value vault door under armed duress. Historical reference demonstration record based on publicly documented information.", true));

        events.add(createEvent(caseEntity, 5, LocalDateTime.of(1978, 12, 11, 4, 16, 0), "NETWORK_EVENT",
                "PAPD Dispatcher", "PORT-AUTHORITY-NET-01", "10.197.8.1", "10.197.8.255",
                null, "PORT_WIDE_EMERGENCY_ALARM", null, 514, 78000000L, ev3.getId(),
                "[Historical Reference Demo] Port Authority Police broadcast terminal-wide emergency alarm following escape of armed perpetrators with 72 cartons of currency. High-volume emergency telemetry dispatch. Historical reference demonstration record based on publicly documented information.", true));

        events.add(createEvent(caseEntity, 6, LocalDateTime.of(1978, 12, 13, 10, 0, 0), "FILE_ACCESS",
                "FBI Forensic Team", "FBI-FIELD-TERMINAL-01", null, null,
                "canarsie_getaway_van_fingerprint_log.dat", "VEHICLE_PROCESSING", null, null, null, ev3.getId(),
                "[Historical Reference Demo] FBI forensic technicians process abandoned 1974 Ford Econoline getaway van recovered in Canarsie, Brooklyn for latent fingerprints. Historical reference demonstration record based on publicly documented information.", true));

        persistEventsAndEvidenceCounts(caseEntity, events, List.of(ev1, ev2, ev3));
        initOrUpdateAnalysisReport(caseEntity, events);
    }

    // =========================================================================
    // CASE 2: Mundhra Scandal — 1957
    // =========================================================================
    private void seedCase2Mundhra() {
        String caseNumber = "CASE-HIST-1957-MUNDHRA";
        String title = "Mundhra Scandal — 1957";
        String description = "Category: Financial Scandal. Historical reference demonstration record based on publicly documented information regarding the 1957 Life Insurance Corporation (LIC) investment scandal involving industrialist Haridas Mundhra. The inquiry conducted by Justice M.C. Chagla established irregular governmental influence in the investment of 1.24 crore INR of public funds into distressed Mundhra concerns.";
        String leadInvestigator = "Chagla Commission of Inquiry (Historical Record)";

        CaseEntity caseEntity = getOrCreateCase(caseNumber, title, description, CasePriority.HIGH, CaseStatus.CLOSED, leadInvestigator);

        EvidenceEntity ev1 = getOrCreateEvidence(
                caseEntity,
                "mundhra_lic_transaction_ledger.csv",
                "CSV Evidence",
                11200L,
                "Life Insurance Corporation Investment Records (Demonstration)",
                "Historical reference demonstration record based on transaction records for shares acquired in Richardson & Cruddas, Jessop & Co., and Smith Stainistreet."
        );

        EvidenceEntity ev2 = getOrCreateEvidence(
                caseEntity,
                "mundhra_chagla_commission_proceedings.csv",
                "CSV Evidence",
                19800L,
                "Chagla Commission Public Record (Demonstration)",
                "Historical reference demonstration record based on testimony and documentation submitted to the Commission of Inquiry in Bombay."
        );

        List<NormalizedEventEntity> events = new ArrayList<>();
        events.add(createEvent(caseEntity, 1, LocalDateTime.of(1957, 6, 21, 11, 0, 0), "FILE_ACCESS",
                "Haridas Mundhra", "LIC-CALCUTTA-TERMINAL-01", null, null,
                "mundhra_share_sale_proposal.doc", "INVESTMENT_PROPOSAL_SUBMISSION", null, null, null, ev1.getId(),
                "[Historical Reference Demo] Haridas Mundhra submits formal offer to LIC officials proposing purchase of shares in his enterprises to relieve financial pressure.", true));

        events.add(createEvent(caseEntity, 2, LocalDateTime.of(1957, 6, 23, 15, 45, 0), "FILE_ACCESS",
                "H.M. Patel", "MINISTRY-FINANCE-DESK-01", null, null,
                "lic_investment_directive_memo.doc", "EXECUTIVE_DIRECTIVE_ISSUE", null, null, null, ev1.getId(),
                "[Historical Reference Demo] Principal Finance Secretary instructs LIC management to examine proposal and execute bulk purchase of Mundhra group shares.", true));

        events.add(createEvent(caseEntity, 3, LocalDateTime.of(1957, 6, 25, 14, 15, 0), "NETWORK_EVENT",
                "LIC Investment Dept", "RESERVE-BANK-TRANSFER-01", "10.195.7.10", "10.195.7.90",
                null, "BULK_DISBURSEMENT_TRANSMISSION", null, 443, 62000000L, ev1.getId(),
                "[Historical Reference Demo] Funds transfer dispatch of 1.24 crore INR from LIC accounts to brokers for purchase of shares without statutory committee consultation.", true));

        events.add(createEvent(caseEntity, 4, LocalDateTime.of(1957, 9, 4, 12, 0, 0), "BROWSER_ACTIVITY",
                "Feroze Gandhi", "PARLIAMENT-RESEARCH-01", null, null,
                null, "PARLIAMENTARY_STARRED_QUESTION", "http://parliament.gov.in/proceedings/1957/lic_investment_query", null, null, ev2.getId(),
                "[Historical Reference Demo] MP Feroze Gandhi queries Finance Ministry in Lok Sabha regarding questionable investment of public insurance revenues in Mundhra shares.", true));

        events.add(createEvent(caseEntity, 5, LocalDateTime.of(1958, 1, 15, 10, 30, 0), "FILE_ACCESS",
                "Justice M.C. Chagla", "CHAGLA-COMMISSION-DESK-01", null, null,
                "chagla_inquiry_exhibit_list.dat", "EVIDENCE_EVALUATION", null, null, null, ev2.getId(),
                "[Historical Reference Demo] Commission of Inquiry reviews transaction timing, share market prices, and absence of independent valuation prior to purchase.", true));

        persistEventsAndEvidenceCounts(caseEntity, events, List.of(ev1, ev2));
        initOrUpdateAnalysisReport(caseEntity, events);
    }

    // =========================================================================
    // CASE 3: Bhopal Gas Tragedy — 1984
    // =========================================================================
    private void seedCase3Bhopal() {
        String caseNumber = "CASE-HIST-1984-BHOPAL";
        String title = "Bhopal Gas Tragedy — 1984";
        String description = "Category: Industrial Disaster. Historical reference demonstration record based on publicly documented information regarding the toxic methyl isocyanate (MIC) gas release from Tank 610 at the Union Carbide India Limited pesticide plant in Bhopal on December 2–3, 1984. Resulted in immediate mass fatalities and long-term public health devastation.";
        String leadInvestigator = "CBI / CSIR Scientific Investigation (Historical Record)";

        CaseEntity caseEntity = getOrCreateCase(caseNumber, title, description, CasePriority.CRITICAL, CaseStatus.CLOSED, leadInvestigator);

        EvidenceEntity ev1 = getOrCreateEvidence(
                caseEntity,
                "bhopal_tank610_telemetry_manifest.csv",
                "CSV Evidence",
                16400L,
                "UCIL Plant Instrument Telemetry Archive (Demonstration)",
                "Historical reference demonstration record based on pressure transmitter readings, temperature sensors, and relief valve logs for Tank 610."
        );

        EvidenceEntity ev2 = getOrCreateEvidence(
                caseEntity,
                "bhopal_safety_maintenance_manifest.csv",
                "CSV Evidence",
                12500L,
                "UCIL Safety Maintenance Records (Demonstration)",
                "Historical reference demonstration record based on maintenance worksheets for the vent gas scrubber, refrigeration chiller, and flare tower."
        );

        List<NormalizedEventEntity> events = new ArrayList<>();
        events.add(createEvent(caseEntity, 1, LocalDateTime.of(1984, 12, 2, 21, 30, 0), "FILE_ACCESS",
                "Plant Maintenance Supervisor", "UCIL-MAINT-TERMINAL-01", null, null,
                "filter_header_washing_instruction.doc", "LINE_WASHING_ORDER", null, null, null, ev2.getId(),
                "[Historical Reference Demo] Water-washing of clogged bleeder lines ordered without inserting required slip-blind safety isolation plate.", true));

        events.add(createEvent(caseEntity, 2, LocalDateTime.of(1984, 12, 2, 22, 45, 0), "FILE_ACCESS",
                "Control Room Operator", "UCIL-CONTROL-ROOM-01", null, null,
                "tank610_instrument_readout.dat", "PRESSURE_GAUGE_CHECK", null, null, null, ev1.getId(),
                "[Historical Reference Demo] Control room operator observes Tank 610 pressure increase from 2 psi to 10 psi; reading assumed to be instrument malfunction.", true));

        events.add(createEvent(caseEntity, 3, LocalDateTime.of(1984, 12, 2, 23, 30, 0), "LOGIN",
                "Night Shift Supervisor", "UCIL-CONTROL-ROOM-01", "10.198.4.10", null,
                null, "LOGIN_SUCCESS", null, null, null, ev1.getId(),
                "[Historical Reference Demo] Shift changeover occurs; field operators report eye irritation and minor MIC vapor leak near the structure.", true));

        events.add(createEvent(caseEntity, 4, LocalDateTime.of(1984, 12, 3, 0, 30, 0), "FILE_ACCESS",
                "Plant Safety Officer", "UCIL-SAFETY-DESK-01", null, null,
                "vent_gas_scrubber_log.dat", "SCRUBBER_SYSTEM_STATUS", null, null, null, ev2.getId(),
                "[Historical Reference Demo] Safety systems inspection reveals vent gas scrubber is set to manual/standby and flare tower is decommissioned for piping repair.", true));

        events.add(createEvent(caseEntity, 5, LocalDateTime.of(1984, 12, 3, 0, 50, 0), "NETWORK_EVENT",
                "Automated Siren System", "UCIL-SAFETY-DESK-01", "10.198.4.1", "10.198.4.255",
                null, "EMERGENCY_SIREN_SIGNAL", null, 502, 85000000L, ev1.getId(),
                "[Historical Reference Demo] Safety rupture disk bursts on Tank 610; high-volume emergency signal triggered as approximately 40 tons of MIC vapor escape into the night atmosphere.", true));

        events.add(createEvent(caseEntity, 6, LocalDateTime.of(1984, 12, 5, 11, 0, 0), "FILE_ACCESS",
                "CSIR Investigation Team", "CSIR-MOBILE-LAB-01", null, null,
                "tank610_polymeric_residue_spec.dat", "CHEMICAL_RESIDUE_ANALYSIS", null, null, null, ev2.getId(),
                "[Historical Reference Demo] CSIR forensic investigation team samples solid residues inside Tank 610, confirming metallic catalyst contamination and exothermic polymerization.", true));

        persistEventsAndEvidenceCounts(caseEntity, events, List.of(ev1, ev2));
        initOrUpdateAnalysisReport(caseEntity, events);
    }

    // =========================================================================
    // CASE 4: Harshad Mehta Securities Scam — 1992
    // =========================================================================
    private void seedCase4HarshadMehta() {
        String caseNumber = "CASE-HIST-1992-HARSHAD-MEHTA";
        String title = "Harshad Mehta Securities Scam — 1992";
        String description = "Category: Financial Fraud / Securities Scam. Historical reference demonstration record based on publicly documented information regarding the 1992 securities manipulation in India. Unbacked Bank Receipts (BRs) and ready forward government bond transactions were used to siphon banking funds into equity markets, culminating in market collapse and sweeping structural regulatory reforms.";
        String leadInvestigator = "Janakiraman Committee / JPC (Historical Record)";

        CaseEntity caseEntity = getOrCreateCase(caseNumber, title, description, CasePriority.CRITICAL, CaseStatus.CLOSED, leadInvestigator);

        EvidenceEntity ev1 = getOrCreateEvidence(
                caseEntity,
                "harshad_mehta_bank_receipt_manifest.csv",
                "CSV Evidence",
                14900L,
                "Bank of Karad & Metropolitan Bank BR Ledger (Demonstration)",
                "Historical reference demonstration record based on recorded Bank Receipts issued without underlying government security holdings."
        );

        EvidenceEntity ev2 = getOrCreateEvidence(
                caseEntity,
                "harshad_mehta_market_operations_manifest.csv",
                "CSV Evidence",
                21300L,
                "Bombay Stock Exchange Clearing House (Demonstration)",
                "Historical reference demonstration record based on brokerage accounts and contract notes detailing speculative transactions in ACC and other equities."
        );

        List<NormalizedEventEntity> events = new ArrayList<>();
        events.add(createEvent(caseEntity, 1, LocalDateTime.of(1992, 3, 9, 10, 15, 0), "FILE_ACCESS",
                "Harshad Mehta", "BSE-BROKER-TERMINAL-01", null, null,
                "bank_receipt_series_bok_72.dat", "BR_LEDGER_CREATION", null, null, null, ev1.getId(),
                "[Historical Reference Demo] Bank Receipt issued by Bank of Karad for 7% 1998 government securities without physical securities in custody.", true));

        events.add(createEvent(caseEntity, 2, LocalDateTime.of(1992, 3, 12, 11, 45, 0), "NETWORK_EVENT",
                "SBI Treasury Operations", "SBI-TREASURY-GATEWAY-01", "10.199.2.11", "10.199.2.80",
                null, "TREASURY_CHEQUE_CLEARANCE", null, 443, 92000000L, ev1.getId(),
                "[Historical Reference Demo] Inter-bank clearing cheque disbursed by State Bank of India treasury credited to broker routing account against unbacked Bank Receipt.", true));

        events.add(createEvent(caseEntity, 3, LocalDateTime.of(1992, 4, 10, 15, 30, 0), "FILE_ACCESS",
                "GrowMore Research", "GROWMORE-SYSTEM-01", null, null,
                "acc_shares_buy_orders.doc", "EQUITY_ACCUMULATION_ORDER", null, null, null, ev2.getId(),
                "[Historical Reference Demo] Aggressive purchase orders executed for Associated Cement Companies (ACC) driving stock valuation from 200 INR to over 9,000 INR.", true));

        events.add(createEvent(caseEntity, 4, LocalDateTime.of(1992, 4, 23, 6, 30, 0), "FILE_ACCESS",
                "Sucheta Dalal", "TIMES-EDITORIAL-DESK-01", null, null,
                "sbi_unreconciled_cheques_story.doc", "INVESTIGATIVE_REPORT_PUBLISHED", null, null, null, ev2.getId(),
                "[Historical Reference Demo] Investigative article published in The Times of India revealing 500 crore INR reconciliation discrepancy in State Bank of India securities portfolio.", true));

        events.add(createEvent(caseEntity, 5, LocalDateTime.of(1992, 5, 8, 9, 0, 0), "LOGIN",
                "RBI Special Audit Team", "RBI-AUDIT-CONSOLE-01", "10.199.2.5", null,
                null, "LOGIN_SUCCESS", null, null, null, ev1.getId(),
                "[Historical Reference Demo] Reserve Bank of India inspection team initiates forensic audit into ready forward transactions and inter-bank receipt authenticity.", true));

        persistEventsAndEvidenceCounts(caseEntity, events, List.of(ev1, ev2));
        initOrUpdateAnalysisReport(caseEntity, events);
    }

    // =========================================================================
    // CASE 5: Lindbergh Kidnapping — 1932
    // =========================================================================
    private void seedCase5Lindbergh() {
        String caseNumber = "CASE-HIST-1932-LINDBERGH";
        String title = "Lindbergh Kidnapping — 1932";
        String description = "Category: Kidnapping. Historical reference demonstration record based on publicly documented information regarding the abduction of Charles Augustus Lindbergh Jr. from the Highfields estate on March 1, 1932. Ransom of $50,000 paid in gold certificates enabled investigators to trace banknotes, leading to the arrest of Bruno Richard Hauptmann in September 1934.";
        String leadInvestigator = "New Jersey State Police / FBI (Historical Record)";

        CaseEntity caseEntity = getOrCreateCase(caseNumber, title, description, CasePriority.HIGH, CaseStatus.CLOSED, leadInvestigator);

        EvidenceEntity ev1 = getOrCreateEvidence(
                caseEntity,
                "lindbergh_gold_certificates_manifest.csv",
                "CSV Evidence",
                18400L,
                "Federal Reserve Bank Gold Certificate Register (Demonstration)",
                "Historical reference demonstration record based on serial numbers of 5,145 gold certificate banknotes packaged for ransom payment."
        );

        EvidenceEntity ev2 = getOrCreateEvidence(
                caseEntity,
                "lindbergh_crime_scene_forensics_manifest.csv",
                "CSV Evidence",
                13700L,
                "NJ State Police Crime Scene Repository (Demonstration)",
                "Historical reference demonstration record based on physical evidence cataloging, ladder toolmarks, and wood species identification."
        );

        List<NormalizedEventEntity> events = new ArrayList<>();
        events.add(createEvent(caseEntity, 1, LocalDateTime.of(1932, 3, 1, 21, 0, 0), "FILE_ACCESS",
                "Unknown Intruder", "HIGHFIELDS-NURSERY-01", null, null,
                "nursery_ransom_envelope.dat", "UNAUTHORIZED_ENTRY", null, null, null, ev2.getId(),
                "[Historical Reference Demo] Abduction from second-floor nursery window via three-section wooden ladder; ransom note demanding $50,000 discovered on windowsill.", true));

        events.add(createEvent(caseEntity, 2, LocalDateTime.of(1932, 4, 2, 20, 45, 0), "FILE_ACCESS",
                "Dr. John F. Condon", "WOODLAWN-CEMETERY-01", null, null,
                "gold_certificates_recorded_package.dat", "RANSOM_DELIVERY", null, null, null, ev1.getId(),
                "[Historical Reference Demo] Delivery of $50,000 ransom package in gold certificates and Federal Reserve notes executed at St. Raymond's Cemetery to individual identifying as Cemetery John.", true));

        events.add(createEvent(caseEntity, 3, LocalDateTime.of(1932, 5, 12, 15, 15, 0), "FILE_ACCESS",
                "NJ State Police Field Unit", "MT-ROSE-STATION-01", null, null,
                "woodlands_identification_report.doc", "MORTUARY_RECOVERY_LOG", null, null, null, ev2.getId(),
                "[Historical Reference Demo] Infant remains discovered in roadside woods off Hopewell-Amwell road; formal identification confirmed by family physicians.", true));

        events.add(createEvent(caseEntity, 4, LocalDateTime.of(1934, 9, 15, 10, 30, 0), "FILE_ACCESS",
                "Walter Lyle", "WARNER-QUINLAN-PUMP-01", null, null,
                "gold_certificate_bill_b12571729a.dat", "BILL_VERIFICATION", null, null, null, ev1.getId(),
                "[Historical Reference Demo] Gas station attendant accepts $10 gold certificate bill matching Treasury alert sheet and pencils customer license plate 4U-13-41 NY on margin.", true));

        events.add(createEvent(caseEntity, 5, LocalDateTime.of(1934, 9, 19, 9, 15, 0), "LOGIN",
                "FBI Special Agent", "NYPD-PRECINCT-DESK-01", "10.193.4.1", null,
                null, "LOGIN_SUCCESS", null, null, null, ev1.getId(),
                "[Historical Reference Demo] Arrest of suspect Bruno Richard Hauptmann in Bronx; search of garage recovers $14,600 in ransom gold certificates concealed in wall partitions.", true));

        persistEventsAndEvidenceCounts(caseEntity, events, List.of(ev1, ev2));
        initOrUpdateAnalysisReport(caseEntity, events);
    }

    // =========================================================================
    // CASE 6: Mona Lisa Theft — 1911
    // =========================================================================
    private void seedCase6MonaLisa() {
        String caseNumber = "CASE-HIST-1911-MONALISA";
        String title = "Mona Lisa Theft — 1911";
        String description = "Category: Art Theft. Historical reference demonstration record based on publicly documented information regarding the theft of Leonardo da Vinci's Mona Lisa from the Musée du Louvre on August 21, 1911 by former glazier Vincenzo Peruggia. The portrait was recovered in Florence, Italy in December 1913.";
        String leadInvestigator = "Paris Prefecture of Police / Alphonse Bertillon (Historical Record)";

        CaseEntity caseEntity = getOrCreateCase(caseNumber, title, description, CasePriority.HIGH, CaseStatus.CLOSED, leadInvestigator);

        EvidenceEntity ev1 = getOrCreateEvidence(
                caseEntity,
                "mona_lisa_louvre_inventory_manifest.csv",
                "CSV Evidence",
                9800L,
                "Louvre Museum Inventory Department (Demonstration)",
                "Historical reference demonstration record based on Salon Carré gallery inventory registries and guard attendance sheets."
        );

        EvidenceEntity ev2 = getOrCreateEvidence(
                caseEntity,
                "mona_lisa_florence_recovery_manifest.csv",
                "CSV Evidence",
                14100L,
                "Florence Carabinieri / Uffizi Archive (Demonstration)",
                "Historical reference demonstration record based on authentication reports and recovery seizure receipts from Hotel Tripoli."
        );

        List<NormalizedEventEntity> events = new ArrayList<>();
        events.add(createEvent(caseEntity, 1, LocalDateTime.of(1911, 8, 21, 7, 15, 0), "LOGIN",
                "Vincenzo Peruggia", "LOUVRE-SERVICE-DOOR-01", "10.191.1.2", null,
                null, "LOGIN_SUCCESS", null, null, null, ev1.getId(),
                "[Historical Reference Demo] Vincenzo Peruggia enters the Louvre on Monday maintenance closure day wearing glazier white worker coat. Off-hours unauthorized access.", true));

        events.add(createEvent(caseEntity, 2, LocalDateTime.of(1911, 8, 21, 7, 35, 0), "FILE_ACCESS",
                "Vincenzo Peruggia", "SALON-CARRE-GALLERY-01", null, null,
                "mona_lisa_wood_panel.dat", "PAINTING_UNHOOK_REMOVAL", null, null, null, ev1.getId(),
                "[Historical Reference Demo] Leonardo da Vinci portrait unhooked from wall in Salon Carré and carried into service stairwell where protective shadowbox frame is dismantled.", true));

        events.add(createEvent(caseEntity, 3, LocalDateTime.of(1911, 8, 21, 8, 10, 0), "FILE_ACCESS",
                "Sauvet (Plumber)", "COUR-DU-SPHINX-GATE-01", null, null,
                "courtyard_exit_hardware.dat", "LOCK_UNSCREW_EXIT", null, null, null, ev1.getId(),
                "[Historical Reference Demo] Peruggia aided at locked courtyard door by museum plumber Sauvet who unscrews lock mechanism, permitting exit with concealed panel.", true));

        events.add(createEvent(caseEntity, 4, LocalDateTime.of(1911, 8, 22, 12, 0, 0), "FILE_ACCESS",
                "Louis Béroud", "LOUVRE-CURATOR-DESK-01", null, null,
                "salon_carre_daily_inspection.doc", "MISSING_OBJECT_REPORT", null, null, null, ev1.getId(),
                "[Historical Reference Demo] Artist Louis Béroud notices missing painting; museum guards confirm empty iron pegs on wall, prompting immediate museum shutdown.", true));

        events.add(createEvent(caseEntity, 5, LocalDateTime.of(1913, 12, 10, 14, 0, 0), "FILE_ACCESS",
                "Giovanni Poggi", "UFFIZI-EXAM-ROOM-01", null, null,
                "louvre_accession_stamp_inspection.dat", "AUTHENTICATION_CERTIFICATE", null, null, null, ev2.getId(),
                "[Historical Reference Demo] Uffizi Director Giovanni Poggi inspects panel in Florence hotel room, identifying genuine Louvre catalog number and notifying Italian authorities.", true));

        persistEventsAndEvidenceCounts(caseEntity, events, List.of(ev1, ev2));
        initOrUpdateAnalysisReport(caseEntity, events);
    }

    // =========================================================================
    // Idempotent Persistence Helpers
    // =========================================================================

    private CaseEntity getOrCreateCase(String caseNumber, String title, String description,
                                       CasePriority priority, CaseStatus status, String leadInvestigator) {
        Optional<CaseEntity> existing = caseRepository.findByCaseNumber(caseNumber);
        if (existing.isPresent()) {
            return existing.get();
        }

        String stableId = UUID.nameUUIDFromBytes(("cybertrace:case:" + caseNumber).getBytes(StandardCharsets.UTF_8)).toString();
        CaseEntity entity = new CaseEntity(stableId, caseNumber, title, description, status, priority, leadInvestigator);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(entity.getCreatedAt());
        CaseEntity saved = caseRepository.save(entity);
        log.info("Seeded historical case: {} ({})", caseNumber, saved.getId());
        return saved;
    }

    private EvidenceEntity getOrCreateEvidence(CaseEntity caseEntity, String fileName, String fileType,
                                               Long fileSizeBytes, String source, String description) {
        String hash = sha256HashService.calculateHash(
                ("cybertrace:evidence:" + caseEntity.getCaseNumber() + ":" + fileName).getBytes(StandardCharsets.UTF_8)
        );

        if (evidenceRepository.existsByCaseEntity_IdAndHashSha256(caseEntity.getId(), hash)) {
            return evidenceRepository.findByCaseEntity_IdOrderByUploadedAtDesc(caseEntity.getId()).stream()
                    .filter(e -> fileName.equalsIgnoreCase(e.getFileName()))
                    .findFirst()
                    .orElseGet(() -> evidenceRepository.findByCaseEntity_IdOrderByUploadedAtDesc(caseEntity.getId()).get(0));
        }

        String stableId = UUID.nameUUIDFromBytes(
                ("cybertrace:evidence:" + caseEntity.getCaseNumber() + ":" + fileName).getBytes(StandardCharsets.UTF_8)
        ).toString();

        EvidenceEntity entity = new EvidenceEntity(
                stableId,
                caseEntity,
                fileName,
                fileType,
                fileSizeBytes,
                hash,
                Instant.now(),
                EvidenceStatus.PROCESSED,
                source,
                description,
                0,
                "Historical Reference Ingestion"
        );

        EvidenceEntity saved = evidenceRepository.save(entity);
        log.info("Seeded historical evidence: {} for case {}", fileName, caseEntity.getCaseNumber());
        return saved;
    }

    private NormalizedEventEntity createEvent(CaseEntity caseEntity, int seq, LocalDateTime timestamp,
                                              String eventType, String user, String device,
                                              String ipAddress, String destinationIp, String fileName,
                                              String action, String url, Integer port, Long bytes,
                                              String sourceEvidenceId, String description, Boolean success) {
        String stableEventId = "urn:uuid:" + UUID.nameUUIDFromBytes(
                ("cybertrace:event:" + caseEntity.getCaseNumber() + ":" + seq).getBytes(StandardCharsets.UTF_8)
        ).toString();

        String fullDescription = description != null ? description.trim() : "";
        if (!fullDescription.contains("Historical reference demonstration record")) {
            fullDescription = fullDescription + " Historical reference demonstration record based on publicly documented information.";
        }

        return new NormalizedEventEntity(
                stableEventId,
                caseEntity,
                timestamp,
                eventType,
                user,
                device,
                ipAddress,
                destinationIp,
                fileName,
                action,
                url,
                port,
                bytes,
                sourceEvidenceId,
                fullDescription,
                success
        );
    }

    private void persistEventsAndEvidenceCounts(CaseEntity caseEntity, List<NormalizedEventEntity> events, List<EvidenceEntity> evidenceList) {
        Map<String, Integer> countByEvidence = new HashMap<>();

        for (NormalizedEventEntity event : events) {
            if (!normalizedEventRepository.existsById(event.getEventId())) {
                normalizedEventRepository.save(event);
            }
            if (event.getSourceEvidenceId() != null) {
                countByEvidence.merge(event.getSourceEvidenceId(), 1, Integer::sum);
            }
        }

        // Update evidence event counts
        for (EvidenceEntity ev : evidenceList) {
            int count = countByEvidence.getOrDefault(ev.getId(), 0);
            if (count > 0 && (ev.getEventCount() == null || ev.getEventCount() == 0)) {
                ev.setEventCount(count);
                ev.setStatus(EvidenceStatus.PROCESSED);
                evidenceRepository.save(ev);
            }
        }
    }

    private void initOrUpdateAnalysisReport(CaseEntity caseEntity, List<NormalizedEventEntity> events) {
        if (analysisReportRepository.findByCaseEntity_Id(caseEntity.getId()).isPresent()) {
            return;
        }

        // 1. Convert events to DTOs for M3 analysis
        List<NormalizedEventDTO> eventDTOs = new ArrayList<>();
        for (NormalizedEventEntity e : events) {
            NormalizedEventDTO dto = new NormalizedEventDTO();
            dto.setEventId(e.getEventId());
            dto.setId(e.getEventId());
            dto.setCaseId(caseEntity.getId());
            dto.setTimestamp(e.getTimestamp());
            dto.setEventType(e.getEventType());
            dto.setUser(e.getUser());
            dto.setDevice(e.getDevice());
            dto.setIpAddress(e.getIpAddress());
            dto.setDestinationIp(e.getDestinationIp());
            dto.setFileName(e.getFileName());
            dto.setAction(e.getAction());
            dto.setUrl(e.getUrl());
            dto.setPort(e.getPort());
            dto.setBytes(e.getBytes());
            dto.setSourceEvidenceId(e.getSourceEvidenceId());
            dto.setDescription(e.getDescription());
            dto.setSuccess(e.getSuccess());
            eventDTOs.add(dto);
        }

        // 2. Try live analysis via M3
        if (m3Client != null) {
            try {
                Optional<AnalysisDTO> analysisOpt = m3Client.analyzeEvents(caseEntity.getId(), eventDTOs);
                if (analysisOpt.isPresent()) {
                    analysisReportService.saveAnalysisReport(caseEntity.getId(), analysisOpt.get());
                    log.info("Generated live M3 analysis report for case {}", caseEntity.getCaseNumber());
                    return;
                }
            } catch (Exception ex) {
                log.debug("M3 engine not immediately reachable at startup for case {}: {}", caseEntity.getCaseNumber(), ex.getMessage());
            }
        }

        // 3. Fallback: Generate structured baseline demonstration report
        AnalysisDTO baselineReport = buildDemonstrationBaselineReport(caseEntity, events);
        analysisReportService.saveAnalysisReport(caseEntity.getId(), baselineReport);
        log.info("Saved baseline demonstration analysis report for case {}", caseEntity.getCaseNumber());
    }

    /**
     * Builds a deterministic demonstration analysis report with findings aligned with M3 detectors.
     */
    private AnalysisDTO buildDemonstrationBaselineReport(CaseEntity caseEntity, List<NormalizedEventEntity> events) {
        List<FindingDTO> findings = new ArrayList<>();
        int totalEvents = events.size();

        for (NormalizedEventEntity ev : events) {
            int hour = ev.getTimestamp().getHour();
            String type = ev.getEventType();

            // Off-hours detection (outside 09:00 - 18:00) for LOGIN or FILE_ACCESS
            if (("LOGIN".equalsIgnoreCase(type) || "FILE_ACCESS".equalsIgnoreCase(type)) && (hour < 9 || hour >= 18)) {
                String sev = (hour < 5 || hour >= 22) ? "CRITICAL" : "HIGH";
                double score = (hour < 5 || hour >= 22) ? 0.95 : 0.85;
                String fId = "fnd-" + UUID.nameUUIDFromBytes(("offhours:" + ev.getEventId()).getBytes(StandardCharsets.UTF_8)).toString().substring(0, 8);
                findings.add(new FindingDTO(
                        fId,
                        ev.getEventId(),
                        ev.getTimestamp(),
                        ev.getEventType(),
                        "OFF_HOURS",
                        String.format("Event occurred during off-hours (%02d:%02d) outside standard working schedule (09:00-18:00). Historical reference demonstration record based on publicly documented information.", hour, ev.getTimestamp().getMinute()),
                        sev,
                        score,
                        "RULE_BASED_OFF_HOURS",
                        ev.getUser() != null ? ev.getUser() : ev.getDevice(),
                        "Review access authorization logs for off-hours timeline deviation."
                ));
            }

            // High network volume detection (> 50 MB)
            if ("NETWORK_EVENT".equalsIgnoreCase(type) && ev.getBytes() != null && ev.getBytes() > 50_000_000L) {
                String fId = "fnd-" + UUID.nameUUIDFromBytes(("netvol:" + ev.getEventId()).getBytes(StandardCharsets.UTF_8)).toString().substring(0, 8);
                findings.add(new FindingDTO(
                        fId,
                        ev.getEventId(),
                        ev.getTimestamp(),
                        ev.getEventType(),
                        "NETWORK_VOLUME",
                        String.format("High network egress detected: %,d bytes transferred exceeding standard baseline threshold (52,428,800 bytes). Historical reference demonstration record based on publicly documented information.", ev.getBytes()),
                        "HIGH",
                        0.88,
                        "HIGH_NETWORK_VOLUME_THRESHOLD",
                        ev.getDestinationIp() != null ? ev.getDestinationIp() : ev.getDevice(),
                        "Perform packet capture analysis and examine egress routing."
                ));
            }
        }

        // Sort findings chronologically
        findings.sort(Comparator.comparing(FindingDTO::getTimestamp));

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total_events", totalEvents);
        stats.put("total_anomalies", findings.size());
        stats.put("max_anomaly_score", findings.isEmpty() ? 0.0 : findings.stream().mapToDouble(FindingDTO::getScore).max().orElse(0.0));
        stats.put("primary_threat_vector", findings.isEmpty() ? "None" : findings.get(0).getAnomalyType());
        stats.put("demonstration_mode", true);

        StringBuilder summaryBuilder = new StringBuilder();
        summaryBuilder.append("Overall Analysis:\n");
        summaryBuilder.append(String.format("%d events analyzed across the timeline; %d %s identified.\n\n",
                totalEvents, findings.size(), findings.size() == 1 ? "anomaly" : "anomalies"));

        if (!findings.isEmpty()) {
            summaryBuilder.append("Finding Breakdown:\n");
            long offHoursCount = findings.stream().filter(f -> "OFF_HOURS".equalsIgnoreCase(f.getAnomalyType())).count();
            long netVolCount = findings.stream().filter(f -> "NETWORK_VOLUME".equalsIgnoreCase(f.getAnomalyType())).count();
            summaryBuilder.append(String.format("- Anomaly Types: OFF_HOURS (%d), NETWORK_VOLUME (%d)\n", offHoursCount, netVolCount));
            long critCount = findings.stream().filter(f -> "CRITICAL".equalsIgnoreCase(f.getSeverity())).count();
            long highCount = findings.stream().filter(f -> "HIGH".equalsIgnoreCase(f.getSeverity())).count();
            summaryBuilder.append(String.format("- Severity Distribution: CRITICAL (%d), HIGH (%d)\n\n", critCount, highCount));

            summaryBuilder.append("Key Findings:\n");
            for (FindingDTO f : findings) {
                summaryBuilder.append(String.format("- [%s] %s on event '%s' at %s: %s (Score: %.4f)\n",
                        f.getSeverity(), f.getAnomalyType(), f.getEventId(), f.getTimestamp(), f.getReason(), f.getScore()));
            }
        }

        summaryBuilder.append("\nInvestigation Conclusion:\n");
        summaryBuilder.append("System-generated demonstration analysis based on structured historical demonstration events. Historical reference demonstration record based on publicly documented information. Does not constitute an official legal or judicial finding.");

        return new AnalysisDTO(
                caseEntity.getId(),
                totalEvents,
                findings.size(),
                findings,
                stats,
                summaryBuilder.toString(),
                Instant.now()
        );
    }
}
