package com.auction.server.services;

import static org.junit.jupiter.api.Assertions.*;

import com.auction.server.services.AuctionServiceTestFixtures.*;
import com.auction.shared.dto.Response;
import com.auction.shared.models.*;
import org.junit.jupiter.api.*;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Timeout(value = 5, unit = TimeUnit.SECONDS)
@DisplayName("AuctionService — Payment & Cancellation Tests")
class AuctionService_BillingTest {

    static {
        System.setProperty("testMode", "true");
    }

    private AuctionService service;
    private FakeItemDAO fakeItemDAO;
    private FakeWalletDAO fakeWalletDAO;
    private FakeInvoiceDAO fakeInvoiceDAO;

    private static final String ITEM_ID   = "ITEM-001";
    private static final String BIDDER_A  = "userA";
    private static final String SELLER_ID = "seller1";

    @BeforeEach
    void setUp() throws Exception {
        Field instanceField = AuctionService.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        AuctionService existing = (AuctionService) instanceField.get(null);
        if (existing != null) existing.shutdown();
        instanceField.set(null, null);

        service = AuctionService.getInstance();

        fakeItemDAO = new FakeItemDAO();
        fakeWalletDAO = new FakeWalletDAO();
        fakeInvoiceDAO = new FakeInvoiceDAO();

        injectField("itemDAO", fakeItemDAO);
        injectField("walletDAO", fakeWalletDAO);
        injectField("invoiceDAO", fakeInvoiceDAO);

        Item item = new Electronics(ITEM_ID, "Test Item", "Desc", 1000.0,
                System.currentTimeMillis(), System.currentTimeMillis() + 3600000L,
                "BrandX", "12 months", SELLER_ID);
        fakeItemDAO.addItem(item);
    }

    @Test
    @DisplayName("processPayment: marks PAID and transfers funds to seller")
    void should_markPaidAndTransferFunds_when_validPayment() {
        Invoice pendingInvoice = new Invoice(
            "INV-001", "AUC-001", ITEM_ID, BIDDER_A, SELLER_ID, 1500.0,
            System.currentTimeMillis(), "PENDING");
        fakeInvoiceDAO.invoices.put("INV-001", pendingInvoice);

        Response res = service.processPayment("INV-001", BIDDER_A);

        assertEquals("SUCCESS", res.getStatus());
        assertEquals("PAID", pendingInvoice.getStatus());
        assertEquals("PAID", fakeItemDAO.updatedStatuses.get(ITEM_ID));
        assertEquals(1500.0, fakeWalletDAO.getBalance(SELLER_ID), 0.001);
    }

    @Test
    @DisplayName("processPayment: rejects when invoice not found")
    void should_returnFail_when_invoiceNotFound() {
        Response res = service.processPayment("GHOST", BIDDER_A);

        assertEquals("FAIL", res.getStatus());
        assertEquals(0.0, fakeWalletDAO.getBalance(SELLER_ID), 0.001);
    }

    @Test
    @DisplayName("processPayment: rejects when user is not the winning bidder")
    void should_returnFail_when_unauthorizedUserTriesToPay() {
        Invoice pendingInvoice = new Invoice(
            "INV-001", "AUC-001", ITEM_ID, BIDDER_A, SELLER_ID, 1500.0,
            System.currentTimeMillis(), "PENDING");
        fakeInvoiceDAO.invoices.put("INV-001", pendingInvoice);

        Response res = service.processPayment("INV-001", "someOtherUser");

        assertEquals("FAIL", res.getStatus());
        assertTrue(res.getMessage().contains("Unauthorized"));
        assertEquals(0.0, fakeWalletDAO.getBalance(SELLER_ID), 0.001);
    }

    @Test
    @DisplayName("processPayment: rejects double payment on already PAID invoice")
    void should_returnFail_when_invoiceAlreadyPaid() {
        Invoice paidInvoice = new Invoice(
            "INV-001", "AUC-001", ITEM_ID, BIDDER_A, SELLER_ID, 1500.0,
            System.currentTimeMillis(), "PAID");
        fakeInvoiceDAO.invoices.put("INV-001", paidInvoice);

        Response res = service.processPayment("INV-001", BIDDER_A);

        assertEquals("FAIL", res.getStatus());
        assertTrue(res.getMessage().contains("already processed"));
    }

    @Test
    @DisplayName("processPayment: rejects payment on CANCELED invoice")
    void should_returnFail_when_invoiceIsCanceled() {
        Invoice canceledInvoice = new Invoice(
            "INV-001", "AUC-001", ITEM_ID, BIDDER_A, SELLER_ID, 1500.0,
            System.currentTimeMillis(), "CANCELED");
        fakeInvoiceDAO.invoices.put("INV-001", canceledInvoice);

        Response res = service.processPayment("INV-001", BIDDER_A);

        assertEquals("FAIL", res.getStatus());
    }

    @Test
    @DisplayName("processCancellation: cancels invoice, penalizes bidder, resets item for re-auction")
    void should_cancelAndPenalize_when_pendingInvoiceCanceled() {
        Invoice pendingInvoice = new Invoice(
            "INV-001", "AUC-001", ITEM_ID, BIDDER_A, SELLER_ID, 1500.0,
            System.currentTimeMillis(), "PENDING");
        fakeInvoiceDAO.invoices.put("INV-001", pendingInvoice);

        Response res = service.processCancellation("INV-001", BIDDER_A);

        assertEquals("SUCCESS", res.getStatus());
        assertEquals("CANCELED", pendingInvoice.getStatus());
        assertEquals(1500.0, fakeWalletDAO.getBalance(SELLER_ID), 0.001);
        assertEquals(1, fakeItemDAO.resetCount);
    }

    @Test
    @DisplayName("processCancellation: rejects when invoice not found")
    void should_returnFail_when_cancelingNonExistentInvoice() {
        Response res = service.processCancellation("GHOST", BIDDER_A);

        assertEquals("FAIL", res.getStatus());
        assertEquals(0.0, fakeWalletDAO.getBalance(SELLER_ID), 0.001);
    }

    @Test
    @DisplayName("processCancellation: rejects when invoice is already PAID (not PENDING)")
    void should_returnFail_when_cancelingAlreadyPaidInvoice() {
        Invoice paidInvoice = new Invoice(
            "INV-001", "AUC-001", ITEM_ID, BIDDER_A, SELLER_ID, 1500.0,
            System.currentTimeMillis(), "PAID");
        fakeInvoiceDAO.invoices.put("INV-001", paidInvoice);

        Response res = service.processCancellation("INV-001", BIDDER_A);

        assertEquals("FAIL", res.getStatus());
        assertEquals(0, fakeItemDAO.resetCount);
    }

    @Test
    @DisplayName("processCancellation: falls back to CANCELED status when resetItemForReauction fails")
    void should_markCanceled_when_reAuctionResetFails() {
        Invoice pendingInvoice = new Invoice(
            "INV-001", "AUC-001", ITEM_ID, BIDDER_A, SELLER_ID, 1500.0,
            System.currentTimeMillis(), "PENDING");
        fakeInvoiceDAO.invoices.put("INV-001", pendingInvoice);
        fakeItemDAO.resetResult = false;

        Response res = service.processCancellation("INV-001", BIDDER_A);

        assertEquals("SUCCESS", res.getStatus());
        assertEquals("CANCELED", pendingInvoice.getStatus());
        assertEquals("CANCELED", fakeItemDAO.updatedStatuses.get(ITEM_ID));
    }

    private void injectField(String fieldName, Object mock) throws Exception {
        Field f = AuctionService.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(service, mock);
    }
}
