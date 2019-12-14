package com.example.milestone4

import android.R.string
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttMessage
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.round
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.app.Notification
import android.view.View
import kotlin.math.roundToInt


class MainActivity : AppCompatActivity() {
    private var notificationManager: NotificationManager? = null

    // I'm using lateinit for these widgets because I read that repeated calls to findViewById
    // are energy intensive
    lateinit var textView: TextView
    lateinit var weather: TextView
    lateinit var maxweather: TextView
    lateinit var minweather: TextView
    lateinit var forecastmaxweather: TextView
    lateinit var forecastminweather: TextView
    lateinit var forecastprecipitation: TextView
    lateinit var precipitation: TextView

    lateinit var retrieveButton: Button
    lateinit var wifiButton: Button
    lateinit var imageView: ImageView
    lateinit var syncButton: Button
    lateinit var goalButton: Button



    lateinit var queue: RequestQueue
    lateinit var gson: Gson
    lateinit var mostRecentWeatherResult: WeatherResult
    lateinit var WeatherforecastResult: WeatherForecast
    lateinit var stepsGoalResult: StepsGoal

    var actualSteps: Int = 0
    var hourlySteps: Int = 0

    // I'm doing a late init here because I need this to be an instance variable but I don't
    // have all the info I need to initialize it yet
    lateinit var mqttAndroidClient: MqttAndroidClient

    // you may need to change this depending on where your MQTT broker is running
    val serverUri = "tcp://192.168.4.1"
    // you can use whatever name you want to here
    val clientId = "Milestone 4"

