package com.example.data.model

import com.example.localization.AppLanguage
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class ServiceCategory(
    @DocumentId val id: String = "",
    val nameEn: String = "",
    val nameBn: String = "",
    val descriptionEn: String = "",
    val descriptionBn: String = "",
    val iconKey: String = "other",
    val estimatedPriceBdt: String = "",
    val sortOrder: Long = 0L,
    @get:PropertyName("isActive") @set:PropertyName("isActive")
    var isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun localizedName(language: AppLanguage): String =
        if (language == AppLanguage.BENGALI && nameBn.isNotBlank()) nameBn else nameEn

    fun localizedDescription(language: AppLanguage): String =
        if (language == AppLanguage.BENGALI && descriptionBn.isNotBlank()) descriptionBn else descriptionEn
}

object DefaultServiceCategories {
    val all: List<ServiceCategory> = listOf(
        ServiceCategory(
            id = "cat_fan",
            nameEn = "Fan Installation & Repair",
            nameBn = "ফ্যান ইনস্টলেশন ও মেরামত",
            descriptionEn = "Ceiling fan, exhaust fan, regulator, capacitor replacement, and coil rewinding.",
            descriptionBn = "সিলিং ফ্যান, এগজস্ট ফ্যান, রেগুলেটর, ক্যাপাসিটর পরিবর্তন ও কয়েল বাঁধাই সার্ভিস।",
            iconKey = "fan",
            sortOrder = 1L,
            isActive = true
        ),
        ServiceCategory(
            id = "cat_motor",
            nameEn = "Electric Motor Installation & Repair",
            nameBn = "ইলেকট্রিক মোটর ইনস্টলেশন ও মেরামত",
            descriptionEn = "Water pump & industrial motor installation, bearing replacement, and motor rewinding.",
            descriptionBn = "পানির পাম্প ও ইলেকট্রিক মোটর স্থাপন, বিয়ারিং পরিবর্তন ও মোটর বাঁধাই কাজ।",
            iconKey = "motor",
            sortOrder = 2L,
            isActive = true
        ),
        ServiceCategory(
            id = "cat_ac",
            nameEn = "AC Installation & Servicing",
            nameBn = "এসি ইনস্টলেশন ও সার্ভিসিং",
            descriptionEn = "Split & window AC installation, master servicing, gas refill, and PCB repair.",
            descriptionBn = "স্প্লিট ও উইন্ডো এসি সেটআপ, মাস্টার সার্ভিসিং, গ্যাস রিফিল ও পিসিবি মেরামত।",
            iconKey = "ac",
            sortOrder = 3L,
            isActive = true
        ),
        ServiceCategory(
            id = "cat_light",
            nameEn = "Light & LED Installation",
            nameBn = "লাইট ও এলইডি ইনস্টলেশন",
            descriptionEn = "LED panel, chandelier, tube light, spotlight, and decorative building lighting setup.",
            descriptionBn = "এলইডি প্যানেল, ঝাড়বাতি, স্পটলাইট, টিউবলাইট ও ইনডোর/আউটডোর লাইটিং।",
            iconKey = "light",
            sortOrder = 4L,
            isActive = true
        ),
        ServiceCategory(
            id = "cat_wiring",
            nameEn = "Electrical Wiring & Troubleshooting",
            nameBn = "ইলেকট্রিক্যাল ওয়্যারিং ও ফল্ট সমাধান",
            descriptionEn = "Short circuit diagnostics, DB box, MCB/RCCB setup, IPS/inverter & full house wiring.",
            descriptionBn = "শর্ট সার্কিট নির্ণয়, ডিবি বক্স, এমসিবি/আরসিসিবি, আইপিএস ও পূর্ণাঙ্গ ওয়্যারিং।",
            iconKey = "wiring",
            sortOrder = 5L,
            isActive = true
        ),
        ServiceCategory(
            id = "cat_cctv",
            nameEn = "CCTV Camera Installation & Repair",
            nameBn = "সিসিটিভি ক্যামেরা ইনস্টলেশন ও মেরামত",
            descriptionEn = "HD/IP CCTV camera installation, DVR/NVR configuration, mobile viewing & maintenance.",
            descriptionBn = "এইচডি ও আইপি সিসিটিভি ক্যামেরা স্থাপন, ডিভিআর/এনভিআর ও মোবাইল ভিউ সেটআপ।",
            iconKey = "cctv",
            sortOrder = 6L,
            isActive = true
        ),
        ServiceCategory(
            id = "cat_plumbing",
            nameEn = "Water Pipeline & Plumbing Work",
            nameBn = "পানির পাইপলাইন ও প্লাম্বিং কাজ",
            descriptionEn = "Pipe leakage repair, bathroom fittings, geyser setup, tank line & sanitary plumbing.",
            descriptionBn = "পাইপ লিকেজ মেরামত, বাথরুম ফিটিংস, গিজার সেটআপ, ট্যাংক লাইন ও স্যানিটারি কাজ।",
            iconKey = "plumbing",
            sortOrder = 7L,
            isActive = true
        ),
        ServiceCategory(
            id = "cat_building_electric",
            nameEn = "Building Electrical Work",
            nameBn = "বিল্ডিং ইলেকট্রিক্যাল কাজ",
            descriptionEn = "Multi-story residential & commercial concealed wiring, sub-station & load balancing.",
            descriptionBn = "বহুতল আবাসিক ও বাণিজ্যিক ভবনের কনসিল্ড ওয়্যারিং, মেইন বোর্ড ও লোড ডিস্ট্রিবিউশন।",
            iconKey = "building_electric",
            sortOrder = 8L,
            isActive = true
        ),
        ServiceCategory(
            id = "cat_maintenance",
            nameEn = "Building Maintenance",
            nameBn = "বিল্ডিং রক্ষণাবেক্ষণ",
            descriptionEn = "Scheduled apartment & office electrical, water pump, generator & utility inspection.",
            descriptionBn = "অ্যাপার্টমেন্ট ও অফিসের নিয়মিত ইলেকট্রিক্যাল, পাম্প ও ইউটিলিটি রক্ষণাবেক্ষণ।",
            iconKey = "maintenance",
            sortOrder = 9L,
            isActive = true
        ),
        ServiceCategory(
            id = "cat_other",
            nameEn = "Other Technical Services",
            nameBn = "অন্যান্য টেকনিক্যাল সার্ভিস",
            descriptionEn = "Voltage stabilizer, microwave, geyser, intercom, doorbell & custom appliance repair.",
            descriptionBn = "ভোল্টেজ স্ট্যাবিলাইজার, গিজার, ইন্টারকম, কলিং বেল ও অন্যান্য যন্ত্রপাতি মেরামত।",
            iconKey = "other",
            sortOrder = 10L,
            isActive = true
        )
    )
}
