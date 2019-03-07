package memphis.myapplication.location

import android.annotation.SuppressLint
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.support.v7.app.AppCompatActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.io.IOException
import java.util.*

class LocationUtils(context: Context){

    private var fusedLocationProviderClient: FusedLocationProviderClient ?= null
    private var location : MutableLiveData<Location> = MutableLiveData()

    // call constructor to get location
    init {
        getInstance(context)
        getLocation()
    }

    // using singleton pattern to get the locationProviderClient
    fun getInstance(appContext: Context): FusedLocationProviderClient{
        if(fusedLocationProviderClient == null)
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(appContext)
        return fusedLocationProviderClient!!
    }


    // whenever location is updated, update the Location in LiveData class
    @SuppressLint("MissingPermission")
    fun getLocation() : LiveData<Location> {
        fusedLocationProviderClient!!.lastLocation
            .addOnSuccessListener {loc: Location? ->
                location.value = loc
            }

        return location
    }


    // static object to get Address of a particular location usin Geocode
    companion object {
        fun getAddress(activity: AppCompatActivity, lat: Double, lng: Double): String? {

            //Log.d(TAG, "get Address for LAT: $lat  LON: $lng")
            if (lat == 0.0 && lng == 0.0)
                return null
            val geocoder = Geocoder(activity, Locale.getDefault())
            try {
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                val obj = addresses[0]

                // remove null from the address and replace with space
                var add = if (obj.thoroughfare == null) "" else obj.thoroughfare + ", "

                add += if (obj.subLocality == null) "" else obj.subLocality + ", "
                add += if (obj.subAdminArea == null) "" else obj.subAdminArea


                //Log.v(TAG, "Address received: $add")

                return add
            } catch (e: IOException) {

                e.printStackTrace()
            }

            return ""
        }
    }

}