package com.yogeshpaliyal.gapwise.data

import com.yogeshpaliyal.gapwise.L10n
import com.yogeshpaliyal.gapwise.R

/** Starter events for the welcome tour. [typicalDays] seeds an approximate "last time". */
data class Suggestion(
    val name: String,
    val emoji: String,
    val category: String,
    val typicalDays: Int,
    val multiDay: Boolean = false,
    /** Counted from one date (a wedding, a birthday) instead of repeated. */
    val milestone: Boolean = false,
) {
    /** Where the welcome tour lists it. */
    val group: String get() = if (milestone) L10n.s(R.string.filter_milestones) else category
}

object Categories {
    val HOME: String get() = L10n.s(R.string.cat_home)
    val HEALTH: String get() = L10n.s(R.string.cat_health)
    val SELF_CARE: String get() = L10n.s(R.string.cat_self_care)
    val PEOPLE: String get() = L10n.s(R.string.cat_people)
    val VEHICLE: String get() = L10n.s(R.string.cat_vehicle)
    val MONEY: String get() = L10n.s(R.string.cat_money)
    val OTHER: String get() = L10n.s(R.string.cat_other)

    val defaults: List<String> get() = listOf(HOME, HEALTH, SELF_CARE, PEOPLE, VEHICLE, MONEY, OTHER)
}

object Palette {
    val colors: List<Long> = listOf(
        0xFF8E3B8E, 0xFF3B82F6, 0xFF8B5CF6, 0xFFEC4899,
        0xFFEF4444, 0xFFF97316, 0xFFEAB308, 0xFF22C55E,
        0xFF14B8A6, 0xFF0EA5E9, 0xFF64748B, 0xFFA16207,
    )

    fun forIndex(i: Int): Long = colors[Math.floorMod(i, colors.size)]
}

