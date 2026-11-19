package com.yogeshpaliyal.gapwise.data

/** Starter events for the welcome tour. [typicalDays] seeds an approximate "last time". */
data class Suggestion(
    val name: String,
    val emoji: String,
    val category: String,
    val typicalDays: Int,
    val multiDay: Boolean = false,
)

object Categories {
    const val HOME = "Home"
    const val HEALTH = "Health"
    const val SELF_CARE = "Self-care"
    const val PEOPLE = "People"
    const val VEHICLE = "Vehicle"
    const val MONEY = "Money"
    const val OTHER = "Other"

    val defaults = listOf(HOME, HEALTH, SELF_CARE, PEOPLE, VEHICLE, MONEY, OTHER)
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
    val all: List<Suggestion> = listOf(
        // Home
        Suggestion("Water plants", "🪴", Categories.HOME, 4),
        Suggestion("Fresh bedsheets", "🛏️", Categories.HOME, 10),
        Suggestion("Deep clean kitchen", "🧽", Categories.HOME, 30),
        Suggestion("Clean bathroom", "🚿", Categories.HOME, 7),
        Suggestion("AC filter clean", "❄️", Categories.HOME, 45),
        Suggestion("RO / water filter change", "💧", Categories.HOME, 180),
        Suggestion("Clean fridge", "🧊", Categories.HOME, 30),
        Suggestion("Gas cylinder booked", "🔥", Categories.HOME, 40),
        Suggestion("Pest control", "🐜", Categories.HOME, 120),
        Suggestion("Change towels", "🧺", Categories.HOME, 5),
        Suggestion("Groceries restock", "🛒", Categories.HOME, 7),
        Suggestion("Take out recycling", "♻️", Categories.HOME, 7),
        // Health
        Suggestion("Dentist visit", "🦷", Categories.HEALTH, 180),
        Suggestion("Eye check-up", "👓", Categories.HEALTH, 365),
        Suggestion("Health check-up", "🩺", Categories.HEALTH, 365),
        Suggestion("Prescription refill", "💊", Categories.HEALTH, 30),
        Suggestion("Replace toothbrush", "🪥", Categories.HEALTH, 90),
        Suggestion("Sick day", "🤒", Categories.HEALTH, 90, multiDay = true),
        Suggestion("Period", "🩸", Categories.HEALTH, 28, multiDay = true),
        Suggestion("Blood donation", "🩸", Categories.HEALTH, 120),
        // Self-care
        Suggestion("Haircut", "💇", Categories.SELF_CARE, 35),
        Suggestion("Beard trim", "🧔", Categories.SELF_CARE, 10),
        Suggestion("Nails", "💅", Categories.SELF_CARE, 10),
        Suggestion("Massage", "💆", Categories.SELF_CARE, 60),
        Suggestion("Long run", "🏃", Categories.SELF_CARE, 7),
        Suggestion("Digital detox day", "📵", Categories.SELF_CARE, 30),
        Suggestion("Vacation", "🏖️", Categories.SELF_CARE, 120, multiDay = true),
        // People
        Suggestion("Call Mom", "📞", Categories.PEOPLE, 3),
        Suggestion("Call Dad", "📞", Categories.PEOPLE, 4),
        Suggestion("Visit hometown", "🏡", Categories.PEOPLE, 30, multiDay = true),
        Suggestion("Date night", "🕯️", Categories.PEOPLE, 14),
        Suggestion("Meet friends", "🍻", Categories.PEOPLE, 14),
        Suggestion("Call grandparents", "👵", Categories.PEOPLE, 10),
        // Vehicle
        Suggestion("Bike service", "🏍️", Categories.VEHICLE, 90),
        Suggestion("Car service", "🚗", Categories.VEHICLE, 180),
        Suggestion("Oil change", "🛢️", Categories.VEHICLE, 120),
        Suggestion("Tyre pressure check", "🛞", Categories.VEHICLE, 14),
        Suggestion("Car wash", "🧼", Categories.VEHICLE, 14),
        Suggestion("Fuel refill", "⛽", Categories.VEHICLE, 7),
        // Money
        Suggestion("Review budget", "📒", Categories.MONEY, 30),
        Suggestion("Check credit card statement", "💳", Categories.MONEY, 30),
        Suggestion("Rebalance investments", "📈", Categories.MONEY, 180),
        Suggestion("Backup phone photos", "🗂️", Categories.OTHER, 30),
        Suggestion("Change passwords", "🔐", Categories.OTHER, 180),
    )
}
