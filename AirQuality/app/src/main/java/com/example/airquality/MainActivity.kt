package com.example.airquality

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.airquality.databinding.ActivityMainBinding
import com.example.airquality.retrofit.AirQualityResponse
import com.example.airquality.retrofit.AirQualityService
import com.example.airquality.retrofit.RetrofitConnection
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.lang.IllegalArgumentException
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/*GPS권환 허용방법
* 1. checkAllPermissions() -> 휴대폰에서 GPS가 켜져 있는지 확인 켜져있지 않다면 2번으로, 켜져있다면 4번으로
* 2. showDialogForLocationServiceSetting() -> ActivityResultLauncher를 사용해서 인텐트를 실행한 후에
*                                          -> 결과값을 받아 GPS가 활성화 되었다면 isRunTimePermissionsGranted() 실행(3번) 활성화 되있지 않다면
*                                          -> 다이얼로그를 켜서 GPS활성화 창을 연다음 허용하면 GPS활성화 취소시 앱종료
*
* 3. isRunTimePermissionsGranted() -> FineLocation, CoarseLocation 중 Coarse 만 허용할지 아니면 둘다 허용할지 요청하는 함수
* 4. onRequestPermissionsResult() -> 모든 위치에 관한 권한이 허용되었다면 실행할 함수를 입력하는 함수*/

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var locationProvider: LocationProvider
    private val PERMISSIONS_REQUEST_CODE = 100
    var REQUIRED_PERMISSIONS = arrayOf(
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION
    )

    var latitude: Double = 0.0
    var longitude: Double = 0.0

    var startMapActivityResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result?.resultCode ?: 0 == Activity.RESULT_OK) { //엘비스 연산자 result의 resultcode가 null이라면 0을 resultCode에 넣고 이것은 Activity의 ResultOK와 같음
            latitude = result?.data?.getDoubleExtra("latitude", 0.0) ?: 0.0
            longitude = result?.data?.getDoubleExtra("longitude", 0.0) ?: 0.0
            updateUI()
        }
    }

    lateinit var getGPSPermissionLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkAllPermissions()
        updateUI()
        setRefreshButton()

        setFab()
    }

    private fun setFab(){
        binding.fab.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            intent.putExtra("currentLat", latitude)
            intent.putExtra("currentLng", longitude)
            startMapActivityResult.launch(intent)
        }
    }

    private fun setRefreshButton(){
        binding.btnRefresh.setOnClickListener {
            updateUI()
        }
    }

    private fun updateUI(){
        locationProvider = LocationProvider(this@MainActivity)

        if(latitude == 0.0 || longitude == 0.0){
            latitude = locationProvider.getLocationLatitude()
            longitude = locationProvider.getLocationLongitude()
        }


        if(latitude != 0.0 && longitude != 0.0){
            //1. 현재위치를 가져오고 ui업데이트
            val address = getcurrentAddress(latitude, longitude)
            address?.let{
                if(it.thoroughfare == null){
                    binding.tvLocationTitle.text = "${it.subLocality}"
                }
                else{
                    binding.tvLocationTitle.text = "${it.thoroughfare}" //역삼 1동
                }
                binding.tvLocationSubtitle.text = "${it.countryName} ${it.adminArea}" //대한민국 서울특별시
            }

            //2. 현재 미세먼지 농도 가져오고 ui업데이트
            getAirQualityData(latitude, longitude)
        }
        else{
            Toast.makeText(this@MainActivity,"위도, 경도정보를 가져올 수 없습니다. 새로고침을 눌러주세요", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getAirQualityData(latitude: Double, longitude: Double){
        val retrofitAPI = RetrofitConnection.getInstance().create(AirQualityService::class.java)

        retrofitAPI.getAirQualityData(
            latitude.toString(),
            longitude.toString(),
            "40ec59db-e876-4513-94ba-d4d143eb6ec1"
        ).enqueue(object : Callback<AirQualityResponse>{
            override fun onResponse(
                call: Call<AirQualityResponse>,
                response: Response<AirQualityResponse>
            ) {
                if(response.isSuccessful){
                    Toast.makeText(this@MainActivity, "최신정보 업데이트 완료", Toast.LENGTH_SHORT).show()
                    response.body()?.let { updateAirUI(it) }
                }
                else{
                    Toast.makeText(this@MainActivity, "업데이트 실패", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<AirQualityResponse>, t: Throwable) {
                t.printStackTrace()
                Toast.makeText(this@MainActivity, "업데이트에 실패했습니다", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateAirUI(airQualityData: AirQualityResponse){
        val pollutionData = airQualityData.data.current.pollution

        binding.tvCount.text = pollutionData.aqius.toString()

        val dateTime = ZonedDateTime.parse(pollutionData.ts).withZoneSameInstant(
            ZoneId.of("Asia/Seoul")).toLocalDateTime()

        val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-mm-dd HH:mm")

        binding.tvCheckTime.text = dateTime.format(dateFormatter).toString()

        when(pollutionData.aqius){
            in 0..50 -> {
                binding.tvTitle.text = "좋음"
                binding.imgBg.setImageResource(R.drawable.bg_good)
            }

            in 51..150 -> {
                binding.tvTitle.text = "보통"
                binding.imgBg.setImageResource(R.drawable.bg_soso)
            }

            in 151..200 -> {
                binding.tvTitle.text = "나쁨"
                binding.imgBg.setImageResource(R.drawable.bg_bad)
            }

            else -> {
                binding.tvTitle.text = "매우 나쁨"
                binding.imgBg.setImageResource(R.drawable.bg_worst)
            }
        }
    }

    override fun onRestoreInstanceState(
        savedInstanceState: Bundle?,
        persistentState: PersistableBundle?
    ) {
        super.onRestoreInstanceState(savedInstanceState, persistentState)
    }

    private fun getcurrentAddress(latitude: Double, longitude: Double): Address? {
        val geocoder = Geocoder(this, Locale.getDefault())
        val addresses: List<Address>?

        addresses = try{
            geocoder.getFromLocation(latitude, longitude, 5)
        }catch (ioException: IOException){
            Toast.makeText(this, "지오코더 서비스 사용불가 합니다",Toast.LENGTH_SHORT).show()
            return null
        }catch (illegalArgumentException: IllegalArgumentException){
            Toast.makeText(this, "잘못된 위도, 경도 입니다", Toast.LENGTH_SHORT).show()
            return null
        }

        if(addresses == null || addresses.isEmpty()){
            Toast.makeText(this, "주소가 발견되지 않았습니다", Toast.LENGTH_SHORT).show()
            return null
        }

        val address: Address = addresses[0]
        return address
    }

    private fun checkAllPermissions(){
        //GPS가 켜져있는지 확인
        if(!isLocationServiceAvailable()){
            showDialogForLocationServiceSetting();
        }
        else{ //런타임 앱 권한이 모두 허용되어 있는지 확인
            isRunTimePermissionsGranted();
        }
    }
    //위치 서비스가 켜져있는지 확인하는 함수
    /*GPS나 Network둘중 하나가 켜져있다면 true를 반환*/
    fun isLocationServiceAvailable(): Boolean{
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        return (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
    }


    fun isRunTimePermissionsGranted(){
        val hasFineLocationPermission = ContextCompat.checkSelfPermission(
            this@MainActivity,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        val hasCoarseLocationPermission = ContextCompat.checkSelfPermission(
            this@MainActivity,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if(hasFineLocationPermission != PackageManager.PERMISSION_GRANTED || hasCoarseLocationPermission != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this@MainActivity, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == PERMISSIONS_REQUEST_CODE && grantResults.size == REQUIRED_PERMISSIONS.size){
            var checkResult = true

            for(result in grantResults){ //퍼미션중 한가지라도 허용되지 않는다면 false로 앱종료됨
                if(result != PackageManager.PERMISSION_GRANTED){
                    checkResult = false
                    break
                }
            }
            if(checkResult){
                //위치값을 가져올 수 있음
                updateUI()
            }
            else{
                //퍼미션이 거부되었다면 앱 종료
                Toast.makeText(this@MainActivity, "퍼미션이 거부되었습니다. 앱을 다시 실행하여 퍼미션을 허용해 주세요", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun showDialogForLocationServiceSetting(){
        //이 런처를 이용해서 결과값을 반환해야 하는 인텐트를 실행할 수 있음
        getGPSPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) { result ->
            //결과값을 받았을 경우
            if(result.resultCode == Activity.RESULT_OK){
                //사용자가 GPS를 활성화 했는지 확인
                if(isLocationServiceAvailable()){
                    isRunTimePermissionsGranted() //런타임 권한 확인(AirQuality가 내 기기위치에 액세스하도록 허용하시겠습니까 같은것)
                }
                else{
                    //GPS활성화 하지 않았다면 종료
                    Toast.makeText(this@MainActivity, "위치서비스를 사용할 수 없습니다", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }

        val builder: AlertDialog.Builder = AlertDialog.Builder(this@MainActivity)
        builder.setTitle("위치 서비스 비활성화")
        builder.setMessage("위치 서비스가 꺼져 있습니다. 설정해야 앱을 사용할 수 있습니다.")
        builder.setCancelable(true) //다이얼로그 창 바깥 터치 시 창 닫힘
        builder.setPositiveButton("설정", DialogInterface.OnClickListener{
            dialog, id ->
            val callGPSSettingIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            getGPSPermissionLauncher.launch(callGPSSettingIntent)
        })

        builder.setNegativeButton("취소", DialogInterface.OnClickListener{
            dialog, id ->
            dialog.cancel()
            Toast.makeText(this@MainActivity, "기기에서 위치서비스(GPS) 설정 후 사용해 주세요", Toast.LENGTH_SHORT).show()
            finish()
        })
        builder.create().show()
    }
}