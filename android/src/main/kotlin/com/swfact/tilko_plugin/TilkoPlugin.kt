package com.swfact.tilko_plugin

import android.os.Build
import android.os.Environment
import android.util.Base64
import android.util.Log
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import microsoft.aspnet.signalr.client.hubs.HubConnection
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URLEncoder
import java.security.PrivateKey
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


const val ALGORITHM = "AES"
const val PADDING_MODE = "/CBC/PKCS7Padding"
const val RSA_ALGORITHM = "RSA/ECB/PKCS1Padding"

// 공동인증서 정보(파일경로, 이름, 유효기간) 자료구조
data class CertInfo(
        var filePath: String = "",
        var cn: String = "",
        var validDate: String = ""
)

// 암호화 & 복호화를 위한 메서드
fun byteArrayOfInts(vararg ints: Int) = ByteArray(ints.size) { pos -> ints[pos].toByte() }
fun String.hexStringToByteArray() = ByteArray(this.length / 2) {
  this.substring(
          it * 2,
          it * 2 + 2
  ).toInt(16).toByte()
}

private val HEX_CHARS = "0123456789ABCDEF".toCharArray()
fun ByteArray.toHex() : String{
  val result = StringBuffer()

  forEach {
    val octet = it.toInt()
    val firstIndex = (octet and 0xF0).ushr(4)
    val secondIndex = octet and 0x0F
    result.append(HEX_CHARS[firstIndex])
    result.append(HEX_CHARS[secondIndex])
  }

  return result.toString()
}


/** TilkoAppMigrationPlugin */
@RequiresApi(Build.VERSION_CODES.O)
class TilkoPlugin: FlutterPlugin, FlutterActivity(),  MethodCallHandler {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private lateinit var channel : MethodChannel

  private var rsa:RSA? = null
  private val server_url = "https://cert.tilko.net/"
  private var pem:String = ""
  private var hubConnection: HubConnection? = null
  private var certificateDigitCode: String = ""

  private var apihelper:APIHelper? = null

  val dateFormat = SimpleDateFormat("yyyy.MM.dd")

  // Plugin class 를 Flutter와 연결한다.
  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {

    // 메소드 채널 생성
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "tilko_plugin")
    // 메소드 채널에서 call되어 오는 메세지를 받기 위해 Handler를 Flutter에 등록.
    channel.setMethodCallHandler(this)

