package com.example.airquality

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import java.lang.Exception

class LocationProvider(val context: Context) {
    private var location: Location? = null
    private var locationManager: LocationManager? = null

    init {
        getLocation()
    }

    private fun getLocation(): Location?{
        try{
            locationManager = context.getSystemService(
                Context.LOCATION_SERVICE) as LocationManager

            var gpsLocation: Location? = null
            var netWorkLocation: Location? = null

            val isGPSEnabled: Boolean = locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetWorkEnabled: Boolean = locationManager!!.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if(!isGPSEnabled && !isNetWorkEnabled){ //GPS, Network모두 작동하지 않으면(false) null
                return null
            }
            else{
                val hasFineLocationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                val hasCoarseLocationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)

                if(hasFineLocationPermission != PackageManager.PERMISSION_GRANTED || hasCoarseLocationPermission != PackageManager.PERMISSION_GRANTED){
                    return null
                } //GPS, Network 둘다 권한이 없다면 null

                if(isNetWorkEnabled){
                    netWorkLocation = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                }
                if(isGPSEnabled){
                    gpsLocation = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                } //허가를 받았다면 GPS, Network 위치정보를 받아옴

                if(gpsLocation != null && netWorkLocation != null){ //둘중에 정확도가 높은것을 반환
                    if(gpsLocation.accuracy > netWorkLocation.accuracy){
                        location = gpsLocation
                    }
                    else{
                        location = netWorkLocation
                    }
                    return location
                }
                else{
                    if(gpsLocation != null){
                        location = gpsLocation
                    }
                    if(netWorkLocation != null){
                        location = netWorkLocation
                    }
                }
            }
        }catch (e: Exception){
            e.printStackTrace()
        }
        return location
    }

    fun getLocationLatitude(): Double{
        return location?.latitude?:0.0 //null이면 0.0 반환
    }
    fun getLocationLongitude(): Double{
        return location?.longitude?:0.0 //null이면 0.0 반환
    }
}