package systems.carson.base

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.rsocket.Payload
import io.rsocket.util.DefaultPayload
import org.apache.commons.codec.binary.Base64
import java.util.*


const val PORT = 48626

val gson : Gson = GsonBuilder()
    .setLenient()
    .create()


class DataBlob(
    val intent :String,
    val data :String)

class RequestDataBlob(
    val intent : Request.Response,
    val clientID :String,
    val data :ReceivedData,
    val isVerified :Boolean)

class StreamDataBlob(
    val intent :Request.Stream,
    val clientID :String,
    val data :ReceivedData,
    val isVerified :Boolean)

//class FireDataBlob(
//    val intent :Request.Fire,
//    val clientID :String,
//    val data :ReceivedData,
//    val isVerified :Boolean)

class ReceivedData(val data :String){
    fun base64() :ByteArray{
        return Base64.decodeBase64(data)
    }
    inline fun <reified T :Sendable> fromJson() :T{
        return gson.fromJson(data,T::class.java)
    }
}

class Status(
    val failed :Boolean = false,
    val errorMessage :String = "",
    val extraData :String = "") :Sendable

class ActionUpdate(val actions :List<Action>, val lasthash :String, val difficulty :Long):Sendable


class Message(
    val encryptedData: EncryptedString,
    val clientID: String,
    val signature :String//base64 of Signature
)



interface Sendable{
    fun send() :String{
        return gson.toJson(this)
    }
    fun toPayload(): Payload = DefaultPayload.create(this.send())
    companion object
}

inline fun <reified T :Sendable> Sendable.Companion.deserialize(string :String):T{
    return gson.fromJson(string.trimEnd { it == (0).toChar() },T::class.java)
}

class SendableInteger(val value: Int):Sendable
class SendableString(val value :String):Sendable
class SendableBoolean(val value :Boolean):Sendable
object NoData :Sendable{
    override fun send() = ""
}

//class SendString(val value :String) :Sendable{
//    override fun send(): String {
//        return value
//    }
//}

