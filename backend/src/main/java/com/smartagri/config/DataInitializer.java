package com.smartagri.config;

import com.smartagri.entity.*;
import com.smartagri.enums.FarmerStatus;
import com.smartagri.enums.Role;
import com.smartagri.enums.UserStatus;
import com.smartagri.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final MarketPriceRepository marketPriceRepository;
    private final FarmerRepository farmerRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.enabled:false}")
    private boolean seedEnabled;

    @Override
    public void run(String... args) {
        // Admin account is always created (idempotent check inside)
        seedAdmin();

        if (!seedEnabled) {
            log.info("Seed data is disabled (ENABLE_SEED_DATA=false). Skipping demo data.");
            return;
        }

        log.info("Seed data is enabled. Inserting demo data if missing...");
        seedCategories();
        seedMarketPrices();
        seedFarmersAndProducts();
    }

    private void seedAdmin() {
        if (userRepository.existsByEmail("admin@smartagri.in")) return;
        User admin = User.builder()
                .fullName("Admin")
                .email("admin@smartagri.in")
                .password(passwordEncoder.encode("Admin@123"))
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .phone("9999999999")
                .build();
        userRepository.save(admin);
        log.info("Admin account created: admin@smartagri.in / Admin@123");
    }

    private void seedCategories() {
        if (categoryRepository.count() > 0) return;
        List<Category> categories = List.of(
                Category.builder().categoryName("Vegetables").emoji("🥬").color("#4CAF50").build(),
                Category.builder().categoryName("Fruits").emoji("🍎").color("#FF5722").build(),
                Category.builder().categoryName("Grains & Cereals").emoji("🌾").color("#FFC107").build(),
                Category.builder().categoryName("Dairy Products").emoji("🥛").color("#2196F3").build(),
                Category.builder().categoryName("Spices & Herbs").emoji("🌶️").color("#E91E63").build(),
                Category.builder().categoryName("Pulses & Legumes").emoji("🫘").color("#9C27B0").build(),
                Category.builder().categoryName("Oilseeds").emoji("🌻").color("#FF9800").build(),
                Category.builder().categoryName("Organic Products").emoji("🌿").color("#66BB6A").build()
        );
        categoryRepository.saveAll(categories);
        log.info("{} categories seeded", categories.size());
    }

    private void seedMarketPrices() {
        if (marketPriceRepository.count() > 0) return;
        List<MarketPrice> prices = List.of(
                MarketPrice.builder().commodityName("Tomato").emoji("🍅").price(new BigDecimal("45.00")).change(new BigDecimal("2.5")).unit("per kg").build(),
                MarketPrice.builder().commodityName("Onion").emoji("🧅").price(new BigDecimal("38.00")).change(new BigDecimal("-1.2")).unit("per kg").build(),
                MarketPrice.builder().commodityName("Potato").emoji("🥔").price(new BigDecimal("28.00")).change(new BigDecimal("0.8")).unit("per kg").build(),
                MarketPrice.builder().commodityName("Rice").emoji("🍚").price(new BigDecimal("55.00")).change(new BigDecimal("1.0")).unit("per kg").build(),
                MarketPrice.builder().commodityName("Wheat").emoji("🌾").price(new BigDecimal("32.00")).change(new BigDecimal("-0.5")).unit("per kg").build(),
                MarketPrice.builder().commodityName("Banana").emoji("🍌").price(new BigDecimal("40.00")).change(new BigDecimal("1.5")).unit("per dozen").build(),
                MarketPrice.builder().commodityName("Mango").emoji("🥭").price(new BigDecimal("120.00")).change(new BigDecimal("5.0")).unit("per kg").build()
        );
        marketPriceRepository.saveAll(prices);
        log.info("{} market prices seeded", prices.size());
    }

    // ================================================================
    // SEED FARMERS & 160 PRODUCTS (20 per category)
    // ================================================================
    private void seedFarmersAndProducts() {
        if (productRepository.count() > 0) return;

        // Create 4 seed farmer accounts
        Farmer[] farmers = createSeedFarmers();
        if (farmers.length == 0) {
            log.warn("No seed farmers could be created — skipping product seeding");
            return;
        }

        List<Category> categories = categoryRepository.findAll();
        if (categories.isEmpty()) {
            log.warn("No categories found — skipping product seeding");
            return;
        }

        Map<String, List<String[]>> productData = buildProductData();
        List<Product> allProducts = new ArrayList<>();
        int farmerIdx = 0;

        for (Category category : categories) {
            List<String[]> items = productData.get(category.getCategoryName());
            if (items == null) continue;
            for (String[] item : items) {
                Farmer farmer = farmers[farmerIdx % farmers.length];
                farmerIdx++;
                allProducts.add(Product.builder()
                        .farmer(farmer)
                        .category(category)
                        .productName(item[0])
                        .description(item[1])
                        .price(new BigDecimal(item[2]))
                        .stock(Integer.parseInt(item[3]))
                        .unit(item[4])
                        .image(item[5])
                        .organicCertified(Boolean.parseBoolean(item[6]))
                        .featured(Boolean.parseBoolean(item[7]))
                        .discount(Integer.parseInt(item[8]))
                        .build());
            }
        }

        productRepository.saveAll(allProducts);
        log.info("{} products seeded across {} categories", allProducts.size(), categories.size());
    }

    private Farmer[] createSeedFarmers() {
        String[][] farmerData = {
                {"Rajesh Kumar",     "farmer1@smartagri.in", "9876543210", "Kumar Organic Farm",   "Nashik, Maharashtra",   "10 years", "Organic Vegetables", "A third-generation farmer practicing sustainable agriculture in the fertile lands of Nashik."},
                {"Lakshmi Devi",     "farmer2@smartagri.in", "9876543211", "Green Valley Farm",    "Coimbatore, Tamil Nadu", "8 years",  "Fruits & Spices",   "Passionate about chemical-free farming and known for premium quality tropical fruits."},
                {"Suresh Patel",     "farmer3@smartagri.in", "9876543212", "Patel Agri Farms",     "Anand, Gujarat",        "15 years", "Dairy & Grains",    "Award-winning farmer specializing in dairy products and traditional grain cultivation."},
                {"Anjali Sharma",    "farmer4@smartagri.in", "9876543213", "Sharma Natural Farm",  "Dehradun, Uttarakhand", "6 years",  "Organic Herbs",     "Pioneer of high-altitude organic farming, growing premium herbs and specialty crops."},
        };

        List<Farmer> farmers = new ArrayList<>();
        for (String[] fd : farmerData) {
            if (userRepository.existsByEmail(fd[1])) {
                farmerRepository.findByUser_Email(fd[1]).ifPresent(farmers::add);
                continue;
            }
            User user = userRepository.save(User.builder()
                    .fullName(fd[0]).email(fd[1]).phone(fd[2])
                    .password(passwordEncoder.encode("Farmer@123"))
                    .role(Role.FARMER).status(UserStatus.ACTIVE).emailVerified(true)
                    .build());
            Farmer farmer = farmerRepository.save(Farmer.builder()
                    .user(user).farmName(fd[3]).farmLocation(fd[4])
                    .experience(fd[5]).specialty(fd[6]).description(fd[7])
                    .status(FarmerStatus.APPROVED)
                    .build());
            farmers.add(farmer);
            log.info("Farmer created: {} / Farmer@123", fd[1]);
        }
        return farmers.toArray(new Farmer[0]);
    }

    /**
     * 20 products × 8 categories = 160 products.
     * Each entry: {name, description, price, stock, unit, imageUrl, organic, featured, discount}
     */
    private Map<String, List<String[]>> buildProductData() {
        return Map.of(
            "Vegetables", List.of(
                p("Fresh Tomatoes",          "Vine-ripened red tomatoes, firm and juicy, perfect for salads and curries",                "45.00",  "500", "per kg",    "https://images.unsplash.com/photo-1546470427-0d4db154ceb8?w=400", "false", "true",  "0"),
                p("Green Peas",              "Sweet and tender garden peas, hand-picked at peak freshness",                            "65.00",  "300", "per kg",    "https://images.unsplash.com/photo-1587735243615-c03f25aaff15?w=400", "false", "false", "5"),
                p("Baby Spinach",            "Nutrient-rich baby spinach leaves, ideal for salads, smoothies and cooking",             "40.00",  "200", "per bunch", "https://images.unsplash.com/photo-1576045057995-568f588f82fb?w=400", "true",  "true",  "0"),
                p("Red Onions",              "Premium quality red onions with strong aroma, essential for Indian cooking",             "38.00",  "800", "per kg",    "https://images.unsplash.com/photo-1618512496248-a07fe83aa8cb?w=400", "false", "false", "0"),
                p("Fresh Carrots",           "Crunchy orange carrots, sweet and loaded with beta-carotene",                           "35.00",  "400", "per kg",    "https://images.unsplash.com/photo-1598170845058-32b9d6a5da37?w=400", "false", "false", "10"),
                p("Cauliflower",             "Farm-fresh white cauliflower heads, compact and pesticide-free",                        "30.00",  "350", "per piece", "https://images.unsplash.com/photo-1568702846914-96b305d2aaeb?w=400", "false", "false", "0"),
                p("Green Capsicum",          "Crisp green bell peppers, perfect for stir-fries and stuffing",                         "55.00",  "250", "per kg",    "https://images.unsplash.com/photo-1563565375-f3fdfdbefa83?w=400", "false", "false", "0"),
                p("Fresh Broccoli",          "Premium broccoli florets, rich in vitamins C and K",                                    "70.00",  "180", "per piece", "https://images.unsplash.com/photo-1459411552884-841db9b3cc2a?w=400", "true",  "true",  "0"),
                p("Bottle Gourd (Lauki)",    "Tender bottle gourd, light and nutritious, a staple in Indian kitchens",                "25.00",  "300", "per piece", "https://images.unsplash.com/photo-1622205313162-be1d5712a43f?w=400", "false", "false", "0"),
                p("Ridge Gourd (Turai)",     "Fresh ridge gourd with tender skin, perfect for quick sabzis",                          "28.00",  "250", "per kg",    "https://images.unsplash.com/photo-1604977042946-1eecc30f269e?w=400", "false", "false", "5"),
                p("Bitter Gourd (Karela)",   "Farm-fresh bitter gourd known for its medicinal properties",                            "40.00",  "200", "per kg",    "https://images.unsplash.com/photo-1587735243615-c03f25aaff15?w=400", "false", "false", "0"),
                p("Lady Finger (Bhindi)",    "Tender okra pods, hand-picked for minimal seeds and maximum flavour",                   "45.00",  "350", "per kg",    "https://images.unsplash.com/photo-1604977042946-1eecc30f269e?w=400", "false", "false", "0"),
                p("Fresh Potatoes",          "Starchy Agra potatoes, ideal for frying, mashing or curries",                           "28.00",  "1000","per kg",    "https://images.unsplash.com/photo-1518977676601-b53f82ber5f7?w=400", "false", "true",  "0"),
                p("Drumstick (Moringa)",     "Long tender drumsticks rich in calcium and iron, great for sambar",                     "50.00",  "150", "per bunch", "https://images.unsplash.com/photo-1622205313162-be1d5712a43f?w=400", "true",  "false", "0"),
                p("Cluster Beans (Guar)",    "Fresh guar pods, high in dietary fibre and traditionally cooked dry",                   "55.00",  "200", "per kg",    "https://images.unsplash.com/photo-1604977042946-1eecc30f269e?w=400", "false", "false", "0"),
                p("Sweet Corn",             "Golden sweet corn cobs, perfect for grilling or adding to salads",                       "30.00",  "300", "per piece", "https://images.unsplash.com/photo-1551754655-cd27e38d2076?w=400", "false", "false", "0"),
                p("Red Cabbage",            "Vibrant purple-red cabbage, crunchy and packed with antioxidants",                        "45.00",  "150", "per piece", "https://images.unsplash.com/photo-1594282486552-05b4d80fbb9f?w=400", "false", "false", "10"),
                p("Ash Gourd (Petha)",       "Large ash gourd, used in Ayurvedic cooking and making Agra Petha",                     "20.00",  "200", "per kg",    "https://images.unsplash.com/photo-1622205313162-be1d5712a43f?w=400", "false", "false", "0"),
                p("French Beans",           "Slim and crisp French beans, snap-fresh from the farm",                                   "60.00",  "250", "per kg",    "https://images.unsplash.com/photo-1604977042946-1eecc30f269e?w=400", "true",  "false", "5"),
                p("Green Chillies",         "Spicy green chillies, farm-fresh with intense heat and aroma",                            "30.00",  "500", "per 250g",  "https://images.unsplash.com/photo-1583119022894-919a68a3d0e3?w=400", "false", "false", "0")
            ),
            "Fruits", List.of(
                p("Alphonso Mango",          "The king of mangoes from Ratnagiri, exceptionally sweet and aromatic",                  "350.00", "200", "per kg",    "https://images.unsplash.com/photo-1553279768-865429fa0078?w=400", "false", "true",  "0"),
                p("Fresh Bananas",           "Naturally ripened Cavendish bananas, rich in potassium",                                "40.00",  "600", "per dozen", "https://images.unsplash.com/photo-1571771894821-ce9b6c11b08e?w=400", "false", "true",  "0"),
                p("Pomegranate",            "Ruby red Bhagwa pomegranates, bursting with sweet-tart juice",                            "160.00", "250", "per kg",    "https://images.unsplash.com/photo-1615485290382-441e4d049cb5?w=400", "false", "false", "5"),
                p("Guava (Amrood)",         "Allahabad Safeda guavas, crunchy and vitamin-C rich",                                     "60.00",  "300", "per kg",    "https://images.unsplash.com/photo-1536511132770-e5058c7e8c46?w=400", "false", "false", "0"),
                p("Papaya",                 "Ripe yellow papayas with orange flesh, perfect for breakfast",                             "35.00",  "200", "per piece", "https://images.unsplash.com/photo-1517282009859-f000ec3b26fe?w=400", "false", "false", "0"),
                p("Watermelon",             "Juicy seedless watermelons, refreshingly sweet for summer",                                "25.00",  "150", "per kg",    "https://images.unsplash.com/photo-1589984662646-e7b2e4962f18?w=400", "false", "true",  "10"),
                p("Kesar Mango",            "Premium Kesar mangoes from Gujarat, smooth texture and rich flavour",                     "280.00", "180", "per kg",    "https://images.unsplash.com/photo-1601493700631-2b16ec4b4716?w=400", "false", "false", "0"),
                p("Sweet Lime (Mosambi)",   "Juicy sweet limes, mildly sweet and perfect for fresh juice",                              "50.00",  "350", "per kg",    "https://images.unsplash.com/photo-1590502593747-42a996133562?w=400", "false", "false", "0"),
                p("Indian Gooseberry (Amla)","Tangy amla berries, a superfood rich in vitamin C and antioxidants",                     "80.00",  "200", "per kg",    "https://images.unsplash.com/photo-1587735243615-c03f25aaff15?w=400", "true",  "false", "0"),
                p("Sapota (Chikoo)",        "Ripe chikoo with caramel-like sweetness, a beloved tropical fruit",                        "70.00",  "250", "per kg",    "https://images.unsplash.com/photo-1546470427-0d4db154ceb8?w=400", "false", "false", "0"),
                p("Fresh Grapes",           "Thompson seedless grapes, crisp green and perfectly sweet",                                 "90.00",  "300", "per kg",    "https://images.unsplash.com/photo-1537640538966-79f369143f8f?w=400", "false", "true",  "5"),
                p("Red Grapes",             "Flame seedless red grapes, naturally sweet with thin skin",                                "110.00", "250", "per kg",    "https://images.unsplash.com/photo-1537640538966-79f369143f8f?w=400", "false", "false", "0"),
                p("Custard Apple (Sitaphal)","Creamy sitaphal with intensely sweet pulp, a seasonal favourite",                         "120.00", "150", "per kg",    "https://images.unsplash.com/photo-1546470427-0d4db154ceb8?w=400", "false", "false", "0"),
                p("Fresh Strawberries",     "Bright red Mahabaleshwar strawberries, sweet with a tangy edge",                           "180.00", "120", "per 250g",  "https://images.unsplash.com/photo-1464965911861-746a04b4bca6?w=400", "true",  "true",  "0"),
                p("Jackfruit",              "Large ripe jackfruit with sweet golden bulbs, tropical and aromatic",                      "50.00",  "100", "per kg",    "https://images.unsplash.com/photo-1546470427-0d4db154ceb8?w=400", "false", "false", "0"),
                p("Tender Coconut",         "Fresh green coconuts with sweet water and soft malai inside",                               "40.00",  "200", "per piece", "https://images.unsplash.com/photo-1551893478-d726eaf0442c?w=400", "false", "false", "0"),
                p("Lemon (Nimbu)",          "Tangy and zesty Indian lemons, essential for every kitchen",                                "20.00",  "500", "per 250g",  "https://images.unsplash.com/photo-1590502593747-42a996133562?w=400", "false", "false", "0"),
                p("Dragon Fruit",           "Exotic pink dragon fruit with white speckled flesh, mildly sweet",                         "200.00", "100", "per piece", "https://images.unsplash.com/photo-1527325678964-54921661f888?w=400", "false", "false", "0"),
                p("Fig (Anjeer)",           "Fresh Pune figs, honey-sweet and packed with dietary fibre",                                "250.00", "80",  "per 250g",  "https://images.unsplash.com/photo-1601379329542-31a1098e6a62?w=400", "true",  "false", "0"),
                p("Star Fruit (Kamrakh)",   "Tangy star-shaped fruit, crunchy and refreshing, rich in vitamin C",                       "90.00",  "120", "per kg",    "https://images.unsplash.com/photo-1546470427-0d4db154ceb8?w=400", "false", "false", "10")
            ),
            "Grains & Cereals", List.of(
                p("Basmati Rice",            "Premium aged Basmati rice from Dehradun, extra-long grains with floral aroma",          "180.00", "500", "per kg",    "https://images.unsplash.com/photo-1586201375761-83865001e31c?w=400", "false", "true",  "0"),
                p("Organic Brown Rice",      "Unpolished brown rice with intact bran, nutty flavour and high fibre",                  "120.00", "300", "per kg",    "https://images.unsplash.com/photo-1586201375761-83865001e31c?w=400", "true",  "true",  "5"),
                p("Whole Wheat (Gehun)",     "MP Sharbati wheat, the gold standard for soft rotis",                                   "40.00",  "800", "per kg",    "https://images.unsplash.com/photo-1574323347407-f5e1ad6d020b?w=400", "false", "false", "0"),
                p("Pearl Millet (Bajra)",    "Nutritious bajra grains, gluten-free and perfect for winter rotis",                     "45.00",  "400", "per kg",    "https://images.unsplash.com/photo-1574323347407-f5e1ad6d020b?w=400", "false", "false", "0"),
                p("Finger Millet (Ragi)",    "Calcium-rich ragi, ideal for porridge, dosas and health drinks",                        "55.00",  "350", "per kg",    "https://images.unsplash.com/photo-1574323347407-f5e1ad6d020b?w=400", "true",  "true",  "0"),
                p("Sorghum (Jowar)",         "Ancient grain jowar, gluten-free and perfect for bhakri and rotis",                     "42.00",  "300", "per kg",    "https://images.unsplash.com/photo-1574323347407-f5e1ad6d020b?w=400", "false", "false", "0"),
                p("Corn Flour (Makka Atta)", "Stone-ground maize flour, essential for makki ki roti",                                 "38.00",  "250", "per kg",    "https://images.unsplash.com/photo-1551754655-cd27e38d2076?w=400", "false", "false", "0"),
                p("Amaranth (Rajgira)",      "Tiny gluten-free amaranth seeds, popular during fasting and rich in protein",           "90.00",  "200", "per kg",    "https://images.unsplash.com/photo-1574323347407-f5e1ad6d020b?w=400", "true",  "false", "0"),
                p("Foxtail Millet (Kangni)", "Revived ancient millet, light and easy to digest, cooks like rice",                     "75.00",  "180", "per kg",    "https://images.unsplash.com/photo-1574323347407-f5e1ad6d020b?w=400", "true",  "false", "0"),
                p("Barnyard Millet (Sanwa)", "Low-calorie millet grain, excellent for diabetes-friendly meals",                       "80.00",  "150", "per kg",    "https://images.unsplash.com/photo-1574323347407-f5e1ad6d020b?w=400", "true",  "false", "10"),
                p("Broken Wheat (Dalia)",    "Cracked wheat grains, perfect for upma, kheer and porridge",                            "50.00",  "350", "per kg",    "https://images.unsplash.com/photo-1574323347407-f5e1ad6d020b?w=400", "false", "false", "0"),
                p("Semolina (Suji/Rava)",    "Fine wheat semolina, essential for halwa, upma and idli",                               "48.00",  "400", "per kg",    "https://images.unsplash.com/photo-1574323347407-f5e1ad6d020b?w=400", "false", "false", "0"),
                p("Wheat Flour (Atta)",      "Whole wheat chakki atta, freshly ground for soft rotis",                                "45.00",  "600", "per kg",    "https://images.unsplash.com/photo-1574323347407-f5e1ad6d020b?w=400", "false", "true",  "0"),
                p("Rice Flour",             "Fine rice flour for appam, neer dosa and modak",                                          "55.00",  "250", "per kg",    "https://images.unsplash.com/photo-1586201375761-83865001e31c?w=400", "false", "false", "0"),
                p("Poha (Flattened Rice)",   "Light flattened rice flakes, the base for classic poha breakfast",                       "42.00",  "300", "per kg",    "https://images.unsplash.com/photo-1586201375761-83865001e31c?w=400", "false", "false", "0"),
                p("Puffed Rice (Murmura)",   "Crispy puffed rice, perfect for bhelpuri, chivda and snacks",                           "35.00",  "400", "per kg",    "https://images.unsplash.com/photo-1586201375761-83865001e31c?w=400", "false", "false", "0"),
                p("Sona Masoori Rice",       "Lightweight Sona Masoori rice from Andhra, ideal for daily meals",                      "70.00",  "500", "per kg",    "https://images.unsplash.com/photo-1586201375761-83865001e31c?w=400", "false", "false", "0"),
                p("Red Rice (Matta Rice)",   "Kerala red rice with intact bran, earthy flavour and high fibre",                       "95.00",  "200", "per kg",    "https://images.unsplash.com/photo-1586201375761-83865001e31c?w=400", "true",  "false", "0"),
                p("Black Rice (Forbidden Rice)","Antioxidant-rich black rice, turns deep purple when cooked",                         "220.00", "100", "per kg",    "https://images.unsplash.com/photo-1586201375761-83865001e31c?w=400", "true",  "false", "0"),
                p("Oats (Jau)",              "Rolled whole oats, fibre-rich and heart-healthy for breakfast",                         "85.00",  "250", "per kg",    "https://images.unsplash.com/photo-1574323347407-f5e1ad6d020b?w=400", "false", "false", "5")
            ),
            "Dairy Products", List.of(
                p("Farm Fresh Milk",         "Pasteurized full-cream cow milk, delivered fresh every morning",                         "60.00",  "200", "per litre", "https://images.unsplash.com/photo-1550583724-b2692b85b150?w=400", "false", "true",  "0"),
                p("A2 Cow Milk",             "Premium A2 protein milk from Gir cows, easier to digest",                               "90.00",  "150", "per litre", "https://images.unsplash.com/photo-1550583724-b2692b85b150?w=400", "true",  "true",  "0"),
                p("Buffalo Milk",            "Rich and creamy buffalo milk, higher fat content for thick curd",                        "70.00",  "180", "per litre", "https://images.unsplash.com/photo-1550583724-b2692b85b150?w=400", "false", "false", "0"),
                p("Fresh Paneer",            "Soft and crumbly farm-made paneer, ideal for curries and tikka",                         "320.00", "100", "per kg",    "https://images.unsplash.com/photo-1631452180519-c014fe946bc7?w=400", "false", "true",  "0"),
                p("Organic Paneer",          "Chemical-free paneer made from organic A2 milk, silky texture",                          "420.00", "80",  "per kg",    "https://images.unsplash.com/photo-1631452180519-c014fe946bc7?w=400", "true",  "false", "0"),
                p("Desi Ghee (Cow)",         "Traditional bilona method cow ghee, golden and aromatic",                                "650.00", "120", "per 500g",  "https://images.unsplash.com/photo-1631452180519-c014fe946bc7?w=400", "true",  "true",  "0"),
                p("Buffalo Ghee",            "Rich buffalo ghee, perfect for sweets and Punjabi cooking",                              "550.00", "100", "per 500g",  "https://images.unsplash.com/photo-1631452180519-c014fe946bc7?w=400", "false", "false", "5"),
                p("Fresh Curd (Dahi)",       "Thick-set fresh curd, creamy and probiotic-rich",                                        "45.00",  "250", "per 500g",  "https://images.unsplash.com/photo-1550583724-b2692b85b150?w=400", "false", "false", "0"),
                p("Buttermilk (Chaas)",      "Spiced traditional buttermilk, refreshing and digestive",                                 "25.00",  "300", "per litre", "https://images.unsplash.com/photo-1550583724-b2692b85b150?w=400", "false", "false", "0"),
                p("Fresh Cream",             "Farm-fresh malai cream, perfect for desserts and chai",                                  "180.00", "100", "per 250g",  "https://images.unsplash.com/photo-1550583724-b2692b85b150?w=400", "false", "false", "0"),
                p("Mozzarella Cheese",       "Stretchy mozzarella made from fresh milk, great for pizza and pasta",                    "450.00", "80",  "per 200g",  "https://images.unsplash.com/photo-1631452180519-c014fe946bc7?w=400", "false", "false", "0"),
                p("Cottage Cheese (Chenna)", "Soft chenna for making rasgulla, sandesh and Bengali sweets",                            "350.00", "90",  "per kg",    "https://images.unsplash.com/photo-1631452180519-c014fe946bc7?w=400", "false", "false", "0"),
                p("Flavoured Yogurt",        "Farm-fresh mango yogurt, creamy and naturally sweetened",                                "60.00",  "150", "per 200g",  "https://images.unsplash.com/photo-1550583724-b2692b85b150?w=400", "false", "false", "0"),
                p("Shrikhand",              "Gujarati saffron shrikhand, thick strained yogurt dessert",                                "120.00", "100", "per 250g",  "https://images.unsplash.com/photo-1550583724-b2692b85b150?w=400", "false", "false", "0"),
                p("Khoya (Mawa)",           "Fresh reduced milk solid, essential for Indian sweets",                                    "400.00", "80",  "per 500g",  "https://images.unsplash.com/photo-1631452180519-c014fe946bc7?w=400", "false", "false", "0"),
                p("Lassi (Sweet)",           "Thick Punjabi sweet lassi, topped with cream and cardamom",                              "40.00",  "200", "per glass",  "https://images.unsplash.com/photo-1550583724-b2692b85b150?w=400", "false", "false", "0"),
                p("Rose Lassi",              "Fragrant rose-flavoured lassi with a hint of saffron",                                   "50.00",  "150", "per glass",  "https://images.unsplash.com/photo-1550583724-b2692b85b150?w=400", "false", "false", "0"),
                p("Goat Milk",               "Fresh goat milk, rich in nutrients and closer to human milk composition",                "100.00", "80",  "per litre", "https://images.unsplash.com/photo-1550583724-b2692b85b150?w=400", "true",  "false", "0"),
                p("Milk Powder",             "Farm-made spray-dried milk powder, long shelf life",                                     "280.00", "120", "per 500g",  "https://images.unsplash.com/photo-1631452180519-c014fe946bc7?w=400", "false", "false", "10"),
                p("Fresh Butter (Makhan)",   "Hand-churned white butter from fresh cream, pure and aromatic",                          "350.00", "100", "per 500g",  "https://images.unsplash.com/photo-1631452180519-c014fe946bc7?w=400", "true",  "true",  "0")
            ),
            "Spices & Herbs", List.of(
                p("Kashmiri Red Chilli",     "Sun-dried Kashmiri chillies, vibrant colour with mild heat",                            "250.00", "200", "per 250g",  "https://images.unsplash.com/photo-1583119022894-919a68a3d0e3?w=400", "false", "true",  "0"),
                p("Turmeric Powder (Haldi)", "Lakadong turmeric with 7-9% curcumin, bright yellow and medicinal",                     "180.00", "250", "per 250g",  "https://images.unsplash.com/photo-1615485290382-441e4d049cb5?w=400", "true",  "true",  "0"),
                p("Whole Black Pepper",      "Tellicherry black peppercorns, bold aroma and sharp bite",                              "350.00", "150", "per 250g",  "https://images.unsplash.com/photo-1599909533202-fdd2d0e36c9e?w=400", "false", "false", "5"),
                p("Cumin Seeds (Jeera)",     "Premium Rajasthani cumin, earthy and warm, essential for tempering",                     "200.00", "300", "per 250g",  "https://images.unsplash.com/photo-1599909533202-fdd2d0e36c9e?w=400", "false", "true",  "0"),
                p("Coriander Powder (Dhaniya)","Freshly ground coriander, citrusy and mild, a base spice for every curry",            "120.00", "350", "per 250g",  "https://images.unsplash.com/photo-1599909533202-fdd2d0e36c9e?w=400", "false", "false", "0"),
                p("Cardamom (Elaichi)",      "Green cardamom pods from Kerala, intensely aromatic",                                   "1200.00","100", "per 100g",  "https://images.unsplash.com/photo-1599909533202-fdd2d0e36c9e?w=400", "false", "true",  "0"),
                p("Cinnamon Sticks (Dalchini)","Sri Lankan Ceylon cinnamon, mildly sweet and fragrant bark",                          "280.00", "120", "per 100g",  "https://images.unsplash.com/photo-1599909533202-fdd2d0e36c9e?w=400", "false", "false", "0"),
                p("Cloves (Laung)",          "Hand-picked cloves from Kerala, intensely pungent and medicinal",                        "450.00", "100", "per 100g",  "https://images.unsplash.com/photo-1599909533202-fdd2d0e36c9e?w=400", "false", "false", "0"),
                p("Saffron (Kesar)",         "Authentic Kashmiri saffron threads, the world's most precious spice",                   "850.00", "50",  "per gram",  "https://images.unsplash.com/photo-1615485290382-441e4d049cb5?w=400", "true",  "true",  "0"),
                p("Star Anise (Chakra Phool)","Whole star anise pods, sweet liquorice-like flavour for biryanis",                     "180.00", "150", "per 100g",  "https://images.unsplash.com/photo-1599909533202-fdd2d0e36c9e?w=400", "false", "false", "0"),
                p("Bay Leaves (Tej Patta)",  "Aromatic dried bay leaves, essential for biryani and pulao",                             "80.00",  "300", "per 50g",   "https://images.unsplash.com/photo-1599909533202-fdd2d0e36c9e?w=400", "false", "false", "0"),
                p("Mustard Seeds (Rai)",     "Small black mustard seeds, pungent and essential for South Indian tempering",            "60.00",  "400", "per 250g",  "https://images.unsplash.com/photo-1599909533202-fdd2d0e36c9e?w=400", "false", "false", "0"),
                p("Fenugreek Seeds (Methi Dana)","Slightly bitter methi seeds, great for pickles and dals",                            "70.00",  "300", "per 250g",  "https://images.unsplash.com/photo-1599909533202-fdd2d0e36c9e?w=400", "false", "false", "0"),
                p("Fresh Curry Leaves",      "Fragrant fresh curry leaves, a must for South Indian cooking",                           "15.00",  "500", "per bunch", "https://images.unsplash.com/photo-1576045057995-568f588f82fb?w=400", "true",  "false", "0"),
                p("Fresh Mint (Pudina)",     "Garden-fresh mint leaves, cooling and aromatic for chutneys",                            "20.00",  "400", "per bunch", "https://images.unsplash.com/photo-1576045057995-568f588f82fb?w=400", "false", "false", "0"),
                p("Fresh Coriander Leaves",  "Vibrant coriander sprigs, indispensable garnish for Indian food",                        "15.00",  "600", "per bunch", "https://images.unsplash.com/photo-1576045057995-568f588f82fb?w=400", "false", "false", "0"),
                p("Dried Red Chilli",        "Guntur red chillies, fiery hot with deep colour for gravies",                            "160.00", "200", "per 250g",  "https://images.unsplash.com/photo-1583119022894-919a68a3d0e3?w=400", "false", "false", "0"),
                p("Asafoetida (Hing)",       "Strong pungent hing, a pinch transforms dals and sabzis",                               "200.00", "150", "per 50g",   "https://images.unsplash.com/photo-1599909533202-fdd2d0e36c9e?w=400", "false", "false", "0"),
                p("Nutmeg (Jaiphal)",        "Whole nutmeg with warm, sweet aroma for biryanis and desserts",                         "350.00", "80",  "per 100g",  "https://images.unsplash.com/photo-1599909533202-fdd2d0e36c9e?w=400", "false", "false", "10"),
                p("Garam Masala",            "House-blend garam masala with 12 whole spices, freshly ground",                          "220.00", "200", "per 100g",  "https://images.unsplash.com/photo-1599909533202-fdd2d0e36c9e?w=400", "false", "true",  "0")
            ),
            "Pulses & Legumes", List.of(
                p("Toor Dal (Arhar)",        "Polished split pigeon peas, the backbone of sambar and dal fry",                        "130.00", "400", "per kg",    "https://images.unsplash.com/photo-1585650737035-7c1c042c3094?w=400", "false", "true",  "0"),
                p("Moong Dal (Split)",       "Yellow split moong dal, light and easy to digest, cooks quickly",                        "110.00", "350", "per kg",    "https://images.unsplash.com/photo-1585650737035-7c1c042c3094?w=400", "false", "true",  "5"),
                p("Chana Dal",               "Bengal gram dal, nutty flavour ideal for dal and sweets like puran poli",                "95.00",  "400", "per kg",    "https://images.unsplash.com/photo-1585650737035-7c1c042c3094?w=400", "false", "false", "0"),
                p("Masoor Dal (Red Lentils)","Quick-cooking red lentils, earthy and comforting in any dal",                             "100.00", "350", "per kg",    "https://images.unsplash.com/photo-1585650737035-7c1c042c3094?w=400", "false", "false", "0"),
                p("Urad Dal (Split Black Gram)","Essential for dal makhani, idli batter and medu vada",                                "120.00", "300", "per kg",    "https://images.unsplash.com/photo-1585650737035-7c1c042c3094?w=400", "false", "true",  "0"),
                p("Whole Moong (Green Gram)","Whole green moong for sprouting, khichdi and moong dal chilla",                           "115.00", "300", "per kg",    "https://images.unsplash.com/photo-1585650737035-7c1c042c3094?w=400", "true",  "false", "0"),
                p("Kabuli Chana (Chickpeas)","Large white chickpeas, perfect for chole, hummus and salads",                             "120.00", "350", "per kg",    "https://images.unsplash.com/photo-1585650737035-7c1c042c3094?w=400", "false", "true",  "0"),
                p("Kala Chana (Black Chickpeas)","Desi black chickpeas, high protein and great for sprouting",                         "90.00",  "300", "per kg",    "https://images.unsplash.com/photo-1585650737035-7c1c042c3094?w=400", "false", "false", "0"),
                p("Rajma (Kidney Beans)",    "Premium Kashmiri rajma, dark red and creamy when cooked",                                 "150.00", "250", "per kg",    "https://images.unsplash.com/photo-1585650737035-7c1c042c3094?w=400", "false", "true",  "0"),
                p("Chitra Rajma",            "Speckled Himalayan rajma, tender with distinct nutty taste",                               "170.00", "200", "per kg",    "https://images.unsplash.com/photo-1585650737035-7c1c042c3094?w=400", "false", "false", "0"),
                p("Lobia (Black-Eyed Peas)", "Cream-coloured beans with black eye, versatile in curries and salads",                   "85.00",  "300", "per kg",    "https://images.unsplash.com/photo-1585650737035-7c1c042c3094?w=400", "false", "false", "0"),
                p("Whole Urad (Black Gram)", "Whole black urad for rich dal makhani and festive dishes",                                "130.00", "250", "per kg",    "https://images.unsplash.com/photo-1585650737035-7c1c042c3094?w=400", "false", "false", "0"),
                p("Green Peas (Dried)",      "Dried green peas, rehydrate and cook for ghugni and curries",                             "70.00",  "400", "per kg",    "https://images.unsplash.com/photo-1585650737035-7c1c042c3094?w=400", "false", "false", "0"),
                p("Moth Dal (Moth Beans)",   "Small brown moth beans, popular in Rajasthani cooking",                                   "80.00",  "250", "per kg",    "https://images.unsplash.com/photo-1585650737035-7c1c042c3094?w=400", "false", "false", "0"),
                p("Horse Gram (Kulith)",     "Ancient superfood pulse, high protein and traditionally used in rasam",                   "75.00",  "200", "per kg",    "https://images.unsplash.com/photo-1585650737035-7c1c042c3094?w=400", "true",  "false", "0"),
                p("Sprouted Moong",          "Ready-to-eat fresh moong sprouts, packed with enzymes and vitamins",                      "60.00",  "150", "per 250g",  "https://images.unsplash.com/photo-1585650737035-7c1c042c3094?w=400", "true",  "false", "0"),
                p("Soybean (Whole)",         "Protein-rich whole soybeans for milk, tofu and nuggets",                                  "65.00",  "300", "per kg",    "https://images.unsplash.com/photo-1585650737035-7c1c042c3094?w=400", "false", "false", "0"),
                p("Val Dal (Field Beans)",   "Flat field bean dal, a Maharashtrian and Gujarati staple",                                 "110.00", "200", "per kg",    "https://images.unsplash.com/photo-1585650737035-7c1c042c3094?w=400", "false", "false", "10"),
                p("Mixed Dal (Panchratna)",  "Five-dal blend of toor, moong, masoor, chana and urad",                                   "105.00", "250", "per kg",    "https://images.unsplash.com/photo-1585650737035-7c1c042c3094?w=400", "false", "false", "0"),
                p("Roasted Chana",           "Crunchy roasted Bengal gram, a high-protein snack",                                        "80.00",  "350", "per kg",    "https://images.unsplash.com/photo-1585650737035-7c1c042c3094?w=400", "false", "false", "0")
            ),
            "Oilseeds", List.of(
                p("Mustard Oil (Sarson)",    "Cold-pressed kachi ghani mustard oil, pungent and heart-healthy",                         "180.00", "200", "per litre", "https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?w=400", "false", "true",  "0"),
                p("Groundnut Oil",           "Wood-pressed peanut oil from Gujarat, ideal for frying and cooking",                      "200.00", "180", "per litre", "https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?w=400", "false", "true",  "0"),
                p("Coconut Oil (Cold Pressed)","Virgin cold-pressed coconut oil from Kerala, multipurpose",                            "250.00", "150", "per litre", "https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?w=400", "true",  "true",  "0"),
                p("Sesame Oil (Til)",        "Unrefined til oil, nutty aroma perfect for South Indian cooking",                         "300.00", "120", "per litre", "https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?w=400", "false", "false", "5"),
                p("Sunflower Seeds",         "Raw sunflower seeds, rich in vitamin E, perfect for snacking",                            "150.00", "200", "per 250g",  "https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?w=400", "false", "false", "0"),
                p("Flax Seeds (Alsi)",       "Golden flax seeds, omega-3 powerhouse for smoothies and baking",                          "120.00", "250", "per 250g",  "https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?w=400", "true",  "true",  "0"),
                p("Chia Seeds",              "Premium chia seeds, form gel in water, great for puddings",                                "180.00", "150", "per 200g",  "https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?w=400", "true",  "false", "0"),
                p("Pumpkin Seeds",           "Roasted pumpkin seeds, crunchy zinc-rich superfood snack",                                 "200.00", "120", "per 200g",  "https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?w=400", "false", "false", "0"),
                p("White Sesame Seeds",      "Hulled white sesame, essential for til laddu and tahini",                                  "110.00", "300", "per 250g",  "https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?w=400", "false", "false", "0"),
                p("Black Sesame Seeds",      "Unhulled black sesame, higher in calcium, used in winter sweets",                         "130.00", "200", "per 250g",  "https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?w=400", "false", "false", "0"),
                p("Groundnuts (Raw Peanuts)","Fresh raw peanuts in shell, protein-packed farm snack",                                   "80.00",  "400", "per kg",    "https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?w=400", "false", "false", "0"),
                p("Cashew Nuts (Kaju)",      "Whole W320 cashews from Goa, creamy and versatile",                                       "900.00", "100", "per 500g",  "https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?w=400", "false", "true",  "0"),
                p("Almonds (Badam)",         "Premium California almonds, crunchy and naturally sweet",                                  "750.00", "120", "per 500g",  "https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?w=400", "false", "false", "0"),
                p("Walnuts (Akhrot)",        "Light-halves Kashmiri walnuts, brain-healthy omega-3 rich",                               "650.00", "80",  "per 250g",  "https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?w=400", "false", "false", "10"),
                p("Pine Nuts (Chilgoza)",    "Rare Himalayan pine nuts, buttery and luxurious",                                          "1500.00","50",  "per 100g",  "https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?w=400", "false", "false", "0"),
                p("Safflower Oil (Kardi)",   "Light safflower oil, high in monounsaturated fats, heart-friendly",                       "220.00", "100", "per litre", "https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?w=400", "false", "false", "0"),
                p("Rice Bran Oil",           "Light and versatile rice bran oil, excellent for high-heat cooking",                       "170.00", "150", "per litre", "https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?w=400", "false", "false", "0"),
                p("Castor Oil (Arandi)",     "Cold-pressed castor oil, traditionally used for hair and skin care",                       "200.00", "100", "per 500ml", "https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?w=400", "true",  "false", "0"),
                p("Niger Seeds (Ramtil)",    "Black niger seeds, pressed for oil in tribal and eastern India",                           "90.00",  "200", "per 250g",  "https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?w=400", "false", "false", "0"),
                p("Hemp Seeds",              "Hulled hemp hearts, complete protein source with nutty flavour",                           "350.00", "80",  "per 200g",  "https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?w=400", "true",  "false", "0")
            ),
            "Organic Products", List.of(
                p("Organic Jaggery (Gur)",   "Unrefined sugarcane jaggery, chemical-free and mineral-rich",                            "80.00",  "300", "per 500g",  "https://images.unsplash.com/photo-1615485290382-441e4d049cb5?w=400", "true",  "true",  "0"),
                p("Organic Honey",           "Raw unprocessed forest honey, collected by tribal beekeepers",                           "350.00", "150", "per 500g",  "https://images.unsplash.com/photo-1587049352846-4a222e784d38?w=400", "true",  "true",  "0"),
                p("Organic Turmeric Root",   "Fresh whole turmeric roots, bright orange and medicinal",                                 "90.00",  "200", "per 250g",  "https://images.unsplash.com/photo-1615485290382-441e4d049cb5?w=400", "true",  "true",  "0"),
                p("Organic Ginger",          "Fresh organic ginger rhizomes, pungent and zingy",                                        "70.00",  "250", "per 250g",  "https://images.unsplash.com/photo-1615485290382-441e4d049cb5?w=400", "true",  "false", "0"),
                p("Organic Garlic",          "Whole organic garlic bulbs, strong flavour and allicin-rich",                             "120.00", "200", "per 250g",  "https://images.unsplash.com/photo-1615485290382-441e4d049cb5?w=400", "true",  "false", "5"),
                p("Organic Coconut Sugar",   "Low-GI coconut palm sugar, caramel-flavoured natural sweetener",                         "250.00", "120", "per 250g",  "https://images.unsplash.com/photo-1615485290382-441e4d049cb5?w=400", "true",  "false", "0"),
                p("Organic Amla Powder",     "Spray-dried amla powder, vitamin C supplement for immunity",                              "150.00", "180", "per 200g",  "https://images.unsplash.com/photo-1615485290382-441e4d049cb5?w=400", "true",  "false", "0"),
                p("Organic Moringa Powder",  "Dried moringa leaf powder, the miracle tree superfood",                                   "180.00", "150", "per 200g",  "https://images.unsplash.com/photo-1576045057995-568f588f82fb?w=400", "true",  "true",  "0"),
                p("Organic Spirulina",       "Farm-grown spirulina powder, 60% complete protein source",                                "400.00", "80",  "per 100g",  "https://images.unsplash.com/photo-1576045057995-568f588f82fb?w=400", "true",  "false", "0"),
                p("Organic Aloe Vera Juice", "Cold-pressed aloe vera juice, digestive and skin health tonic",                           "200.00", "120", "per 500ml", "https://images.unsplash.com/photo-1587049352846-4a222e784d38?w=400", "true",  "false", "0"),
                p("Organic Apple Cider Vinegar","Raw unfiltered ACV with mother culture, from Himalayan apples",                       "280.00", "100", "per 500ml", "https://images.unsplash.com/photo-1587049352846-4a222e784d38?w=400", "true",  "false", "0"),
                p("Organic Tulsi Tea",       "Holy basil herbal tea, stress-relieving and caffeine-free",                               "120.00", "200", "per 50g",   "https://images.unsplash.com/photo-1576045057995-568f588f82fb?w=400", "true",  "true",  "0"),
                p("Organic Ashwagandha Powder","Withania root powder, Ayurvedic adaptogen for energy and focus",                        "220.00", "150", "per 100g",  "https://images.unsplash.com/photo-1576045057995-568f588f82fb?w=400", "true",  "false", "0"),
                p("Organic Black Pepper",    "Certified organic Malabar pepper, hand-picked and sun-dried",                             "320.00", "100", "per 100g",  "https://images.unsplash.com/photo-1599909533202-fdd2d0e36c9e?w=400", "true",  "false", "0"),
                p("Organic Fennel Seeds (Saunf)","Sweet aromatic fennel seeds, digestive and mouth freshener",                          "100.00", "250", "per 250g",  "https://images.unsplash.com/photo-1599909533202-fdd2d0e36c9e?w=400", "true",  "false", "0"),
                p("Organic Ajwain (Carom Seeds)","Pungent carom seeds, traditional remedy for digestion",                               "90.00",  "200", "per 250g",  "https://images.unsplash.com/photo-1599909533202-fdd2d0e36c9e?w=400", "true",  "false", "0"),
                p("Organic Ragi Flour",      "Stone-ground organic finger millet flour for healthy rotis",                               "70.00",  "300", "per kg",    "https://images.unsplash.com/photo-1574323347407-f5e1ad6d020b?w=400", "true",  "false", "0"),
                p("Organic Multi-Grain Atta","Seven-grain organic flour blend for nutrient-dense rotis",                                "90.00",  "250", "per kg",    "https://images.unsplash.com/photo-1574323347407-f5e1ad6d020b?w=400", "true",  "false", "10"),
                p("Organic Peanut Butter",   "Stone-ground crunchy peanut butter, no sugar or preservatives",                           "250.00", "120", "per 350g",  "https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?w=400", "true",  "true",  "0"),
                p("Organic Cold-Pressed Neem Oil","Pure neem oil for natural pest control and skincare",                                "180.00", "100", "per 200ml", "https://images.unsplash.com/photo-1576045057995-568f588f82fb?w=400", "true",  "false", "0")
            )
        );
    }

    /** Shorthand builder for product data array. */
    private String[] p(String name, String desc, String price, String stock,
                       String unit, String image, String organic, String featured, String discount) {
        return new String[]{name, desc, price, stock, unit, image, organic, featured, discount};
    }
}
