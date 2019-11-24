package com.example.milestone4

import android.content.Context
import android.content.Intent
import java.text.SimpleDateFormat
import java.util.*
import com.google.gson.annotations.SerializedName
import kotlin.collections.ArrayList
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttMessage
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.view.*
import kotlin.math.round

class MainActivity : AppCompatActivity() {


    // I'm using lateinit for these widgets because I read that repeated calls to findViewById
    // are energy intensive
    lateinit var textView: TextView
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

    lateinit var queue: RequestQueue
    lateinit var gson: Gson
    lateinit var mostRecentWeatherResult: WeatherResult
    lateinit var WeatherforecastResult: WeatherForecast

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
        textView = this.findViewById(R.id.text)
        maxweather = this.findViewById(R.id.maxweather)
        forecastmaxweather = this.findViewById(R.id.forecastmaxweather)
        forecastminweather = this.findViewById(R.id.forecastminweather)
        forecastprecipitation = this.findViewById(R.id.forecastprecipitation)
        minweather = this.findViewById(R.id.minweather)
        precipitation=this.findViewById(R.id.precipitation)
        syncButton = this.findViewById(R.id.syncButton)
        retrieveButton = this.findViewById(R.id.retrieveButton)
        wifiButton = this.findViewById(R.id.wifiButton)
        imageView = this.findViewById(R.id.imageView)

        // when the user presses the syncbutton, this method will get called
        syncButton.setOnClickListener({ syncWithPi() })

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
                println(message)
                textView.text = message.toString()
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

    // this method just connects the paho mqtt client to the broker
    fun syncWithPi(){
        println("+++++++ Connecting...")
        mqttAndroidClient.connect()
    }

    fun switchwifi(){
        startActivity( Intent(Settings.ACTION_WIFI_SETTINGS));
    }

    fun requestWeather(){
        val url = StringBuilder("https://api.openweathermap.org/data/2.5/weather?q=austin&appid=237a2971d5e68747c681fc1e13bfe6d3").toString()
        val stringRequest = object : StringRequest(com.android.volley.Request.Method.GET, url,
                com.android.volley.Response.Listener<String> { response ->
                    //textView.text = response
                    mostRecentWeatherResult = gson.fromJson(response, WeatherResult::class.java)
                    var imageID = mostRecentWeatherResult.weather.get(0).icon
//                    textView.text = mostRecentWeatherResult.weather.get(0).description
                    textView.text = mostRecentWeatherResult.weather.get(0).description
                    maxweather.text = "The maximum tempature for today is ".plus(round(mostRecentWeatherResult.main.temp_max*1.8-459.67).toString())
                    minweather.text = "The minimum tempature for today is ".plus(round(mostRecentWeatherResult.main.temp_min*1.8-459.67).toString())
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
        val url_tmr = StringBuilder("https://api.openweathermap.org/data/2.5/forecast?q=austin&appid=237a2971d5e68747c681fc1e13bfe6d3").toString()
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
                        var date = weatherDetails.dt_txt.substring(0,10)


                        if (date.equals(tomorrowDate.substring(0,10))) { // only use data from tomorrow's forecasts

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
                        day = day+1
                    }

                    // compute averages of all lists
                    val averageTemp : Long = calculateAverage(temps)
                    val averageMinTemp : Long? = minTemps.min()
                    val averageMinTempLong : Long = averageMinTemp!!.toLong()
                    val averageMaxTemp : Long? = maxTemps.max()
                    val averageMaxTempLong : Long = averageMaxTemp!!.toLong()

                    val averageRain : Double = calculateAverageDouble(rains)


                    forecastmaxweather.text = "The maximum tempature for tomorrow is ".plus(averageMaxTemp).toString()
                    forecastminweather.text = "The minimum tempature for tomorrow is ".plus(averageMinTemp).toString()
                    forecastprecipitation.text = "The precipitation for tomorrow is ".plus(averageRain).toString()


                    weatherDescription= "{\"tmrlow\":" + averageMinTemp + ", \"tmrhigh\": " + averageMaxTemp +
                            ", \"tmrrain\":" + averageRain+ ", \"low\": " + round(mostRecentWeatherResult.main.temp_min*1.8-459.67) + ", \"high\": " + round(mostRecentWeatherResult.main.temp_max*1.8-459.67)+ ", \"rain\": " + checkRain(mostRecentWeatherResult) + "}"


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