    rsa = RSA()
    rsa!!.generatorKey()

//    var aesPlainKey = apihelper!!.aesPlainKey
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {

    when (call.method) {

      /** 1. tilko_plugin.dart에서(TilkoPlugin 클래스에서) getPlatformVersion method가 call되면
       * 결과값으로 "Android"를 넘겨준다.*/
      "getPlatformVersion" -> {
        result.success("Android")
      }


      /** 2. 버튼을 눌렀을 때 8자리 인증코드를 넘겨준다.*/
      "getKey" -> {
        buttonClicked {
          if (it != null)
            result.success(it)
          else result.success("cert null@@")
        }
      }


      /** 3. 위와 같은 방식으로 method가 call되면 인증서 목록을 읽는 readCertificates()를 실행한 뒤
       *  공동인증서 정보를 넘겨준다.*/
      "getCertificate" -> {

        val thread = Thread {
          try {
            val certificateDatas = readCertificates()
            val certInfoList = arrayListOf<String>()

            certificateDatas.forEach {
              // 공동인증서 정보들 중 이름과 유효기간을 저장
              var certData = "${it.cn}^${it.validDate}^${it.filePath}"

              certInfoList.add(certData)
              //certsLayout.addView(tv)  // (layout.xml을 위한 코드)
            }
            // 저장공간 액세스 동작 코드를 permission_handler로 대체했다. (API Level 및 안드로이드 보안정책 관련 이슈)
            // Flutter app에서는 동작하는데 Flutter plugin에서는 동작안함. (플러그인에서는 MainActivity를 사용하지 않음)
            /*
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
              requestPermissions(
                arrayOf(
                  android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                  android.Manifest.permission.READ_EXTERNAL_STORAGE
                ), 101
              )
            }
            */
            rsa = RSA()
            rsa!!.generatorKey()

            result.success(certInfoList)
          } catch (e: java.lang.Exception) {
              e.printStackTrace()
            }
          }
          
        thread.start()
      }


      /** 4-1. <건강검진정보 조회> 원하는 인증서를 선택했을때, 조건에 맞는 API의 호출 결과를 반환한다.*/
      "callHealthCheckInfo" -> {
        val postUrl = call.argument<String>("postUrl")
        val apiKey = call.argument<String>("apiKey")
        val filePath = call.argument<String>("filePath")
        val certPass = call.argument<String>("certPass")

        val derPath = filePath + "/signCert.der"
        Log.d("certFile?", derPath)
        val keyPath = filePath + "/signPri.key"
        Log.d("keyFile?", keyPath)

        apihelper = APIHelper(apiKey)
        var aesPlainKey = apihelper!!.getAESPlainKey()

        val thread = Thread {
          try {
            var publicKeyFromServer = apihelper!!.getRSAPubKey().getPublicKey()
            val _aesCipherKey = Util.encodeByRSAPublicKey(aesPlainKey, publicKeyFromServer)

            val apiResult = apihelper!!.getHealthCheckInfo(_aesCipherKey, postUrl, derPath, keyPath, certPass) // 건강검진조회

            result.success(apiResult)
          } catch (e: java.lang.Exception) {
            e.printStackTrace()
          }
        }

        thread.start()

      }

      /** 4-2. <진료 및 투약정보 조회> 원하는 인증서를 선택했을때, 조건에 맞는 API의 호출 결과를 반환한다.*/
      "callMedicalTreatment" -> {
        val postUrl = call.argument<String>("postUrl")
        val apiKey = call.argument<String>("apiKey")
        val filePath = call.argument<String>("filePath")
        val certPass = call.argument<String>("certPass")

        val derPath = filePath + "/signCert.der"
        Log.d("certFile?", derPath)
        val keyPath = filePath + "/signPri.key"
        Log.d("keyFile?", keyPath)

        apihelper = APIHelper(apiKey)
        var aesPlainKey = apihelper!!.getAESPlainKey()

        val thread = Thread {
          try {
            var publicKeyFromServer = apihelper!!.getRSAPubKey().getPublicKey()
            val _aesCipherKey = Util.encodeByRSAPublicKey(aesPlainKey, publicKeyFromServer)

            val apiResult = apihelper!!.getMedicalTreatment(_aesCipherKey, postUrl, derPath, keyPath, certPass) // 진료 및 투약정보

            result.success(apiResult)
          } catch (e: java.lang.Exception) {
            e.printStackTrace()
          }
        }

        thread.start()

      }

      /** 4-3. <인증서등록> - 건강보험공단 인증서 등록 API */
      "callCertRegister" -> {
        val postUrl = call.argument<String>("postUrl")
        val apiKey = call.argument<String>("apiKey")
        val filePath = call.argument<String>("filePath")
        val identityNum = call.argument<String>("identityNum")
        val certPass = call.argument<String>("certPass")

        val derPath = filePath + "/signCert.der"
        Log.d("certFile?", derPath)
        val keyPath = filePath + "/signPri.key"
        Log.d("keyFile?", keyPath)

        apihelper = APIHelper(apiKey)
        var aesPlainKey = apihelper!!.getAESPlainKey()

        val thread = Thread {
          try {
            var publicKeyFromServer = apihelper!!.getRSAPubKey().getPublicKey()
            val _aesCipherKey = Util.encodeByRSAPublicKey(aesPlainKey, publicKeyFromServer)

            val apiResult = apihelper!!.getCertRegister(_aesCipherKey, postUrl, derPath, keyPath, identityNum, certPass) // 인증서등록

            result.success(apiResult)
          } catch (e: java.lang.Exception) {
            e.printStackTrace()
          }
        }

        thread.start()

      }

      "deleteCert" -> {
        val certIndex: Int = call.argument<Int>("index")!!.toInt()
        var isDeleted = false
        val targetCertificate = readCertificates()
        val certFilePathList = arrayListOf<String>()

        targetCertificate.forEach {
          // 공동인증서 정보들 중 파일경로를 저장
          var certData = "${it.filePath}"

          certFilePathList.add(certData)
        }
        if (File(Environment.getExternalStorageDirectory().toString() + "/NPKI").exists()) {
          File(certFilePathList.get(certIndex)).deleteRecursively()
//          for(i in certFilePathList) {
//            File(i).deleteRecursively()
//          }
          isDeleted = true
        }
//        val folder = File(Environment.getExternalStorageDirectory().toString() + "/NPKI")

//        if(File(Environment.getExternalStorageDirectory().toString() + "/NPKI").exists()) {
//          File(Environment.getExternalStorageDirectory().toString() + "/NPKI").deleteRecursively()
//          isDeleted = true
//        }

        result.success(isDeleted)
      }

      else -> {
        result.notImplemented()
      }

    }

  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }


