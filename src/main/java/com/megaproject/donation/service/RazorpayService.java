package com.megaproject.donation.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import jakarta.annotation.PostConstruct;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RazorpayService {

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    private RazorpayClient razorpayClient;

    @PostConstruct
    public void init() throws RazorpayException {
        // Initialize the RazorpayClient with the keys from properties
        this.razorpayClient = new RazorpayClient(keyId, keySecret);
    }

    /**
     * Creates an order in Razorpay for the given amount.
     * @param amountInRupees amount in rupees
     * @return the Razorpay Order object
     */
    public Order createOrder(double amountInRupees) throws RazorpayException {
        JSONObject orderRequest = new JSONObject();
        // Razorpay expects amount in paise (multiply by 100)
        orderRequest.put("amount", (int) (amountInRupees * 100));
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "txn_" + System.currentTimeMillis());

        return razorpayClient.orders.create(orderRequest);
    }

    /**
     * Verifies the payment signature returned by the client.
     */
    public boolean verifySignature(String orderId, String paymentId, String signature) {
        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", orderId);
            options.put("razorpay_payment_id", paymentId);
            options.put("razorpay_signature", signature);

            return Utils.verifyPaymentSignature(options, keySecret);
        } catch (RazorpayException e) {
            e.printStackTrace();
            return false;
        }
    }
}
