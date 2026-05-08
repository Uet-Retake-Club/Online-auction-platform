package com.auction.shared.models;

import com.auction.shared.models.*;

public class ItemFactory {
    public static Item createItem(ItemCategory category) {
        switch (category) {
            case ELECTRONICS:
                return new Electronics();
            case ART:
                return new Art();
            case VEHICLE:
                return new Vehicle();
            default:
                // Trả về một lớp con mặc định nếu không khớp
                return new GeneralItem(); 
        }
    }
}