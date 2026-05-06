package com.auction.shared.models;

public class GeneralItem extends Item {
    
    // Constructor mặc định
    public GeneralItem() {
        super(0, "", "", ItemCategory.GENERAL, 0.0, System.currentTimeMillis(), System.currentTimeMillis() + 86400000); // Ví dụ: kết thúc sau 24 giờ
    }

    // Nếu trong Item có phương thức abstract, bạn phải override ở đây
    @Override
    public void printInfo() {
        System.out.println("General Auction Product");
    }
}