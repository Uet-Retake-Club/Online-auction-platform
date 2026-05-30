package com.auction.shared.models;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;

/**
 * Unit tests for {@link ItemFactory}.
 *
 * <p>Covers:
 * <ul>
 *   <li>Each of the 7 {@link ItemCategory} values → produces the correct concrete subclass</li>
 *   <li>The {@code OTHER} (default) case → produces {@link OtherItem}</li>
 *   <li>All created items are non-null, distinct instances</li>
 * </ul>
 */
@DisplayName("ItemFactory — Unit Tests")
class ItemFactoryTest {

    // ─── Category → correct subclass ─────────────────────────────────────────

    @Test
    @DisplayName("createItem(ELECTRONICS) → Electronics instance")
    void should_createElectronics_when_categoryIsElectronics() {
        Item item = ItemFactory.createItem(ItemCategory.ELECTRONICS);
        assertNotNull(item);
        assertInstanceOf(Electronics.class, item);
    }

    @Test
    @DisplayName("createItem(VEHICLE) → Vehicle instance")
    void should_createVehicle_when_categoryIsVehicle() {
        Item item = ItemFactory.createItem(ItemCategory.VEHICLE);
        assertNotNull(item);
        assertInstanceOf(Vehicle.class, item);
    }

    @Test
    @DisplayName("createItem(HOME_AND_GARDEN) → HomeAndGarden instance")
    void should_createHomeAndGarden_when_categoryIsHomeAndGarden() {
        Item item = ItemFactory.createItem(ItemCategory.HOME_AND_GARDEN);
        assertNotNull(item);
        assertInstanceOf(HomeAndGarden.class, item);
    }

    @Test
    @DisplayName("createItem(SPORTS) → Sports instance")
    void should_createSports_when_categoryIsSports() {
        Item item = ItemFactory.createItem(ItemCategory.SPORTS);
        assertNotNull(item);
        assertInstanceOf(Sports.class, item);
    }

    @Test
    @DisplayName("createItem(FASHION) → Fashion instance")
    void should_createFashion_when_categoryIsFashion() {
        Item item = ItemFactory.createItem(ItemCategory.FASHION);
        assertNotNull(item);
        assertInstanceOf(Fashion.class, item);
    }

    @Test
    @DisplayName("createItem(COLLECTIBLES) → Collectibles instance")
    void should_createCollectibles_when_categoryIsCollectibles() {
        Item item = ItemFactory.createItem(ItemCategory.COLLECTIBLES);
        assertNotNull(item);
        assertInstanceOf(Collectibles.class, item);
    }

    @Test
    @DisplayName("createItem(OTHER) → OtherItem instance (default case)")
    void should_createOtherItem_when_categoryIsOther() {
        Item item = ItemFactory.createItem(ItemCategory.OTHER);
        assertNotNull(item);
        assertInstanceOf(OtherItem.class, item);
    }

    // ─── Every call returns a new distinct instance ───────────────────────────

    @ParameterizedTest(name = "createItem({0}) returns distinct instances on each call")
    @EnumSource(ItemCategory.class)
    @DisplayName("createItem: each call produces a new, non-null instance")
    void should_returnNewInstance_on_eachCallForAllCategories(ItemCategory category) {
        Item first  = ItemFactory.createItem(category);
        Item second = ItemFactory.createItem(category);

        assertNotNull(first);
        assertNotNull(second);
        assertNotSame(first, second, "Factory must produce distinct instances, not a shared singleton");
    }

    // ─── Class coverage cross-check ───────────────────────────────────────────

    @Test
    @DisplayName("createItem: all 7 categories produce 7 distinct concrete classes")
    void should_coverAllSevenConcreteClasses() {
        java.util.Set<Class<?>> classes = new java.util.HashSet<>();
        for (ItemCategory cat : ItemCategory.values()) {
            classes.add(ItemFactory.createItem(cat).getClass());
        }
        assertEquals(7, classes.size(),
            "Expected 7 distinct subclasses for 7 categories, got: " + classes);
    }
}