object Suggestions {
    /** Built on each read, so names follow the current language. */
    val all: List<Suggestion> get() = listOf(
        // Home
        Suggestion(L10n.s(R.string.sug_water_plants), "🪴", Categories.HOME, 4),
        Suggestion(L10n.s(R.string.sug_fresh_bedsheets), "🛏️", Categories.HOME, 10),
        Suggestion(L10n.s(R.string.sug_deep_clean_kitchen), "🧽", Categories.HOME, 30),
        Suggestion(L10n.s(R.string.sug_clean_bathroom), "🚿", Categories.HOME, 7),
        Suggestion(L10n.s(R.string.sug_ac_filter_clean), "❄️", Categories.HOME, 45),
        Suggestion(L10n.s(R.string.sug_ro_water_filter_change), "💧", Categories.HOME, 180),
        Suggestion(L10n.s(R.string.sug_clean_fridge), "🧊", Categories.HOME, 30),
        Suggestion(L10n.s(R.string.sug_gas_cylinder_booked), "🔥", Categories.HOME, 40),
        Suggestion(L10n.s(R.string.sug_pest_control), "🐜", Categories.HOME, 120),
        Suggestion(L10n.s(R.string.sug_change_towels), "🧺", Categories.HOME, 5),
        Suggestion(L10n.s(R.string.sug_groceries_restock), "🛒", Categories.HOME, 7),
        Suggestion(L10n.s(R.string.sug_take_out_recycling), "♻️", Categories.HOME, 7),
        // Health
        Suggestion(L10n.s(R.string.sug_dentist_visit), "🦷", Categories.HEALTH, 180),
        Suggestion(L10n.s(R.string.sug_eye_check_up), "👓", Categories.HEALTH, 365),
        Suggestion(L10n.s(R.string.sug_health_check_up), "🩺", Categories.HEALTH, 365),
        Suggestion(L10n.s(R.string.sug_prescription_refill), "💊", Categories.HEALTH, 30),
        Suggestion(L10n.s(R.string.sug_replace_toothbrush), "🪥", Categories.HEALTH, 90),
        Suggestion(L10n.s(R.string.sug_sick_day), "🤒", Categories.HEALTH, 90, multiDay = true),
        Suggestion(L10n.s(R.string.sug_period), "🩸", Categories.HEALTH, 28, multiDay = true),
        Suggestion(L10n.s(R.string.sug_blood_donation), "🩸", Categories.HEALTH, 120),
        // Self-care
        Suggestion(L10n.s(R.string.sug_haircut), "💇", Categories.SELF_CARE, 35),
        Suggestion(L10n.s(R.string.sug_beard_trim), "🧔", Categories.SELF_CARE, 10),
        Suggestion(L10n.s(R.string.sug_nails), "💅", Categories.SELF_CARE, 10),
        Suggestion(L10n.s(R.string.sug_massage), "💆", Categories.SELF_CARE, 60),
        Suggestion(L10n.s(R.string.sug_long_run), "🏃", Categories.SELF_CARE, 7),
        Suggestion(L10n.s(R.string.sug_digital_detox_day), "📵", Categories.SELF_CARE, 30),
        Suggestion(L10n.s(R.string.sug_vacation), "🏖️", Categories.SELF_CARE, 120, multiDay = true),
        // People
        Suggestion(L10n.s(R.string.sug_call_mom), "📞", Categories.PEOPLE, 3),
        Suggestion(L10n.s(R.string.sug_call_dad), "📞", Categories.PEOPLE, 4),
        Suggestion(L10n.s(R.string.sug_visit_hometown), "🏡", Categories.PEOPLE, 30, multiDay = true),
        Suggestion(L10n.s(R.string.sug_date_night), "🕯️", Categories.PEOPLE, 14),
        Suggestion(L10n.s(R.string.sug_meet_friends), "🍻", Categories.PEOPLE, 14),
        Suggestion(L10n.s(R.string.sug_call_grandparents), "👵", Categories.PEOPLE, 10),
        // Vehicle
        Suggestion(L10n.s(R.string.sug_bike_service), "🏍️", Categories.VEHICLE, 90),
        Suggestion(L10n.s(R.string.sug_car_service), "🚗", Categories.VEHICLE, 180),
        Suggestion(L10n.s(R.string.sug_oil_change), "🛢️", Categories.VEHICLE, 120),
        Suggestion(L10n.s(R.string.sug_tyre_pressure_check), "🛞", Categories.VEHICLE, 14),
        Suggestion(L10n.s(R.string.sug_car_wash), "🧼", Categories.VEHICLE, 14),
        Suggestion(L10n.s(R.string.sug_fuel_refill), "⛽", Categories.VEHICLE, 7),
        // Money
        Suggestion(L10n.s(R.string.sug_review_budget), "📒", Categories.MONEY, 30),
        Suggestion(L10n.s(R.string.sug_check_credit_card_statement), "💳", Categories.MONEY, 30),
        Suggestion(L10n.s(R.string.sug_rebalance_investments), "📈", Categories.MONEY, 180),
        Suggestion(L10n.s(R.string.sug_backup_phone_photos), "🗂️", Categories.OTHER, 30),
        Suggestion(L10n.s(R.string.sug_change_passwords), "🔐", Categories.OTHER, 180),
        // Milestones
        Suggestion(L10n.s(R.string.sug_ms_wedding), "💍", Categories.PEOPLE, 0, milestone = true),
        Suggestion(L10n.s(R.string.sug_ms_birthday), "🎂", Categories.PEOPLE, 0, milestone = true),
        Suggestion(L10n.s(R.string.sug_ms_together), "❤️", Categories.PEOPLE, 0, milestone = true),
        Suggestion(L10n.s(R.string.sug_ms_new_job), "💼", Categories.OTHER, 0, milestone = true),
        Suggestion(L10n.s(R.string.sug_ms_moved_in), "🏠", Categories.HOME, 0, milestone = true),
        Suggestion(L10n.s(R.string.sug_ms_quit_smoking), "🚭", Categories.HEALTH, 0, milestone = true),
        Suggestion(L10n.s(R.string.sug_ms_pet), "🐾", Categories.HOME, 0, milestone = true),
    )
}

/**
 * A starter event the user picked, with when they last did it. [lastAt] null means "not sure".
 * For events with a start and an end, [endAt] null means it's still going.
 */
data class PickedSuggestion(val suggestion: Suggestion, val lastAt: Long?, val endAt: Long?)