    //these should "match" the topics on the "other side" (i.e, on the Raspberry Pi)
    val subscribeTopic = "steps"
    val publishTopic = "weather"
//    var weatherDescription = "Surface Pro"
//    var weatherDescription =  ArrayList<Long>()
    var weatherDescription = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        notificationManager =
                getSystemService(
                        Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(
                "com.ebookfrenzy.notifydemo.news",
                "NotifyDemo News",
                "Example News Channel")

        textView = this.findViewById(R.id.text)
        weather = this.findViewById(R.id.weather)
        maxweather = this.findViewById(R.id.maxweather)
        forecastmaxweather = this.findViewById(R.id.forecastmaxweather)
        forecastminweather = this.findViewById(R.id.forecastminweather)
        forecastprecipitation = this.findViewById(R.id.forecastprecipitation)
        minweather = this.findViewById(R.id.minweather)
        precipitation=this.findViewById(R.id.precipitation)
        syncButton = this.findViewById(R.id.syncButton)
        goalButton = this.findViewById(R.id.goalButton)
        retrieveButton = this.findViewById(R.id.retrieveButton)
        wifiButton = this.findViewById(R.id.wifiButton)
        imageView = this.findViewById(R.id.imageView)


        goalButton.setOnClickListener ({checkgoal()})
        // when the user presses the syncbutton, this method will get called
        syncButton.setOnClickListener({ syncWithPi() })

        publishButton.setOnClickListener { publish() }

        // when the user presses the syncbutton, this method will get called
        retrieveButton.setOnClickListener({ requestWeather() })

        wifiButton.setOnClickListener { switchwifi() }

        queue = Volley.newRequestQueue(this)
        gson = Gson()

        // initialize the paho mqtt client with the uri and client id
        mqttAndroidClient = MqttAndroidClient(getApplicationContext(), serverUri, clientId);

        // when things happen in the mqtt client, these callbacks will be called
        mqttAndroidClient.setCallback(object: MqttCallbackExtended {

            // when the client is successfully connected to the broker, this method gets called
            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                println("Connection Complete!!")
                // this subscribes the client to the subscribe topic
                mqttAndroidClient.subscribe(subscribeTopic, 0)
                val message = MqttMessage()
//                message.payload = ("Hello World").toByteArray()
                message.payload = weatherDescription.toByteArray()

                // this publishes a message to the publish topic
                mqttAndroidClient.publish(publishTopic, message)
            }

            // this method is called when a message is received that fulfills a subscription
            override fun messageArrived(topic: String?, message: MqttMessage?) {
                stepsGoalResult = gson.fromJson(message.toString(), StepsGoal::class.java)
                println(message)
                println(stepsGoalResult.actualSteps)
                println(stepsGoalResult.todayGoal)


                var calendar = Calendar.getInstance() // get current date
                calendar.add(Calendar.DATE, 1)
                val now: Date = calendar.time
                val sdf = SimpleDateFormat("HH:mm:ss")
                val nowtime = sdf.format(now)



                textView.text = message.toString()
                actualSteps = stepsGoalResult.actualSteps
                hourlySteps = stepsGoalResult.todayGoal.roundToInt() / 12
                if (nowtime > "08:50:00" && nowtime < "08:51:00") {
                    if (actualSteps < hourlySteps) {
                        sendnotification(actualSteps, hourlySteps)
                    }
                } else if (nowtime > "09:50:00" && nowtime < "09:51:00") {
                    if (actualSteps < hourlySteps*2) {
                        sendnotification(actualSteps, hourlySteps*2)
                    }
                }
                else if (nowtime > "10:50:00" && nowtime < "10:51:00"){
                    if (actualSteps < hourlySteps*3) {
                        sendnotification(actualSteps, hourlySteps*3)
                    }
                }
                else if (nowtime > "11:50:00" && nowtime < "11:51:00"){
                    if (actualSteps < hourlySteps*4) {
                        sendnotification(actualSteps, hourlySteps*4)
                    }
                }
                else if (nowtime > "12:50:00" && nowtime < "12:51:00"){
                    if (actualSteps < hourlySteps*5) {
                        sendnotification(actualSteps, hourlySteps*5)
                    }
                }
                else if (nowtime > "13:50:00" && nowtime < "13:51:00"){
                    if (actualSteps < hourlySteps*6) {
                        sendnotification(actualSteps, hourlySteps*6)
                    }
                }
                else if (nowtime > "14:50:00" && nowtime < "14:51:00"){
                    if (actualSteps < hourlySteps*7) {
                        sendnotification(actualSteps, hourlySteps*7)
                    }
                }
                else if (nowtime > "15:50:00" && nowtime < "15:51:00"){
                    if (actualSteps < hourlySteps*8) {
                        sendnotification(actualSteps, hourlySteps*8)
                    }
                }
                else if (nowtime > "16:50:00" && nowtime < "16:51:00"){
                    if (actualSteps < hourlySteps*9) {
                        sendnotification(actualSteps, hourlySteps*9)
                    }
                }
                else if (nowtime > "17:50:00" && nowtime < "17:51:00"){
                    if (actualSteps < hourlySteps*10) {
                        sendnotification(actualSteps, hourlySteps*10)
                    }
                }
                else if (nowtime > "18:50:00" && nowtime < "18:51:00"){
                    if (actualSteps < hourlySteps*11) {
                        sendnotification(actualSteps, hourlySteps*11)
                    }
                }
                else if (nowtime > "19:50:00" && nowtime < "19:51:00"){
                    if (actualSteps < hourlySteps*12) {
                        sendnotification(actualSteps, hourlySteps*12)
                    }
                }
                else if (nowtime > "17:23:00" && nowtime < "17:33:00"){
                    if (actualSteps < 20) {
                        sendnotification(actualSteps, 20)
                    }
                }
                //12 if statesment
                // if time is 8:59:
                // send a notification
            }

            override fun connectionLost(cause: Throwable?) {
                println("Connection Lost")
            }

            // this method is called when the client succcessfully publishes to the broker
            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                println("Delivery Complete")
            }
        })

    }

    private fun createNotificationChannel(id: String, name: String,
                                          description: String) {

        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(id, name, importance)

        channel.description = description
        channel.enableLights(true)
        channel.lightColor = Color.RED
        channel.enableVibration(true)
        channel.vibrationPattern =
                longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
        notificationManager?.createNotificationChannel(channel)
    }

    fun sendnotification(actual:Int, hourly:Int ){
        val difference = hourly - actual
        val notificationID = 101
        val channelID = "com.ebookfrenzy.notifydemo.news"

        var calendar = Calendar.getInstance() // get current date
        calendar.add(Calendar.DATE, 1)
        val tomorrow: Date = calendar.time
        val sdf = SimpleDateFormat("hh:mm:ss")
        val tomorrowDate = sdf.format(tomorrow)

        val notification = Notification.Builder(this@MainActivity,
                channelID)
                .setContentTitle("Walk more dude")
                .setContentText("You have only walked "+ actual +" steps. You need to walk " + difference + " steps to reach your end of the hour steps goal of " + hourly + " steps.")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setChannelId(channelID)
                .build()

        notificationManager?.notify(notificationID, notification)
    }


    fun publish(){
        val message = MqttMessage()
//                message.payload = ("Hello World").toByteArray()
        message.payload = weatherDescription.toByteArray()
        mqttAndroidClient.publish(publishTopic, message)
    }

    // this method just connects the paho mqtt client to the broker
    fun syncWithPi(){
        println("+++++++ Connecting...")
        mqttAndroidClient.connect()



//        val notificationID = 101
//
//        val channelID = "com.ebookfrenzy.notifydemo.news"
//
//        var calendar = Calendar.getInstance() // get current date
//        calendar.add(Calendar.DATE, 1)
//        val tomorrow: Date = calendar.time
//        val sdf = SimpleDateFormat("hh:mm:ss")
//        val tomorrowDate = sdf.format(tomorrow)
//
//        val notification = Notification.Builder(this@MainActivity,
//                channelID)
//                .setContentTitle("Example Notification")
//                .setContentText("This is an example notification."+ tomorrowDate)
//                .setSmallIcon(android.R.drawable.ic_dialog_info)
//                .setChannelId(channelID)
//                .build()
//
//        notificationManager?.notify(notificationID, notification)
    }

    fun checkgoal(){
        val notificationID = 101

        val channelID = "com.ebookfrenzy.notifydemo.news"

        var calendar = Calendar.getInstance() // get current date
        calendar.add(Calendar.DATE, 1)
        val tomorrow: Date = calendar.time
        val sdf = SimpleDateFormat("HH:mm:ss")
        val tomorrowDate = sdf.format(tomorrow)

        val notification = Notification.Builder(this@MainActivity,
                channelID)
                .setContentTitle("Example Notification")
                .setContentText("This is an example notification."+ tomorrowDate)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setChannelId(channelID)
                .build()

        notificationManager?.notify(notificationID, notification)
    }

    fun switchwifi(){
        startActivity( Intent(Settings.ACTION_WIFI_SETTINGS));
    }

    fun requestWeather(){
//        var zipcode = zipcode.text
        val zip = zipcode.text.toString().trim()
        if (TextUtils.isEmpty(zip)) {
            zipcode.setError("Please Enter a valid Zip Code!")
        } else { //do something
            if (::mostRecentWeatherResult.isInitialized) {
                val url = StringBuilder("https://api.openweathermap.org/data/2.5/weather?zip=" + zip + ",us&appid=237a2971d5e68747c681fc1e13bfe6d3").toString()
                val stringRequest = object : StringRequest(com.android.volley.Request.Method.GET, url,
                        com.android.volley.Response.Listener<String> { response ->
                            //textView.text = response
                            mostRecentWeatherResult = gson.fromJson(response, WeatherResult::class.java)
                            var imageID = mostRecentWeatherResult.weather.get(0).icon
//                    textView.text = mostRecentWeatherResult.weather.get(0).description
                            weather.text = mostRecentWeatherResult.weather.get(0).description
                            maxweather.text = "The maximum tempature for today is ".plus(round(mostRecentWeatherResult.main.temp_max * 1.8 - 459.67).toString()).plus("℉")
                            minweather.text = "The minimum tempature for today is ".plus(round(mostRecentWeatherResult.main.temp_min * 1.8 - 459.67).toString()).plus("℉")
                            var raintoday: Double? = checkRain(mostRecentWeatherResult)
                            precipitation.text = "The precipitation for today is ".plus(raintoday.toString())


//                    weatherDescription = mostRecentWeatherResult.weather.get(0).description
                            Picasso.with(this)
                                    .load("https://openweathermap.org/img/wn/" + imageID + ".png")
                                    //.load("https://openweathermap.org/img/wn/03d.png")
                                    .error(R.drawable.error)
                                    .resize(400, 400)
                                    .into(imageView)
                        },
                        com.android.volley.Response.ErrorListener { println("******That didn't work!") }) {}
                queue.add(stringRequest)
//
                val url_tmr = StringBuilder("https://api.openweathermap.org/data/2.5/forecast?zip=" + zip + ",us&appid=237a2971d5e68747c681fc1e13bfe6d3").toString()
                val stringRequest_tmr = object : StringRequest(com.android.volley.Request.Method.GET, url_tmr,
                        com.android.volley.Response.Listener<String> { response ->
                            //textView.text = response
                            WeatherforecastResult = gson.fromJson(response, WeatherForecast::class.java)
                            // get tomorrow date
                            val tomorrowDate = getTomorrowDate()

                            // declare lists to hold multiple temperature/precipitation values
                            val temps = ArrayList<Long>()
                            val minTemps = ArrayList<Long>()
                            val maxTemps = ArrayList<Long>()
                            val rains = ArrayList<Double>()
                            var snows = ArrayList<Double>()

                            var day = 0


                            for (i in WeatherforecastResult.list) { // iterate through all the forecasts for next 5 days
                                // get the current day's weather details object from the list of forecast
                                val weatherDetails = WeatherforecastResult.list.get(day)
                                var date = weatherDetails.dt_txt.substring(0, 10)


                                if (date.equals(tomorrowDate.substring(0, 10))) { // only use data from tomorrow's forecasts

                                    println("Tomorrow: " + date)

                                    // append to metric to appropriate list:

                                    var temp = convertTemperature(weatherDetails.main.temp)
                                    var temp_min = convertTemperature(weatherDetails.main.temp_min)
                                    var temp_max = convertTemperature(weatherDetails.main.temp_max)

                                    println("max temp: " + temp_max)


                                    temps.add(temp)
                                    minTemps.add(temp_min)
                                    maxTemps.add(temp_max)

                                    var rainForecasted: Double = checkRainForecast(WeatherforecastResult, day)
//                            var snowForecasted: Double = checkSnowForecast(WeatherforecastResult, day)

                                    rains.add(rainForecasted)
//                            snows.add(snowForecasted)
                                }
                                day = day + 1
                            }

                            // compute averages of all lists
                            val averageTemp: Long = calculateAverage(temps)
                            val averageMinTemp: Long? = minTemps.min()
                            val averageMinTempLong: Long = averageMinTemp!!.toLong()
                            val averageMaxTemp: Long? = maxTemps.max()
                            val averageMaxTempLong: Long = averageMaxTemp!!.toLong()

                            val averageRain: Double = calculateAverageDouble(rains)


                            forecastmaxweather.text = "The maximum tempature for tomorrow is ".plus(averageMaxTemp).plus("℉")
                            forecastminweather.text = "The minimum tempature for tomorrow is ".plus(averageMinTemp).plus("℉")
                            forecastprecipitation.text = "The precipitation for tomorrow is ".plus(averageRain)


                            weatherDescription = "{\"tmrlow\":" + averageMinTemp + ", \"tmrhigh\": " + averageMaxTemp +
                                    ", \"tmrrain\":" + averageRain + ", \"low\": " + round(mostRecentWeatherResult.main.temp_min * 1.8 - 459.67) + ", \"high\": " + round(mostRecentWeatherResult.main.temp_max * 1.8 - 459.67) + ", \"rain\": " + checkRain(mostRecentWeatherResult) + "}"


//                    val averageSnow : Double = calculateAverageDouble(snows)

                            // Create json object of forecasted weather data to send to Raspberry Pi
//                    prepareForecastWeatherData(averageTemp, averageMinTempLong, averageMaxTempLong, averageRain, averageSnow)

                            // combine two sets of weather data
//                    weatherData = currentWeatherData + forecastWeatherData
//                    println("complete weather data: " + weatherData)

                        },
                        com.android.volley.Response.ErrorListener { println("******Not able to get forecast!") }) {}
                // Add the request to the RequestQueue.
                queue.add(stringRequest_tmr)
            }
            else{
                println("bugs")
                var weatherResult : WeatherResult? = null
                val url = StringBuilder("https://api.openweathermap.org/data/2.5/weather?zip=" + zip + ",us&appid=237a2971d5e68747c681fc1e13bfe6d3").toString()
                val stringRequest = object : StringRequest(com.android.volley.Request.Method.GET, url,
                        com.android.volley.Response.Listener<String> { response ->
                            //textView.text = response
                            mostRecentWeatherResult = gson.fromJson(response, WeatherResult::class.java)
                            var imageID = mostRecentWeatherResult.weather.get(0).icon
//                    textView.text = mostRecentWeatherResult.weather.get(0).description
                            weather.text = mostRecentWeatherResult.weather.get(0).description
                            maxweather.text = "The maximum tempature for today is ".plus(round(mostRecentWeatherResult.main.temp_max * 1.8 - 459.67).toString()).plus("℉")
                            minweather.text = "The minimum tempature for today is ".plus(round(mostRecentWeatherResult.main.temp_min * 1.8 - 459.67).toString()).plus("℉")
                            var raintoday: Double? = checkRain(mostRecentWeatherResult)
                            precipitation.text = "The precipitation for today is ".plus(raintoday.toString())


//                    weatherDescription = mostRecentWeatherResult.weather.get(0).description
                            Picasso.with(this)
                                    .load("https://openweathermap.org/img/wn/" + imageID + ".png")
                                    //.load("https://openweathermap.org/img/wn/03d.png")
                                    .error(R.drawable.error)
                                    .resize(400, 400)
                                    .into(imageView)
                        },
                        com.android.volley.Response.ErrorListener { println("******That didn't work!") }) {}
                queue.add(stringRequest)
//
                val url_tmr = StringBuilder("https://api.openweathermap.org/data/2.5/forecast?zip=" + zip + ",us&appid=237a2971d5e68747c681fc1e13bfe6d3").toString()
                val stringRequest_tmr = object : StringRequest(com.android.volley.Request.Method.GET, url_tmr,
                        com.android.volley.Response.Listener<String> { response ->
                            //textView.text = response
                            WeatherforecastResult = gson.fromJson(response, WeatherForecast::class.java)
                            // get tomorrow date
                            val tomorrowDate = getTomorrowDate()

                            // declare lists to hold multiple temperature/precipitation values
                            val temps = ArrayList<Long>()
                            val minTemps = ArrayList<Long>()
                            val maxTemps = ArrayList<Long>()
                            val rains = ArrayList<Double>()
                            var snows = ArrayList<Double>()

                            var day = 0


                            for (i in WeatherforecastResult.list) { // iterate through all the forecasts for next 5 days
                                // get the current day's weather details object from the list of forecast
                                val weatherDetails = WeatherforecastResult.list.get(day)
                                var date = weatherDetails.dt_txt.substring(0, 10)


                                if (date.equals(tomorrowDate.substring(0, 10))) { // only use data from tomorrow's forecasts

                                    println("Tomorrow: " + date)

                                    // append to metric to appropriate list:

                                    var temp = convertTemperature(weatherDetails.main.temp)
                                    var temp_min = convertTemperature(weatherDetails.main.temp_min)
                                    var temp_max = convertTemperature(weatherDetails.main.temp_max)

                                    println("max temp: " + temp_max)


                                    temps.add(temp)
                                    minTemps.add(temp_min)
                                    maxTemps.add(temp_max)

                                    var rainForecasted: Double = checkRainForecast(WeatherforecastResult, day)
//                            var snowForecasted: Double = checkSnowForecast(WeatherforecastResult, day)

                                    rains.add(rainForecasted)
//                            snows.add(snowForecasted)
                                }
                                day = day + 1
                            }

                            // compute averages of all lists
                            val averageTemp: Long = calculateAverage(temps)
                            val averageMinTemp: Long? = minTemps.min()
                            val averageMinTempLong: Long = averageMinTemp!!.toLong()
                            val averageMaxTemp: Long? = maxTemps.max()
                            val averageMaxTempLong: Long = averageMaxTemp!!.toLong()

                            val averageRain: Double = calculateAverageDouble(rains)


                            forecastmaxweather.text = "The maximum tempature for tomorrow is ".plus(averageMaxTemp).plus("℉")
                            forecastminweather.text = "The minimum tempature for tomorrow is ".plus(averageMinTemp).plus("℉")
                            forecastprecipitation.text = "The precipitation for tomorrow is ".plus(averageRain)


                            weatherDescription = "{\"tmrlow\":" + averageMinTemp + ", \"tmrhigh\": " + averageMaxTemp +
                                    ", \"tmrrain\":" + averageRain + ", \"low\": " + round(mostRecentWeatherResult.main.temp_min * 1.8 - 459.67) + ", \"high\": " + round(mostRecentWeatherResult.main.temp_max * 1.8 - 459.67) + ", \"rain\": " + checkRain(mostRecentWeatherResult) + "}"


//                    val averageSnow : Double = calculateAverageDouble(snows)

                            // Create json object of forecasted weather data to send to Raspberry Pi
//                    prepareForecastWeatherData(averageTemp, averageMinTempLong, averageMaxTempLong, averageRain, averageSnow)

                            // combine two sets of weather data
//                    weatherData = currentWeatherData + forecastWeatherData
//                    println("complete weather data: " + weatherData)

                        },
                        com.android.volley.Response.ErrorListener { println("******Not able to get forecast!") }) {}
                // Add the request to the RequestQueue.
                queue.add(stringRequest_tmr)

            }
        }
    }
}


