package com.auction.client.utils;

import com.auction.shared.models.Collectibles;
import com.auction.shared.models.Electronics;
import com.auction.shared.models.Fashion;
import com.auction.shared.models.HomeAndGarden;
import com.auction.shared.models.Item;
import com.auction.shared.models.ItemCategory;
import com.auction.shared.models.OtherItem;
import com.auction.shared.models.Sports;
import com.auction.shared.models.Vehicle;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;

/**
 * Custom Gson deserializer for the abstract {@link Item} class.
 *
 * <p>Gson cannot instantiate abstract types directly. This deserializer reads the {@code category}
 * field from the JSON object and delegates deserialization to the correct concrete subclass so that
 * polymorphic item lists can be round-tripped over the network.
 */
public class ItemDeserializer implements JsonDeserializer<Item> {

    @Override
    public Item deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        final JsonObject obj = json.getAsJsonObject();

        // Determine concrete type from the category field
        final JsonElement categoryEl = obj.get("category");
        if (categoryEl == null || categoryEl.isJsonNull()) {
            return context.deserialize(obj, OtherItem.class);
        }

        final String categoryStr = categoryEl.getAsString();
        final ItemCategory category;
        try {
            category = ItemCategory.valueOf(categoryStr);
        } catch (IllegalArgumentException e) {
            return context.deserialize(obj, OtherItem.class);
        }

        switch (category) {
            case ELECTRONICS:
                return context.deserialize(obj, Electronics.class);
            case VEHICLE:
                return context.deserialize(obj, Vehicle.class);
            case HOME_AND_GARDEN:
                return context.deserialize(obj, HomeAndGarden.class);
            case SPORTS:
                return context.deserialize(obj, Sports.class);
            case FASHION:
                return context.deserialize(obj, Fashion.class);
            case COLLECTIBLES:
                return context.deserialize(obj, Collectibles.class);
            default:
                return context.deserialize(obj, OtherItem.class);
        }
    }
}
