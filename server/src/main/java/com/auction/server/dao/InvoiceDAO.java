package com.auction.server.dao;

import java.util.List;

import com.auction.shared.models.Invoice;

public interface InvoiceDAO {
    boolean createInvoice(Invoice invoice);
    Invoice getInvoiceById(String id);
    List<Invoice> getInvoicesByUserId(String userId); // Xem hóa đơn của 1 người (cả mua và bán)
    boolean updateInvoiceStatus(String invoiceId, String status);
}