  // 공동인증서 정보 불러오기()
  fun readCertificates() : ArrayList<CertInfo> {
    var certDatas = ""

    var certs = arrayListOf<CertInfo>()

    var arrCert = arrayListOf<String>()


    // 공동인증서 NPKI 폴더 체크
    val folder = File(Environment.getExternalStorageDirectory().toString() + "/NPKI")
    if (folder.exists()) {
      val children1 = folder.list()
      for (i in children1.indices) {
        val subFolder1 = File(folder, children1[i].toString() + "/USER")
        if (subFolder1.exists()) {
          val children2 = subFolder1.list()
          for (j in children2.indices) {
            val subFolder2 = File(subFolder1, children2[j])
            arrCert.add(subFolder2.absolutePath)

            // 파일명 소문자로 변환
            val subChildren = subFolder2.list()
            for (childIdx in subChildren.indices) {
              subChildren[childIdx] = subChildren[childIdx].toLowerCase()
            }
            val list: List<String> = subChildren.toList()
            if (!list.contains("signcert.der") || !list.contains("signpri.key")) {
              arrCert.removeAt(arrCert.size - 1)
            }
          }
        }
      }
    }
    for (i in arrCert.indices) {
      val info = CertInfo()
      info.filePath = arrCert[i]
      val derFile = File(arrCert[i], "signCert.der")
      try {
        val cf: CertificateFactory = CertificateFactory.getInstance("X.509")
        val cert: X509Certificate =
                cf.generateCertificate(FileInputStream(derFile)) as X509Certificate


        //cn (공동인증서 소유자 이름)
        val dn: String = cert.getSubjectDN().toString()
        //Log.wtf("DN: ", dn)

        val split = dn.split(",".toRegex()).toTypedArray()
        for (x in split) {
          if (x.contains("CN=")) {
            var cn = x.trim { it <= ' ' }.replace("CN=", "")
            //println("CN is $cn")
            cn = cn.replace("\\p{Punct}|\\p{Digit}|[A-Za-z]".toRegex(), "")
            info.cn = cn
          }
        }


        //valid date (공동인증서의 유효기간)
        val validFrom: String = dateFormat.format(cert.notBefore)
        val validTo: String = dateFormat.format(cert.notAfter)
        //println("Valid Date = $validFrom - $validTo")
        info.validDate = "$validFrom - $validTo"

      } catch (ex: java.lang.Exception) {
        ex.printStackTrace()
      }
      certs.add(info)
    }

    // parseCerts : 공동인증서 정보를 저장하기 위한 Helper Function.
    // certDatas = parseCerts(certs)
    return certs
  }


//  // parseCerts : 공동인증서 정보를 저장하기 위한 Helper Function.
//  fun parseCerts(certs:ArrayList<CertInfo>) : String {
//    //certsLayout.removeAllViewsInLayout() // (layout.xml을 위한 코드)
//    var certData = ""
//    certs.forEach {
//
//      Log.wtf("CERTIFICATE: ", it.toString())
//
//      // 공동인증서 정보들 중 이름과 유효기간을 저장
//      certData = "${it.cn} , ${it.validDate} , ${it.filePath}"
//      return certData
//
//      //certsLayout.addView(tv)  // (layout.xml을 위한 코드)
//    }
//
//    return certData
//  }