fun getTomorrowDate() : String  {
    var calendar = Calendar.getInstance() // get current date
    calendar.add(Calendar.DATE, 1)
    val tomorrow: Date = calendar.time
    val sdf = SimpleDateFormat("YYYY-MM-dd hh:mm:ss")
    val tomorrowDate = sdf.format(tomorrow)
    println("Tomorrow: " + tomorrowDate)
    return tomorrowDate
}


fun calculateAverageDouble(numbers:ArrayList<Double>) : Double {

    var sum : Double = 0.0

    for (i in numbers) {
        sum += i
    }

    var average : Double = sum/numbers.size
    return average
}

fun convertTemperature  (tempKelvin:Double) : Long {

    var fahrenheitTemp : Double = (tempKelvin - 273.15) * (9/5) + 32
    val fahrenheitRounded = Math.round(fahrenheitTemp)
    return fahrenheitRounded
}

fun calculateAverage(numbers:ArrayList<Long>) : Long {

    var sum : Long = 0

    for (i in numbers) {
        sum += i
    }

    var average : Long = sum/numbers.size
    return average
}

fun checkRainForecast(weatherForecast: WeatherForecast, day: Int) : Double {
    var rain: Double

    try {
        rain = weatherForecast.list.get(day).rain.threehour
        println("rain forecast on day " + day + ": " + rain)
    } catch (e:Exception) {
        println("no rain on day " + day + "!!! ")
        rain = 0.0
    }

    return rain
}

