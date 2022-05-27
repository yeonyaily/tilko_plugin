import Flutter
import UIKit

@available(iOS 13.0, *)
public class SwiftTilkoPlugin: NSObject, FlutterPlugin {
  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "tilko_plugin", binaryMessenger: registrar.messenger())
    let instance = SwiftTilkoPlugin()
    registrar.addMethodCallDelegate(instance, channel: channel)
  }

    let hubManager = HubManager()

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    if call.method == "getPlatformVersion" {
        result("iOS " + UIDevice.current.systemVersion)
    }else if call.method == "getKey" {
        hubManager.createKeypair()
        hubManager.connectToServer(){(info) in
            result(info)
        }
    }else if call.method == "getCertificate" {
        hubManager.list()
        var certificates : [String] = []
        for cert in hubManager.certificates {
            let temp = cert.cn + "^" + cert.validDate + "^" + cert.filePath
            certificates.append(temp)
        }
        result(certificates)
    }else if call.method == "callHealthCheckInfo" {
        var path = ""
        var certPass = ""
        guard let args = call.arguments else {
            return
        }
        if let myArgs = args as? [String: Any],
           let val = myArgs["postUrl"] as? String{
            hubManager.postUrl = "https://api.tilko.net/" + val
        }else {
            result(FlutterError(code: "-1", message: "InvalidArguments callAPI must be called with a filePath", details: nil))
        }
        if let myArgs = args as? [String: Any],
           let val = myArgs["apiKey"] as? String{
            hubManager.apiKey = val
        }else {
            result(FlutterError(code: "-2", message: "InvalidArguments callAPI must be called with a filePath", details: nil))
        }
        if let myArgs = args as? [String: Any],
           let val = myArgs["filePath"] as? String{
            path = val
        }else {
            result(FlutterError(code: "-3", message: "InvalidArguments callAPI must be called with a filePath", details: nil))
        }
        if let myArgs = args as? [String: Any],
           let val = myArgs["certPass"] as? String{
            certPass = val
        }else {
            result(FlutterError(code: "-4", message: "InvalidArguments callAPI must be called with a filePath", details: nil))        }
        hubManager.getPublicKey(){ _ in
            self.hubManager.healthCheckInfo(filePath: path, certPassword: certPass){ (info) in
                result(info)
            }
        }
    }else if call.method == "callMedicalTreatment" {
        var path = ""
        var certPass = ""
        guard let args = call.arguments else {
            return
        }
        if let myArgs = args as? [String: Any],
           let val = myArgs["postUrl"] as? String{
            hubManager.postUrl = "https://api.tilko.net/" + val
        }else {
            result(FlutterError(code: "-1", message: "InvalidArguments callAPI must be called with a filePath", details: nil))
        }
        if let myArgs = args as? [String: Any],
           let val = myArgs["apiKey"] as? String{
            hubManager.apiKey = val
        }else {
            result(FlutterError(code: "-2", message: "InvalidArguments callAPI must be called with a filePath", details: nil))
        }
        if let myArgs = args as? [String: Any],
           let val = myArgs["filePath"] as? String{
            path = val
        }else {
            result(FlutterError(code: "-3", message: "InvalidArguments callAPI must be called with a filePath", details: nil))
        }
        if let myArgs = args as? [String: Any],
           let val = myArgs["certPass"] as? String{
            certPass = val
        }else {
            result(FlutterError(code: "-4", message: "InvalidArguments callAPI must be called with a filePath", details: nil))        }
        hubManager.getPublicKey(){ _ in
            self.hubManager.medicalTreatment(filePath: path, certPassword: certPass){ (info) in
                result(info)
            }
        }
    }
  }
}
