package com.example.inventoryapp.model

import android.os.Parcel
import android.os.Parcelable

data class InventoryItem(
    val id: Long,
    val barcode: String,
    val name: String,
    val quantity: Int,
    val location: String,
    val notes: String,
    val photoPath: String,
    val lastUpdated: String
) : Parcelable {

    constructor(parcel: Parcel) : this(
        id = parcel.readLong(),
        barcode = parcel.readString() ?: "",
        name = parcel.readString() ?: "",
        quantity = parcel.readInt(),
        location = parcel.readString() ?: "",
        notes = parcel.readString() ?: "",
        photoPath = parcel.readString() ?: "",
        lastUpdated = parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(barcode)
        parcel.writeString(name)
        parcel.writeInt(quantity)
        parcel.writeString(location)
        parcel.writeString(notes)
        parcel.writeString(photoPath)
        parcel.writeString(lastUpdated)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<InventoryItem> {
        override fun createFromParcel(parcel: Parcel): InventoryItem = InventoryItem(parcel)
        override fun newArray(size: Int): Array<InventoryItem?> = arrayOfNulls(size)
    }

    fun isLowStock(threshold: Int = 5): Boolean = quantity <= threshold
}