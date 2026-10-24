package com.example.data.catalog

import com.example.data.local.entity.SubjectEntity
import java.util.UUID

object GhanaShsCatalog {
    val defaultCatalog = listOf(
        // Core Subjects
        SubjectEntity(
            id = "core_math",
            name = "Core Mathematics",
            category = "CORE",
            colorHex = "#1E6F5C"
        ),
        SubjectEntity(
            id = "core_english",
            name = "English Language",
            category = "CORE",
            colorHex = "#0F1E36"
        ),
        SubjectEntity(
            id = "core_int_science",
            name = "Integrated Science",
            category = "CORE",
            colorHex = "#0284C7"
        ),
        SubjectEntity(
            id = "core_social_studies",
            name = "Social Studies",
            category = "CORE",
            colorHex = "#D97706"
        ),

        // Elective Science & Mathematics
        SubjectEntity(
            id = "elec_physics",
            name = "Physics",
            category = "ELECTIVE",
            colorHex = "#4338CA"
        ),
        SubjectEntity(
            id = "elec_chemistry",
            name = "Chemistry",
            category = "ELECTIVE",
            colorHex = "#7C3AED"
        ),
        SubjectEntity(
            id = "elec_biology",
            name = "Biology",
            category = "ELECTIVE",
            colorHex = "#059669"
        ),
        SubjectEntity(
            id = "elec_math",
            name = "Elective Mathematics",
            category = "ELECTIVE",
            colorHex = "#0D9488"
        ),

        // Elective General Arts & Humanities
        SubjectEntity(
            id = "elec_economics",
            name = "Economics",
            category = "ELECTIVE",
            colorHex = "#B45309"
        ),
        SubjectEntity(
            id = "elec_geography",
            name = "Geography",
            category = "ELECTIVE",
            colorHex = "#4D7C0F"
        ),
        SubjectEntity(
            id = "elec_government",
            name = "Government",
            category = "ELECTIVE",
            colorHex = "#9333EA"
        ),
        SubjectEntity(
            id = "elec_history",
            name = "History",
            category = "ELECTIVE",
            colorHex = "#C2410C"
        ),
        SubjectEntity(
            id = "elec_literature",
            name = "Literature-in-English",
            category = "ELECTIVE",
            colorHex = "#BE185D"
        ),
        SubjectEntity(
            id = "elec_crs",
            name = "Christian Religious Studies (CRS)",
            category = "ELECTIVE",
            colorHex = "#2563EB"
        ),
        SubjectEntity(
            id = "elec_irs",
            name = "Islamic Religious Studies (IRS)",
            category = "ELECTIVE",
            colorHex = "#0F766E"
        ),
        SubjectEntity(
            id = "elec_french",
            name = "French",
            category = "ELECTIVE",
            colorHex = "#4F46E5"
        ),
        SubjectEntity(
            id = "elec_gh_language",
            name = "Ghanaian Language (Twi/Fante/Ewe/Ga)",
            category = "ELECTIVE",
            colorHex = "#047857"
        ),

        // Elective Business
        SubjectEntity(
            id = "elec_fin_accounting",
            name = "Financial Accounting",
            category = "ELECTIVE",
            colorHex = "#15803D"
        ),
        SubjectEntity(
            id = "elec_cost_accounting",
            name = "Cost Accounting",
            category = "ELECTIVE",
            colorHex = "#166534"
        ),
        SubjectEntity(
            id = "elec_bus_mgmt",
            name = "Business Management",
            category = "ELECTIVE",
            colorHex = "#334155"
        ),

        // Elective Visual Arts & Home Economics & Tech
        SubjectEntity(
            id = "elec_visual_arts",
            name = "General Knowledge in Art",
            category = "ELECTIVE",
            colorHex = "#E11D48"
        ),
        SubjectEntity(
            id = "elec_food_nutr",
            name = "Food and Nutrition",
            category = "ELECTIVE",
            colorHex = "#DB2777"
        ),
        SubjectEntity(
            id = "elec_tech_drawing",
            name = "Technical Drawing",
            category = "ELECTIVE",
            colorHex = "#0369A1"
        ),
        SubjectEntity(
            id = "elec_ict",
            name = "Elective ICT",
            category = "ELECTIVE",
            colorHex = "#0891B2"
        )
    )
}
