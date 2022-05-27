import 'dart:async';
import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:permission_handler/permission_handler.dart';

class TilkoPlugin {
  // Method channel을 생성한다.
  static const MethodChannel _channel = const MethodChannel('tilko_plugin');

  // 8자리 인증코드를 문자열로 받아오기 위한 메소드 콜
  static Future<String> getKey() async {
    String platformVersion = await _channel.invokeMethod('getPlatformVersion');
    if (platformVersion.contains("Android")) {
      await requestPermission();
    }
    var code = await _channel.invokeMethod('getKey');
    return code;
  }

  // 공동인증서정보를 받아오기 위한 메소드 콜
  static Future<Map<String, List<String>>> getCertificate() async {
    String platformVersion = await _channel.invokeMethod('getPlatformVersion');
    if (platformVersion.contains("Android")) {
      await requestPermission();
    }

    var certificate = await _channel.invokeMethod('getCertificate');
    List certNameList = [];
    List certValidDateList = [];
    List certFilePathList = [];
    for (int i = 0; i < certificate.length; i++) {
      var temp = certificate[i].toString();
      var cert = temp.split("^");
      if (!certNameList.contains(cert[0])) certNameList.add(cert[0]);
      if (!certValidDateList.contains(cert[1])) certValidDateList.add(cert[1]);
      if (!certFilePathList.contains(cert[2])) certFilePathList.add(cert[2]);
    }

    Map<String, dynamic> dynamicMap = {
      'name': certNameList,
      'valid': certValidDateList,
      'file': certFilePathList,
    };
    Map<String, List<String>> typedMap = {};
    dynamicMap.forEach((key, value) {
      List<String> list = List.castFrom(value);
      typedMap[key] = list;
    });
    return typedMap;
  }

  static Future<void> requestPermission() async {
    var status = await Permission.storage.status;
    if (status.isDenied) {
      await Permission.storage.request();
    }
  }

  // 건강검진정보조회 API를 사용하기 위한 메소드 콜
  static Future<Map<String, dynamic>> callHealthCheckInfo(
      String apiKey, String filePath, String certPass) async {
    var jsonString = await _channel.invokeMethod('callHealthCheckInfo', {
      'postUrl': "api/v1.0/nhis/ggpab003m0105",
      'apiKey': apiKey,
      'filePath': filePath,
      'certPass': certPass
    });
    debugPrint(jsonString, wrapWidth: 1024);

    Map<String, dynamic> jsonData = jsonDecode(jsonString);
    print(
        'status : ${jsonData['Status']} \n message! : ${jsonData['Message']} \n number : ${jsonData['StatusSeq']}');
    return jsonData;
  }

  // 진료 및 투약정보 조회 API를 사용하기 위한 메소드 콜
  static Future<Map<String, dynamic>> callMedicalTreatment(
      String apiKey, String filePath, String certPass) async {
    var jsonString = await _channel.invokeMethod('callMedicalTreatment', {
      'postUrl': "api/v1.0/nhis/retrievetreatmentinjectioninformationperson",
      'apiKey': apiKey,
      'filePath': filePath,
      'certPass': certPass
    });
    debugPrint(jsonString, wrapWidth: 1024);

    Map<String, dynamic> jsonData = jsonDecode(jsonString);
    print(
        'status : ${jsonData['Status']} \n message! : ${jsonData['Message']} \n number : ${jsonData['StatusSeq']}');
    return jsonData;
  }

  // 인증서 등록 API
  static Future<Map<String, dynamic>> callCertRegister(String apiKey,
      String filePath, String identityNum, String certPass) async {
    var jsonString = await _channel.invokeMethod('callCertRegister', {
      'postUrl': "api/v1.0/nhis/register",
      'apiKey': apiKey,
      'filePath': filePath,
      'identityNum': identityNum,
      'certPass': certPass
    });
    debugPrint(jsonString, wrapWidth: 1024);

    Map<String, dynamic> jsonData = jsonDecode(jsonString);
    print(
        'status : ${jsonData['Status']} \n message! : ${jsonData['Message']} \n number : ${jsonData['StatusSeq']}');
    return jsonData;
  }

  static Future<bool> deleteCert(int index) async {
    var isDeleted = await _channel.invokeMethod('deleteCert', {
      'index': index,
    });
    if (isDeleted == true) {
      print("deleted@@@@@!");
    } else {
      print("not deleted@@@@@~");
    }
    return isDeleted;
  }
}