fun checkRain(weather: WeatherResult) : Double? {
    var rain: Double?

    try {
        rain = weather.rain.onehour
        println("rain: " + rain)
    } catch (e:Exception) {
        println("no rain !!! ")
        rain = 0.0
    }
    return rain

}


class WeatherResult(val id: Int, val name: String, val cod: Int, val coord: Coordinates, val main: WeatherMain, val weather: Array<Weather>,val rain: Rain)
class WeatherForecast(val cod: String, val message: Int, val cnt: Int, val list: Array<WeatherDetails>, val city: City)
class City(val name: String, val coord: Coordinates, val country: String, val timezone: Int, val sunrise: Int, val sunset: Int)
class WeatherDetails(val dt: Int, val main: WeatherMain, val weather: Array<Weather>, val dt_txt: String,val rain: RainForecast)
class RainForecast( @SerializedName("3h") val threehour : Double)
class Rain( @SerializedName("3h") val threehour : Double?, @SerializedName("1h") val onehour : Double?)
class Coordinates(val lon: Double, val lat: Double)
class Weather(val id: Int, val main: String, val description: String, val icon: String)
class WeatherMain(val temp: Double, val pressure: Int, val humidity: Int, val temp_min: Double, val temp_max: Double)
class StepsGoal(val actualSteps: Int, val todayGoal: Double)