  // "인증서 가져오기" 버튼을 눌렀을 때 TilkoSign과 연결하여 8자리 인증코드 생성 및 인증 진행
  fun buttonClicked(completion: (String) -> Unit) {

    pem = Base64.encodeToString(rsa!!.publicKey.encoded, 0)

    //Log.wtf("BASE 64 Public Key", pem)

    //Log.wtf("Public Key After URL Encoding", URLEncoder.encode(pem, "UTF-8"))


    hubConnection = microsoft.aspnet.signalr.client.hubs.HubConnection(server_url)
    val mHub = hubConnection!!.createHubProxy("AuthHub")
    hubConnection!!.headers.put("client_type", "Mobile")
    hubConnection!!.headers.put("public_cert", URLEncoder.encode(pem, "UTF-8"))
    hubConnection!!.connected {
      //Log.d("CODE RECEIVED!", "CONNECTED@")

    }
    hubConnection!!.closed {

    }
    hubConnection!!.received {
      //Log.d("CODE RECEIVED#", "RECEIVED$")

      //Log.d("JSON%", it.toString())

      val obj = JSONObject(it.toString())

      when (obj.getString("M")) {
        "ShowCode" -> {
          val arr = obj.getJSONArray("A")
          //Log.wtf("NUMBER:", arr.getString(0))

          var code = arr.getString(0)

          runOnUiThread {
            certificateDigitCode = code // (For layout.xml)
            completion(code)
          }
        }

        "SaveCertificate" -> {

          val arr = obj.getJSONArray("A")

          val encryptedAesKey = arr.getString(0)
          val encryptedPublicKey = arr.getString(1)
          val encryptedPrivateKey = arr.getString(2)
          val subjectDN = arr.getString(3)
          val sessionId = arr.getString(4)

          Log.d("암호화된 AES 키: ", "$encryptedAesKey")
          Log.d("SubjectDN: ", "$subjectDN")
          Log.d("암호화된 공개키 HEX 정보: ", "$encryptedPublicKey")
          Log.d("암호화된 개인키 HEX 정보: ", "$encryptedPrivateKey")
          Log.d("SessionId: ", "$sessionId")

          val hexAesKeyByteArray = encryptedAesKey.hexStringToByteArray()
          val decryptedAesKey = decryptWithRSA(hexAesKeyByteArray, rsa!!.privateKey)!!

          Log.d("[HEADER]RSA로 암호화한 AES 키", "$decryptedAesKey")

          val iv = byteArrayOfInts(
                  123,
                  140,
                  56,
                  128,
                  22,
                  11,
                  170,
                  121,
                  33,
                  113,
                  73,
                  28,
                  208,
                  42,
                  247,
                  134
          )

          val decryptedPublicBytes = decryptWithAES(
                  decryptedAesKey,
                  iv,
                  encryptedPublicKey.hexStringToByteArray()
          )!!
          val decryptedPrivateBytes = decryptWithAES(
                  decryptedAesKey,
                  iv,
                  encryptedPrivateKey.hexStringToByteArray()
          )!!


          val e1 = String(decryptedPublicBytes).hexStringToByteArray()
          val e2 = String(decryptedPrivateBytes).hexStringToByteArray()

          var issuedBy = ""
          val dnList = subjectDN.split(",")
          dnList.forEach {

            Log.wtf("dn: ", it)

            val dn = it.split("=")
            if (dn[0] == "O") {
              issuedBy = dn[1]
              Log.wtf("Issued By:", issuedBy)
            }
          }

          if (issuedBy.equals("")) {
            return@received
          }

          val folderPath =
                  Environment.getExternalStorageDirectory().absolutePath + "/NPKI/${issuedBy}/USER/"

          val username = subjectDN

          val folder = File(folderPath + username)
          if (!folder.exists()) {
            folder.mkdirs()
          }

          val derPath = folderPath + username + "/signCert.der"
          val keyPath = folderPath + username + "/signPri.key"


          val derOut = FileOutputStream(derPath)
          derOut.write(e1)
          derOut.close()

          val keyOut = FileOutputStream(keyPath)
          keyOut.write(e2)
          keyOut.close()

          readCertificates()

        }
      }


    }

    hubConnection!!.error {
      Log.e("ERR", it.localizedMessage)
    }

    try {

      var awaitConnection = hubConnection!!.start()
      awaitConnection.get()

    } catch (e: Exception) {
      Log.e("SignalR Error", e.localizedMessage)
    }


  }


  // AES로 복호화
  private fun decryptWithAES(
          aesKey: ByteArray, aesIV: ByteArray,
          encryptedData: ByteArray
  ): ByteArray? {
    val skeySpec = SecretKeySpec(aesKey, ALGORITHM)
    val aesCipher = Cipher.getInstance(
            ALGORITHM + PADDING_MODE
    )

    aesCipher.init(
            Cipher.DECRYPT_MODE, skeySpec,
            IvParameterSpec(aesIV)
    )

    return aesCipher.doFinal(encryptedData)
  }

  // RSA로 복호화
  private fun decryptWithRSA(encryptedAesKey: ByteArray, privKey: PrivateKey): ByteArray? {
    val rsaCipher = Cipher.getInstance(RSA_ALGORITHM)
    rsaCipher.init(Cipher.DECRYPT_MODE, privKey)
    return rsaCipher.doFinal(encryptedAesKey)
  }


  override fun onDestroy() {
    super.onDestroy()
    hubConnection?.stop()
  }

}